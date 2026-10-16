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
                'credit_exchange_order':('user_id','request_id'),
                'consumer_inbox':('consumer_name','event_id'), 'consumer_receipt':('consumer_name','operation_key')}
    if table in ('consumer_inbox','consumer_receipt'): return families[table]
    for family, keys in families.items():
        if re.fullmatch(family+r'_00[0-3]',table): return keys
    raise ValueError('unsupported import table: '+table)


def digest(data):
    return hashlib.sha256(json.dumps(data,sort_keys=True,separators=(',',':'),ensure_ascii=False,default=str).encode()).hexdigest()


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
    if data.get('logical_db') not in ('01','02'):errors.append('audited logical user database required')
    for table,rows in data.get('tables',{}).items():
        try: keys=key_for(table)
        except ValueError as e: errors.append(str(e)); continue
        seen=set()
        for row in rows:
            if row.get('user_id'):
                slot=route_slot(row['user_id'])
                if slot[:2]!=data.get('logical_db') or (re.search(r'_00[0-3]$',table) and table[-3:]!=slot[3:]):
                    errors.append('audited user row is on wrong logical database/table')
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
    tables=data.get('tables',{})
    accounts={r.get('user_id'):r for r in tables.get('user_credit_account',[])}
    changes={}
    for table,rows in tables.items():
        if re.fullmatch(r'user_credit_ledger_00[0-3]',table):
            for row in rows:
                try: changes[row['user_id']]=changes.get(row['user_id'],Decimal(0))+Decimal(str(row['delta']))
                except (KeyError,InvalidOperation): errors.append('invalid imported ledger')
    if changes.keys()-accounts.keys(): errors.append('orphan imported credit ledger')
    for user,row in accounts.items():
        if not {'balance','opening_balance','status','version','audit_source'}<=row.keys():
            errors.append('incomplete audited credit opening'); continue
        try:
            if Decimal(str(row['balance']))!=Decimal(str(row['opening_balance']))+changes.get(user,Decimal(0)):
                errors.append('opening plus imported credit differs from balance')
        except InvalidOperation: errors.append('invalid credit opening')
    required_quota={'total_remaining','day_limit','month_limit','effective_draw_count',
                    'opening_total','opening_day_limit','opening_month_limit','opening_effective_count','audit_source'}
    for row in tables.get('raffle_activity_account',[]):
        if not required_quota<=row.keys():errors.append('incomplete audited quota opening')
    for table,key in [('raffle_activity_account_day','business_date'),('raffle_activity_account_month','business_month')]:
        for row in tables.get(table,[]):
            if not {key,'remaining','quota_limit','opening_quota','audit_source'}<=row.keys():
                errors.append('incomplete audited period opening')
    quota=[]
    for table,rows in tables.items():
        if re.fullmatch(r'user_quota_ledger_00[0-3]',table): quota.extend(rows)
    for row in tables.get('raffle_activity_account',[]):
        if not required_quota<=row.keys():continue
        entries=[q for q in quota if q.get('user_id')==row['user_id'] and q.get('activity_id')==row['activity_id']]
        try:
            total=sum(int(q['delta']) for q in entries if q['bucket_type']=='TOTAL')
            day=sum(int(q['limit_delta']) for q in entries if q['bucket_type']=='DAY')
            month=sum(int(q['limit_delta']) for q in entries if q['bucket_type']=='MONTH')
            effective=-sum(int(q['delta']) for q in entries if q['bucket_type']=='TOTAL' and q['biz_type'].startswith(('CONSUME:','REFUND:')))
            if any(int(row[current])!=int(row[opening])+delta for current,opening,delta in [
                    ('total_remaining','opening_total',total),('day_limit','opening_day_limit',day),
                    ('month_limit','opening_month_limit',month),('effective_draw_count','opening_effective_count',effective)]):
                errors.append('quota opening plus imported ledger mismatch')
        except (KeyError,ValueError):errors.append('invalid quota evidence')
    for table,bucket,key in [('raffle_activity_account_day','DAY','business_date'),('raffle_activity_account_month','MONTH','business_month')]:
        for row in tables.get(table,[]):
            try:
                delta=sum(int(q['delta']) for q in quota if q.get('user_id')==row['user_id'] and q.get('activity_id')==row['activity_id'] and q.get('bucket_type')==bucket and q.get('bucket_key')==row[key])
                if int(row['remaining'])!=int(row['opening_quota'])+delta:errors.append('period opening mismatch')
            except (KeyError,ValueError):errors.append('invalid period evidence')
    seen=set()
    for row in data.get('identities',[]):
        identity=(row.get('entry'),row.get('legacy_id'))
        if not all(identity) or identity in seen or not row.get('evidence') or not row.get('biz_id') or not re.fullmatch(r'[A-Z_]{1,32}',row.get('biz_type','')) or not re.fullmatch(r'[a-f0-9]{64}',row.get('request_hash','')):
            errors.append('ambiguous legacy identity')
        seen.add(identity)
    if data.get('unresolved'): errors.append('unresolved legacy effects/messages')
    # Original asset receipt is the deduplication fact. A map alone is not one.
    for row in data.get('identities',[]):
        user=row.get('user_id');slot=route_slot(user) if isinstance(user,str) and user else None
        if slot is None or row.get('route_slot')!=slot or data.get('logical_db')!=slot[:2]:
            errors.append('identity user/route proof missing or wrong');continue
        kind=row.get('effect_kind');receipt=row.get('receipt')
        if kind not in ('CREDIT','QUOTA') or not isinstance(receipt,dict):
            errors.append('original asset receipt proof required');continue
        biz=receipt.get('bizId',{})
        if biz!={'type':row.get('biz_type'),'id':row.get('biz_id'),'hash':row.get('request_hash')}:
            errors.append('receipt business identity conflict');continue
        table=('user_credit_ledger_' if kind=='CREDIT' else 'user_quota_ledger_')+slot[3:]
        matches=[r for r in tables.get(table,[]) if r.get('user_id')==user and r.get('biz_id')==row['biz_id'] and r.get('request_hash')==row['request_hash'] and (r.get('biz_type')==row['biz_type'] if kind=='CREDIT' else r.get('biz_type','').endswith(':'+row['biz_type']))]
        if len(matches)!=(1 if kind=='CREDIT' else 3) or any(canonical_cell('receipt',r.get('receipt'))!=canonical_cell('receipt',receipt) for r in matches):
            errors.append('imported original ledger/receipt missing or conflicting')
        if kind=='CREDIT' and len(matches)==1:
            try:
                if Decimal(str(matches[0]['delta']))!=Decimal(str(receipt['delta'])) or Decimal(str(matches[0]['balance_after']))!=Decimal(str(receipt['balanceAfter'])):
                    errors.append('original receipt amount/balance differs from ledger')
            except (KeyError,InvalidOperation):errors.append('incomplete original credit receipt')

    for row in tables.get('consumer_receipt',[]):
        if not isinstance(row.get('responses'),list) or not re.fullmatch(r'[a-f0-9]{64}',row.get('payload_hash','')):
            errors.append('invalid imported original consumer receipt')
    return {'cutover_allowed':not errors,'errors':errors,'input_sha256':digest(data),
            'rows':{t:len(r) for t,r in data.get('tables',{}).items()}}


