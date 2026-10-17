#!/usr/bin/env python3
"""从每请求与独立终态记录统计；缺证据即不认定安全容量。时间戳为UTC epoch秒。"""
import argparse
import json
import math
from pathlib import Path
from seed_v2_testdata import route

SCOPES={'CREDIT','QUOTA','STOCK','DELIVERY','PAYMENT','SIGNIN'}
TERMINAL={'DELIVERED','CANCELLED'}

def percentile(values):
    values=sorted(values)
    return {f'p{p}':values[max(0,math.ceil(len(values)*p/100)-1)] if values else None for p in (50,95,99)}

def backlog_evidence(meta):
    start=meta.get('loadStartedAt');end=meta.get('loadEndedAt')
    if not isinstance(start,(int,float)) or not isinstance(end,(int,float)) or end<=start:return False
    samples=[r for r in meta.get('backlogSamples',[]) if start<=r.get('observedAt',-1)<=end]
    samples.sort(key=lambda r:r['observedAt'])
    if len(samples)<3 or samples[-1]['observedAt']-samples[0]['observedAt']<(end-start)*.6:return False
    width=max(1,len(samples)//3)
    if any('domains' in r for r in samples):
        for domain in ('ORDER','OUTBOX','RETRY','BROKER'):
            values=[]
            for sample in samples:
                item=sample.get('domains',{}).get(domain,{})
                count=item.get('count')
                if item.get('valid') is not True or not isinstance(count,int) or count<0:return False
                values.append(count)
            before=sum(values[:width])/width
            after=sum(values[-width:])/width
            if after>before+max(2,before*.05):return False
        return True
    if any(not {'ORDER','OUTBOX','RETRY','BROKER'}<=set(r.get('backlogScopes',[])) for r in samples):return False
    before=sum(r['backlog'] for r in samples[:width])/width
    after=sum(r['backlog'] for r in samples[-width:])/width
    return after<=before+max(2,before*.05)


def repeat_evidence(meta):
    paths=meta.get('repeatRunDirs',[])
    if len(paths)<3 or len({str(Path(p).resolve()) for p in paths})!=len(paths):return False
    ids=set();fingerprint=meta.get('configurationSha256')
    if not isinstance(fingerprint,str) or len(fingerprint)!=64:return False
    try:
        for directory in paths:
            root=Path(directory);execution=json.loads((root/'execution.json').read_text())
            other=json.loads((root/'metadata.json').read_text())
            if execution.get('runId') in ids or not execution.get('runId'):return False
            ids.add(execution['runId'])
            if execution.get('k6Exit')!=0 or execution.get('trackerExit')!=0 or execution.get('samplerExit')!=0:return False
            if other.get('configurationSha256')!=fingerprint or other.get('arrivalRate')!=meta.get('arrivalRate'):return False
            if execution.get('loadEndedAt',0)-execution.get('startedAt',0)<600:return False
            # Recompute from raw records and measured resource window; a results boolean is not proof.
            other['backlogSamples']=read_jsonl(root/'resources.jsonl')
            other['loadStartedAt']=execution['startedAt'];other['loadEndedAt']=execution['loadEndedAt']
            result=collect(read_jsonl(root/'requests.jsonl'),read_jsonl(root/'completions.jsonl'),other,check_repeats=False)
            if not result['singleRunPassed']:return False
    except (OSError,ValueError,KeyError,TypeError):return False
    return len(ids)>=3


def collect(requests,completions,meta,check_repeats=True):
    # Same request retry is not additional accepted business. sent remains actual network attempts.
    requests=[r for r in requests if r.get('phase','formal')=='formal']
    accepted={};received=rejected=overload=business=unknown=unconfirmed=0
    http503_with_order=http503_without_order=0;uncertain_orders=set()
    users_by_operation={};requests_by_database={}
    query_latency=[];trade_latency=[];invalid_latency=0
    for r in requests:
        status=int(r.get('status',0))
        operation=r.get('operation')
        user=r.get('user')
        if operation and user:
            users_by_operation.setdefault(operation,set()).add(user)
            db,_=route(user)
            key='db%02d'%db
            requests_by_database[key]=requests_by_database.get(key,0)+1
        if status==0: unknown+=1;continue
        received+=1
        if status==503:
            if r.get('orderId'):
                http503_with_order+=1;uncertain_orders.add(str(r['orderId']))
            else:http503_without_order+=1
        if status>=500: unconfirmed+=1
        rejected_state=r.get('state')=='REJECTED'
        contracts={'draw':{'ACCEPTED','CANDIDATE_FIXED','RESULT_RECORDED','COMMITTED','DELIVERED','CANCELLING','CANCELLED','REJECTED'},'payment':{'CREATED','PAID','DELIVERED'},'exchange':{'DELIVERED','REJECTED'},'signin':{'DELIVERED','REJECTED'}}
        invalid_contract=200<=status<300 and operation in contracts and (status not in (200,202) or (not rejected_state and not r.get('orderId')) or r.get('state') not in contracts[operation])
        if invalid_contract or status<200 or 300<=status<400:unconfirmed+=1
        if status==429: overload+=1
        elif rejected_state or 400<=status<500: business+=1
        if status==429 or rejected_state or 400<=status<500: rejected+=1
        if 200<=status<300 and not rejected_state and not invalid_contract and r.get('orderId') and r.get('operation') not in ('query','result'):
            accepted.setdefault(str(r['orderId']),r)
        latency=r.get('latencyMs')
        if isinstance(latency,(int,float)) and math.isfinite(latency) and latency>=0:
            (query_latency if r.get('operation') in ('query','result') else trade_latency).append(latency)
        else: invalid_latency+=1
    terminal={};orphans=set();completion_latency=[];confirmation_latency=[];draw_delivery_latency=[];observed_delay=[];missing_time=0
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
        # Same DB stage times do not require client clock calibration.
        if accepted[order].get('operation')=='draw' and isinstance(start,(int,float)):
            confirmed=c.get('serverConfirmedAt')
            if isinstance(confirmed,(int,float)) and confirmed>=start:
                confirmation_latency.append((confirmed-start)*1000)
            if isinstance(end,(int,float)) and end>=start:
                draw_delivery_latency.append((end-start)*1000)
    failures=[]
    def require(condition,reason):
        if not condition: failures.append(reason)
    q=percentile(query_latency);t=percentile(trade_latency);c=percentile(completion_latency)
    pending=len(accepted)-len(terminal)
    require(meta.get('durationSeconds',0)>=600,'duration evidence incomplete')
    expected=meta.get('arrivalRate',0)*meta.get('durationSeconds',0)
    require(expected>0 and len(requests)>=math.floor(expected*0.99),'offered arrival rate not achieved')
    require(meta.get('manifestComplete') is True,'artifact/config/resource manifest incomplete')
    require(calibrated,'clock calibration missing/out of bounds')
    require(meta.get('loadGeneratorHealthy') is True and meta.get('droppedIterations')==0,'load generator missed arrivals/unverified')
    require(meta.get('resourceBudgetOk') is True,'resource budget unverified/exceeded')
    require(SCOPES<=set(meta.get('checkedScopes',[])) and meta.get('differences')==0,'six-scope zero-difference evidence missing')
    require(meta.get('backlogEnd') is not None and meta.get('backlogStart') is not None and meta['backlogEnd']<=meta['backlogStart'],'backlog growing/unverified')
    require(backlog_evidence(meta),'load-period backlog growth/unmeasured')
    require(isinstance(meta.get('processingPerSecond'),(int,float)) and isinstance(meta.get('incomingPerSecond'),(int,float)) and meta['processingPerSecond']>meta['incomingPerSecond'],'recovery processing does not exceed new arrivals')
    require(pending==0 and unknown==0 and unconfirmed==0 and not orphans,'unfinished/unknown/orphan results')
    require(rejected/len(requests)<=meta.get('maxRejectionRatio',0) if requests else False,'rejection ratio exceeds declared ceiling')
    require(invalid_latency==0 and missing_time==0,'invalid/missing latency evidence')
    require(q['p99'] is None or q['p99']<=200,'query P99 exceeds 200ms')
    require(t['p99'] is None or t['p99']<=500,'acceptance P99 exceeds 500ms')
    async_orders=any(r.get('operation') in ('draw','payment') for r in accepted.values())
    require(not async_orders or c['p99'] is not None and c['p99']<=5000,'completion P99 exceeds 5s/unmeasured')
    processing=meta.get('processingPerSecond');incoming=meta.get('incomingPerSecond')
    rates_known=isinstance(processing,(int,float)) and isinstance(incoming,(int,float))
    estimate=meta.get('backlogEnd',0)/(processing-incoming) if rates_known and processing>incoming and meta.get('backlogEnd') is not None else None
    single_run_passed=not failures
    draw_orders=[oid for oid,r in accepted.items() if r.get('operation')=='draw']
    draw_stages_complete=all(oid in terminal and isinstance(terminal[oid].get('serverConfirmedAt'),(int,float))
                             and isinstance(terminal[oid].get('serverCompletedAt'),(int,float)) for oid in draw_orders)
    smoke_data_complete=(bool(requests) and pending==0 and unknown==0 and unconfirmed==0 and not orphans
                         and invalid_latency==0 and missing_time==0 and draw_stages_complete)
    execution=meta.get('execution') or {}
    reconciliation=meta.get('reconciliation') or {}
    smoke_failures=[]
    def smoke_require(condition,reason):
        if not condition:smoke_failures.append(reason)
    smoke_require(smoke_data_complete,'formal accepted ID terminal/stage evidence incomplete')
    smoke_require(meta.get('durationSeconds',0)>=120 and expected>0
                  and len(requests)>=math.floor(expected*.99),'120s offered arrival evidence incomplete')
    smoke_require(execution.get('k6Exit')==0 and execution.get('trackerExit')==0
                  and execution.get('resourceIdentityStable') is True,'load/tracker exit or owned resource identity unverified')
    smoke_require(meta.get('droppedIterations')==0 and meta.get('resourceBudgetOk') is True,
                  'dropped iterations or resource budget unverified')
    smoke_require(calibrated,'clock calibration missing/out of bounds')
    smoke_require(backlog_evidence(meta),'four-domain load-period backlog growth/unmeasured')
    smoke_require(reconciliation.get('passed') is True and all(reconciliation.get(key)==0 for key in
                  ('totalDifferences','pendingTasks','unfinishedOrders','unresolvedReviews')),
                  'post-drain reconciliation incomplete')
    smoke_require(rejected==0,'formal business/overload rejection observed')
    smoke_require(q['p99'] is not None and q['p99']<=200 and t['p99'] is not None and t['p99']<=500,
                  'query/acceptance P99 missing or above smoke limits')
    smoke_require(not async_orders or c['p99'] is not None and c['p99']<=5000,
                  'async completion P99 missing or above smoke limit')
    if check_repeats:require(repeat_evidence(meta),'three actual matching successful runs required')
    return {'singleRunPassed':single_run_passed,'sent':len(requests),'received':received,'accepted':len(accepted),'completed':len(terminal),
            'rejected':rejected,'unconfirmed':unconfirmed,'overloadRejected':overload,'businessRejected':business,'unknown':unknown,
            'pending':pending,'orphanCompletions':len(orphans),'queryLatencyMs':q,'acceptanceLatencyMs':t,
            'completionLatencyMs':c,'pollObservationDelayMs':percentile(observed_delay),
            'serverDrawConfirmationLatencyMs':percentile(confirmation_latency),
            'serverDrawDeliveryLatencyMs':percentile(draw_delivery_latency),
            'http503WithOrderId':http503_with_order,'http503WithoutOrderId':http503_without_order,
            'uncertainOrderIds':sorted(uncertain_orders),'unknownAdmission':unknown+http503_with_order+http503_without_order,
            'userCountsByOperation':{op:len(users) for op,users in sorted(users_by_operation.items())},
            'requestsByDatabase':requests_by_database,
            'transportRetries':meta.get('transportRetries'),'redriveEmitted':meta.get('redriveEmitted'),
            'redriveSuppressed':meta.get('redriveSuppressed'),'smokeDataComplete':smoke_data_complete,
            'smokeTechnicalPassed':not smoke_failures,'smokeFailures':smoke_failures,
            'fullCapacityPassed':False,'releaseAuthorized':False,
            'oldestAge':meta.get('oldestAgeSeconds'),'drainSeconds':meta.get('drainSeconds'),
            'estimatedDrainSeconds':estimate,'differences':meta.get('differences'),
            'checkedScopes':meta.get('checkedScopes',[]),'droppedIterations':meta.get('droppedIterations'),
            'safeCapacityCandidate':not failures,'gateFailures':failures}

def read_jsonl(path):
    return [json.loads(line) for line in Path(path).read_text().splitlines() if line.strip()]

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--run-dir',required=True);a=p.parse_args();root=Path(a.run_dir)
    meta=json.loads((root/'metadata.json').read_text())
    execution=json.loads((root/'execution.json').read_text())
    sample_files=meta.get('task8SampleFiles')
    if sample_files is not None:
        samples=[json.loads(Path(path).read_text()) for path in sample_files]
    elif 'backlogSamples' in meta:
        samples=meta['backlogSamples']
    elif (root/'resources.jsonl').exists():
        samples=read_jsonl(root/'resources.jsonl')
    else:
        samples=[]
    meta.update(backlogSamples=samples,loadStartedAt=execution['startedAt'],loadEndedAt=execution['loadEndedAt'],execution=execution)
    if 'clockSkewMs' not in meta:meta['clockSkewMs']=execution.get('clockBefore',{}).get('skewMs')
    if not execution.get('collectionComplete'):meta['manifestComplete']=False
    result=collect(read_jsonl(root/'requests.jsonl'),read_jsonl(root/'completions.jsonl'),meta)
    (root/'results.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
    # A report records failed capacity without pretending script execution failed.
