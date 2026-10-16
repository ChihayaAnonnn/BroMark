"""只运行显式隔离容器；测试创建专属数据库，不 DROP。"""
import importlib.util
import json
import os
import test_cutover as fixtures
from pathlib import Path
import subprocess
import unittest
import uuid

spec=importlib.util.spec_from_file_location('cutover',Path(os.environ.get('V2_CUTOVER_MODULE',str(Path(__file__).parents[1]/'rehearse_cutover.py'))))
m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
m.ROOT=Path(__file__).parents[1]/"migrations"

class CutoverIntegration(unittest.TestCase):
    def test_interrupted_ddl_resumes_and_checksum_change_blocks(self):
        self.assertTrue(os.environ.get('V2_MYSQL_PASSWORD'),'isolated dependency required')
        db='router_cutover_it_'+uuid.uuid4().hex[:12]
        subprocess.run(['docker','exec','-e','MYSQL_PWD',
                        'bromark-v2-mysql','mysql','-uroot','-e','CREATE DATABASE '+db],check=True,capture_output=True,env=dict(os.environ,MYSQL_PWD=os.environ['V2_MYSQL_PASSWORD']))
        d=fixtures.CutoverValidation().fixture();d['run_id']='ddl_fixture'
        d['tables']['raffle_activity_account']=[{'user_id':'u','activity_id':12,'total_remaining':7,
                         'day_limit':3,'month_limit':5,'effective_draw_count':2,'version':0,'opening_total':7,
                         'opening_day_limit':3,'opening_month_limit':5,'opening_effective_count':2,
                         'audit_source':'audited original'}]
        with self.assertRaisesRegex(RuntimeError,'interruption'): m.run(d,db,'prepare',True,fail_after=2)
        self.assertEqual('1',m.mysql(db,"SELECT progress FROM cutover_audit WHERE run_id='ddl_fixture'"))
        report=m.run(d,db,'prepare',True)
        self.assertTrue(report['cutover_allowed'],report)
        self.assertTrue(m.run(d,db,'verify',True)['cutover_allowed'])
        m.mysql(db,"UPDATE cutover_identity SET biz_id='changed' WHERE legacy_id='17'")
        self.assertFalse(m.run(d,db,'verify',True)['cutover_allowed'])
        m.mysql(db,"UPDATE cutover_identity SET biz_id='award-17' WHERE legacy_id='17'")
        d['tables']['user_credit_account'][0]['balance']='13.00' 
        d['tables']['user_credit_account'][0]['opening_balance']='1.00'
        with self.assertRaisesRegex(ValueError,'checksum changed'): m.run(d,db,'prepare',True)
        print('CUTOVER_EVIDENCE '+json.dumps(report,sort_keys=True))

    def test_decimal_cents_are_compared_exactly_in_real_mysql(self):
        db='router_cutover_it_'+uuid.uuid4().hex[:12]
        subprocess.run(['docker','exec','-e','MYSQL_PWD','bromark-v2-mysql','mysql','-uroot','-e','CREATE DATABASE '+db],check=True,capture_output=True,env=dict(os.environ,MYSQL_PWD=os.environ['V2_MYSQL_PASSWORD']))
        m.mysql(db,"CREATE TABLE cent_probe(user_id varchar(64),balance decimal(18,2));INSERT INTO cent_probe VALUES('u',9007199254740992.02)")
        report=m.table_digest(db,'cent_probe',[{'user_id':'u','balance':'9007199254740992.01'}])
        self.assertFalse(report['matches'])

if __name__=='__main__': unittest.main()