def sql_value(value):
    if isinstance(value,(dict,list)): value=json.dumps(value,sort_keys=True,separators=(',',':'),default=str)
    # Hex bytes are independent of SQL string escaping and NO_BACKSLASH_ESCAPES.
    return "CONVERT(X'"+str(value).encode().hex()+"' USING utf8mb4)"


def mysql(database,sql):
    if not execution_allowed('v2-it',True,database): raise ValueError('isolated database required')
    container=os.environ.get('V2_MYSQL_CONTAINER','bromark-v2-mysql')
    if container!='bromark-v2-mysql': raise ValueError('isolated container allowlist')
    if not os.environ.get('V2_MYSQL_PASSWORD'): raise ValueError('password required')
    return subprocess.run(['docker','exec','-i','-e','MYSQL_PWD',container,
                           'mysql','--batch','--skip-column-names','-uroot',database],
                          input=sql,text=True,capture_output=True,check=True,env=dict(os.environ,MYSQL_PWD=os.environ['V2_MYSQL_PASSWORD'])).stdout.strip()


def route_slot(user):
    h=0
    for c in user: h=(31*h+ord(c))&0xffffffff
    slot=(h^(h>>16))&7
    return f'{slot//4+1:02d}.{slot%4:03d}'


def canonical_cell(column,value):
    if column in {'balance','opening_balance','delta','balance_after','amount','credit','credit_cost'}:
        return str(Decimal(str(value)).normalize())
    if column in {'receipt','snapshot','responses','admission_snapshot','quota_snapshot'}:
        if isinstance(value,str):value=json.loads(value,parse_float=Decimal)
        return json.dumps(value,sort_keys=True,separators=(',',':'),default=str)
    return str(value)


