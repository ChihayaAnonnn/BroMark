import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('cutover', Path(__file__).parents[1] / 'rehearse_cutover.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

class CutoverValidation(unittest.TestCase):
    def fixture(self):
        return {'run_id': 'fixture', 'auditor': 'test', 'stopped': True, 'drained': True,
                'tables': {'user_credit_account': [{'user_id':'u','balance':'12.00','opening_balance':'12.00',
                           'audit_source':'approved','status':'ACTIVE','version':0}]},
                'identities': [{'entry':'award','legacy_id':'17','biz_type':'LEGACY_AWARD','biz_id':'award-17',
                                'request_hash':'a'*64,'evidence':'issued original ledger'}]}
    def test_duplicates_block_instead_of_sum(self):
        d=self.fixture(); d['tables']['user_credit_account'] *= 2
        self.assertFalse(m.precheck(d)['cutover_allowed'])
    def test_unknown_message_and_unstopped_write_block(self):
        d=self.fixture(); d['identities'][0]['request_hash']=''
        self.assertFalse(m.precheck(d)['cutover_allowed'])
        d=self.fixture(); d['stopped']=False
        self.assertFalse(m.precheck(d)['cutover_allowed'])
    def test_default_dry_run_and_production_rejected(self):
        self.assertFalse(m.execution_allowed('v2-it',False,'router_cutover_it_fixture'))
        self.assertFalse(m.execution_allowed('prod',True,'router_control'))
        self.assertFalse(m.execution_allowed('v2-it',True,'router_one'))
        self.assertTrue(m.execution_allowed('v2-it',True,'router_cutover_it_fixture'))
    def test_valid_explicit_audit_and_checksum(self):
        d=self.fixture(); self.assertTrue(m.precheck(d)['cutover_allowed'])
        changed=self.fixture(); changed['tables']['user_credit_account'][0]['balance']='13.00'
        self.assertNotEqual(m.digest(d), m.digest(changed))

if __name__=='__main__': unittest.main()

class AccountingPrecheck(unittest.TestCase):
    def test_credit_opening_plus_imported_ledger_must_equal_balance(self):
        d=CutoverValidation().fixture();d['tables']['user_credit_account'][0]['balance']='13.00'
        self.assertFalse(m.precheck(d)['cutover_allowed'])
    def test_missing_opening_is_not_silently_defaulted(self):
        d=CutoverValidation().fixture();d['tables']['user_credit_account'][0].pop('opening_balance')
        self.assertFalse(m.precheck(d)['cutover_allowed'])
