#!/usr/bin/env python3
"""为 mixed 的结果查询预置已交付订单；原请求身份可重放，输出不含 JWT。"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.request

from run_v2_experiment import ROOT, assert_owned_target_unchanged, validate_owned_target


def http(base, path, token, body=None):
    raw=None if body is None else json.dumps(body,separators=(',',':')).encode()
    request=urllib.request.Request(base+path,data=raw,headers={
        'Authorization':'Bearer '+token,'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(request,timeout=6) as response:
            return response.status,json.loads(response.read().decode())
    except urllib.error.HTTPError as error:
        try:value=json.loads(error.read().decode())
        except ValueError:value={}
        return error.code,value
    except (urllib.error.URLError,TimeoutError,OSError):
        return 0,{}


def append(path,row):
    with path.open('a') as handle:
        handle.write(json.dumps(row,separators=(',',':'))+'\n')
        handle.flush();os.fsync(handle.fileno())


def prepare(seed_path,out,app_number=1):
    seed_path=Path(seed_path).resolve();seed=json.loads(seed_path.read_text())
    target=validate_owned_target(seed,app_number)
    token_path=Path(os.environ['V2_TOKEN_FILE']).resolve()
    if token_path!=Path(seed['tokenFile']).resolve():raise ValueError('seed token mapping mismatch')
    tokens=json.loads(token_path.read_text())
    if not tokens or len(tokens)!=seed['users']:raise ValueError('seed identities missing')
    out=Path(out).resolve()
    if out==ROOT.parents[1] or ROOT.parents[1] in out.parents:raise ValueError('evidence must stay outside repository')
    out.mkdir(parents=True,exist_ok=True)
    journal=out/'requests.jsonl';polls=out/'polls.jsonl'
    mapping=dict(seed.get('resultOrders') or {})
    prior={}
    if journal.exists():
        for line in journal.read_text().splitlines():
            row=json.loads(line)
            if row.get('orderId'):prior[row['user']]=row['orderId']
    base='http://127.0.0.1:%d'%target['port']
    for index,user in enumerate(sorted(tokens)):
        assert_owned_target_unchanged(seed,app_number,target)
        request_id='pre:%s:%d'%(seed['runId'],index)
        order=mapping.get(user) or prior.get(user)
        for _ in range(5):
            if order:break
            status,value=http(base,'/api/v2/draws',tokens[user],
                              {'requestId':request_id,'activityId':seed['activityId']})
            order=value.get('orderId') if isinstance(value,dict) else None
            append(journal,{'requestId':request_id,'phase':'precreate','user':user,'operation':'draw',
                            'status':status,'state':value.get('state') if isinstance(value,dict) else None,
                            'orderId':order,'observedAt':time.time()})
            if 400<=status<500 and status!=429:raise RuntimeError('precreate rejected; original request journal retained')
            if not order:time.sleep(.2)
        if not order:raise RuntimeError('precreate original request unresolved; resume with same ID and journal')
        deadline=time.monotonic()+120
        while time.monotonic()<deadline:
            status,value=http(base,'/api/v2/draws/'+order,tokens[user])
            state=value.get('state') if isinstance(value,dict) else None
            append(polls,{'requestId':request_id,'phase':'precreate','user':user,'orderId':order,
                          'status':status,'state':state,'observedAt':time.time()})
            if status==200 and state=='DELIVERED':break
            if state in ('CANCELLED','REJECTED'):raise RuntimeError('precreate order terminal without delivery')
            time.sleep(.25)
        else:raise RuntimeError('precreate original order not delivered; journal retained')
        mapping[user]=order
        seed['resultOrders']=mapping
        temporary=seed_path.with_name(seed_path.name+'.task9-tmp')
        temporary.write_text(json.dumps(seed,indent=2)+'\n')
        os.replace(temporary,seed_path)
    assert_owned_target_unchanged(seed,app_number,target)
    result={'runId':seed['runId'],'identities':len(tokens),'delivered':len(mapping),
            'resultOrders':len(mapping),'appId':target['id'],'rabbitId':target['rabbitId'],
            'requestsSha256':hashlib.sha256(journal.read_bytes()).hexdigest() if journal.exists() else None,
            'pollsSha256':hashlib.sha256(polls.read_bytes()).hexdigest() if polls.exists() else None,
            'seedSha256':hashlib.sha256(seed_path.read_bytes()).hexdigest(),
            'scope':'precreate; excluded from warmup and formal denominators'}
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--seed',required=True);parser.add_argument('--out',required=True)
    parser.add_argument('--app',type=int,choices=(1,2),default=1)
    args=parser.parse_args()
    print(json.dumps(prepare(args.seed,args.out,args.app)))
