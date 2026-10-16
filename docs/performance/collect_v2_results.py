#!/usr/bin/env python3
"""从每请求与独立终态记录统计；缺证据即不认定安全容量。时间戳为UTC epoch秒。"""
import argparse
import json
import math
from pathlib import Path

SCOPES={'CREDIT','QUOTA','STOCK','DELIVERY','PAYMENT','SIGNIN'}
TERMINAL={'DELIVERED','CANCELLED'}

def percentile(values):
    values=sorted(values)
    return {f'p{p}':values[max(0,math.ceil(len(values)*p/100)-1)] if values else None for p in (50,95,99)}

def collect(requests,completions,meta):
    # Same request retry is not additional accepted business. sent remains actual network attempts.
    accepted={};received=rejected=overload=business=unknown=unconfirmed=0
    query_latency=[];trade_latency=[];invalid_latency=0
    for r in requests:
        status=int(r.get('status',0))
        if status==0: unknown+=1;continue
        received+=1
        if status>=500: unconfirmed+=1
        rejected_state=r.get('state')=='REJECTED'
        if status==429: overload+=1
        elif rejected_state or 400<=status<500: business+=1
        if status==429 or rejected_state or status>=400: rejected+=1
        if 200<=status<300 and not rejected_state and r.get('orderId') and r.get('operation') not in ('query','result'):
            accepted.setdefault(str(r['orderId']),r)
        latency=r.get('latencyMs')
        if isinstance(latency,(int,float)) and math.isfinite(latency) and latency>=0:
            (query_latency if r.get('operation') in ('query','result') else trade_latency).append(latency)
        else: invalid_latency+=1
    terminal={};orphans=set();completion_latency=[];observed_delay=[];missing_time=0
    calibrated=isinstance(meta.get('clockSkewMs'),(int,float)) and abs(meta['clockSkewMs'])<=meta.get('clockMaxSkewMs',10)
    for c in completions:
        order=str(c.get('orderId',''))
        if order not in accepted: orphans.add(order);continue
        if c.get('state') in TERMINAL: terminal.setdefault(order,c)
    # Synchronous exchange/signin completed response is already a terminal receipt.
    for order,r in accepted.items():
        if r.get('state')=='DELIVERED' and r.get('operation') in ('exchange','signin'):
            terminal.setdefault(order,dict(r,kind=r['operation']))
    for order,c in terminal.items():
        if accepted[order].get('operation') in ('exchange','signin'): continue
        start=c.get('serverAcceptedAt');end=c.get('serverCompletedAt')
        if calibrated and isinstance(start,(int,float)) and isinstance(end,(int,float)) and end>=start:
            completion_latency.append((end-start)*1000)
            if isinstance(c.get('observedAt'),(int,float)): observed_delay.append(max(0,c['observedAt']-end)*1000)
        else: missing_time+=1
    failures=[]
    def require(condition,reason):
        if not condition: failures.append(reason)
    q=percentile(query_latency);t=percentile(trade_latency);c=percentile(completion_latency)
    pending=len(accepted)-len(terminal)
    require(meta.get('durationSeconds',0)>=600 and meta.get('repeats',0)>=3,'duration/repeat evidence incomplete')
    expected=meta.get('arrivalRate',0)*meta.get('durationSeconds',0)
    require(expected>0 and len(requests)>=math.floor(expected*0.99),'offered arrival rate not achieved')
    require(meta.get('manifestComplete') is True,'artifact/config/resource manifest incomplete')
    require(calibrated,'clock calibration missing/out of bounds')
    require(meta.get('loadGeneratorHealthy') is True and meta.get('droppedIterations')==0,'load generator missed arrivals/unverified')
    require(meta.get('resourceBudgetOk') is True,'resource budget unverified/exceeded')
    require(SCOPES<=set(meta.get('checkedScopes',[])) and meta.get('differences')==0,'six-scope zero-difference evidence missing')
    require(meta.get('backlogEnd') is not None and meta.get('backlogStart') is not None and meta['backlogEnd']<=meta['backlogStart'],'backlog growing/unverified')
    require(meta.get('processingPerSecond',0)>meta.get('incomingPerSecond',float('inf')),'recovery processing does not exceed new arrivals')
    require(pending==0 and unknown==0 and unconfirmed==0 and not orphans,'unfinished/unknown/orphan results')
    require(rejected/len(requests)<=meta.get('maxRejectionRatio',0) if requests else False,'rejection ratio exceeds declared ceiling')
    require(invalid_latency==0 and missing_time==0,'invalid/missing latency evidence')
    require(q['p99'] is None or q['p99']<=200,'query P99 exceeds 200ms')
    require(t['p99'] is None or t['p99']<=500,'acceptance P99 exceeds 500ms')
    async_orders=any(r.get('operation') in ('draw','payment') for r in accepted.values())
    require(not async_orders or c['p99'] is not None and c['p99']<=5000,'completion P99 exceeds 5s/unmeasured')
    processing=meta.get('processingPerSecond',0);incoming=meta.get('incomingPerSecond',0)
    estimate=meta.get('backlogEnd',0)/(processing-incoming) if processing>incoming and meta.get('backlogEnd') is not None else None
    return {'sent':len(requests),'received':received,'accepted':len(accepted),'completed':len(terminal),
            'rejected':rejected,'unconfirmed':unconfirmed,'overloadRejected':overload,'businessRejected':business,'unknown':unknown,
            'pending':pending,'orphanCompletions':len(orphans),'queryLatencyMs':q,'acceptanceLatencyMs':t,
            'completionLatencyMs':c,'pollObservationDelayMs':percentile(observed_delay),
            'oldestAge':meta.get('oldestAgeSeconds'),'drainSeconds':meta.get('drainSeconds'),
            'estimatedDrainSeconds':estimate,'differences':meta.get('differences'),
            'checkedScopes':meta.get('checkedScopes',[]),'droppedIterations':meta.get('droppedIterations'),
            'safeCapacityCandidate':not failures,'gateFailures':failures}

def read_jsonl(path):
    return [json.loads(line) for line in Path(path).read_text().splitlines() if line.strip()]

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--run-dir',required=True);a=p.parse_args();root=Path(a.run_dir)
    result=collect(read_jsonl(root/'requests.jsonl'),read_jsonl(root/'completions.jsonl'),json.loads((root/'metadata.json').read_text()))
    (root/'results.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
    # A report records failed capacity without pretending script execution failed.
