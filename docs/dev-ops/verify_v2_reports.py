#!/usr/bin/env python3
"""验收本次生成的 Surefire XML；零测试、跳过、缺失必需类全部失败。"""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

REQUIRED={'MigrationRehearsalV2Test','FailureMatrixV2Test','ReconciliationV2Test',
          'CapacityEvidenceV2Test','PaymentIntegrationV2Test','RaffleIntegrationV2Test',
          'SignInAtomicV2Test','ExchangeAtomicV2Test','PublisherConfirmV2Test'}

def verify(root,required=REQUIRED):
    suites=[]
    for path in sorted(Path(root).glob('**/surefire-reports/TEST-*.xml')):
        r=ET.parse(path).getroot()
        suites.append({'name':r.get('name'),'tests':int(r.get('tests','0')),
                       'failures':int(r.get('failures','0')),'errors':int(r.get('errors','0')),
                       'skipped':int(r.get('skipped','0')),
                       'methods':[c.get('name') for c in r.findall('testcase')]})
    names={s['name'].split('.')[-1] for s in suites}
    problems=[]
    if not suites: problems.append('no XML/test execution')
    for s in suites:
        if s['tests']==0 or s['failures'] or s['errors'] or s['skipped'] or len(s['methods'])!=s['tests']:
            problems.append('non-green suite: '+s['name'])
    if required-names: problems.append('missing suites: '+','.join(sorted(required-names)))
    return {'passed':not problems,'problems':problems,'tests':sum(s['tests'] for s in suites),'suites':suites}

if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('--root',default='.');p.add_argument('--out',required=True);a=p.parse_args()
    report=verify(a.root);Path(a.out).write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='suites'}));raise SystemExit(0 if report['passed'] else 1)
