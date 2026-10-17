#!/usr/bin/env python3
"""停写后检查专属实验库六域，遇到缺表/异常不输出通过。不会改账或隐藏核查记录。"""
import argparse
from decimal import Decimal
import json
import os
from pathlib import Path
from seed_v2_testdata import mysql


def count(db,query):return int(mysql(db,query))
def rows(db,table):
    return [json.loads(line,parse_float=Decimal) for line in mysql(db,'SELECT JSON_OBJECT('+','.join("'"+c+"',`"+c+'`' for c in mysql(db,"SELECT COLUMN_NAME FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='"+table+"' ORDER BY ORDINAL_POSITION").splitlines())+') FROM `'+table+'`').splitlines()]

def draw_quota_mismatch(order,quota):
    entries=[q for q in quota if q.get('user_id')==order['user_id'] and q.get('activity_id')==order['activity_id'] and q.get('biz_id')==order['order_id']]
    consumed=[q for q in entries if q.get('biz_type')=='CONSUME:DRAW']
    refunded=[q for q in entries if q.get('biz_type')=='REFUND:DRAW']
    if order['state']=='REJECTED':return bool(consumed or refunded)
    time=order['snapshot'].get('time',{})
    expected={'TOTAL':'ALL','DAY':time.get('businessDate'),'MONTH':time.get('businessMonth')}
    def valid(rows,delta):
        return len(rows)==3 and {r['bucket_type']:r['bucket_key'] for r in rows}==expected and all(r['delta']==delta for r in rows)
    if not valid(consumed,-1):return True
    if order['state']=='CANCELLED':return not valid(refunded,1)
    return bool(refunded)


