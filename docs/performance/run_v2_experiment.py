#!/usr/bin/env python3
"""运行一个固定到达率档位（不自动调高压力），同时独立跟踪与采集。"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time
from track_v2_orders import records

K6='grafana/k6:1.3.0@sha256:3ddc8b1a33a2c3d8edc6e99b6a762ae36cba08788463458f5e6a7703e14eb77d'
ROOT=Path(__file__).resolve().parent

def run(seed_file,out,rate,duration,scenario,run_id):
    out=Path(out).resolve();out.mkdir(parents=True,exist_ok=True)
    if any(out.iterdir()):raise ValueError('new empty run directory required')
    seed_file=Path(seed_file).resolve();seed=json.loads(seed_file.read_text())
    token_file=Path(os.environ['V2_TOKEN_FILE']).resolve()
    if not token_file.is_file():raise ValueError('token mapping required')
    base=os.environ.get('V2_BASE_URL','http://host.docker.internal:19861')
    if base!='http://host.docker.internal:19861':raise ValueError('only owned isolated endpoint allowed')
    envs={'BASE_URL':base,'RATE':str(rate),'DURATION':f'{duration}s','SCENARIO':scenario,'RUN_ID':run_id,
          'TOKEN_FILE':'/seed/tokens.json','SEED_FILE':'/seed/seed.json'}
    args=['docker','run','--rm','--name','bromark-phase6-load','--user',f'{os.getuid()}:{os.getgid()}',
          '-v',str(ROOT)+':/scripts:ro','-v',str(seed_file)+':/seed/seed.json:ro',
          '-v',str(token_file)+':/seed/tokens.json:ro','-v',str(out)+':/results']
    for k,v in envs.items():args+=['-e',k+'='+v]
    if scenario.startswith('signin'):
        args+=['-e','BUSINESS_DATE='+os.environ['V2_BUSINESS_DATE']]
    if scenario=='payment-replay':
        args+=['-v',str(Path(os.environ['V2_NOTIFICATION_FILE']).resolve())+':/seed/notifications.json:ro','-e','NOTIFICATION_FILE=/seed/notifications.json']
    args+=[K6,'run','--log-format','json','--console-output','/results/k6-console.jsonl',
           '--summary-export','/results/k6-summary.json','/scripts/v2-load.js']
    tracker=subprocess.Popen([sys.executable,str(ROOT/'track_v2_orders.py'),'--accepted',str(out/'k6-console.jsonl'),
                             '--base-url','http://127.0.0.1:19861','--out',str(out/'tracked.jsonl'),
                             '--seconds',str(duration+120),'--max-rps','50'])
    sampler=subprocess.Popen([sys.executable,str(ROOT/'sample_v2_runtime.py'),'--seed',str(seed_file),
                             '--out',str(out/'resources.jsonl'),'--seconds',str(duration+120)])
    start=time.time()
    try:
        with (out/'k6.log').open('w') as f: result=subprocess.run(args,stdout=f,stderr=subprocess.STDOUT)
        (out/'requests.jsonl').write_text(''.join(json.dumps(row)+'\n' for row in records(out/'k6-console.jsonl')))
        tracker.wait(timeout=duration+150);sampler.wait(timeout=duration+150)
        return {'k6Exit':result.returncode,'startedAt':start,'endedAt':time.time(),'runId':run_id,
                'rate':rate,'durationSeconds':duration,'scenario':scenario}
    finally:
        for child in (tracker,sampler):
            if child.poll() is None:child.terminate();child.wait(timeout=10)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seed',required=True);p.add_argument('--out',required=True)
    p.add_argument('--rate',type=int,required=True);p.add_argument('--duration',type=int,default=600)
    p.add_argument('--scenario',default='mixed');p.add_argument('--run-id',required=True);a=p.parse_args()
    if a.rate<=0 or a.duration<=0:p.error('positive load required')
    result=run(a.seed,a.out,a.rate,a.duration,a.scenario,a.run_id)
    (Path(a.out)/'execution.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
