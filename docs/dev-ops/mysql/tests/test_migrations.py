"""真实隔离 MySQL；运行前设置 MYSQL_PWD 和 V2_MYSQL_HOST/PORT，mysql CLI可用。"""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
import unittest.mock

spec = importlib.util.spec_from_file_location('migrations', Path(__file__).parents[1] / 'apply_migrations.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class MigrationTest(unittest.TestCase):
    def test_upgrade_field_manifest_is_checksummed(self):
        import shutil
        original = m.ROOT
        with tempfile.TemporaryDirectory() as temp:
            for path in original.iterdir():
                if path.is_file():
                    shutil.copy(path, temp)
            m.ROOT = Path(temp)
            try:
                fields = m.ROOT / '001_upgrade_fields.json'
                fields.write_text(fields.read_text() + '\n')
                with self.assertRaisesRegex(RuntimeError, 'upgrade fields checksum'):
                    m.apply('v2-it', '001', True, 'upgrade')
            finally:
                m.ROOT = original

    def test_execution_requires_explicit_allowlist(self):
        with unittest.mock.patch.dict(os.environ, {'V2_MIGRATION_ALLOW': ''}):
            with self.assertRaisesRegex(RuntimeError, 'allowlist'):
                m.apply('v2-it', '001', False, 'fresh')
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            m.apply('prod', '002', True, 'fresh')
        self.assertNotIn('"execute": true', output.getvalue())

    def test_reentry_after_committed_ddl_and_checksum_change(self):
        import hashlib
        database = 'router_migration_checksum_it'
        m.mysql('router_control', f'CREATE DATABASE IF NOT EXISTS {database}')
        for table in m.mysql(database, 'SHOW TABLES').splitlines():
            m.mysql(database, f'DROP TABLE `{table}`')
        root, databases, sql_client = m.ROOT, m.DATABASES, m.mysql
        with tempfile.TemporaryDirectory() as temp:
            m.ROOT = Path(temp)
            m.DATABASES = {'control': [database]}
            source = 'CREATE TABLE IF NOT EXISTS crash_probe(id bigint PRIMARY KEY);'
            entry = {'file': '001_probe.sql', 'scope': 'control', 'shards': 1,
                     'sha256': hashlib.sha256(source.encode()).hexdigest()}
            (m.ROOT / entry['file']).write_text(source)
            (m.ROOT / 'manifest.json').write_text(json.dumps({'scripts': [entry]}))
            failed = False
            def interrupted(db, sql):
                nonlocal failed
                if 'ON DUPLICATE KEY UPDATE step' in sql and not failed:
                    failed = True
                    raise RuntimeError('lost progress acknowledgement after DDL')
                return sql_client(db, sql)
            os.environ['V2_MIGRATION_ALLOW'] = 'isolated-router-test-only'
            try:
                m.mysql = interrupted
                with self.assertRaisesRegex(RuntimeError, 'lost progress'):
                    m.apply('v2-it', '001', False, 'fresh')
                m.mysql = sql_client
                self.assertEqual('1', m.mysql(database, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='crash_probe'"))
                source += '\n'
                entry['sha256'] = hashlib.sha256(source.encode()).hexdigest()
                (m.ROOT / entry['file']).write_text(source)
                (m.ROOT / 'manifest.json').write_text(json.dumps({'scripts': [entry]}))
                with self.assertRaisesRegex(RuntimeError, 'applied checksum mismatch'):
                    m.apply('v2-it', '001', False, 'fresh')
            finally:
                m.ROOT, m.DATABASES, m.mysql = root, databases, sql_client

    def test_upgrade_preserves_duplicates_and_opening_conflicts_and_reenters(self):
        database = 'router_migration_it'
        m.mysql('router_control', f'CREATE DATABASE IF NOT EXISTS {database}')
        # This database is exclusively owned by this integration test.
        for table in m.mysql(database, 'SHOW TABLES').splitlines():
            m.mysql(database, f'DROP TABLE `{table}`')
        m.mysql(database, "CREATE TABLE user_credit_account(user_id varchar(64),available_amount decimal(18,2));INSERT INTO user_credit_account VALUES('u',100),('u',200)")
        for table in ['raffle_activity_account', 'raffle_activity_account_month', 'raffle_activity_account_day']:
            m.mysql(database, f'CREATE TABLE {table}(user_id varchar(64),activity_id bigint)')
        original_databases = m.DATABASES
        m.DATABASES = {'user': [database], 'control': [database], 'all': [database]}
        os.environ['V2_MIGRATION_ALLOW'] = 'isolated-router-test-only'
        try:
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                m.apply('v2-it', '001', False, 'upgrade')
            self.assertIn('"duplicate_groups": "1"', output.getvalue())
            self.assertIn('"cutover_allowed": false', output.getvalue())
            self.assertEqual('100.00\n200.00', m.mysql(database, 'SELECT available_amount FROM user_credit_account ORDER BY available_amount'))
            m.mysql(database, "UPDATE user_credit_account SET opening_balance=10 WHERE available_amount=100")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                m.apply('v2-it', '001', False, 'upgrade')
            self.assertIn('"opening_conflicts": "1"', output.getvalue())
            self.assertEqual('2', m.mysql(database, 'SELECT COUNT(*) FROM user_credit_account'))
        finally:
            m.DATABASES = original_databases


if __name__ == '__main__':
    import unittest.mock
    unittest.main()