def reconcile(seed):
    os.environ['V2_TARGET_RUN_ID']=seed['runId']
    differences={k:0 for k in ('CREDIT','QUOTA','STOCK','DELIVERY','PAYMENT','SIGNIN')};counts={};reviews=0;unfinished=0
    sqls=(Path(__file__).parents[1]/'verification/2026-10-15-assets-signin-exchange/reconciliation.sql').read_text().split(';')
    credit_by_biz={};all_draws=[]
    for db in seed['databases'][1:]:
        checks=[int(v) for statement in sqls if statement.strip() for v in mysql(db,statement).splitlines()]
        if len(checks)!=6:raise ValueError('six asset checks must execute per database')
        differences['CREDIT']+=checks[0];differences['QUOTA']+=sum(checks[1:])
        counts[db]={}
        for suffix in range(4):
            tag=f'{suffix:03d}'
            ledgers=rows(db,'user_credit_ledger_'+tag)
            for l in ledgers:credit_by_biz[(l['user_id'],l['biz_type'],l['biz_id'])]=l
            quota=rows(db,'user_quota_ledger_'+tag)
            orders=rows(db,'v2_draw_order_'+tag);all_draws+=orders
            records={r['order_id']:r for r in rows(db,'v2_award_record_'+tag)}
            delivery={r['order_id']:r for r in rows(db,'v2_award_delivery_'+tag)}
            for order in orders:
                differences['QUOTA']+=int(draw_quota_mismatch(order,quota))
                oid=order['order_id'];snapshot=order['snapshot'];award=snapshot.get('finalAward')
                ledger=credit_by_biz.get((order['user_id'],'DRAW_AWARD',oid))
                if order['state']=='DELIVERED':
                    if oid not in records or oid not in delivery or records[oid]['state']!='DELIVERED':differences['DELIVERY']+=1
                    if award and award['type']=='CREDIT' and (not ledger or str(ledger['delta'])!=str(award['amount'])):
                        # Decimal JSON can carry either number or fixed string.
                        from decimal import Decimal
                        if not ledger or Decimal(str(ledger['delta']))!=Decimal(str(award['amount'])):differences['DELIVERY']+=1
                    if award and award['type']=='NONE' and ledger:differences['DELIVERY']+=1
                elif oid in delivery or ledger:differences['DELIVERY']+=1
                if order['state'] not in ('DELIVERED','CANCELLED','REJECTED'):
                    unfinished+=1
                if order['recovery_status']!='ACTIVE':reviews+=1
            if set(delivery)-{o['order_id'] for o in orders}:differences['DELIVERY']+=len(set(delivery)-{o['order_id'] for o in orders})
            signs=rows(db,'sign_in_order_'+tag);rewards=rows(db,'sign_in_reward_'+tag)
            for sign in signs:
                actual=[r['receipt'] for r in rewards if r['sign_in_order_id']==sign['order_id']]
                expected=sign['receipt']['rewards']
                if sorted(json.dumps(r,sort_keys=True) for r in actual)!=sorted(json.dumps(r,sort_keys=True) for r in expected):differences['SIGNIN']+=1
                for reward in expected:
                    receipt=reward.get('credit')
                    if receipt:
                        biz=receipt['bizId'];l=credit_by_biz.get((sign['user_id'],biz['type'],biz['id']))
                        if not l or l['receipt']!=receipt:differences['SIGNIN']+=1
                    receipt=reward.get('quota')
                    if receipt:
                        biz=receipt['bizId'];q=[q for q in quota if q['user_id']==sign['user_id'] and q['activity_id']==receipt['activityId'] and q['biz_type']=='GRANT:'+biz['type'] and q['biz_id']==biz['id']]
                        if len(q)!=3 or any(q['receipt']!=receipt for q in q):differences['SIGNIN']+=1
            if {r['sign_in_order_id'] for r in rewards}-{s['order_id'] for s in signs}:differences['SIGNIN']+=1
            counts[db][tag]={'draws':len(orders),'signins':len(signs),'creditLedgers':len(ledgers)}
        reviews+=count(db,'SELECT COUNT(*) FROM recovery_case WHERE closed_at IS NULL')
    control=seed['databases'][0]
    for stock in rows(control,'award_stock'):
        pool=stock['stock_pool_id'];logs=[r for r in rows(control,'stock_change_log') if r['stock_pool_id']==pool]
        reservations=[r for r in rows(control,'award_reservation') if r['stock_pool_id']==pool]
        mismatch=stock['total']!=stock['available']+stock['reserved']+stock['allocated']
        for field in ('total','available','reserved','allocated'):
            mismatch |= stock[field]!=sum(r[field+'_delta'] for r in logs)
        mismatch |= stock['reserved']!=sum(r['quantity'] for r in reservations if r['state']=='HELD')
        mismatch |= stock['allocated']!=sum(r['quantity'] for r in reservations if r['state']=='CONFIRMED')
        differences['STOCK']+=int(mismatch)
        if stock['recovery_status']!='ACTIVE':reviews+=1
    payments=rows(control,'payment_order');facts=rows(control,'provider_payment_fact');payment_ids={p['order_id'] for p in payments}
    for p in payments:
        snapshot=p['snapshot'];user=p['user_id'];oid=p['order_id'];l=credit_by_biz.get((user,'CHARGE','charge:'+oid))
        paid=[f for f in facts if f['order_id']==oid and f['paid']]
        mismatch=False
        if p['state']!='CREATED':
            mismatch=len(paid)!=1
            if len(paid)==1:
                f=paid[0];product=snapshot['product']
                from decimal import Decimal
                mismatch |= f['reversed'] or f['user_id']!=user or f['provider']!=snapshot['provider'] or f['account']!=snapshot['providerAccount'] or f['environment']!=snapshot['environment'] or f['currency']!=product['currency'] or Decimal(str(f['amount']))!=Decimal(str(product['amount']))
        if p['state']=='DELIVERED':mismatch |= not l or l['receipt']!=snapshot.get('receipt')
        if p['state']=='CREATED' and l:mismatch=True
        differences['PAYMENT']+=int(mismatch)
        if p['state']!='DELIVERED':unfinished+=1
        if p['recovery_status']!='ACTIVE':reviews+=1
    differences['PAYMENT']+=len({f['order_id'] for f in facts}-payment_ids)
    for (_,kind,biz),l in credit_by_biz.items():
        if kind=='CHARGE' and (not biz.startswith('charge:') or biz[7:] not in payment_ids):differences['PAYMENT']+=1
    reviews+=count(control,'SELECT COUNT(*) FROM recovery_case WHERE closed_at IS NULL')
    pending=0
    for db in seed['databases']:
        pending+=count(db,"SELECT COUNT(*) FROM outbox WHERE state<>'PUBLISHED'")
        pending+=count(db,"SELECT COUNT(*) FROM retry_job WHERE state NOT IN ('PUBLISHED','DONE')")
    return {'checkedScopes':list(differences),'differences':differences,'totalDifferences':sum(differences.values()),
            'unresolvedReviews':reviews,'unfinishedOrders':unfinished,'pendingTasks':pending,'counts':counts,
            'passed':sum(differences.values())==0 and reviews==0 and unfinished==0 and pending==0}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seed',required=True);p.add_argument('--out',required=True)
    p.add_argument('--stopped',action='store_true');a=p.parse_args()
    if not a.stopped:p.error('requires confirmed maintenance window and stopped writers')
    report=reconcile(json.loads(Path(a.seed).read_text()));Path(a.out).write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2));raise SystemExit(0 if report['passed'] else 1)
