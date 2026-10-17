#!/usr/bin/env python3
"""运行一个固定到达率档位（不自动调高压力），同时独立跟踪与采集。"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
from seed_v2_testdata import mysql
from track_v2_orders import records

K6='grafana/k6:1.3.0@sha256:3ddc8b1a33a2c3d8edc6e99b6a762ae36cba08788463458f5e6a7703e14eb77d'
ROOT=Path(__file__).resolve().parent

def clock_calibration(control_db):
    before=time.time()
    server=float(mysql(control_db,"SET time_zone='+00:00';SELECT UNIX_TIMESTAMP(UTC_TIMESTAMP(6))"))
    after=time.time()
    return {'clientBefore':before,'server':server,'clientAfter':after,
            'skewMs':(server-(before+after)/2)*1000,'uncertaintyMs':(after-before)*500}

def wait_collectors(tracker,sampler,timeout):
    tracker_exit=tracker.wait(timeout=timeout);sampler_exit=sampler.wait(timeout=timeout)
    return {'trackerExit':tracker_exit,'samplerExit':sampler_exit,'collectionComplete':tracker_exit==0 and sampler_exit==0}


def validate_owned_target(seed,app_number):
    """将端口、应用 ID 和本轮三库绑定到同一隔离运行。"""
    run=seed.get('runId')
    if not isinstance(run,str) or not re.fullmatch(r'[a-z0-9_]{1,16}',run) or os.environ.get('V2_TARGET_RUN_ID')!=run:
        raise ValueError('target run ID mismatch')
    databases=[f'router_perf_it_{run}_{part}' for part in ('control','one','two')]
    if seed.get('databases')!=databases:raise ValueError('target databases mismatch')
    name=f'bromark-relay-20261016-app-{app_number}'
    if os.environ.get(f'V2_APP_CONTAINER_{app_number}')!=name or os.environ.get('V2_TEST_DOCKER_NETWORK')!='bromark-assets-cluster-it':
        raise ValueError('owned app/network environment required')
    state=json.loads(subprocess.run(['docker','inspect',name],text=True,capture_output=True,check=True,timeout=10).stdout)[0]
    port=19870+app_number
    if (state.get('Name')!='/'+name or not state['State']['Running']
            or 'bromark-assets-cluster-it' not in state['NetworkSettings']['Networks']
            or state['NetworkSettings']['Ports'].get('8080/tcp')!=[{'HostIp':'127.0.0.1','HostPort':str(port)}]):
        raise ValueError('owned app identity/network/port mismatch')
    env=set(state['Config']['Env'])
    if any(f'V2_DB{index:02d}={db}' not in env for index,db in enumerate(databases)):
        raise ValueError('owned app target databases mismatch')
    rabbit=os.environ.get('V2_ACTIVE_RABBIT_CONTAINER')
    if not rabbit or not re.fullmatch(r'bromark-relay-20261016-task[0-9]+-rabbit',rabbit) or f'V2_RABBIT_HOST={rabbit}' not in env:
        raise ValueError('owned fresh Rabbit binding required')
    broker=json.loads(subprocess.run(['docker','inspect',rabbit],text=True,capture_output=True,check=True,timeout=10).stdout)[0]
    if broker.get('Name')!='/'+rabbit or not broker['State']['Running'] or 'bromark-assets-cluster-it' not in broker['NetworkSettings']['Networks']:
        raise ValueError('owned Rabbit identity/network mismatch')
    for db in databases:
        if mysql(db,'SELECT DATABASE()')!=db:raise ValueError('target database inaccessible')
    return {'id':state['Id'],'name':name,'port':port,'image':state['Image'],'rabbitId':broker['Id']}


def assert_owned_target_unchanged(seed,app_number,expected):
    current=validate_owned_target(seed,app_number)
    if any(current[key]!=expected[key] for key in ('id','image','rabbitId','port')):
        raise ValueError('owned app or Rabbit identity changed')
    return current


def run(seed_file,out,rate,duration,scenario,run_id,user_distribution='legacy',app_number=1,phase='formal'):
    out=Path(out).resolve();out.mkdir(parents=True,exist_ok=True)
    if any(out.iterdir()):raise ValueError('new empty run directory required')
    if ROOT.parents[1] in out.parents or out==ROOT.parents[1]:raise ValueError('raw output must stay outside repository')
    seed_file=Path(seed_file).resolve();seed=json.loads(seed_file.read_text())
    if user_distribution not in ('legacy','uniform') or phase not in ('warmup','formal'):
        raise ValueError('invalid distribution/phase')
    if not re.fullmatch(r'[a-zA-Z0-9_-]{1,40}',run_id):raise ValueError('invalid load run ID')
    target=validate_owned_target(seed,app_number)
    token_file=Path(os.environ['V2_TOKEN_FILE']).resolve()
    if not token_file.is_file():raise ValueError('token mapping required')
    if seed.get('tokenFile') and token_file!=Path(seed['tokenFile']).resolve():raise ValueError('seed token mapping mismatch')
    base=f'http://host.docker.internal:{target["port"]}'
    envs={'BASE_URL':base,'RATE':str(rate),'DURATION':f'{duration}s','SCENARIO':scenario,'RUN_ID':run_id,
          'TOKEN_FILE':'/seed/tokens.json','SEED_FILE':'/seed/seed.json',
          'USER_DISTRIBUTION':user_distribution,'PHASE':phase}
    args=['docker','run','--rm','--name','bromark-relay-20261016-task9-load-'+run_id,'--user',f'{os.getuid()}:{os.getgid()}',
          '-v',str(ROOT)+':/scripts:ro','-v',str(seed_file)+':/seed/seed.json:ro',
          '-v',str(token_file)+':/seed/tokens.json:ro','-v',str(out)+':/results']
    for k,v in envs.items():args+=['-e',k+'='+v]
    if scenario.startswith('signin'):
        args+=['-e','BUSINESS_DATE='+os.environ['V2_BUSINESS_DATE']]
    if scenario=='payment-replay':
        args+=['-v',str(Path(os.environ['V2_NOTIFICATION_FILE']).resolve())+':/seed/notifications.json:ro','-e','NOTIFICATION_FILE=/seed/notifications.json']
    args+=[K6,'run','--log-format','json','--console-output','/results/k6-console.jsonl',
           '--summary-export','/results/k6-summary.json','/scripts/v2-load.js']
    clock_before=clock_calibration(seed['databases'][0])
    (out/'clock-before.json').write_text(json.dumps(clock_before,indent=2)+'\n')
    tracker=subprocess.Popen([sys.executable,str(ROOT/'track_v2_orders.py'),'--accepted',str(out/'k6-console.jsonl'),
                             '--base-url',f'http://127.0.0.1:{target["port"]}','--out',str(out/'tracked.jsonl'),
                             '--seconds',str(duration+120),'--max-rps','50'])
    start=time.time()
    try:
        assert_owned_target_unchanged(seed,app_number,target)
        with (out/'k6.log').open('w') as f: result=subprocess.run(args,stdout=f,stderr=subprocess.STDOUT)
        load_end=time.time()
        (out/'requests.jsonl').write_text(''.join(json.dumps(row)+'\n' for row in records(out/'k6-console.jsonl')))
        tracker_exit=tracker.wait(timeout=duration+150)
        try:
            assert_owned_target_unchanged(seed,app_number,target)
            stable=True
        except (ValueError,subprocess.CalledProcessError,OSError):
            stable=False
        collection={'trackerExit':tracker_exit,'samplerExit':None,'collectionComplete':False,
                    'task8SamplerRequired':True,'resourceIdentityStable':stable}
        return dict(collection,k6Exit=result.returncode,startedAt=start,loadEndedAt=load_end,endedAt=time.time(),runId=run_id,
                rate=rate,durationSeconds=duration,scenario=scenario,phase=phase,userDistribution=user_distribution,
                targetAppId=target['id'],targetAppImage=target['image'],targetRabbitId=target['rabbitId'],
                targetDatabases=seed['databases'],
                clockBefore=clock_before)
    finally:
        for child in (tracker,):
            if child.poll() is None:child.terminate();child.wait(timeout=10)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seed',required=True);p.add_argument('--out',required=True)
    p.add_argument('--rate',type=int,required=True);p.add_argument('--duration',type=int,default=600)
    p.add_argument('--scenario',default='mixed');p.add_argument('--run-id',required=True)
    p.add_argument('--user-distribution',choices=('legacy','uniform'),default='legacy')
    p.add_argument('--app',type=int,choices=(1,2),default=1)
    p.add_argument('--phase',choices=('warmup','formal'),default='formal');a=p.parse_args()
    if a.rate<=0 or a.duration<=0:p.error('positive load required')
    result=run(a.seed,a.out,a.rate,a.duration,a.scenario,a.run_id,a.user_distribution,a.app,a.phase)
    (Path(a.out)/'execution.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
