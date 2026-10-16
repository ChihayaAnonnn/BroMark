"""只运行显式隔离容器；测试创建专属数据库，不 DROP。"""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import unittest
import uuid

spec=importlib.util.spec_from_file_location('cutover',Path(__file__).parents[1]/'rehearse_cutover.py')
m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

class CutoverIntegration(unittest.TestCase):
    def test_interrupted_ddl_resumes_and_checksum_change_blocks(self):
        self.assertTrue(os.environ.get('V2_MYSQL_PASSWORD'),'isolated dependency required')
        db='router_cutover_it_'+uuid.uuid4().hex[:12]
        subprocess.run(['docker','exec','-e','MYSQL_PWD='+os.environ['V2_MYSQL_PASSWORD'],
                        'bromark-v2-mysql','mysql','-uroot','-e','CREATE DATABASE '+db],check=True,capture_output=True)
        d={'run_id':'ddl_fixture','auditor':'test','stopped':True,'drained':True,
           'tables':{'user_credit_account':[{'user_id':'legacy-u','balance':'12.00','opening_balance':'12.00',
                     'status':'ACTIVE','version':0,'audit_source':'audited original'}],
                     'raffle_activity_account':[{'user_id':'legacy-u','activity_id':12,'total_remaining':7,
                         'day_limit':3,'month_limit':5,'effective_draw_count':2,'version':0,'opening_total':7,
                         'opening_day_limit':3,'opening_month_limit':5,'opening_effective_count':2,
                         'audit_source':'audited original'}]},
           'identities':[{'entry':'award','legacy_id':'old-issued-17','biz_type':'LEGACY_AWARD',
                          'biz_id':'award-17','request_hash':'a'*64,'evidence':'paid original receipt'}]}
        with self.assertRaisesRegex(RuntimeError,'interruption'): m.run(d,db,'prepare',True,fail_after=2)
        self.assertEqual('1',m.mysql(db,"SELECT progress FROM cutover_audit WHERE run_id='ddl_fixture'"))
        report=m.run(d,db,'prepare',True)
        self.assertTrue(report['cutover_allowed'],report)
        self.assertTrue(m.run(d,db,'verify',True)['cutover_allowed'])
        d['tables']['user_credit_account'][0]['balance']='13.00'
        with self.assertRaisesRegex(ValueError,'checksum changed'): m.run(d,db,'prepare',True)
        print('CUTOVER_EVIDENCE '+json.dumps(report,sort_keys=True))

if __name__=='__main__': unittest.main()
