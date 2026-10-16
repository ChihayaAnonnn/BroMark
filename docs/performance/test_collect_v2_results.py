import importlib.util
from pathlib import Path
import tempfile
import json
import unittest
s=importlib.util.spec_from_file_location('collect',Path(__file__).with_name('collect_v2_results.py'))
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

class CapacityStatistics(unittest.TestCase):
    def fixture(self):
        requests=[{'requestId':str(i),'operation':'draw','status':202 if i<600 else 429,
                   'orderId':str(i) if i<600 else None,'latencyMs':10,'state':'ACCEPTED'} for i in range(1000)]
        completions=[{'orderId':str(i),'state':'DELIVERED','kind':'draw','serverAcceptedAt':100,
                      'serverCompletedAt':101,'observedAt':110} for i in range(550)]
        metadata={'durationSeconds':600,'repeats':3,'clockSkewMs':1,'clockMaxSkewMs':10,
                  'loadStartedAt':100,'loadEndedAt':700,'backlogSamples':[{'observedAt':100,'backlog':0,'backlogScopes':['ORDER','OUTBOX','RETRY','BROKER']},{'observedAt':400,'backlog':0,'backlogScopes':['ORDER','OUTBOX','RETRY','BROKER']},{'observedAt':700,'backlog':0,'backlogScopes':['ORDER','OUTBOX','RETRY','BROKER']}],
                  'arrivalRate':100,'loadGeneratorHealthy':True,'resourceBudgetOk':True,
                  'backlogStart':0,'backlogEnd':50,'oldestAgeSeconds':10,'incomingPerSecond':20,
                  'processingPerSecond':10,'drainSeconds':None,
                  'checkedScopes':['CREDIT','QUOTA','STOCK','DELIVERY','PAYMENT','SIGNIN'],
                  'differences':0,'droppedIterations':0,'manifestComplete':True}
        return requests,completions,metadata
    def test_acceptance_is_not_completion_and_growing_backlog_is_unsafe(self):
        r,c,meta=self.fixture();result=m.collect(r,c,meta)
        self.assertEqual((1000,1000,600,400,550),(result['sent'],result['received'],result['accepted'],result['rejected'],result['completed']))
        self.assertEqual(50,result['pending']);self.assertFalse(result['safeCapacityCandidate'])
        self.assertEqual(1000,result['completionLatencyMs']['p99'])
        self.assertIsNone(result['estimatedDrainSeconds'])
    def test_duplicates_and_foreign_completions_do_not_inflate(self):
        r,c,meta=self.fixture(); c+=c[:3]+[{'orderId':'foreign','state':'DELIVERED','kind':'draw'}]
        result=m.collect(r,c,meta);self.assertEqual(550,result['completed'])
        self.assertEqual(1,result['orphanCompletions'])
    def test_network_unknown_and_business_rejection_separate(self):
        r,c,meta=self.fixture();r[999]['status']=0;r[998]['status']=200;r[998]['state']='REJECTED'
        result=m.collect(r,c,meta);self.assertEqual(999,result['received']);self.assertEqual(1,result['unknown'])
        self.assertEqual(398,result['overloadRejected']);self.assertEqual(1,result['businessRejected'])
    def test_missing_scope_or_clock_evidence_fails_closed(self):
        r,c,meta=self.fixture();meta.pop('clockSkewMs');meta['checkedScopes']=[]
        result=m.collect(r,c,meta);self.assertFalse(result['safeCapacityCandidate'])
        self.assertIsNone(result['completionLatencyMs']['p99'])
    def test_complete_bounded_zero_difference_run_can_be_candidate(self):
        r,c,meta=self.fixture();r=r[:550];meta.update(backlogEnd=0,processingPerSecond=30,
              incomingPerSecond=20,drainSeconds=0,arrivalRate=550/600)
        result=m.collect(r,c,meta);self.assertTrue(result['singleRunPassed'],result['gateFailures']);self.assertFalse(result['safeCapacityCandidate'])
    def test_missing_terminal_timestamp_cannot_claim_latency(self):
        r,c,meta=self.fixture();r=r[:550];c[0].pop('serverCompletedAt');meta['backlogEnd']=0
        self.assertFalse(m.collect(r,c,meta)['safeCapacityCandidate'])
    def test_negative_or_nan_latency_does_not_pass(self):
        r,c,meta=self.fixture();r=r[:550];r[0]['latencyMs']=float('nan');meta['backlogEnd']=0
        self.assertFalse(m.collect(r,c,meta)['safeCapacityCandidate'])

if __name__=='__main__':unittest.main()

class EvidenceIntegrity(unittest.TestCase):
    def test_claimed_arrival_rate_without_enough_sent_requests_is_unsafe(self):
        r,c,meta=CapacityStatistics().fixture();r=r[:550];meta.update(backlogEnd=0,processingPerSecond=30)
        result=m.collect(r,c,meta)
        self.assertFalse(result['safeCapacityCandidate'])
        self.assertIn('offered arrival rate not achieved',result['gateFailures'])
    def test_server_error_does_not_mean_proven_business_rejection(self):
        result=m.collect([{'status':503,'operation':'draw','latencyMs':1}],[],{})
        self.assertEqual(1,result['unconfirmed'])
