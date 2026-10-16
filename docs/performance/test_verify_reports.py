import importlib.util
from pathlib import Path
import tempfile
import unittest
s=importlib.util.spec_from_file_location('reports',Path(__file__).parents[1]/'dev-ops/verify_v2_reports.py')
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class ReportGateTest(unittest.TestCase):
    def test_empty_skipped_and_missing_suites_fail(self):
        with tempfile.TemporaryDirectory() as d:
            self.assertFalse(m.verify(d,set())['passed'])
            p=Path(d)/'surefire-reports';p.mkdir()
            f=p/'TEST-A.xml';f.write_text('<testsuite name="A" tests="1" skipped="1"><testcase name="a"><skipped/></testcase></testsuite>')
            self.assertFalse(m.verify(d,{'A'})['passed'])
            f.write_text('<testsuite name="A" tests="1"><testcase name="a"/></testsuite>')
            self.assertTrue(m.verify(d,{'A'})['passed'])
            self.assertFalse(m.verify(d,{'B'})['passed'])

class CombinedValidationGate(unittest.TestCase):
    def test_green_xml_without_six_domain_financial_checks_cannot_pass(self):
        self.assertFalse(m.combine_validation({'passed':True,'tests':1},None)['passed'])
        financial={'passed':True,'checkedScopes':['CREDIT','QUOTA','STOCK','DELIVERY','PAYMENT','SIGNIN'],
                   'totalDifferences':0,'unfinishedOrders':0,'unresolvedReviews':0,'pendingTasks':0}
        self.assertTrue(m.combine_validation({'passed':True,'tests':1},financial)['passed'])
        financial['checkedScopes'].remove('SIGNIN')
        self.assertFalse(m.combine_validation({'passed':True,'tests':1},financial)['passed'])
