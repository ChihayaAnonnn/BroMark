#!/usr/bin/env python3
"""Task8 专属只读采样；所有资源必须由 fault harness 传入，不选择历史容器。"""
import json
import subprocess
import time
import urllib.request
from pathlib import Path
from seed_v2_testdata import mysql, sql


def parse_json_rows(raw):
    return [json.loads(line) for line in raw.splitlines() if line]


def backlog(rows):
    pending = [r for r in rows if r['state'] in ('PENDING', 'PROCESSING')]
    ages = [float(r['age']) for r in pending if r['age'] is not None]
    return {'valid': True, 'count': sum(int(r['count']) for r in pending),
            'oldestAgeSeconds': max(ages) if ages else None, 'byTypeState': rows}


def aligned_window(before, after):
    seconds = after['observedAt'] - before['observedAt']
    if seconds <= 0 or before['phase'] != after['phase']:
        return None
    old = {r['id'] for r in before['orders']}
    new = {r['id'] for r in after['orders']}
    old_done = {r['id'] for r in before['orders'] if r['state'] == 'DELIVERED'}
    new_done = {r['id'] for r in after['orders'] if r['state'] == 'DELIVERED'}
    return {'seconds': seconds, 'acceptedIds': sorted(new - old), 'completedIds': sorted(new_done - old_done),
            'acceptedPerSecond': len(new - old) / seconds, 'completedPerSecond': len(new_done - old_done) / seconds,
            'openBefore': before['domains']['ORDER']['count'], 'openAfter': after['domains']['ORDER']['count'],
            'samplingSkewSeconds': before['elapsedSeconds'] + after['elapsedSeconds']}


def validate_continuous_arrivals(records, start, end):
    starts = sorted(record['startedAt'] for record in records if start <= record['startedAt'] <= end)
    if end <= start or len(starts) < 10:
        raise RuntimeError('insufficient continuous-arrival window')
    points = [start] + starts + [end]
    maximum_gap = max(b - a for a, b in zip(points, points[1:]))
    actual_rate = len(starts) / (end - start)
    if maximum_gap > 3 or actual_rate < 0.8:
        raise RuntimeError('continuous 1rps arrival contract violated: maxGap=%.3fs actualRate=%.3f/s' % (maximum_gap, actual_rate))
    return {'maximumStartGapSeconds': maximum_gap, 'attemptedPerSecond': actual_rate,
            'windowSeconds': end - start, 'attemptCount': len(starts), 'requiredMaxGapSeconds': 3,
            'requiredMinAttemptedPerSecond': 0.8}


def validate_final_sample(sample):
    broker = sample['domains']['BROKER']
    if not broker['valid'] or broker['count'] != 0:
        raise RuntimeError('final owned broker unavailable or queues not drained')
    if not sample['orders'] or any(row['state'] != 'DELIVERED' for row in sample['orders']):
        raise RuntimeError('nonzero real accepted/delivered population required')
    if sample['domains']['ORDER']['count']:
        raise RuntimeError('business orders remain unfinished')


