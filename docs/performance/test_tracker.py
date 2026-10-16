import importlib.util
from pathlib import Path
import tempfile
import unittest
s=importlib.util.spec_from_file_location('tracker',Path(__file__).with_name('track_v2_orders.py'))
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class TrackerParsing(unittest.TestCase):
    def test_partial_tail_never_fabricates_accepted_orders(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'events.jsonl';p.write_text('{"msg":"{\\"requestId\\":\\"r\\",\\"status\\":202,\\"orderId\\":\\"o\\"}"}\n{"msg":')
            rows=m.records(p);self.assertEqual(1,len(rows));self.assertEqual('o',rows[0]['orderId'])
    def test_refuses_external_target_before_reading_tokens(self):
        with self.assertRaisesRegex(ValueError,'isolated'):
            m.track('missing','https://example.com','missing',1,1)
