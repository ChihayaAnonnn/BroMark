import os
import unittest
from unittest.mock import patch

from seed_v2_testdata import mysql


class MysqlOwnershipTest(unittest.TestCase):
    def test_rejects_old_container_before_docker_exec(self):
        with patch.dict(os.environ, {"V2_MYSQL_CONTAINER": "bromark-v2-mysql", "V2_TARGET_RUN_ID": "t8"}):
            with patch("subprocess.run") as run:
                with self.assertRaises(ValueError):
                    mysql("router_perf_it_t8_control", "SELECT 1")
                run.assert_not_called()

    def test_rejects_other_run_database_before_docker_exec(self):
        with patch.dict(os.environ, {"V2_MYSQL_CONTAINER": "bromark-relay-20261016-mysql-1", "V2_TARGET_RUN_ID": "t8"}):
            with patch("subprocess.run") as run:
                with self.assertRaises(ValueError):
                    mysql("router_perf_it_other_control", "SELECT 1")
                run.assert_not_called()


if __name__ == "__main__":
    unittest.main()
