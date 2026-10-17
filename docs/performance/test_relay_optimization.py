"""简化 Task9：真实 k6 分布探针和正式受理边界。"""
import json
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import unittest
import uuid
from unittest.mock import patch

from collect_v2_results import backlog_evidence, collect
from export_v2_stages import export
from prepare_v2_results import prepare
from run_v2_experiment import assert_owned_target_unchanged, clock_calibration, validate_owned_target
from track_v2_orders import records, track


ROOT = Path(__file__).parent
K6 = "grafana/k6:1.3.0@sha256:3ddc8b1a33a2c3d8edc6e99b6a762ae36cba08788463458f5e6a7703e14eb77d"


class ProbeHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.answer(200, {"state": "DELIVERED"})

    def do_POST(self):
        if self.path == "/api/v2/draws":
            self.answer(202, {"state": "ACCEPTED", "orderId": "probe"})
        else:
            self.answer(200, {"state": "DELIVERED", "orderId": "probe"})

    def answer(self, code, body):
        raw = json.dumps(body).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def log_message(self, *_):
        pass


class RelayOptimization(unittest.TestCase):
    def test_smoke_verdict_requires_execution_four_domains_and_reconciliation(self):
        requests = [
            {"requestId": "r1", "phase": "formal", "user": "u1", "operation": "draw",
             "status": 202, "orderId": "o1", "state": "ACCEPTED", "latencyMs": 10},
            {"requestId": "r2", "phase": "formal", "user": "u2", "operation": "query",
             "status": 200, "orderId": None, "state": None, "latencyMs": 5},
        ]
        completions = [{"orderId": "o1", "kind": "draw", "state": "DELIVERED",
                        "serverAcceptedAt": 100, "serverConfirmedAt": 100.1,
                        "serverCompletedAt": 100.2, "observedAt": 101}]
        samples = [{"observedAt": at, "domains": {key: {"count": 0, "valid": True}
                    for key in ("ORDER", "OUTBOX", "RETRY", "BROKER")}}
                   for at in (100, 160, 220)]
        meta = {"durationSeconds": 120, "arrivalRate": 2 / 120,
                "loadStartedAt": 100, "loadEndedAt": 220, "backlogSamples": samples,
                "clockSkewMs": 1, "clockMaxSkewMs": 10, "droppedIterations": 0,
                "resourceBudgetOk": True,
                "execution": {"k6Exit": 0, "trackerExit": 0, "resourceIdentityStable": True},
                "reconciliation": {"passed": True, "totalDifferences": 0,
                    "pendingTasks": 0, "unfinishedOrders": 0, "unresolvedReviews": 0}}
        result = collect(requests, completions, meta)
        self.assertTrue(result["smokeTechnicalPassed"])
        self.assertFalse(result["fullCapacityPassed"])
        meta["execution"]["resourceIdentityStable"] = False
        self.assertFalse(collect(requests, completions, meta)["smokeTechnicalPassed"])
        meta["execution"]["resourceIdentityStable"] = True
        meta["backlogSamples"] = []
        self.assertFalse(collect(requests, completions, meta)["smokeTechnicalPassed"])

    def test_precreate_reuses_original_request_id_after_503_without_order_id(self):
        seen = []
        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                seen.append(body["requestId"])
                code, value = (503, {"state": "UNKNOWN"}) if len(seen) == 1 else (202, {"orderId": "o1", "state": "ACCEPTED"})
                raw = json.dumps(value).encode()
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(raw)))
                self.end_headers()
                self.wfile.write(raw)
            def do_GET(self):
                raw = b'{"orderId":"o1","state":"DELIVERED"}'
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(raw)))
                self.end_headers()
                self.wfile.write(raw)
            def log_message(self, *_):pass
        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        try:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                tokens = root / "tokens.json"
                tokens.write_text(json.dumps({"u": "synthetic"}))
                seed = root / "seed.json"
                seed.write_text(json.dumps({"runId": "probe", "users": 1, "activityId": 610016,
                                            "tokenFile": str(tokens), "databases": []}))
                with patch.dict(os.environ, {"V2_TOKEN_FILE": str(tokens)}), \
                        patch("prepare_v2_results.validate_owned_target", return_value={
                            "port": server.server_port, "id": "app", "rabbitId": "broker", "image": "image"}), \
                        patch("prepare_v2_results.assert_owned_target_unchanged"):
                    result = prepare(seed, root / "precreate")
                self.assertEqual(2, len(seen))
                self.assertEqual(seen[0], seen[1])
                self.assertEqual({"u": "o1"}, json.loads(seed.read_text())["resultOrders"])
                journal = records(root / "precreate" / "requests.jsonl")
                self.assertEqual([503, 202], [r["status"] for r in journal])
                self.assertEqual(1, result["delivered"])
        finally:
            server.shutdown()
            server.server_close()
            worker.join()

    def test_runner_detects_app_or_rabbit_replacement_before_and_after_load(self):
        initial = {"id": "app-a", "rabbitId": "broker-a", "image": "image-a"}
        with patch("run_v2_experiment.validate_owned_target", return_value=dict(initial, id="app-b")):
            with self.assertRaises(ValueError):
                assert_owned_target_unchanged({}, 1, initial)
        with patch("run_v2_experiment.validate_owned_target", return_value=dict(initial, rabbitId="broker-b")):
            with self.assertRaises(ValueError):
                assert_owned_target_unchanged({}, 1, initial)

    def test_task8_four_domain_growth_is_not_hidden_by_order_or_post_stop_drain(self):
        samples = []
        for at, outbox in ((100, 0), (400, 50), (700, 100), (800, 0)):
            samples.append({"observedAt": at, "backlog": 0,
                            "backlogScopes": ["ORDER", "OUTBOX", "RETRY", "BROKER"],
                            "domains": {name: {"count": outbox if name == "OUTBOX" else 0, "valid": True}
                                        for name in ("ORDER", "OUTBOX", "RETRY", "BROKER")}})
        self.assertFalse(backlog_evidence({"loadStartedAt": 100, "loadEndedAt": 700,
                                           "backlogSamples": samples}))

    def test_clock_calibration_records_before_client_and_after_client(self):
        with patch("run_v2_experiment.time.time", side_effect=[100.0, 100.02]), \
                patch("run_v2_experiment.mysql", return_value="100.01"):
            sample = clock_calibration("router_perf_it_probe_control")
        self.assertEqual((100.0, 100.01, 100.02),
                         (sample["clientBefore"], sample["server"], sample["clientAfter"]))
        self.assertAlmostEqual(10, sample["uncertaintyMs"])

    def test_stage_export_uses_only_formal_accepted_order_and_same_db_times(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "requests.jsonl").write_text("\n".join(json.dumps(row) for row in [
                {"requestId": "warm:1", "phase": "warmup", "operation": "draw", "user": "u",
                 "status": 202, "orderId": "pre"},
                {"requestId": "formal:1", "phase": "formal", "operation": "draw", "user": "u",
                 "status": 202, "orderId": "draw"},
            ]) + "\n")
            (root / "tracked.jsonl").write_text(json.dumps({"requestId": "formal:1", "orderId": "draw",
                                                         "kind": "draw", "state": "DELIVERED", "observedAt": 102}) + "\n")
            queries = []
            def fake_mysql(db, statement):
                queries.append(statement)
                if "SELECT UNIX_TIMESTAMP(UTC_TIMESTAMP(6))" in statement:
                    return "100"
                if "FROM v2_raffle_stage" in statement:
                    return "draw\tACCEPTED\t100"
                if "FROM v2_draw_order_" in statement:
                    return "draw\tDELIVERED\t100.5\t101"
                return ""
            seed = {"databases": ["router_perf_it_probe_control", "router_perf_it_probe_one",
                                  "router_perf_it_probe_two"]}
            with patch("export_v2_stages.mysql", side_effect=fake_mysql):
                output, clock = export(seed, root / "requests.jsonl", root / "tracked.jsonl")
            self.assertEqual(["draw"], [row["orderId"] for row in output])
            self.assertEqual((100, 100.5, 101),
                             (output[0]["serverAcceptedAt"], output[0]["serverConfirmedAt"],
                              output[0]["serverCompletedAt"]))
            self.assertTrue(any("draw" in query for query in queries))
            self.assertTrue(all("pre" not in query for query in queries))
            self.assertIn("clockCalibrationUncertaintyMs", clock)

    def test_503_with_original_order_id_is_polled_without_claiming_acceptance(self):
        server = ThreadingHTTPServer(("127.0.0.1", 0), ProbeHandler)
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        try:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / "tokens.json").write_text(json.dumps({"u": "synthetic"}))
                (root / "requests.jsonl").write_text(json.dumps({"requestId": "r1", "user": "u", "operation": "draw",
                                                         "status": 503, "orderId": "original"}) + "\n")
                with patch.dict(os.environ, {"V2_TOKEN_FILE": str(root / "tokens.json")}):
                    summary = track(root / "requests.jsonl", "http://127.0.0.1:%d" % server.server_port,
                                    root / "tracked.jsonl", .2, .01, 100)
                self.assertEqual(1, summary["observedTerminals"])
                self.assertEqual("original", records(root / "tracked.jsonl")[0]["orderId"])
                report = collect(records(root / "requests.jsonl"), records(root / "tracked.jsonl"), {})
                self.assertEqual(0, report["accepted"])
                self.assertEqual(1, report["http503WithOrderId"])
        finally:
            server.shutdown()
            server.server_close()
            worker.join()

    def test_runner_requires_owned_app_and_seed_databases(self):
        seed = {"runId": "task9x", "databases": [
            "router_perf_it_task9x_control", "router_perf_it_task9x_one", "router_perf_it_task9x_two"]}
        rabbit_name = "bromark-relay-20261016-task9-rabbit"
        state = {"Name": "/bromark-relay-20261016-app-1", "Id": "owned-id", "Image": "owned-image",
                 "State": {"Running": True}, "NetworkSettings": {
                     "Networks": {"bromark-assets-cluster-it": {}},
                     "Ports": {"8080/tcp": [{"HostIp": "127.0.0.1", "HostPort": "19871"}]}},
                 "Config": {"Env": ["V2_DB00=" + seed["databases"][0],
                                    "V2_DB01=" + seed["databases"][1],
                                    "V2_DB02=" + seed["databases"][2],
                                    "V2_RABBIT_HOST=" + rabbit_name]}}
        rabbit = {"Name": "/" + rabbit_name, "Id": "rabbit-id", "Image": "rabbit-image",
                  "State": {"Running": True},
                  "NetworkSettings": {"Networks": {"bromark-assets-cluster-it": {}}}}
        def fake_inspect(args, **_):
            return subprocess.CompletedProcess(args, 0, json.dumps([rabbit if args[-1] == rabbit_name else state]), "")
        with patch.dict(os.environ, {"V2_APP_CONTAINER_1": "bromark-relay-20261016-app-1",
                                  "V2_TARGET_RUN_ID": "task9x", "V2_TEST_DOCKER_NETWORK": "bromark-assets-cluster-it",
                                  "V2_ACTIVE_RABBIT_CONTAINER": rabbit_name}), \
                patch("run_v2_experiment.subprocess.run", side_effect=fake_inspect), \
                patch("run_v2_experiment.mysql", side_effect=lambda db, _: db):
            self.assertEqual("owned-id", validate_owned_target(seed, 1)["id"])
            state["Config"]["Env"][-1] = "V2_RABBIT_HOST=bromark-v2-rabbit"
            with self.assertRaises(ValueError):
                validate_owned_target(seed, 1)
            state["Config"]["Env"][-1] = "V2_RABBIT_HOST=" + rabbit_name
            state["Name"] = "/bromark-phase6-app-1"
            with self.assertRaises(ValueError):
                validate_owned_target(seed, 1)

    def test_formal_denominator_and_server_stage_boundaries(self):
        requests = [
            {"requestId": "warm:1", "phase": "warmup", "user": "u0", "operation": "draw",
             "status": 202, "orderId": "pre", "state": "ACCEPTED", "latencyMs": 2},
            {"requestId": "formal:1", "user": "u1", "operation": "draw",
             "status": 202, "orderId": "draw", "state": "ACCEPTED", "latencyMs": 3},
            {"requestId": "formal:2", "user": "u2", "operation": "query",
             "status": 200, "orderId": None, "state": None, "latencyMs": 1},
        ]
        completions = [
            {"orderId": "pre", "kind": "draw", "state": "DELIVERED", "serverAcceptedAt": 99,
             "serverConfirmedAt": 99.5, "serverCompletedAt": 100},
            {"orderId": "draw", "kind": "draw", "state": "DELIVERED", "serverAcceptedAt": 100,
             "serverConfirmedAt": 100.5, "serverCompletedAt": 101},
        ]
        report = collect(requests, completions, {})
        self.assertEqual((2, 1, 1), (report["sent"], report["accepted"], report["completed"]))
        self.assertEqual(500, report["serverDrawConfirmationLatencyMs"]["p99"])
        self.assertEqual(1000, report["serverDrawDeliveryLatencyMs"]["p99"])
        self.assertEqual({"draw": 1, "query": 1}, report["userCountsByOperation"])
        self.assertEqual(1, report["orphanCompletions"])

    def test_unknown_503_and_transport_are_separate_from_redrive(self):
        requests = [
            {"requestId": "r1", "user": "u", "operation": "draw", "status": 503,
             "orderId": "original", "state": None, "latencyMs": 1},
            {"requestId": "r2", "user": "u", "operation": "draw", "status": 503,
             "orderId": None, "state": None, "latencyMs": 1},
            {"requestId": "r3", "user": "u", "operation": "draw", "status": 0,
             "orderId": None, "state": None, "latencyMs": 1},
            {"requestId": "r4", "user": "u", "operation": "draw", "status": 429,
             "orderId": None, "state": "REJECTED", "latencyMs": 1},
        ]
        report = collect(requests, [], {"transportRetries": 2})
        self.assertEqual(1, report["http503WithOrderId"])
        self.assertEqual(1, report["http503WithoutOrderId"])
        self.assertEqual(["original"], report["uncertainOrderIds"])
        self.assertEqual(1, report["overloadRejected"])
        self.assertEqual(2, report["transportRetries"])
        self.assertIsNone(report["redriveEmitted"])
        self.assertFalse(report["smokeDataComplete"])

    def test_uniform_distribution_and_incomplete_evidence_fail_closed(self):
        if not shutil.which("docker") or subprocess.run(["docker", "info"], capture_output=True).returncode:
            self.skipTest("Docker k6 probe unavailable")
        server = ThreadingHTTPServer(("0.0.0.0", 0), ProbeHandler)
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        try:
            with tempfile.TemporaryDirectory(prefix="bromark-relay-task9-probe-") as directory:
                root = Path(directory)
                tokens = {f"probe-{i:03d}": "synthetic" for i in range(200)}
                (root / "tokens.json").write_text(json.dumps(tokens))
                (root / "seed.json").write_text(json.dumps({
                    "runId": "probe", "activityId": 610016, "sku": 61, "version": "probe",
                    "resultOrders": {user: "pre-" + user for user in tokens},
                }))
                counts = {}
                for distribution in ("legacy", "uniform"):
                    output = root / (distribution + ".jsonl")
                    command = ["docker", "run", "--rm", "--name",
                               "bromark-relay-20261016-task9-k6-probe-" + uuid.uuid4().hex[:10],
                               "-v", str(ROOT) + ":/scripts:ro", "-v", str(root) + ":/seed:ro",
                               "-v", str(root) + ":/results",
                               "-e", "BASE_URL=http://host.docker.internal:%d" % server.server_port,
                               "-e", "TOKEN_FILE=/seed/tokens.json", "-e", "SEED_FILE=/seed/seed.json",
                               "-e", "SCENARIO=mixed", "-e", "RATE=500", "-e", "DURATION=5s",
                               "-e", "USER_DISTRIBUTION=" + distribution,
                               K6, "run", "--log-format", "json", "--console-output", "/results/" + output.name,
                               "/scripts/v2-load.js"]
                    result = subprocess.run(command, capture_output=True, text=True, timeout=90)
                    self.assertEqual(0, result.returncode, result.stderr[-1500:])
                    raw = records(output)
                    first = {int(row["requestId"].rsplit(":", 1)[1]): row for row in raw
                             if int(row["requestId"].rsplit(":", 1)[1]) < 2000}
                    self.assertEqual(set(range(2000)), set(first),
                                     "k6 must execute every global iteration in probe; observed=%d" % len(raw))
                    first = [first[i] for i in range(2000)]
                    self.assertEqual({"query": 1400, "result": 200, "exchange": 200, "draw": 200},
                                     {op: sum(r["operation"] == op for r in first)
                                      for op in ("query", "result", "exchange", "draw")})
                    counts[distribution] = len({r["user"] for r in first if r["operation"] == "draw"})
                    if distribution == "uniform":
                        for operation in ("query", "result", "exchange", "draw"):
                            self.assertEqual(200, len({r["user"] for r in first if r["operation"] == operation}))
                measured_uniform_draw_users = counts["uniform"]
                measured_legacy_draw_users = counts["legacy"]
                self.assertEqual(200, measured_uniform_draw_users)
                self.assertEqual(20, measured_legacy_draw_users)
                evidence = os.environ.get("TASK9_PROBE_EVIDENCE_DIR")
                if evidence:
                    destination = Path(evidence).resolve()
                    if ROOT.parents[1] in destination.parents or destination == ROOT.parents[1]:
                        raise ValueError("probe evidence must stay outside repository")
                    destination.mkdir(parents=True, exist_ok=False)
                    hashes = {}
                    for distribution in ("legacy", "uniform"):
                        source = root / (distribution + ".jsonl")
                        target = destination / source.name
                        shutil.copyfile(source, target)
                        hashes[target.name] = hashlib.sha256(target.read_bytes()).hexdigest()
                    (destination / "probe-manifest.json").write_text(json.dumps({
                        "iterationsChecked": 2000, "vusConfigured": 100,
                        "userCount": 200, "drawUsers": counts, "sha256": hashes,
                        "scope": "synthetic HTTP k6 distribution probe, not capacity evidence",
                    }, indent=2) + "\n")
        finally:
            server.shutdown()
            server.server_close()
            worker.join()

        incomplete_report = collect([
            {"requestId": "r1", "user": "u", "operation": "draw", "status": 202,
             "orderId": "o1", "state": "ACCEPTED", "latencyMs": 1},
            {"requestId": "r2", "user": "u", "operation": "draw", "status": 503,
             "orderId": "o2", "state": None, "latencyMs": 1},
            {"requestId": "r3", "user": "u", "operation": "draw", "status": 503,
             "orderId": None, "state": None, "latencyMs": 1},
        ], [], {})
        self.assertEqual(1, incomplete_report["accepted"])
        self.assertEqual(1, incomplete_report["pending"])
        self.assertEqual(1, incomplete_report["http503WithOrderId"])
        self.assertEqual(1, incomplete_report["http503WithoutOrderId"])
        self.assertFalse(incomplete_report["safeCapacityCandidate"])
        self.assertFalse(incomplete_report["fullCapacityPassed"])
        self.assertFalse(incomplete_report["releaseAuthorized"])


if __name__ == "__main__":
    unittest.main()
