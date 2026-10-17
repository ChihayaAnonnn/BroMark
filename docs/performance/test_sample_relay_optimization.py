import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch
from sample_relay_optimization import Sampler, aligned_window, backlog, validate_final_sample, validate_continuous_arrivals


class SampleTest(unittest.TestCase):
    def test_suspend_gap_cannot_pass_continuous_recovery(self):
        rows = [{'startedAt': float(i)} for i in range(45)]
        self.assertEqual(validate_continuous_arrivals(rows, 0, 44)['maximumStartGapSeconds'], 1)
        suspended = [{'startedAt': r['startedAt'] + (584 if r['startedAt'] > 10 else 0)} for r in rows]
        with self.assertRaises(RuntimeError):
            validate_continuous_arrivals(suspended, 0, 628)
        with self.assertRaises(RuntimeError):
            validate_continuous_arrivals([{'startedAt': float(i * 2)} for i in range(30)], 0, 58)

    def test_final_gate_requires_nonzero_delivered_and_actual_broker_zero(self):
        valid = {'orders': [{'id': 'one', 'state': 'DELIVERED'}],
                 'domains': {'ORDER': {'count': 0}, 'BROKER': {'valid': True, 'count': 0}}}
        validate_final_sample(valid)
        with self.assertRaises(RuntimeError):
            validate_final_sample(dict(valid, orders=[]))
        for broker in ({'valid': False, 'count': None}, {'valid': True, 'count': 1}):
            with self.assertRaises(RuntimeError):
                validate_final_sample(dict(valid, domains={'ORDER': {'count': 0}, 'BROKER': broker}))

    def test_down_is_missing_not_zero(self):
        sampler = object.__new__(Sampler)
        sampler.h = SimpleNamespace(RABBIT='fresh')
        sampler.rabbit_id = 'owned-id'
        sampler.verify = lambda name, cid: {'State': {'Running': False}}
        with tempfile.TemporaryDirectory() as path:
            result = sampler.broker(Path(path))
        self.assertFalse(result['valid'])
        self.assertIsNone(result['count'])
        self.assertIsNone(result['oldestAgeSeconds'])

    def test_stale_resource_identity_rejected(self):
        sampler = object.__new__(Sampler)
        sampler.h = SimpleNamespace(NETWORK='owned-network', inspect=lambda name:
            {'Id': 'replaced', 'NetworkSettings': {'Networks': {'owned-network': {}}}})
        with self.assertRaises(ValueError):
            sampler.verify('fresh', 'owned-id')

    def test_old_broker_cannot_be_selected_by_dependency_env(self):
        harness = SimpleNamespace(validate_seed_and_names=lambda seed: None)
        with tempfile.TemporaryDirectory() as path:
            with self.assertRaises(ValueError):
                Sampler(harness, {'runId': 'run'}, [], 'old-dependency', ['fresh-id'],
                        'bromark.it.task8.run', Path(path) / 'samples')

    def test_backlog_counts_pending_and_processing_not_published(self):
        value = backlog([{'state': 'PENDING', 'count': 2, 'age': 10},
                         {'state': 'PROCESSING', 'count': 1, 'age': 4},
                         {'state': 'PUBLISHED', 'count': 10, 'age': 50}])
        self.assertEqual(value['count'], 3)
        self.assertEqual(value['oldestAgeSeconds'], 10)
        self.assertIsNone(backlog([])['oldestAgeSeconds'])

    def test_completion_deduplicates_and_drain_cannot_prove_recovery(self):
        def sample(at, phase, rows, count):
            return {'observedAt': at, 'phase': phase, 'orders': rows, 'elapsedSeconds': .2,
                    'domains': {'ORDER': {'count': count}}}
        before = sample(10, 'recovery_with_arrivals', [{'id': 'old', 'state': 'ACCEPTED'}], 1)
        after = sample(12, 'recovery_with_arrivals', [{'id': 'old', 'state': 'DELIVERED'},
                       {'id': 'new', 'state': 'DELIVERED'}], 0)
        result = aligned_window(before, after)
        self.assertEqual(result['acceptedPerSecond'], .5)
        self.assertEqual(result['completedPerSecond'], 1)
        self.assertIsNone(aligned_window(before, dict(after, phase='drain')))

    def test_real_cli_header_and_fixed_empty_legacy_declarations(self):
        sampler = object.__new__(Sampler)
        sampler.h = SimpleNamespace(RABBIT='fresh')
        sampler.rabbit_id = 'owned-id'
        sampler.prefix = 'bromark.it.task8.run'
        sampler.verify = lambda name, cid: {'State': {'Running': True}}
        raw = ('name\tmessages_ready\tmessages_unacknowledged\n'
               'bromark.it.task8.run.workflow\t2\t1\n'
               'bromark.it.task8.run.notifications\t0\t0\n'
               'activity_sku_stock_zero\t0\t0\ncredit_adjust_success\t0\t0\n'
               'bromark.it.send_rebate\t0\t0\nbromark.it.send_award\t0\t0\n')
        with tempfile.TemporaryDirectory() as path, patch('sample_relay_optimization.subprocess.run',
                return_value=SimpleNamespace(returncode=0, stdout=raw, stderr='')) as command:
            result = sampler.broker(Path(path))
            self.assertEqual(result['count'], 3)
            self.assertEqual(len(result['excludedQueues']), 4)
            command.return_value.stdout = raw.replace('activity_sku_stock_zero\t0', 'activity_sku_stock_zero\t1')
            with self.assertRaises(ValueError):
                sampler.broker(Path(path))
            command.return_value.stdout = raw + 'bromark.it.task8.run.extra\t0\t0\n'
            with self.assertRaises(ValueError):
                sampler.broker(Path(path))

    def test_unexpected_queue_fails_closed(self):
        sampler = object.__new__(Sampler)
        sampler.h = SimpleNamespace(RABBIT='fresh')
        sampler.rabbit_id = 'owned-id'
        sampler.prefix = 'bromark.it.task8.run'
        sampler.verify = lambda name, cid: {'State': {'Running': True}}
        with tempfile.TemporaryDirectory() as path, patch('sample_relay_optimization.subprocess.run',
                return_value=SimpleNamespace(returncode=0, stdout='old.queue\t2\t0\n', stderr='')) as command:
            with self.assertRaises(ValueError):
                sampler.broker(Path(path))
            self.assertEqual(command.call_args.args[0][4], 'owned-id')


if __name__ == '__main__':
    unittest.main()
