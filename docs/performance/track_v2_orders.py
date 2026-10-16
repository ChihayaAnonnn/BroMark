#!/usr/bin/env python3
"""独立增量查单。仅从环境提供的token映射读取身份；每次轮询额外计数。"""
import argparse
import json
import os
from pathlib import Path
import time
import urllib.request
import urllib.error

TERMINAL={'DELIVERED','CANCELLED','REJECTED'}

def records(path):
    if not Path(path).exists():return []
    out=[]
    for line in Path(path).read_text().splitlines():
        try:
            value=json.loads(line)
            if 'msg' in value:value=json.loads(value['msg'])
            if 'requestId' in value:out.append(value)
        except (ValueError,TypeError):continue # concurrent trailing partial log line
    return out

def track(accepted,base,out,seconds,interval,max_rps=50):
    if not base.startswith(('http://127.0.0.1:','http://localhost:','http://host.docker.internal:')):
        raise ValueError('isolated loopback HTTP endpoint required')
    if seconds<=0 or interval<=0 or max_rps<=0:raise ValueError('positive tracker budgets required')
    tokens=json.loads(Path(os.environ['V2_TOKEN_FILE']).read_text())
    out=Path(out);done=set();polls=0;errors=0;deadline=time.monotonic()+seconds
    if out.exists():done={v['orderId'] for v in records(out) if v.get('state') in TERMINAL}
    with out.open('a') as f:
        while time.monotonic()<deadline:
            pending={}
            for row in records(accepted):
                if 200<=row.get('status',0)<300 and row.get('orderId') and row.get('operation') in ('draw','payment') and row['orderId'] not in done:
                    pending[row['orderId']]=row
            for order,row in pending.items():
                if time.monotonic()>=deadline:break
                time.sleep(1/max_rps)
                route='/api/v2/draws/' if row['operation']=='draw' else '/api/v2/payment/orders/'
                request=urllib.request.Request(base+route+order,headers={'Authorization':'Bearer '+tokens[row['user']]})
                polls+=1
                try:
                    with urllib.request.urlopen(request,timeout=6) as response:value=json.load(response)
                    record={'requestId':row['requestId'],'user':row['user'],'orderId':order,'kind':row['operation'],
                            'state':value.get('state'),'observedAt':time.time(),'pollStatus':response.status}
                    f.write(json.dumps(record)+'\n');f.flush()
                    if value.get('state') in TERMINAL:done.add(order)
                except (urllib.error.URLError,ValueError,KeyError):errors+=1
            time.sleep(interval)
    summary={'pollRequests':polls,'pollErrors':errors,'observedTerminals':len(done)}
    out.with_suffix('.polling.json').write_text(json.dumps(summary,indent=2)+'\n');return summary

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--accepted',required=True);p.add_argument('--base-url',required=True)
    p.add_argument('--out',required=True);p.add_argument('--seconds',type=float,default=900);p.add_argument('--interval',type=float,default=1);p.add_argument('--max-rps',type=float,default=50)
    a=p.parse_args();print(json.dumps(track(a.accepted,a.base_url,a.out,a.seconds,a.interval,a.max_rps)))
