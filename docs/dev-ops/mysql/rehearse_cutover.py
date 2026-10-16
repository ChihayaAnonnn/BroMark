#!/usr/bin/env python3
"""隔离存量演练。只接受已审计的规范化目标行；默认 dry-run，不读取原始用户数据。"""
import argparse
from decimal import Decimal, InvalidOperation
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).parent / 'migrations'
KEYS = {'user_credit_account': ('user_id',),
        'raffle_activity_account': ('user_id','activity_id'),
        'raffle_activity_account_day': ('user_id','activity_id','business_date'),
        'raffle_activity_account_month': ('user_id','activity_id','business_month')}


def key_for(table):
    if table in KEYS: return KEYS[table]
    families = {'user_credit_ledger':('user_id','biz_type','biz_id'),
                'user_quota_ledger':('user_id','activity_id','biz_type','biz_id','bucket_type','bucket_key'),
                'sign_in_order':('user_id','business_date'),
                'sign_in_reward':('sign_in_order_id','reward_item_id'),
                'user_request_receipt':('user_id','operation','request_id'),
                'inbox':('event_id',)}
    for family, keys in families.items():
        if re.fullmatch(family+r'_00[0-3]',table): return keys
    raise ValueError('unsupported import table: '+table)


def digest(data):
    return hashlib.sha256(json.dumps(data,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()).hexdigest()


def execution_allowed(env, execute, database):
    return env=='v2-it' and execute and bool(re.fullmatch(r'router_cutover_it_[a-z0-9_]{1,32}',database))


def precheck(data):
    errors=[]
    if not data.get('auditor') or not re.fullmatch(r'[a-zA-Z0-9_-]{1,64}',data.get('run_id','')):
        errors.append('auditor/run_id required')
    if data.get('stopped') is not True or data.get('drained') is not True:
        errors.append('global old writes and workers must be stopped and drained')
    if not data.get('tables') or 'user_credit_account' not in data['tables']:
        errors.append('audited accounts required')
    for table,rows in data.get('tables',{}).items():
        try: keys=key_for(table)
        except ValueError as e: errors.append(str(e)); continue
        seen=set()
        for row in rows:
            identity=tuple(str(row.get(k,'')) for k in keys)
            if '' in identity or identity in seen: errors.append('null/duplicate identity: '+table)
            seen.add(identity)
            if any(v is None for v in row.values()): errors.append('unaudited null: '+table)
            if table in KEYS and not row.get('audit_source'): errors.append('missing audit_source: '+table)
            for field in ('balance','opening_balance','remaining','opening_quota','total_remaining',
                          'day_limit','month_limit','effective_draw_count','opening_total',
                          'opening_day_limit','opening_month_limit','opening_effective_count','quota_limit'):
                if field in row:
                    try:
                        amount=Decimal(str(row[field]))
                        if not amount.is_finite() or amount<0 or amount>Decimal('9999999999999999.99'):
                            raise ValueError()
                        if field in ('balance','opening_balance') and amount.as_tuple().exponent < -2:
                            raise ValueError()
                    except (InvalidOperation,ValueError): errors.append('invalid amount: '+table+'.'+field)
    seen=set()
    for row in data.get('identities',[]):
        identity=(row.get('entry'),row.get('legacy_id'))
        if not all(identity) or identity in seen or not row.get('evidence') or not row.get('biz_id') or not re.fullmatch(r'[A-Z_]{1,32}',row.get('biz_type','')) or not re.fullmatch(r'[a-f0-9]{64}',row.get('request_hash','')):
            errors.append('ambiguous legacy identity')
        seen.add(identity)
    if data.get('unresolved'): errors.append('unresolved legacy effects/messages')
    return {'cutover_allowed':not errors,'errors':errors,'input_sha256':digest(data),
            'rows':{t:len(r) for t,r in data.get('tables',{}).items()}}


def sql_value(value):
    if isinstance(value,(dict,list)): value=json.dumps(value,sort_keys=True,separators=(',',':'))
    # Hex bytes are independent of SQL string escaping and NO_BACKSLASH_ESCAPES.
    return "CONVERT(X'"+str(value).encode().hex()+"' USING utf8mb4)"


def mysql(database,sql):
    if not execution_allowed('v2-it',True,database): raise ValueError('isolated database required')
    container=os.environ.get('V2_MYSQL_CONTAINER','bromark-v2-mysql')
    if container!='bromark-v2-mysql': raise ValueError('isolated container allowlist')
    if not os.environ.get('V2_MYSQL_PASSWORD'): raise ValueError('password required')
    return subprocess.run(['docker','exec','-i','-e','MYSQL_PWD='+os.environ['V2_MYSQL_PASSWORD'],container,
                           'mysql','--batch','--skip-column-names','-uroot',database],
                          input=sql,text=True,capture_output=True,check=True).stdout.strip()


def table_digest(database,table,rows):
    if not rows: return {'count':int(mysql(database,f'SELECT COUNT(*) FROM `{table}`')), 'matches':True}
    columns=sorted(rows[0])
    for row in rows:
        if sorted(row)!=columns: raise ValueError('heterogeneous import columns')
    actual=[]
    # HEX safely transports tabs/newlines and exact decimals through mysql batch output.
    for line in mysql(database,'SELECT '+','.join(f'HEX(CAST(`{c}` AS CHAR))' for c in columns)+f' FROM `{table}`').splitlines():
        actual.append(tuple(bytes.fromhex(v).decode() for v in line.split('\t')))
    def norm(v):
        return json.dumps(v,sort_keys=True,separators=(',',':')) if isinstance(v,(dict,list)) else str(v)
    expected=[tuple(norm(r[c]) for c in columns) for r in rows]
    # MySQL JSON formatting is normalized rather than compared as whitespace.
    def canonical(values):
        result=[]
        for values_row in values:
            row=[]
            for v in values_row:
                try: row.append(json.dumps(json.loads(v),sort_keys=True,separators=(',',':')))
                except (ValueError,TypeError): row.append(v)
            result.append(tuple(row))
        return sorted(result)
    return {'count':len(actual),'expected':len(rows),'matches':canonical(actual)==canonical(expected),
            'checksum':digest(canonical(actual))}


def run(data,database,phase,execute=False,fail_after=None):
    result=precheck(data); result.update(phase=phase,execute=execute,database=database)
    if not result['cutover_allowed'] or not execute or phase=='precheck': return result
    if not execution_allowed('v2-it',execute,database): raise ValueError('isolated database required')
    # Bootstrap additive v2 tables, before tightening. Each statement reentrant; never DROP.
    for file in ('001_asset_additive.sql','001_outbox_base.sql','002_signin_exchange.sql','003_messaging_recovery.sql','003_recovery_guard.sql'):
        source=(ROOT/file).read_text()
        for suffix in range(4 if '{{suffix}}' in source else 1):
            for s in source.replace('{{suffix}}',f'{suffix:03d}').split(';'):
                if s.strip(): mysql(database,s)
    ddl=[s for s in (ROOT/'006_cutover_constraints.sql').read_text().split(';') if s.strip()]
    for s in ddl[:2]: mysql(database,s)
    rid=sql_value(data['run_id']); sha=result['input_sha256']
    old=mysql(database,f'SELECT input_sha256 FROM cutover_audit WHERE run_id={rid}')
    if old and old!=sha: raise ValueError('resume input checksum changed')
    if not old:
        mysql(database,f'INSERT INTO cutover_audit VALUES({rid},{sql_value(sha)},{sql_value(data["auditor"])},\'PREPARE\',0,JSON_OBJECT())')
    if phase=='prepare':
        # Single import transaction. Partial DDL can persist; partial imported assets cannot.
        statements=[]
        progress=int(mysql(database,f'SELECT progress FROM cutover_audit WHERE run_id={rid}'))
        if progress==0:
            for table,rows in data['tables'].items():
                if int(mysql(database,f'SELECT COUNT(*) FROM `{table}`')): raise ValueError('target must be empty before first import: '+table)
                for row in rows:
                    if not all(re.fullmatch(r'[a-z_]+',c) for c in row): raise ValueError('invalid column')
                    statements.append(f'INSERT INTO `{table}` ('+','.join('`'+c+'`' for c in row)+') VALUES ('+','.join(sql_value(v) for v in row.values())+')')
            for row in data.get('identities',[]):
                columns=('entry','legacy_id','biz_type','biz_id','request_hash','evidence')
                statements.append('INSERT INTO cutover_identity VALUES ('+','.join(sql_value(row[c]) for c in columns)+')')
            statements.append(f'UPDATE cutover_audit SET progress=1 WHERE run_id={rid}')
            mysql(database,'START TRANSACTION;'+ ';'.join(statements)+';COMMIT;')
            progress=1
        for n,s in enumerate(ddl[2:],2):
            if n<=progress: continue
            mysql(database,s)
            if fail_after==n: raise RuntimeError('injected interruption after DDL commit before progress')
            mysql(database,f'UPDATE cutover_audit SET progress={n} WHERE run_id={rid}')
    result['tables']={t:table_digest(database,t,r) for t,r in data['tables'].items()}
    result['identity_count']=int(mysql(database,'SELECT COUNT(*) FROM cutover_identity'))
    result['cutover_allowed']=all(r['matches'] and r['count']==r.get('expected',0) for r in result['tables'].values()) and result['identity_count']==len(data.get('identities',[]))
    result['progress']=int(mysql(database,f'SELECT progress FROM cutover_audit WHERE run_id={rid}'))
    result['cutover_allowed'] &= result['progress']==len(ddl)-1
    mysql(database,f'UPDATE cutover_audit SET evidence={sql_value(result)},state={sql_value("VERIFIED" if result["cutover_allowed"] else "REVIEW")} WHERE run_id={rid}')
    return result


def main():
    p=argparse.ArgumentParser(); p.add_argument('--env',required=True,choices=['v2-it'])
    p.add_argument('--phase',required=True,choices=['precheck','prepare','verify'])
    p.add_argument('--input',required=True); p.add_argument('--database',required=True)
    p.add_argument('--execute',action='store_true'); p.add_argument('--out'); a=p.parse_args()
    if a.execute and not execution_allowed(a.env,True,a.database): p.error('only router_cutover_it_* writable')
    result=run(json.loads(Path(a.input).read_text()),a.database,a.phase,a.execute)
    out=json.dumps(result,indent=2,ensure_ascii=False); print(out)
    if a.out: Path(a.out).write_text(out+'\n')
    return 0 if result['cutover_allowed'] else 1

if __name__=='__main__': raise SystemExit(main())