class Sampler:
    def __init__(self, harness, seed, apps, rabbit_id, owned, prefix, directory):
        self.h = harness
        self.seed = seed
        self.apps = apps
        self.rabbit_id = rabbit_id
        self.owned = tuple(owned)
        self.prefix = prefix
        self.directory = Path(directory)
        self.directory.mkdir()
        self.h.validate_seed_and_names(seed)
        if prefix != 'bromark.it.task8.' + seed['runId'] or rabbit_id not in self.owned:
            raise ValueError('sampler requires fresh owned broker/run')
        self.verify(self.h.RABBIT, rabbit_id)
        for app in apps:
            if app['id'] not in self.owned or app['port'] not in (19871, 19872):
                raise ValueError('sampler app not owned')
            self.verify(app['name'], app['id'])

    def verify(self, name, cid):
        state = self.h.inspect(name)
        if state['Id'] != cid or self.h.NETWORK not in state['NetworkSettings']['Networks']:
            raise ValueError('sampler resource identity changed')
        return state

    def query(self, folder, name, db, statement):
        started = time.time()
        raw = mysql(db, statement)
        (folder / (name + '.sql')).write_text(statement + '\n')
        (folder / (name + '.jsonl')).write_text(raw + '\n')
        return {'startedAt': started, 'endedAt': time.time(), 'rows': parse_json_rows(raw)}

    def broker(self, folder):
        state = self.verify(self.h.RABBIT, self.rabbit_id)
        result = {'id': self.rabbit_id, 'valid': False, 'count': None, 'oldestAgeSeconds': None,
                  'oldestAgeMissingReason': 'Rabbit list_queues has no exact oldest-message timestamp'}
        if not state['State']['Running']:
            return dict(result, missingReason='owned Rabbit stopped')
        try:
            command = ['docker', 'exec', '--user', 'rabbitmq', self.rabbit_id, 'rabbitmqctl', '-q',
                       'list_queues', 'name', 'messages_ready', 'messages_unacknowledged']
            reply = subprocess.run(command, text=True, capture_output=True, timeout=8)
            (folder / 'rabbit-queues.txt').write_text(reply.stdout + reply.stderr)
            if reply.returncode:
                return dict(result, missingReason='owned Rabbit CLI not ready', exitCode=reply.returncode)
            rows = []
            excluded = []
            legacy = {'activity_sku_stock_zero', 'credit_adjust_success', 'bromark.it.send_rebate', 'bromark.it.send_award'}
            for line in reply.stdout.splitlines():
                if line == 'name\tmessages_ready\tmessages_unacknowledged':
                    continue
                cols = line.split('\t')
                if len(cols) == 3 and cols[0] in legacy:
                    if int(cols[1]) != 0 or int(cols[2]) != 0:
                        raise ValueError('unexpected nonempty legacy queue in fresh test broker')
                    excluded.append({'queue': cols[0], 'ready': 0, 'unacked': 0,
                                     'reason': 'startup legacy declaration outside target V2 prefix'})
                    continue
                if len(cols) != 3 or cols[0] not in {self.prefix + '.workflow', self.prefix + '.notifications'}:
                    raise ValueError('unexpected queue or Rabbit CLI output')
                rows.append({'queue': cols[0], 'ready': int(cols[1]), 'unacked': int(cols[2])})
            if len(rows) != 2:
                raise ValueError('missing owned queues')
            return dict(result, valid=True, count=sum(r['ready'] + r['unacked'] for r in rows), queues=rows, excludedQueues=excluded)
        except subprocess.TimeoutExpired:
            return dict(result, missingReason='owned Rabbit CLI timeout')

    def sample(self, phase, sequence):
        started = time.time()
        monotonic_started = time.monotonic_ns()
        folder = self.directory / ('%04d' % sequence)
        folder.mkdir()
        task_rows = {'OUTBOX': [], 'RETRY': []}
        orders = []
        query_windows = []
        for index, db in enumerate(self.seed['databases']):
            for domain, table in (('OUTBOX', 'outbox'), ('RETRY', 'retry_job')):
                query = ("SELECT JSON_OBJECT('database',DATABASE(),'type',event_type,'state',state,'count',COUNT(*),"
                         "'age',TIMESTAMPDIFF(MICROSECOND,MIN(created_at),UTC_TIMESTAMP(6))/1000000.0) "
                         "FROM " + table + ' GROUP BY event_type,state')
                result = self.query(folder, str(index) + '-' + table, db, query)
                task_rows[domain].extend(result.pop('rows'));query_windows.append(result)
            if index:
                parts = ["SELECT JSON_OBJECT('id',o.order_id,'state',o.state,'user',o.user_id,"
                         "'acceptedAt',UNIX_TIMESTAMP(s.recorded_at),'deliveredAt',UNIX_TIMESTAMP(o.delivered_at),"
                         "'age',TIMESTAMPDIFF(MICROSECOND,s.recorded_at,UTC_TIMESTAMP(6))/1000000.0) "
                         "FROM v2_draw_order_%03d o JOIN v2_raffle_stage s ON s.order_id=o.order_id "
                         "AND s.stage='ACCEPTED'" % suffix for suffix in range(4)]
                result = self.query(folder, str(index) + '-orders', db, ' UNION ALL '.join(parts))
                orders.extend(result.pop('rows'));query_windows.append(result)
        opened = [r for r in orders if r['state'] not in ('DELIVERED', 'CANCELLED', 'REJECTED')]
        domains = {key: backlog(rows) for key, rows in task_rows.items()}
        domains['ORDER'] = {'valid': True, 'count': len(opened),
                            'oldestAgeSeconds': max((float(r['age']) for r in opened), default=None),
                            'source': 'fresh run all draw rows joined original ACCEPTED stage'}
        domains['BROKER'] = self.broker(folder)
        apps = []
        for app in self.apps:
            self.verify(app['name'], app['id'])
            reply = subprocess.run(['docker', 'exec', app['id'], 'cat', '/sys/fs/cgroup/cpu.stat'],
                                   text=True, capture_output=True, timeout=5, check=True)
            (folder / (app['name'] + '-cpu.stat')).write_text(reply.stdout)
            cpu = {line.split()[0]: int(line.split()[1]) for line in reply.stdout.splitlines()}
            number = app['port'] - 19870
            source = self.directory.parent.parent / ('app-%d' % number) / 'metrics.prom'
            if not source.is_file() or time.time() - source.stat().st_mtime > 10:
                raise RuntimeError('test-only local registry scrape missing or stale')
            raw = source.read_text()
            (folder / (app['name'] + '-prometheus.txt')).write_text(raw)
            apps.append({'id': app['id'], 'cpu': cpu, 'prometheusAvailable': True})
        names = ','.join(sql(db) for db in self.seed['databases'])
        status = self.query(folder, 'mysql-status', None,
            "SELECT JSON_OBJECT('name',VARIABLE_NAME,'value',VARIABLE_VALUE) FROM performance_schema.global_status "
            "WHERE VARIABLE_NAME IN ('Innodb_row_lock_waits','Innodb_row_lock_time','Questions','Com_select','Com_insert','Com_update','Com_delete')")
        digest = self.query(folder, 'mysql-digest', None,
            "SELECT JSON_OBJECT('schema',SCHEMA_NAME,'digest',DIGEST,'text',DIGEST_TEXT,'count',COUNT_STAR,'timerPicoseconds',SUM_TIMER_WAIT) "
            "FROM performance_schema.events_statements_summary_by_digest WHERE SCHEMA_NAME IN (" + names + ')')
        server = self.query(folder, 'mysql-clock', None,
            "SELECT JSON_OBJECT('uuid',@@server_uuid,'utc',UTC_TIMESTAMP(6),'epoch',UNIX_TIMESTAMP(UTC_TIMESTAMP(6)))")
        digest['available'] = bool(digest['rows'])
        if not digest['available']:
            digest['missingReason'] = 'no target-schema digest rows; unavailable or empty, not zero SQL'
        expected = {'Innodb_row_lock_waits', 'Innodb_row_lock_time', 'Questions', 'Com_select',
                    'Com_insert', 'Com_update', 'Com_delete'}
        status['missingCounters'] = sorted(expected - {row['name'] for row in status['rows']})
        status['available'] = not status['missingCounters']
        if status['missingCounters']:
            status['missingReason'] = 'server global_status does not expose these counters; present raw rows remain usable'
        ended = time.time()
        result = {'runId': self.seed['runId'], 'sequence': sequence, 'phase': phase, 'startedAt': started,
                  'observedAt': ended, 'elapsedSeconds': ended - started,
                  'monotonicStartedNs': monotonic_started, 'monotonicEndedNs': time.monotonic_ns(), 'domains': domains, 'orders': orders,
                  'apps': apps, 'queryWindows': query_windows, 'mysqlGlobal': status, 'mysqlDigest': digest, 'mysqlClock': server,
                  'globalScope': 'physical MySQL instance; includes sampler and other activity, not request attribution'}
        self.h.write_json(folder / 'sample.json', result)
        return result
