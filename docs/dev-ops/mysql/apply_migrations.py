#!/usr/bin/env python3
"""显式迁移；凭据从 MYSQL_PWD 读取，生产环境只输出清单。需要 mysql CLI。"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).parent / 'migrations'
DATABASES = {'control': ['router_control'], 'user': ['router_one', 'router_two']}


def mysql(database, sql):
    cmd = ['mysql', '--batch', '--skip-column-names', '-h', os.environ.get('V2_MYSQL_HOST', '127.0.0.1'),
           '-P', os.environ.get('V2_MYSQL_PORT', '13316'), '-u', 'root', database]
    return subprocess.run(cmd, input=sql, text=True, capture_output=True, check=True).stdout.strip()


def apply(env, through, dry_run, mode):
    entries = json.loads((ROOT / 'manifest.json').read_text())['scripts']
    for item in entries:
        if item['file'][:3] > through:
            continue
        source = (ROOT / item['file']).read_text()
        if hashlib.sha256(source.encode()).hexdigest() != item['sha256']:
            raise RuntimeError('checksum mismatch: ' + item['file'])
        for database in DATABASES[item['scope']]:
            for suffix in range(item['shards']):
                statements = [s.strip() for s in source.replace('{{suffix}}', f'{suffix:03d}').split(';') if s.strip()]
                identity = f"{mode}:{item['file']}:{suffix:03d}"
                print(json.dumps({'database': database, 'script': identity, 'sha256': item['sha256'], 'execute': not dry_run}))
                if dry_run:
                    continue
                if env != 'v2-it' or os.environ.get('V2_MIGRATION_ALLOW') != 'isolated-router-test-only':
                    raise RuntimeError('actual execution requires v2-it and isolated test allowlist')
                mysql(database, 'CREATE TABLE IF NOT EXISTS schema_migration(script varchar(255) PRIMARY KEY,checksum char(64) NOT NULL,step int NOT NULL) ENGINE=InnoDB;')
                old = mysql(database, f"SELECT CONCAT(checksum,':',step) FROM schema_migration WHERE script='{identity}'")
                step = 0
                if old:
                    checksum, step = old.split(':')
                    if checksum != item['sha256']:
                        raise RuntimeError('applied checksum mismatch')
                    step = int(step)
                if mode == 'upgrade' and 'upgrade_fields' in item:
                    fields = json.loads((ROOT / item['upgrade_fields']).read_text())
                    additions = []
                    for table, columns in fields.items():
                        existing = set(mysql(database, f"SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='{table}'").splitlines())
                        if not existing:
                            raise RuntimeError('upgrade requires existing table: ' + table)
                        for column, kind in columns.items():
                            if column not in existing:
                                additions.append(f'ALTER TABLE {table} ADD COLUMN {column} {kind} NULL')
                    # Check schema on every resume; DDL may commit before progress is recorded.
                    for statement in additions:
                        mysql(database, statement)
                    statements = [s for s in statements if not any(re.match(r'CREATE TABLE IF NOT EXISTS '+t+r'\s', s) for t in fields)]
                    evidence = {}
                    for table, columns in fields.items():
                        key = 'user_id' if table == 'user_credit_account' else 'user_id,activity_id'
                        if table.endswith('_day') or table.endswith('_month'):
                            continue  # legacy period key differs; phase6 reconciles period identities
                        evidence[table] = {'duplicate_groups': mysql(database, f'SELECT COUNT(*) FROM (SELECT {key} FROM {table} GROUP BY {key} HAVING COUNT(*)>1) d'),
                                           'unaudited_rows': mysql(database, f'SELECT COUNT(*) FROM {table} WHERE audit_source IS NULL')}
                    if 'user_credit_account' in fields:
                        evidence['opening_conflicts'] = mysql(database, 'SELECT COUNT(*) FROM user_credit_account WHERE opening_balance IS NOT NULL AND opening_balance<>available_amount')
                    print(json.dumps({'cutover_allowed': False, 'precheck': evidence}))
                for n, statement in enumerate(statements, 1):
                    if n <= step:
                        continue
                    # New objects use IF NOT EXISTS; interrupted step can be safely inspected/reentered.
                    mysql(database, statement)
                    mysql(database, f"INSERT INTO schema_migration VALUES('{identity}','{item['sha256']}',{n}) ON DUPLICATE KEY UPDATE step={n}")


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--env', required=True)
    parser.add_argument('--through', default='001')
    parser.add_argument('--dry-run', action='store_true')
    parser.add_argument('--mode', choices=['fresh', 'upgrade'], default='fresh')
    args = parser.parse_args()
    if not args.dry_run and args.env != 'v2-it':
        parser.error('production execution prohibited; use --dry-run')
    apply(args.env, args.through, args.dry_run, args.mode)