def table_digest(database,table,rows):
    if not rows:return {'count':int(mysql(database,f'SELECT COUNT(*) FROM `{table}`')), 'expected':0,'matches':True}
    columns=sorted(rows[0])
    for row in rows:
        if sorted(row)!=columns:raise ValueError('heterogeneous import columns')
    actual=[]
    for line in mysql(database,'SELECT '+','.join(f'HEX(CAST(`{c}` AS CHAR))' for c in columns)+f' FROM `{table}`').splitlines():
        actual.append(tuple(canonical_cell(c,bytes.fromhex(v).decode()) for c,v in zip(columns,line.split('\t'))))
    expected=[tuple(canonical_cell(c,r[c]) for c in columns) for r in rows]
    return {'count':len(actual),'expected':len(rows),'matches':sorted(actual)==sorted(expected),'checksum':digest(sorted(actual))}


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
    ddl=[s for f in ('006_cutover_constraints.sql','006_cutover_defaults.sql','006_legacy_receipt_proof.sql') for s in (ROOT/f).read_text().split(';') if s.strip()]
    for statement in ddl:
        if statement.lstrip().startswith('CREATE TABLE'):mysql(database,statement)
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
                proof_columns=('entry','legacy_id','user_id','route_slot','effect_kind','biz_type','biz_id','request_hash','receipt')
                statements.append('INSERT INTO cutover_identity_proof VALUES ('+','.join(sql_value(row[c]) for c in proof_columns)+')')
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
    columns=('entry','legacy_id','biz_type','biz_id','request_hash','evidence')
    proofs=('entry','legacy_id','user_id','route_slot','effect_kind','biz_type','biz_id','request_hash','receipt')
    result['identity_check']=table_digest(database,'cutover_identity',[{c:r[c] for c in columns} for r in data.get('identities',[])])
    result['proof_check']=table_digest(database,'cutover_identity_proof',[{c:r[c] for c in proofs} for r in data.get('identities',[])])
    result['cutover_allowed']=all(r['matches'] and r['count']==r.get('expected',0) for r in result['tables'].values()) and result['identity_count']==len(data.get('identities',[])) and result['identity_check']['matches'] and result['proof_check']['matches']
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
    result=run(json.loads(Path(a.input).read_text(),parse_float=Decimal),a.database,a.phase,a.execute)
    out=json.dumps(result,indent=2,ensure_ascii=False,default=str); print(out)
    if a.out: Path(a.out).write_text(out+'\n')
    return 0 if result['cutover_allowed'] else 1

if __name__=='__main__': raise SystemExit(main())
