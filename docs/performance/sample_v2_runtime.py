#!/usr/bin/env python3
"""采集实际隔离进程资源与任务年龄，原始JSONL不进入git。"""
import argparse
import json
import subprocess
from pathlib import Path
import time
from seed_v2_testdata import mysql


def sample(seed):
    backlog=0;oldest=0;connections={};row={'observedAt':time.time()}
    for db in seed['databases']:
        values=mysql(db,"SELECT COUNT(*),COALESCE(MAX(TIMESTAMPDIFF(MICROSECOND,created_at,UTC_TIMESTAMP(6)))/1000000,0) FROM outbox WHERE state IN ('PENDING','PROCESSING')").split('\t')
        backlog+=int(values[0]);oldest=max(oldest,float(values[1]))
        connections[db]=int(mysql(db,"SELECT COUNT(*) FROM information_schema.processlist WHERE DB="+"'"+db+"'"))
    physical=int(mysql(seed['databases'][0],"SHOW GLOBAL STATUS LIKE 'Threads_connected'").split('\t')[1])
    row.update(backlog=backlog,oldestAgeSeconds=oldest,physicalDatabaseConnections=physical,selectedLogicalConnections=sum(connections.values()),logicalConnections=connections)
    order_backlog=0;order_age=0;retry=0
    for index,db in enumerate(seed['databases']):
        retry+=int(mysql(db,"SELECT COUNT(*) FROM retry_job WHERE state IN ('PENDING','PROCESSING')"))
        if index==0:
            value=mysql(db,"SELECT COUNT(*),COALESCE(MAX(TIMESTAMPDIFF(MICROSECOND,last_progress_at,UTC_TIMESTAMP(6)))/1000000,0) FROM payment_order WHERE state<>'DELIVERED'").split('\t')
            order_backlog+=int(value[0]);order_age=max(order_age,float(value[1]))
        else:
            for suffix in range(4):
                value=mysql(db,f"SELECT COUNT(*),COALESCE(MAX(TIMESTAMPDIFF(MICROSECOND,s.recorded_at,UTC_TIMESTAMP(6)))/1000000,0) FROM v2_draw_order_{suffix:03d} o JOIN v2_raffle_stage s ON s.order_id=o.order_id AND s.stage='ACCEPTED' WHERE o.state NOT IN ('DELIVERED','CANCELLED','REJECTED')").split('\t')
                order_backlog+=int(value[0]);order_age=max(order_age,float(value[1]))
    queues=subprocess.run(['docker','exec','bromark-v2-rabbit','rabbitmqctl','-q','list_queues','name','messages_ready','messages_unacknowledged'],text=True,capture_output=True,check=True).stdout
    prefix=seed.get('messagingPrefix','bromark.it.phase6.'+seed['runId'])
    matching=[line.split('\t') for line in queues.splitlines() if line.startswith(prefix+'.')]
    if len(matching)!=2:raise ValueError('both dedicated queues must be observed')
    row.update(outboxBacklog=backlog,retryBacklog=retry,unfinishedOrders=order_backlog,
               backlog=order_backlog,oldestOrderAgeSeconds=order_age,
               queueReady=sum(int(v[1]) for v in matching),queueUnacked=sum(int(v[2]) for v in matching),
               backlogScopes=['ORDER','OUTBOX','RETRY','BROKER'])
    names=['bromark-phase6-app-1','bromark-phase6-app-2','bromark-v2-mysql','bromark-v2-rabbit']
    stats=subprocess.run(['docker','stats','--no-stream','--format','{{json .}}']+names,text=True,capture_output=True)
    row['containers']=[json.loads(line) for line in stats.stdout.splitlines() if line.strip()]
    row['resourceSampleComplete']=stats.returncode==0 and len(row['containers'])==len(names)
    return row

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seed',required=True);p.add_argument('--out',required=True)
    p.add_argument('--seconds',type=float,default=900);p.add_argument('--interval',type=float,default=5);a=p.parse_args()
    seed=json.loads(Path(a.seed).read_text());end=time.monotonic()+a.seconds
    with Path(a.out).open('a') as f:
        while time.monotonic()<end:
            f.write(json.dumps(sample(seed))+'\n');f.flush();time.sleep(a.interval)
