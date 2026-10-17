#!/usr/bin/env python3
"""从隔离数据库导出服务端时间；时钟校准只用于观测延迟，完成耗时两端均取DB。"""
import argparse
import json
from pathlib import Path
import time
from seed_v2_testdata import mysql, route, sql
from track_v2_orders import records


def export(seed,requests,completions):
    before=time.time();server=float(mysql(seed['databases'][0],"SET time_zone='+00:00';SELECT UNIX_TIMESTAMP(UTC_TIMESTAMP(6))"));after=time.time()
    candidates={}
    for row in records(requests):
        if row.get('phase','formal')!='formal' or row.get('operation') not in ('draw','payment'):
            continue
        if not row.get('orderId') or not (200<=row.get('status',0)<300 or row.get('status')==503):
            continue
        candidates.setdefault(str(row['orderId']),row)
    stages={};orders={}
    draw_by_db={};draw_by_shard={};payments=[]
    for order,row in candidates.items():
        if row['operation']=='payment':payments.append(order);continue
        db_index,suffix=route(row['user']);draw_by_db.setdefault(db_index,[]).append(order)
        draw_by_shard.setdefault((db_index,suffix),[]).append(order)
    def ids(values):return ','.join(sql(value) for value in values)
    def timestamp(value):return None if value in ('NULL','') else float(value)
    for db_index,values in draw_by_db.items():
        db=seed['databases'][db_index]
        statement=("SET time_zone='+00:00';SELECT order_id,stage,UNIX_TIMESTAMP(recorded_at) "
                   "FROM v2_raffle_stage WHERE order_id IN ("+ids(values)+")")
        for line in mysql(db,statement).splitlines():
            order,stage,stamp=line.split('\t');stages.setdefault(order,{})[stage]=timestamp(stamp)
    for (db_index,suffix),values in draw_by_shard.items():
        db=seed['databases'][db_index]
        statement=("SET time_zone='+00:00';SELECT order_id,state,UNIX_TIMESTAMP(confirmed_at),"
                   "UNIX_TIMESTAMP(delivered_at) FROM v2_draw_order_%03d WHERE order_id IN ("%suffix+ids(values)+")")
        for line in mysql(db,statement).splitlines():
            order,state,confirmed,delivered=line.split('\t')
            orders[order]={'state':state,'confirmed':timestamp(confirmed),'delivered':timestamp(delivered)}
    if payments:
        control=seed['databases'][0]
        statement=("SET time_zone='+00:00';SELECT order_id,stage,UNIX_TIMESTAMP(recorded_at) "
                   "FROM payment_stage WHERE order_id IN ("+ids(payments)+")")
        for line in mysql(control,statement).splitlines():
            order,stage,stamp=line.split('\t');stages.setdefault(order,{})[stage]=timestamp(stamp)
        statement=("SET time_zone='+00:00';SELECT order_id,state,UNIX_TIMESTAMP(paid_at),"
                   "UNIX_TIMESTAMP(delivered_at) FROM payment_order WHERE order_id IN ("+ids(payments)+")")
        for line in mysql(control,statement).splitlines():
            order,state,paid,delivered=line.split('\t')
            orders[order]={'state':state,'paid':timestamp(paid),'delivered':timestamp(delivered)}
    tracked={str(row['orderId']):row for row in records(completions) if row.get('orderId') in candidates}
    output=[]
    for order,request in candidates.items():
        row=orders.get(order,{})
        seen=tracked.get(order,{})
        stamps=stages.get(order,{})
        kind=request['operation']
        start=row.get('paid') or stamps.get('PAID') if kind=='payment' else stamps.get('ACCEPTED')
        output.append({'requestId':request['requestId'],'orderId':order,'kind':kind,
                       'state':row.get('state',seen.get('state')),'observedAt':seen.get('observedAt'),
                       'originalStatus':request['status'],'serverAcceptedAt':start,
                       'serverConfirmedAt':row.get('confirmed') if kind=='draw' else None,
                       'serverCompletedAt':row.get('delivered')})
    return output,{'clockSkewMs':(server-(before+after)/2)*1000,'clockCalibrationUncertaintyMs':(after-before)*500,
                   'clockSamples':{'clientBefore':before,'server':server,'clientAfter':after}}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seed',required=True);p.add_argument('--requests',required=True)
    p.add_argument('--completions',required=True);p.add_argument('--out',required=True);a=p.parse_args()
    result,clock=export(json.loads(Path(a.seed).read_text()),a.requests,a.completions)
    Path(a.out).write_text(''.join(json.dumps(r)+'\n' for r in result));print(json.dumps(clock,indent=2))
