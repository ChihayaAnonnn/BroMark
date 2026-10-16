import importlib.util
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).parents[1]
sys.path.insert(0,str(Path(__file__).parent))
from collect_v2_results import collect
import test_collect_v2_results as fixtures
s=importlib.util.spec_from_file_location('cutover_review',ROOT/'dev-ops/mysql/rehearse_cutover.py')
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

class ReviewRegressions(unittest.TestCase):
    def test_issued_mapping_without_imported_receipt_blocks_cutover(self):
        d={'run_id':'audit','auditor':'test','stopped':True,'drained':True,
           'tables':{'user_credit_account':[{'user_id':'u','balance':'12.00','opening_balance':'12.00','status':'ACTIVE','version':0,'audit_source':'approved'}]},
           'identities':[{'entry':'award:u','legacy_id':'17','biz_type':'LEGACY_AWARD','biz_id':'award-17','request_hash':'a'*64,'evidence':'issued'}]}
        self.assertFalse(m.precheck(d)['cutover_allowed'])
    def test_cent_comparison_never_uses_binary_float(self):
        self.assertNotEqual(m.canonical_cell('balance','9007199254740992.01'),m.canonical_cell('balance','9007199254740992.02'))
        self.assertNotEqual(m.canonical_cell('user_id','001'),m.canonical_cell('user_id','1'))
    def test_http202_without_stable_order_identity_is_unconfirmed(self):
        _,_,meta=fixtures.CapacityStatistics().fixture();meta.update(arrivalRate=1/600,backlogEnd=0,processingPerSecond=30)
        r=collect([{'operation':'draw','status':202,'latencyMs':1,'state':'ACCEPTED'}],[],meta)
        self.assertEqual(1,r['unconfirmed']);self.assertFalse(r['safeCapacityCandidate'])
    def test_load_period_growth_cannot_be_hidden_by_post_stop_drain(self):
        r,c,meta=fixtures.CapacityStatistics().fixture();r=r[:550];meta.update(arrivalRate=550/600,backlogEnd=0,processingPerSecond=30,
              loadStartedAt=100,loadEndedAt=700,backlogSamples=[{'observedAt':100,'backlog':0},{'observedAt':400,'backlog':50},{'observedAt':700,'backlog':100},{'observedAt':800,'backlog':0}])
        self.assertFalse(collect(r,c,meta)['safeCapacityCandidate'])
    def test_repeats_scalar_cannot_substitute_three_actual_runs(self):
        r,c,meta=fixtures.CapacityStatistics().fixture();r=r[:550];meta.update(arrivalRate=550/600,backlogEnd=0,processingPerSecond=30)
        self.assertFalse(collect(r,c,meta)['safeCapacityCandidate'])
    def test_cancelled_order_without_original_bucket_refund_is_mismatch(self):
        from reconcile_v2_run import draw_quota_mismatch
        order={'order_id':'dr','user_id':'u','activity_id':12,'state':'CANCELLED',
               'snapshot':{'time':{'businessDate':'2026-10-16','businessMonth':'2026-10'}}}
        q=[{'user_id':'u','activity_id':12,'biz_id':'dr','biz_type':'CONSUME:DRAW','bucket_type':k,'bucket_key':v,'delta':-1} for k,v in [('TOTAL','ALL'),('DAY','2026-10-16'),('MONTH','2026-10')]]
        self.assertTrue(draw_quota_mismatch(order,q))

