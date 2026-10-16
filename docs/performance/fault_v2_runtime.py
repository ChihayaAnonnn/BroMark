#!/usr/bin/env python3
"""只操作此次实验的两个应用和专用Rabbit容器；真实SIGKILL、broker断开、跨实例接管。"""
import argparse
import concurrent.futures
import hashlib
import hmac
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.request
import uuid
from seed_v2_testdata import mysql,route,sql


def docker(*args):return subprocess.run(['docker',*args],check=True,text=True,capture_output=True).stdout.strip()
def request(port,path,token,body=None,headers=None):
    hdr={'Authorization':'Bearer '+token,'Content-Type':'application/json'};hdr.update(headers or {})
    r=urllib.request.Request('http://127.0.0.1:'+str(port)+path,headers=hdr,
                             data=json.dumps(body,separators=(',',':')).encode() if body is not None else None)
    with urllib.request.urlopen(r,timeout=7) as response:
        raw=response.read().decode()
        return json.loads(raw) if raw.startswith('{') else raw

def run(seed):
    tokens=json.loads(Path(os.environ['V2_TOKEN_FILE']).read_text());user=next(iter(tokens));token=tokens[user]
    records=[]
    # Before touching containers, prove they are this run's dedicated database clients.
    for n in (1,2):
        state=json.loads(docker('inspect','bromark-phase6-app-'+str(n)))[0]
        if 'V2_DB00='+seed['databases'][0] not in state['Config']['Env']:raise ValueError('container ownership/run mismatch')
    request_id='fault-'+uuid.uuid4().hex
    try:
        docker('stop','-t','10','bromark-v2-rabbit')
        accepted=request(19861,'/api/v2/draws',token,{'requestId':request_id,'activityId':seed['activityId']})
        order=accepted['orderId'];db,suffix=route(user);table='v2_draw_order_'+f'{suffix:03d}'
        before=mysql(seed['databases'][db],f'SELECT state FROM {table} WHERE order_id={sql(order)}')
        docker('kill','--signal','KILL','bromark-phase6-app-1')
        survivor=request(19862,'/api/v2/draws/'+order,token)
        records.append({'fault':'broker STOP + app1 SIGKILL','orderId':order,'sourceBefore':before,
                        'survivingInstanceState':survivor['state'],'recordedAt':time.time()})
    finally:
        docker('start','bromark-v2-rabbit');docker('start','bromark-phase6-app-1')
    end=time.monotonic()+90
    while time.monotonic()<end:
        state=request(19862,'/api/v2/draws/'+order,token)
        if state['state']=='DELIVERED':break
        time.sleep(0.5)
    else:raise RuntimeError('accepted order did not recover')
    replay=request(19862,'/api/v2/draws',token,{'requestId':request_id,'activityId':seed['activityId']})
    if replay['orderId']!=order:raise RuntimeError('retry changed order')
    credit=int(mysql(seed['databases'][db],f"SELECT COUNT(*) FROM user_credit_ledger_{suffix:03d} WHERE user_id={sql(user)} AND biz_type='DRAW_AWARD' AND biz_id={sql(order)}"))
    if credit!=1:raise RuntimeError('non-unique award credit')
    records[-1].update(finalState=state['state'],originalOrderReplay=True,awardCreditRows=credit)
    # Two independently running real HTTP processes submit the same identity.
    body={'requestId':'dual-'+uuid.uuid4().hex,'activityId':seed['activityId']}
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        responses=list(pool.map(lambda port:request(port,'/api/v2/draws',token,body),[19861,19862]))
    if len({r['orderId'] for r in responses})!=1:raise RuntimeError('multi-instance idempotency broken')
    records.append({'fault':'two independent application processes','orderId':responses[0]['orderId'],
                    'uniqueOrders':1,'recordedAt':time.time()})
    return {'passed':True,'faults':records,'databaseConnectionFault':'DatabaseDisconnectV2Test.mysqlKillBeforeCommitCannotPublishUncommittedAssetsOrOutbox'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--env',required=True,choices=['v2-it']);p.add_argument('--seed',required=True);p.add_argument('--out',required=True);a=p.parse_args()
    result=run(json.loads(Path(a.seed).read_text()));Path(a.out).write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
