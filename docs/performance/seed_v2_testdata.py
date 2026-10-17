#!/usr/bin/env python3
"""只在停止受理的全新隔离库准备合成期初；密钥/JWT输出目录必须位于仓库外。"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time

ROOT=Path(__file__).resolve().parents[2]
_owned_mysql_id = None


def owned_mysql_id():
    """锁定本次专属容器 ID，后续 exec 不通过可被替换的名称寻址。"""
    global _owned_mysql_id
    if _owned_mysql_id is not None:
        return _owned_mysql_id
    name = os.environ.get('V2_MYSQL_CONTAINER')
    if name != 'bromark-relay-20261016-mysql-1':
        raise ValueError('Task8 requires owned relay MySQL container')
    raw = subprocess.run(['docker','inspect',name],text=True,capture_output=True,check=True).stdout
    state = json.loads(raw)[0]
    if state['Name'] != '/'+name or not state['State']['Running'] or 'bromark-relay-20261016_default' not in state['NetworkSettings']['Networks']:
        raise ValueError('MySQL container ownership/network mismatch')
    _owned_mysql_id = state['Id']
    return _owned_mysql_id

def sql(v):
    if isinstance(v,(dict,list)):v=json.dumps(v,separators=(',',':'))
    return "CONVERT(X'"+str(v).encode().hex()+"' USING utf8mb4)"

def mysql(db,statement):
    if db and not re.fullmatch(r'router_perf_it_[a-z0-9_]+',db):raise ValueError('isolated databases only')
    run = os.environ.get('V2_TARGET_RUN_ID')
    if not run or not re.fullmatch(r'[a-z0-9_]{1,16}',run) or (db and db not in {f'router_perf_it_{run}_{part}' for part in ('control','one','two')}):
        raise ValueError('target run/database ownership mismatch')
    container = owned_mysql_id()
    cmd=['docker','exec','-i','-e','MYSQL_PWD',container,
         'mysql','--batch','--raw','--skip-column-names','-uroot']+([db] if db else [])
    return subprocess.run(cmd,input=statement,text=True,capture_output=True,check=True,env=dict(os.environ,MYSQL_PWD=os.environ['V2_MYSQL_PASSWORD'])).stdout.rstrip('\r\n')

def route(user):
    h=0
    units=user.encode('utf-16-be',errors='surrogatepass')
    for index in range(0,len(units),2):h=(31*h+int.from_bytes(units[index:index+2],'big'))&0xffffffff
    slot=(h^(h>>16))&7
    return slot//4+1,slot%4

def token(private,user):
    def b64(b):return base64.urlsafe_b64encode(b).decode().rstrip('=')
    now=int(time.time());data=b64(b'{"alg":"RS256","typ":"JWT"}')+'.'+b64(json.dumps(
        {'iss':'https://issuer.test','aud':'bromark','sub':user,'iat':now,'nbf':now-1,'exp':now+86400}).encode())
    signature=subprocess.run(['openssl','dgst','-sha256','-sign',str(private)],input=data.encode(),capture_output=True,check=True).stdout
    return data+'.'+b64(signature)

def seed(run,users,out,stock):
    if not re.fullmatch(r'[a-z0-9_]{1,16}',run) or not 1<=users<=100000 or stock<1:raise ValueError('invalid seed size/run')
    os.environ['V2_TARGET_RUN_ID'] = run
    out=Path(out).resolve()
    if out==ROOT or ROOT in out.parents:raise ValueError('secrets must stay outside git workspace')
    if out.exists() and any(out.iterdir()):raise ValueError('output directory must be empty')
    out.mkdir(parents=True,exist_ok=True);out.chmod(0o700)
    dbs=[f'router_perf_it_{run}_{x}' for x in ('control','one','two')]
    existing=mysql(None,"SELECT SCHEMA_NAME FROM information_schema.schemata WHERE SCHEMA_NAME IN ("+','.join(sql(x) for x in dbs)+')')
    if existing:raise ValueError('fresh databases required; existing run cannot be overwritten')
    for db in dbs:mysql(None,'CREATE DATABASE '+db+' CHARACTER SET utf8mb4 COLLATE utf8mb4_bin')
    manifest=json.loads((ROOT/'docs/dev-ops/mysql/migrations/manifest.json').read_text())
    for entry in manifest['scripts']:
        raw=(ROOT/'docs/dev-ops/mysql/migrations'/entry['file']).read_bytes()
        if hashlib.sha256(raw).hexdigest()!=entry['sha256']:raise ValueError('migration checksum')
        for i,db in enumerate(dbs):
            if entry['scope']=='control' and i!=0 or entry['scope']=='user' and i==0:continue
            for suffix in range(entry['shards']):
                for statement in raw.decode().replace('{{suffix}}',f'{suffix:03d}').split(';'):
                    if statement.strip():mysql(db,statement)
    private=out/'private.pem';public=out/'public.der'
    subprocess.run(['openssl','genpkey','-algorithm','RSA','-pkeyopt','rsa_keygen_bits:2048','-out',str(private)],check=True,capture_output=True)
    private.chmod(0o600)
    subprocess.run(['openssl','pkey','-in',str(private),'-pubout','-outform','DER','-out',str(public)],check=True,capture_output=True)
    activity=610016;version='phase6-'+run;pool='phase6-'+run
    prize={'awardId':101,'type':'CREDIT','minAmount':'1.00','maxAmount':'1.00','stockPoolId':pool,'rate':'1.00','unlockAfter':0}
    fallback={'awardId':0,'type':'NONE','minAmount':None,'maxAmount':None,'stockPoolId':None,'rate':'1.00','unlockAfter':0}
    strategy={'activityId':activity,'strategyId':activity,'version':version,'state':'OPEN',
              'beginsAt':'2020-01-01T00:00:00Z','endsAt':'2099-01-01T00:00:00Z','prizes':[prize],
              'fallback':fallback,'weightedPools':{},'blacklistAwardId':None,'blacklistVersion':'phase6',
              'tables':{'default':{'ids':[101],'probabilities':[1.0],'aliases':[0]}}}
    mysql(dbs[0],f"INSERT INTO award_stock VALUES({sql(pool)},{stock},{stock},0,0,0,'ACTIVE');"
          +f"INSERT INTO stock_change_log(stock_pool_id,biz_id,kind,total_delta,available_delta,reserved_delta,allocated_delta,reason) VALUES({sql(pool)},'seed','INIT',{stock},{stock},0,0,'synthetic audited budget');"
          +f"INSERT INTO strategy_version VALUES({activity},{sql(version)},{sql(strategy)});"
          +f"INSERT INTO v2_activity_pointer VALUES({activity},{activity},{sql(version)});"
          +f"INSERT INTO strategy_stock_binding VALUES({activity},{sql(version)},101,{sql(pool)});")
    snapshot={'activityId':activity,'activityState':'OPEN','version':version,'beginsAt':'2020-01-01T00:00:00Z',
              'endsAt':'2099-01-01T00:00:00Z','readAt':'2026-10-16T00:00:00Z','expiresAt':'2099-01-01T00:00:00Z',
              'blacklisted':False,'creditCost':'1.00','quota':{'totalDelta':100,'dayLimitDelta':100,'monthLimitDelta':100},
              'rewards':[{'itemId':'credit','activityId':0,'credit':'2.00','quota':None}]}
    for kind,key in (('EXCHANGE','61'),('SIGNIN','daily'),('ACTIVITY',str(activity))):
        mysql(dbs[0],f'INSERT INTO v2_admission_config VALUES({sql(kind)},{sql(key)},{sql(snapshot)})')
    mysql(dbs[0],"INSERT INTO payment_product VALUES('sku',10.00,'CNY',100.00,TRUE)")
    mapping={};batch={db:[] for db in dbs[1:]}
    for n in range(users):
        user=f'p6-{run}-{n}';db,tb=route(user);mapping[user]=token(private,user)
        batch[dbs[db]].append(f"INSERT INTO user_credit_account(user_id,balance,opening_balance,status,version,audit_source) VALUES({sql(user)},1000000.00,1000000.00,'ACTIVE',0,'phase6 audited seed')")
        batch[dbs[db]].append(f'INSERT INTO raffle_activity_account(user_id,activity_id,total_remaining,day_limit,month_limit,effective_draw_count,opening_total,opening_day_limit,opening_month_limit,opening_effective_count,audit_source) VALUES({sql(user)},{activity},1000000,1000000,1000000,0,1000000,1000000,1000000,0,\'phase6 audited seed\')')
    for db,statements in batch.items():mysql(db,'START TRANSACTION;'+';'.join(statements)+';COMMIT;')
    tokens=out/'tokens.json';tokens.write_text(json.dumps(mapping));tokens.chmod(0o600)
    manifest={'runId':run,'users':users,'databases':dbs,'activityId':activity,'sku':61,'version':version,
              'writerAssertion':'stopped-before-fresh-seed',
              'stockPool':pool,'allocatedSlots':stock,'tokenFile':str(tokens),
              'publicKey':base64.b64encode(public.read_bytes()).decode(),
              'migrationHashes':{e['file']:e['sha256'] for e in manifest['scripts']},
              'openingCreditPerUser':'1000000.00','openingQuotaPerUser':1000000}
    (out/'seed.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps({k:v for k,v in manifest.items() if k not in ('publicKey','tokenFile')},indent=2))
    return manifest

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--env',required=True,choices=['v2-it']);p.add_argument('--users',type=int,required=True)
    p.add_argument('--run-id',required=True);p.add_argument('--out',required=True);p.add_argument('--stock',type=int,default=1000000)
    p.add_argument('--stopped',action='store_true',help='operator confirms all writers for NEW dedicated databases stopped')
    a=p.parse_args()
    if not a.stopped:p.error('explicit stopped-writer assertion required')
    seed(a.run_id,a.users,a.out,a.stock)