class CollectorProcessRegressions(unittest.TestCase):
    def test_tracker_survives_real_http_disconnect_and_reports_it(self):
        import tempfile,os,threading
        from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
        from unittest.mock import patch
        from track_v2_orders import track,records
        class Handler(BaseHTTPRequestHandler):
            calls=0
            def do_GET(self):
                Handler.calls+=1
                if Handler.calls==1:self.close_connection=True;return
                self.send_response(200);self.end_headers();self.wfile.write(b'{"state":"DELIVERED"}')
            def log_message(self,*args):pass
        server=ThreadingHTTPServer(('127.0.0.1',0),Handler)
        thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        try:
            with tempfile.TemporaryDirectory() as directory:
                root=Path(directory);(root/'tokens.json').write_text('{"u":"synthetic"}')
                (root/'requests.jsonl').write_text('{"requestId":"r","user":"u","operation":"draw","status":202,"orderId":"o"}\n')
                with patch.dict(os.environ,V2_TOKEN_FILE=str(root/'tokens.json')):
                    report=track(root/'requests.jsonl','http://127.0.0.1:'+str(server.server_port),root/'tracked.jsonl',.25,.01,100)
                self.assertEqual(1,report['observedTerminals']);self.assertGreaterEqual(report['pollErrors'],1)
        finally:server.shutdown();server.server_close();thread.join()
    def test_failed_sampler_exit_is_not_a_complete_experiment(self):
        import subprocess
        from run_v2_experiment import wait_collectors
        tracker=subprocess.Popen([sys.executable,'-c','raise SystemExit(0)'])
        sampler=subprocess.Popen([sys.executable,'-c','raise SystemExit(3)'])
        result=wait_collectors(tracker,sampler,5)
        self.assertEqual(3,result['samplerExit']);self.assertFalse(result['collectionComplete'])

class RepeatedRawEvidence(unittest.TestCase):
    def test_three_distinct_raw_runs_are_verified_and_duplicate_run_is_rejected(self):
        import tempfile
        from collect_v2_results import repeat_evidence
        r,c,meta=fixtures.CapacityStatistics().fixture();r=r[:550]
        meta.update(arrivalRate=550/600,backlogEnd=0,processingPerSecond=30,configurationSha256='a'*64)
        with tempfile.TemporaryDirectory() as directory:
            paths=[]
            for n in range(3):
                root=Path(directory)/str(n);root.mkdir();paths.append(str(root))
                (root/'metadata.json').write_text(json.dumps(meta))
                (root/'execution.json').write_text(json.dumps({'runId':str(n),'k6Exit':0,'trackerExit':0,'samplerExit':0,'startedAt':100,'loadEndedAt':700}))
                for filename,records in [('requests.jsonl',r),('completions.jsonl',c),('resources.jsonl',meta['backlogSamples'])]:
                    (root/filename).write_text(''.join(json.dumps(row)+'\n' for row in records))
            meta['repeatRunDirs']=paths
            self.assertTrue(repeat_evidence(meta))
            meta['repeatRunDirs']=[paths[0]]*3
            self.assertFalse(repeat_evidence(meta))

class CompleteBacklogScope(unittest.TestCase):
    def test_outbox_only_cannot_prove_non_growing_business_backlog(self):
        from collect_v2_results import backlog_evidence
        self.assertFalse(backlog_evidence({'loadStartedAt':100,'loadEndedAt':700,
              'backlogSamples':[{'observedAt':100,'backlog':0},{'observedAt':400,'backlog':0},{'observedAt':700,'backlog':0}]}))

class LegacyImportLayout(unittest.TestCase):
    def test_old_exchange_order_can_be_imported_with_original_request_identity(self):
        self.assertEqual(('user_id','request_id'),m.key_for('credit_exchange_order_001'))
    def test_wrong_logical_database_blocks_audited_account_import(self):
        import importlib.util
        spec=importlib.util.spec_from_file_location('cutover_fixtures',ROOT/'dev-ops/mysql/tests/test_cutover.py')
        fixture=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixture)
        data=fixture.CutoverValidation().fixture();data['identities']=[];data['logical_db']='01'
        self.assertFalse(m.precheck(data)['cutover_allowed'])

class JavaRouteInteroperability(unittest.TestCase):
    def test_supplementary_unicode_uses_java_utf16_hash_route(self):
        from seed_v2_testdata import route
        self.assertEqual('01.000',m.route_slot('\U0001f600'))
        self.assertEqual((1,0),route('\U0001f600'))
