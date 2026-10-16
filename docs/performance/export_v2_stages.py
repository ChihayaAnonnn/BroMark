#!/usr/bin/env python3
"""从隔离数据库导出服务端时间；时钟校准只用于观测延迟，完成耗时两端均取DB。"""
import argparse
import json
from pathlib import Path
import time
from seed_v2_testdata import mysql
from track_v2_orders import records


def export(seed,requests,completions):
    before=time.time();server=float(mysql(seed['databases'][0],"SET time_zone='+00:00';SELECT UNIX_TIMESTAMP(UTC_TIMESTAMP(6))"));after=time.time()
    stages={}
    for index,db in enumerate(seed['databases']):
        table='payment_stage' if index==0 else 'v2_raffle_stage'
        for line in mysql(db,"SET time_zone='+00:00';SELECT order_id,stage,UNIX_TIMESTAMP(recorded_at) FROM "+table).splitlines():
            order,stage,timestamp=line.split('\t');stages.setdefault(order,{})[stage]=float(timestamp)
    output=[]
    for record in records(completions):
        stamps=stages.get(record['orderId'],{})
        start=stamps.get('PAID') if record['kind']=='payment' else stamps.get('ACCEPTED')
        end=stamps.get('DELIVERED')
        output.append(dict(record,serverAcceptedAt=start,serverCompletedAt=end))
    return output,{'clockSkewMs':(server-(before+after)/2)*1000,'clockCalibrationUncertaintyMs':(after-before)*500,
                   'clockSamples':{'clientBefore':before,'server':server,'clientAfter':after}}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seed',required=True);p.add_argument('--requests',required=True)
    p.add_argument('--completions',required=True);p.add_argument('--out',required=True);a=p.parse_args()
    result,clock=export(json.loads(Path(a.seed).read_text()),a.requests,a.completions)
    Path(a.out).write_text(''.join(json.dumps(r)+'\n' for r in result));print(json.dumps(clock,indent=2))
