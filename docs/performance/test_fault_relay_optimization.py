import os
import json
from pathlib import Path
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from fault_relay_optimization import (cleanup_owned, event_row, kill_connection_timed, marker_fields, port_binding,
                                      source_rows, java_instant_ns, validate_seed_and_names, verify_marker, verify_settle_connection,
                                      writer_inventory, receipt_snapshot, verify_preserved_tasks, verify_graceful_stops)


SEED = {
    "runId": "t8proof",
    "databases": ["router_perf_it_t8proof_control", "router_perf_it_t8proof_one", "router_perf_it_t8proof_two"],
    "messagingPrefix": "bromark.it.t8proof",
    "writerAssertion": "stopped-before-fresh-seed",
}


class FaultSafetyTest(unittest.TestCase):
    def test_remedial_original_requires_identity_when_redrive_also_exists(self):
        snapshot = {"owned": {"outbox": [
            {"eventId": "original", "eventType": "PrepareDraw"},
            {"eventId": "redrive", "eventType": "PrepareDraw"}]}}
        with self.assertRaises(RuntimeError):
            event_row(snapshot)
        self.assertEqual(event_row(snapshot, "original")[1]["eventId"], "original")
        with self.assertRaises(RuntimeError):
            event_row(snapshot, "missing")

    def test_rollback_rejects_crash_oom_error_and_wrong_id(self):
        good = {"id": "owned", "inspectedId": "owned", "stopOutput": "owned", "running": False,
                "exitCode": 143, "oomKilled": False, "stateError": "", "elapsedSeconds": 2}
        verify_graceful_stops([good, {**good, "exitCode": 0}])
        for mutation in ({"exitCode": 1}, {"exitCode": 137}, {"oomKilled": True},
                         {"stateError": "runtime failed"}, {"inspectedId": "replaced"},
                         {"running": True}, {"elapsedSeconds": 31}):
            with self.assertRaises(RuntimeError):
                verify_graceful_stops([{**good, **mutation}])

    def test_rollback_retains_original_event_and_dispatch_facts(self):
        import copy
        before = {"owned": {"outbox": [{"event_id": "original", "payload_hash": "hash", "attempt": 2}],
                            "retry_job": [], "v2_recovery_dispatch": [
                                {"aggregate_type": "DRAW", "aggregate_id": "order", "redrive_count": 1}]}}
        verify_preserved_tasks(before, copy.deepcopy(before))
        for mutation in ("delete", "hash", "attempt", "dispatch"):
            after = copy.deepcopy(before)
            if mutation == "delete": after["owned"]["outbox"] = []
            elif mutation == "hash": after["owned"]["outbox"][0]["payload_hash"] = "other"
            elif mutation == "attempt": after["owned"]["outbox"][0]["attempt"] = 0
            else: after["owned"]["v2_recovery_dispatch"][0]["redrive_count"] = 0
            with self.assertRaises(RuntimeError):
                verify_preserved_tasks(before, after)

    def test_mysql_raw_preserves_nested_json_and_trailing_empty_tsv(self):
        import seed_v2_testdata as seed_module
        nested = json.dumps({"payload": json.dumps({"path": "a\\b", "text": "quote\""})})
        with patch.dict(os.environ, {"V2_TARGET_RUN_ID": "t8proof", "V2_MYSQL_PASSWORD": "test"}), \
             patch.object(seed_module, "owned_mysql_id", return_value="owned"), \
             patch.object(seed_module.subprocess, "run", return_value=SimpleNamespace(stdout=nested + "\t\n")) as called:
            result = seed_module.mysql(SEED["databases"][0], "select fixture")
        self.assertEqual(result, nested + "\t")
        self.assertEqual(json.loads(result.rstrip("\t"))["payload"], json.dumps({"path": "a\\b", "text": "quote\""}))
        self.assertIn("--raw", called.call_args.args[0])

    def test_nonempty_sql_json_parse_error_is_failfast_and_saved(self):
        with tempfile.TemporaryDirectory() as directory, \
             patch("fault_relay_optimization.route", return_value=(1, 2)), \
             patch("fault_relay_optimization.source_rows", return_value={}), \
             patch("reconcile_v2_run.rows", side_effect=json.JSONDecodeError("broken", "bad JSON", 1)):
            with self.assertRaisesRegex(RuntimeError, "nonempty SQL JSON invalid"):
                receipt_snapshot({"databases": SEED["databases"]}, "user", "order", Path(directory))
            raw = list(Path(directory).glob("sql-json-error-*.json"))
            self.assertEqual(len(raw), 1)
            self.assertEqual(json.loads(raw[0].read_text())["rawSqlJson"], "bad JSON")

    def test_receipt_snapshot_uses_real_asset_scopes(self):
        seed = {"databases": SEED["databases"]}
        called = []
        def fake_rows(database, table):
            called.append((database, table))
            return []
        with patch("fault_relay_optimization.route", return_value=(1, 2)), \
             patch("fault_relay_optimization.source_rows", return_value={}), \
             patch("reconcile_v2_run.rows", side_effect=fake_rows):
            receipt_snapshot(seed, "user", "order")
        self.assertIn((seed["databases"][1], "user_credit_account"), called)
        self.assertIn((seed["databases"][1], "raffle_activity_account_day"), called)
        self.assertIn((seed["databases"][1], "user_credit_ledger_002"), called)
        self.assertIn((seed["databases"][0], "award_reservation"), called)
        self.assertNotIn((seed["databases"][0], "user_credit_account"), called)

    def test_java_instant_nanosecond_marker_keeps_deadline_fraction(self):
        marker = java_instant_ns("2026-10-16T16:11:31.806067881Z")
        deadline = java_instant_ns("2026-10-16T16:11:32.006067881Z")
        self.assertEqual(deadline - marker, 200_000_000)
        self.assertEqual(java_instant_ns("2026-10-16T16:11:31Z") + 806_000_000,
                         java_instant_ns("2026-10-16T16:11:31.806Z"))
        self.assertEqual(java_instant_ns("2026-10-16T16:11:31.806067Z") + 881,
                         marker)
        with self.assertRaises(ValueError):
            java_instant_ns("2026-10-16T16:11:31.1+08:00")

    def test_kill_receipt_needs_both_mysql_server_clock_bounds(self):
        with patch("fault_relay_optimization.mysql", return_value="1792167091800000\n1792167091800300") as invoked:
            self.assertEqual(kill_connection_timed(371), (1792167091800000, 1792167091800300))
            self.assertIn("KILL CONNECTION 371", invoked.call_args.args[1])
        with patch("fault_relay_optimization.mysql", return_value="1792167091800000"):
            with self.assertRaisesRegex(RuntimeError, "receipt missing"):
                kill_connection_timed(371)

    def test_rejects_old_app_name_before_any_docker_call(self):
        env = {
            "V2_MYSQL_CONTAINER": "bromark-relay-20261016-mysql-1",
            "V2_RABBIT_CONTAINER": "bromark-relay-20261016-rabbitmq-1",
            "V2_REDIS_CONTAINER": "bromark-relay-20261016-redis-1",
            "V2_APP_CONTAINER_1": "bromark-phase6-app-1",
            "V2_APP_CONTAINER_2": "bromark-relay-20261016-app-2",
            "V2_TEST_DOCKER_NETWORK": "bromark-assets-cluster-it",
        }
        with patch.dict(os.environ, env):
            with self.assertRaises(ValueError):
                validate_seed_and_names(SEED)

    def test_rejects_database_from_other_run(self):
        env = {
            "V2_MYSQL_CONTAINER": "bromark-relay-20261016-mysql-1",
            "V2_RABBIT_CONTAINER": "bromark-relay-20261016-rabbitmq-1",
            "V2_REDIS_CONTAINER": "bromark-relay-20261016-redis-1",
            "V2_APP_CONTAINER_1": "bromark-relay-20261016-app-1",
            "V2_APP_CONTAINER_2": "bromark-relay-20261016-app-2",
            "V2_TEST_DOCKER_NETWORK": "bromark-assets-cluster-it",
        }
        with patch.dict(os.environ, env):
            bad = {**SEED, "databases": [SEED["databases"][0], "router_perf_it_other_one", SEED["databases"][2]]}
            with self.assertRaises(ValueError):
                validate_seed_and_names(bad)

    def test_rejects_endpoint_not_bound_to_owned_container(self):
        state = {"Name": "/bromark-relay-20261016-mysql-1",
                 "NetworkSettings": {"Ports": {"3306/tcp": [{"HostIp": "0.0.0.0", "HostPort": "13316"}]}}}
        with self.assertRaisesRegex(ValueError, "port binding mismatch"):
            port_binding(state, "3306/tcp", 13316)

    def test_rejects_running_writer_that_refs_target_database(self):
        seed = {**SEED, "databases": SEED["databases"]}
        state = {"Name": "/stray-writer", "Id": "id1",
                 "Config": {"Env": ["DB=" + seed["databases"][1]]}}
        def fake_docker(*args, **_):
            return SimpleNamespace(stdout="id1\n" if args[0] == "ps" else json.dumps([state]))
        with patch("fault_relay_optimization.docker", side_effect=fake_docker):
            with self.assertRaisesRegex(ValueError, "running container refers"):
                writer_inventory(seed)

    def test_marker_rejects_wrong_event_even_with_same_pid(self):
        row = {"eventId": "e1", "leaseToken": "token-1", "attempt": "1", "state": "PROCESSING"}
        app = {"ready": "1234 8080 now", "name": "bromark-relay-20261016-app-1", "id": "cid"}
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "marker"
            path.write_text("1234\te2\to1\ttoken-1\t1\t2026-10-16T00:00:00Z")
            with self.assertRaisesRegex(RuntimeError, "identity/PID mismatch"):
                verify_marker(marker_fields(path), app, "o1", row)

    def test_three_database_snapshot_queries_real_retry_schema(self):
        seed = {"databases": SEED["databases"]}
        statements = []
        def fake_mysql(database, statement):
            statements.append((database, statement))
            return ""
        with patch("fault_relay_optimization.mysql", side_effect=fake_mysql):
            snapshot = source_rows(seed, "user", "order")
        self.assertEqual(set(seed["databases"]), set(snapshot))
        self.assertEqual(3, sum("FROM retry_job" in statement for _, statement in statements))
        self.assertTrue(all("failure_count" in statement for _, statement in statements if "FROM retry_job" in statement))
        self.assertFalse(any("generation" in statement for _, statement in statements))
        db_index, _ = __import__("seed_v2_testdata").route("user")
        self.assertEqual([seed["databases"][db_index]],
                         [db for db, statement in statements if "FROM v2_draw_order_" in statement])
        self.assertEqual([seed["databases"][db_index]],
                         [db for db, statement in statements if "FROM user_credit_ledger_" in statement])
        self.assertEqual([seed["databases"][0]],
                         [db for db, statement in statements if "FROM award_reservation " in statement])

    def test_original_event_must_be_one_row_across_databases(self):
        snapshot = {db: {"outbox": [{"eventId": "e1", "eventType": "PrepareDraw"}]} for db in SEED["databases"][:2]}
        with self.assertRaisesRegex(RuntimeError, "exactly one"):
            event_row(snapshot, "e1")

    def test_cleanup_retains_own_rabbit_volume_for_recovery(self):
        volume = "bromark-relay-20261016-task8-rabbit-t8proof17-123456789abc"
        with tempfile.TemporaryDirectory() as temporary:
            with patch("fault_relay_optimization.docker", return_value=SimpleNamespace(returncode=0)) as invoked:
                result = cleanup_owned(Path(temporary), [], [volume],
                                       {"bromark-relay-20261016-rabbitmq-1": {"image": "sha256:owned"}})
        self.assertTrue(result[0]["retainedForRecovery"])
        self.assertFalse(any(call.args[:2] == ("volume", "rm") for call in invoked.call_args_list))

    def test_mysql_kill_rejects_connection_from_other_database(self):
        marker = ["1234", "original-event", "371", "2026-10-16T00:00:00Z", "2026-10-16T00:00:01Z"]
        app = {"ready": "1234 8080 now"}
        with self.assertRaisesRegex(RuntimeError, "not bound to target database"):
            verify_settle_connection(marker, app, "original-event", "router_perf_it_t8proof_one",
                                     [{"id": "371", "database": "router_perf_it_other_one", "command": "Sleep"}])


if __name__ == "__main__":
    unittest.main()
