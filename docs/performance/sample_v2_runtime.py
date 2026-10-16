#!/usr/bin/env python3
"""采集实际隔离进程资源与任务年龄，原始JSONL不进入git。"""
import argparse
import json
import subprocess
from pathlib import Path
import time
from seed_v2_testdata import mysql


def sample(seed):
    backlog=0;oldest=0;connections={};row={'observedAt':time.time()}
    for db in seed['databases']:
        values=mysql(db,"SELECT COUNT(*),COALESCE(MAX(TIMESTAMPDIFF(MICROSECOND,created_at,UTC_TIMESTAMP(6)))/1000000,0) FROM outbox WHERE state IN ('PENDING','PROCESSING')").split('\t')
        backlog+=int(values[0]);oldest=max(oldest,float(values[1]))
        connections[db]=int(mysql(db,"SELECT COUNT(*) FROM information_schema.processlist WHERE DB="+"'"+db+"'"))
    row.update(backlog=backlog,oldestAgeSeconds=oldest,physicalDatabaseConnections=sum(connections.values()),logicalConnections=connections)
    names=['bromark-phase6-app-1','bromark-phase6-app-2','bromark-v2-mysql','bromark-v2-rabbit']
    stats=subprocess.run(['docker','stats','--no-stream','--format','{{json .}}']+names,text=True,capture_output=True)
    row['containers']=[json.loads(line) for line in stats.stdout.splitlines() if line.strip()]
    row['resourceSampleComplete']=stats.returncode==0 and len(row['containers'])==len(names)
    return row

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seed',required=True);p.add_argument('--out',required=True)
    p.add_argument('--seconds',type=float,default=900);p.add_argument('--interval',type=float,default=5);a=p.parse_args()
    seed=json.loads(Path(a.seed).read_text());end=time.monotonic()+a.seconds
    with Path(a.out).open('a') as f:
        while time.monotonic()<end:
            f.write(json.dumps(sample(seed))+'\n');f.flush();time.sleep(a.interval)
