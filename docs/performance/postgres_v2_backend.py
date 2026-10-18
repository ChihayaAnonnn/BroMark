"""仅供本次隔离PostgreSQL三库种子与查询；证据目录和owned容器元数据必须在仓库外。"""
import base64,hashlib,json,os,re,subprocess,sys,time
from pathlib import Path
B=Path(os.environ['V2_PG_EVIDENCE_DIR']).resolve()
ROOT=Path(__file__).resolve().parents[2]
SCHEMA=ROOT/'docs/dev-ops/postgresql/v2-schema.sql'
sys.path.insert(0,str(ROOT/'docs/performance'))
from seed_v2_testdata import route,token
OWNED=json.loads((B/'postgres-owned.json').read_text())
def pg(db,statement):
 state=json.loads(subprocess.check_output(['docker','inspect',OWNED['id']]))[0]
 assert state['Id']==OWNED['id'] and state['Name']=='/'+OWNED['name'] and state['State']['Running'] and 'bromark-assets-cluster-it' in state['NetworkSettings']['Networks']
 if db:
  run=os.environ.get('V2_TARGET_RUN_ID','')
  assert re.fullmatch(r'pg[a-z0-9_]{1,14}',run) and db in {f'router_perf_it_{run}_{part}' for part in ['control','one','two']}
 statement=statement.replace('SELECT DATABASE()', 'SELECT current_database()').replace('select database()', 'select current_database()').replace('CAST(a.business_date AS CHAR)','CAST(a.business_date AS TEXT)')
 p=subprocess.run(['docker','exec','-i',OWNED['id'],'psql','-X','-q','-t','-A','-F','\t','-v','ON_ERROR_STOP=1','-U','postgres','-d',db or 'postgres'],input=statement,text=True,capture_output=True)
 if p.returncode:raise RuntimeError('PostgreSQL admin SQL failed: '+p.stderr[:1000])
 return p.stdout.rstrip('\r\n')
def sql(value):
 if isinstance(value,(dict,list)):value=json.dumps(value,separators=(',',':'))
 return "'"+str(value).replace("'","''")+"'"
def seed(run,users,out,stock):
 assert re.fullmatch(r'pg[a-z0-9_]{1,14}',run) and 1<=users<=100000 and stock>0
 os.environ['V2_TARGET_RUN_ID']=run
 out=Path(out).resolve();assert not (out==ROOT or ROOT in out.parents)
 if out.exists() and any(out.iterdir()):raise ValueError('fresh seed directory required')
 out.mkdir(parents=True,exist_ok=True);out.chmod(0o700)
 dbs=[f'router_perf_it_{run}_{x}' for x in ['control','one','two']]
 assert not pg(None,'select datname from pg_database where datname in ('+','.join(sql(x) for x in dbs)+')')
 for db in dbs:
  pg(None,'CREATE DATABASE '+db+' TEMPLATE template0 LC_COLLATE \'C\' LC_CTYPE \'C\'')
  pg(db,SCHEMA.read_text());pg(db,'CREATE EXTENSION IF NOT EXISTS pg_stat_statements')
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
 pg(dbs[0],f"INSERT INTO award_stock VALUES({sql(pool)},{stock},{stock},0,0,0,'ACTIVE');"
       +f"INSERT INTO stock_change_log(stock_pool_id,biz_id,kind,total_delta,available_delta,reserved_delta,allocated_delta,reason) VALUES({sql(pool)},'seed','INIT',{stock},{stock},0,0,'synthetic audited budget');"
       +f"INSERT INTO strategy_version VALUES({activity},{sql(version)},{sql(strategy)});"
       +f"INSERT INTO v2_activity_pointer VALUES({activity},{activity},{sql(version)});"
       +f"INSERT INTO strategy_stock_binding VALUES({activity},{sql(version)},101,{sql(pool)});")
 snapshot={'activityId':activity,'activityState':'OPEN','version':version,'beginsAt':'2020-01-01T00:00:00Z',
           'endsAt':'2099-01-01T00:00:00Z','readAt':'2026-10-16T00:00:00Z','expiresAt':'2099-01-01T00:00:00Z',
           'blacklisted':False,'creditCost':'1.00','quota':{'totalDelta':100,'dayLimitDelta':100,'monthLimitDelta':100},
           'rewards':[{'itemId':'credit','activityId':0,'credit':'2.00','quota':None}]}
 for kind,key in (('EXCHANGE','61'),('SIGNIN','daily'),('ACTIVITY',str(activity))):
     pg(dbs[0],f'INSERT INTO v2_admission_config VALUES({sql(kind)},{sql(key)},{sql(snapshot)})')
 pg(dbs[0],"INSERT INTO payment_product VALUES('sku',10.00,'CNY',100.00,TRUE)")
 mapping={};batch={db:[] for db in dbs[1:]}
 for n in range(users):
     user=f'p6-{run}-{n}';db,tb=route(user);mapping[user]=token(private,user)
     batch[dbs[db]].append(f"INSERT INTO user_credit_account(user_id,balance,opening_balance,status,version,audit_source) VALUES({sql(user)},1000000.00,1000000.00,'ACTIVE',0,'phase6 audited seed')")
     batch[dbs[db]].append(f'INSERT INTO raffle_activity_account(user_id,activity_id,total_remaining,day_limit,month_limit,effective_draw_count,opening_total,opening_day_limit,opening_month_limit,opening_effective_count,audit_source) VALUES({sql(user)},{activity},1000000,1000000,1000000,0,1000000,1000000,1000000,0,\'phase6 audited seed\')')
 for db,statements in batch.items():pg(db,'START TRANSACTION;'+';'.join(statements)+';COMMIT;')
 tokens=out/'tokens.json';tokens.write_text(json.dumps(mapping));tokens.chmod(0o600)
 manifest={'runId':run,'users':users,'databases':dbs,'activityId':activity,'sku':61,'version':version,
           'writerAssertion':'stopped-before-fresh-seed',
           'stockPool':pool,'allocatedSlots':stock,'tokenFile':str(tokens),
           'publicKey':base64.b64encode(public.read_bytes()).decode(),
           'migrationHashes':{'v2-schema.sql':hashlib.sha256(SCHEMA.read_bytes()).hexdigest()},
           'openingCreditPerUser':'1000000.00','openingQuotaPerUser':1000000}
 (out/'seed.json').write_text(json.dumps(manifest,indent=2)+'\n')
 print(json.dumps({k:v for k,v in manifest.items() if k not in ('publicKey','tokenFile')},indent=2))
 return manifest
