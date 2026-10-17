#!/usr/bin/env python3
"""本次隔离双应用故障实验；拒绝旧资源，原始记录保存在全新外部目录。"""

import argparse
from datetime import datetime, timezone
import calendar
import hashlib
import hmac
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tarfile
import time
import urllib.error
import urllib.request
import uuid
import xml.etree.ElementTree as ET

from seed_v2_testdata import mysql, route, sql


ROOT = Path(__file__).resolve().parents[2]
OWNED = "bromark-relay-20261016-"
NETWORK = "bromark-assets-cluster-it"
DEPENDENCIES = {
    "V2_MYSQL_CONTAINER": "bromark-relay-20261016-mysql-1",
    "V2_RABBIT_CONTAINER": "bromark-relay-20261016-rabbitmq-1",
    "V2_REDIS_CONTAINER": "bromark-relay-20261016-redis-1",
    "V2_APP_CONTAINER_1": "bromark-relay-20261016-app-1",
    "V2_APP_CONTAINER_2": "bromark-relay-20261016-app-2",
    "V2_TEST_DOCKER_NETWORK": NETWORK,
}
ASSETS = ("bromark-assets-redis-1", "bromark-assets-redis-2", "bromark-assets-redis-3")
RABBIT = "bromark-relay-20261016-task8-rabbit"
IMAGE = "maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976"


def validate_seed_and_names(seed):
    for key, expected in DEPENDENCIES.items():
        if os.environ.get(key) != expected:
            raise ValueError("owned resource mismatch: " + key)
    run = seed.get("runId")
    if not isinstance(run, str) or not re.fullmatch(r"[a-z0-9_]{1,16}", run):
        raise ValueError("invalid owned run")
    expected = ["router_perf_it_%s_%s" % (run, part) for part in ("control", "one", "two")]
    if seed.get("databases") != expected:
        raise ValueError("target databases must exactly match owned run")
    if not seed.get("tokenFile") or not seed.get("publicKey"):
        raise ValueError("seed token/public key missing")
    if seed.get("writerAssertion") != "stopped-before-fresh-seed":
        raise ValueError("fresh seed writer stop assertion missing")
    return run


def docker(*args, check=True, env=None):
    return subprocess.run(["docker", *args], check=check, text=True, capture_output=True, env=env)


def inspect(name):
    result = docker("inspect", name, check=False)
    if result.returncode:
        raise ValueError("required container missing: " + name)
    state = json.loads(result.stdout)[0]
    if state["Name"] != "/" + name:
        raise ValueError("container name mismatch: " + name)
    return state


def port_binding(state, container_port, host_port):
    bindings = state["NetworkSettings"]["Ports"].get(container_port)
    if bindings != [{"HostIp": "127.0.0.1", "HostPort": str(host_port)}]:
        raise ValueError("owned dependency port binding mismatch: " + state["Name"])


def endpoints(states):
    mysql_state = inspect(DEPENDENCIES["V2_MYSQL_CONTAINER"])
    redis_state = inspect(DEPENDENCIES["V2_REDIS_CONTAINER"])
    if mysql_state["Id"] != states[DEPENDENCIES["V2_MYSQL_CONTAINER"]]["id"]:
        raise ValueError("MySQL identity changed")
    if redis_state["Id"] != states[DEPENDENCIES["V2_REDIS_CONTAINER"]]["id"]:
        raise ValueError("Redis identity changed")
    port_binding(mysql_state, "3306/tcp", 13316)
    port_binding(redis_state, "6379/tcp", 16386)
    nodes = []
    for index, name in enumerate(ASSETS, 1):
        state = inspect(name)
        if state["Id"] != states[name]["id"]:
            raise ValueError("asset Redis identity changed: " + name)
        aliases = state["NetworkSettings"]["Networks"][NETWORK]["Aliases"]
        alias = "redis-%d" % index
        if alias not in aliases:
            raise ValueError("asset Redis alias missing: " + alias)
        nodes.append("redis://%s:6379" % alias)
    return {"V2_MYSQL_HOST": "host.docker.internal:13316", "V2_REDIS_HOST": "host.docker.internal",
            "V2_REDIS_PORT": "16386", "V2_REDIS_CLUSTER_NODES": ",".join(nodes)}


def writer_inventory(seed):
    ids = docker("ps", "-q").stdout.splitlines()
    checked = []
    for cid in ids:
        result = json.loads(docker("inspect", cid).stdout)[0]
        if any(any(database in value for database in seed["databases"])
               for value in result["Config"].get("Env", [])):
            raise ValueError("running container refers to target database: " + result["Name"])
        checked.append({"name": result["Name"], "id": result["Id"]})
    return {"seedAssertion": seed["writerAssertion"], "runningContainersChecked": checked,
            "targetDatabaseProcesslist": 0}


def schema_check(database):
    expected_columns = {"aggregate_type", "aggregate_id", "observed_state", "observed_version",
                        "last_event_type", "last_event_id", "last_redrive_at", "next_redrive_at", "redrive_count"}
    actual_columns = set(mysql(database, "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA="
                               + sql(database) + " AND TABLE_NAME='v2_recovery_dispatch'").splitlines())
    if actual_columns != expected_columns:
        raise ValueError("007 dispatch columns mismatch: " + database)
    for table, required in (("outbox", {"pending_type", "aggregate_event"}),
                            ("retry_job", {"pending_type", "aggregate_event", "published_recovery"})):
        keys = set(mysql(database, "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA="
                         + sql(database) + " AND TABLE_NAME=" + sql(table)).splitlines())
        if not required.issubset(keys):
            raise ValueError("007 indexes missing: " + database + "." + table)


def preflight(seed):
    run = validate_seed_and_names(seed)
    os.environ["V2_TARGET_RUN_ID"] = run
    states = {}
    for key in ("V2_MYSQL_CONTAINER", "V2_RABBIT_CONTAINER", "V2_REDIS_CONTAINER"):
        name = DEPENDENCIES[key]
        state = inspect(name)
        if not state["State"]["Running"] or "bromark-relay-20261016_default" not in state["NetworkSettings"]["Networks"]:
            raise ValueError("dependency not owned/running: " + name)
        states[name] = {"id": state["Id"], "image": state["Image"]}
    for name in ASSETS:
        state = inspect(name)
        if not state["State"]["Running"] or NETWORK not in state["NetworkSettings"]["Networks"]:
            raise ValueError("asset Redis ownership mismatch: " + name)
        states[name] = {"id": state["Id"], "image": state["Image"]}
    for name in (DEPENDENCIES["V2_APP_CONTAINER_1"], DEPENDENCIES["V2_APP_CONTAINER_2"], RABBIT):
        if docker("inspect", name, check=False).returncode == 0:
            raise ValueError("app/task8 Rabbit name already exists; script must start its own: " + name)
    for database in seed["databases"]:
        if mysql(database, "SELECT DATABASE()") != database:
            raise ValueError("target database not accessible: " + database)
        if not mysql(database, "SHOW TABLES LIKE 'v2_recovery_dispatch'"):
            raise ValueError("007 dispatch missing: " + database)
        schema_check(database)
    names = ",".join(sql(db) for db in seed["databases"])
    active = mysql(None, "SELECT COUNT(*) FROM information_schema.processlist WHERE DB IN (" + names + ")")
    if int(active) != 0:
        raise ValueError("old/unknown target database writer or reader still connected")
    migration_hashes = seed.get("migrationHashes") or {}
    for file, expected_hash in migration_hashes.items():
        source = ROOT / "docs/dev-ops/mysql/migrations" / file
        if not source.is_file() or hashlib.sha256(source.read_bytes()).hexdigest() != expected_hash:
            raise ValueError("migration source mismatch: " + file)
    for name in ("007_recovery_dispatch.sql", "007_outbox_indexes.sql"):
        if name not in migration_hashes:
            raise ValueError("007 missing in seed manifest: " + name)
    return states, endpoints(states), writer_inventory(seed)


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def artifact_fingerprint():
    files = list((ROOT / "docs/performance").glob("*.py")) + [ROOT / "bm-app/target/bm-app.jar"]
    for module in ROOT.glob("bm-*"):
        for location, pattern in ((module / "src", "*.java"), (module / "target/classes", "*.class"),
                                  (module / "target/test-classes", "*.class")):
            if location.is_dir():
                files.extend(location.rglob(pattern))
    digest = hashlib.sha256()
    for path in sorted(files):
        digest.update(str(path.relative_to(ROOT)).encode() + b"\0")
        digest.update(hashlib.sha256(path.read_bytes()).digest())
    return {"sha256": digest.hexdigest(), "fileCount": len(files)}


def snapshot_artifact_bytes(out):
    destination = out / "artifact-bytes"
    destination.mkdir()
    for source in (ROOT / "bm-app/target/bm-app.jar",
                   ROOT / "bm-app/src/test/java/com/jay/test/v2/RelayFaultProbe.java",
                   ROOT / "docs/performance/fault_relay_optimization.py"):
        shutil.copyfile(source, destination / source.name)
    for source in sorted((ROOT / "docs/performance").glob("*.py")):
        shutil.copyfile(source, destination / source.name)
    for source in sorted((ROOT / "bm-app/target/test-classes/com/jay/test/v2").glob("RelayFaultProbe*.class")):
        shutil.copyfile(source, destination / source.name)
    with tarfile.open(destination / "source-java.tar", "w") as archive:
        for module in ROOT.glob("bm-*"):
            for source in sorted((module / "src").rglob("*.java")):
                archive.add(source, arcname=source.relative_to(ROOT))


def write_json(path, value):
    Path(path).write_text(json.dumps(value, indent=2, sort_keys=True, default=str) + "\n")


def classpath():
    xml = ROOT / "bm-app/target/surefire-reports/TEST-com.jay.test.v2.RelayOptimizationFaultV2Test.xml"
    if not xml.is_file():
        raise ValueError("focused Java fault probe test must be built first")
    for node in ET.parse(str(xml)).getroot().findall(".//property"):
        if node.attrib.get("name") == "java.class.path":
            value = node.attrib.get("value", "")
            if "/workspace/bm-app/target/test-classes" not in value:
                raise ValueError("test-only classpath missing")
            entries = [item for item in value.split(":") if item]
            normalized = [item.replace("/root/.m2/repository/", "/m2/repository/", 1)
                          if item.startswith("/root/.m2/repository/") else item for item in entries]
            if any(item.startswith("/root/.m2/") for item in normalized):
                raise ValueError("unmounted Maven repository in test-only classpath")
            for item in normalized:
                if item.startswith("/workspace/"):
                    source = ROOT / item.removeprefix("/workspace/")
                elif item.startswith("/m2/repository/"):
                    source = Path("/tmp/bromark-java25-m2") / item.removeprefix("/m2/")
                else:
                    raise ValueError("test-only classpath uses unverified mount")
                if not source.exists():
                    raise ValueError("test-only classpath entry missing: " + item)
            return ":".join(normalized)
    raise ValueError("fault probe classpath absent")


def wait_for(check, seconds, message):
    end = time.monotonic() + seconds
    last_error = None
    while time.monotonic() < end:
        try:
            result = check()
            if result:
                return result
        except (OSError, urllib.error.URLError, ValueError) as error:
            last_error = error
        time.sleep(0.25)
    if last_error is not None:
        raise RuntimeError(message + "; last error: " + type(last_error).__name__ + ": " + str(last_error)) from last_error
    raise RuntimeError(message)


def wait_for_file_fast(path, seconds, message):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        if path.is_file():
            return
        time.sleep(0.01)
    raise RuntimeError(message)


def start_rabbit(owned, owned_volumes, run):
    original = inspect(DEPENDENCIES["V2_RABBIT_CONTAINER"])
    rabbit_image = original["Image"]
    volume = "bromark-relay-20261016-task8-rabbit-" + run + "-" + uuid.uuid4().hex[:12]
    if docker("volume", "inspect", volume, check=False).returncode == 0:
        raise ValueError("task8 Rabbit volume already exists: " + volume)
    docker("volume", "create", volume)
    owned_volumes.append(volume)
    env = dict(os.environ, RABBITMQ_DEFAULT_USER="bromark_it", RABBITMQ_DEFAULT_PASS=os.environ["V2_RABBIT_PASSWORD"])
    result = docker("run", "-d", "--name", RABBIT, "--network", NETWORK, "--hostname", "task8rabbit",
                    "--mount", "type=volume,src=" + volume + ",dst=/var/lib/rabbitmq",
                    "-e", "RABBITMQ_DEFAULT_USER", "-e", "RABBITMQ_DEFAULT_PASS",
                    rabbit_image, env=env)
    cid = result.stdout.strip()
    owned.append(cid)
    # CLI 与 server 同一 UID；root CLI 抢先创建 0400 cookie 会让 rabbitmq UID 无法启动。
    wait_for(lambda: docker("exec", "--user", "rabbitmq", cid, "rabbitmq-diagnostics", "-q", "ping", check=False).returncode == 0,
             45, "owned Rabbit did not start")
    wait_for(lambda: docker("exec", "--user", "rabbitmq", cid, "rabbitmqctl", "authenticate_user", "bromark_it",
                            os.environ["V2_RABBIT_PASSWORD"], check=False).returncode == 0,
             45, "owned Rabbit authentication failed")
    return cid


def start_app(number, seed, evidence, prefix, cp, bound_endpoints, owned, relay_mode="bounded", payment=False,
              settle_seconds=1):
    name = DEPENDENCIES["V2_APP_CONTAINER_%d" % number]
    port = 19870 + number
    key = "V2_APP_CONTAINER_%d" % number
    env = dict(os.environ)
    env.update(bound_endpoints)
    for index, db in enumerate(seed["databases"]):
        env["V2_DB%02d" % index] = db
    env.update(V2_RABBIT_HOST=RABBIT, V2_RABBIT_PORT="5672")
    args = [
        "docker", "run", "-d", "--name", name, "--network", NETWORK,
        "--cpus", "1", "--memory", "512m", "-p", "127.0.0.1:%d:8080" % port,
        "-v", str(ROOT) + ":/workspace:ro", "-v", "/tmp/bromark-java25-m2:/m2:ro",
        "-v", str(evidence) + ":/evidence", "-w", "/evidence/app-%d" % number,
    ]
    for variable in ("V2_MYSQL_PASSWORD", "V2_MYSQL_HOST", "V2_RABBIT_PASSWORD", "V2_RABBIT_HOST",
                     "V2_RABBIT_PORT", "V2_REDIS_CLUSTER_NODES", "V2_REDIS_HOST", "V2_REDIS_PORT",
                     "V2_DB00", "V2_DB01", "V2_DB02"):
        args.extend(["-e", variable])
    if payment:
        if not os.environ.get("V2_PAYMENT_ACCOUNT") or len(os.environ.get("V2_PAYMENT_SIGNING_SECRET", "")) < 32:
            raise ValueError("isolated sandbox payment credentials missing")
        args.extend(["-e", "V2_PAYMENT_ACCOUNT", "-e", "V2_PAYMENT_SIGNING_SECRET"])
    args.extend([
        IMAGE, "java", "-Xmx320m", "-Xlog:gc*:file=/evidence/gc-%d.log:time,uptime,level,tags" % number,
        "-cp", cp, "com.jay.test.v2.RelayFaultProbe", "/evidence/app-%d" % number,
        "--spring.profiles.active=" + ("v2-it,local-sandbox" if payment else "v2-it"),
        "--server.port=8080", "--bm.v2.enabled=true",
        "--bm.v2.asset-write-mode=V2", "--bm.v2.messaging.enabled=true", "--bm.v2.raffle.enabled=true",
        "--bm.v2.payment.enabled=" + str(payment).lower(),
        "--bm.v2.messaging.relay.mode=" + relay_mode, "--bm.v2.recovery.redrive.mode=guarded",
        "--bm.v2.messaging.relay.settle-timeout-seconds=" + str(settle_seconds),
        "--bm.v2.budget.replica-count=2", "--bm.v2.budget.physical-connection-limits.mysql-it=80",
        "--bm.v2.messaging.prefix=" + prefix, "--bm.v2.auth.issuer=https://issuer.test",
        "--bm.v2.auth.audience=bromark", "--bm.v2.auth.public-key=" + seed["publicKey"],
        "--bm.v2.local-fallback-rps=1", "--bm.v2.cancel-rps=100", "--bm.v2.cancel-max-in-flight=4",
    ])
    for name_part in ("user-rps", "activity-rps", "max-in-flight", "query-rps", "query-max-in-flight"):
        args.append("--bm.v2.%s=100" % name_part)
    proc = subprocess.run(args, env=env, text=True, capture_output=True, check=True)
    cid = proc.stdout.strip()
    owned.append(cid)
    ready = evidence / ("app-%d" % number) / "ready"
    wait_for(lambda: ready.is_file() and ready.read_text().strip(), 45, "application %d did not start" % number)
    state = inspect(name)
    if state["Id"] != cid or NETWORK not in state["NetworkSettings"]["Networks"] or state["Image"] != docker("image", "inspect", "--format", "{{.Id}}", IMAGE).stdout.strip():
        raise ValueError("app ownership changed during startup")
    return {"name": name, "id": cid, "port": port, "ready": ready.read_text().strip(), "image": state["Image"]}


def request(port, path, token, body=None):
    headers = {"Authorization": "Bearer " + token, "Content-Type": "application/json"}
    raw = None if body is None else json.dumps(body, separators=(",", ":")).encode()
    req = urllib.request.Request("http://127.0.0.1:%d%s" % (port, path), data=raw, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=7) as response:
            return {"status": response.status, "body": json.loads(response.read().decode())}
    except urllib.error.HTTPError as error:
        text = error.read().decode()
        try:
            value = json.loads(text)
        except ValueError:
            value = text
        return {"status": error.code, "body": value}


def smoke_health(app):
    with urllib.request.urlopen("http://127.0.0.1:%d/actuator/health" % app["port"], timeout=5) as response:
        body = json.loads(response.read().decode())
        if response.status != 200 or body.get("status") != "UP":
            raise RuntimeError("app health not UP: " + app["name"])
    state = inspect(app["name"])
    mounts = {item["Destination"]: item for item in state["Mounts"]}
    if mounts["/workspace"]["RW"] or not mounts["/evidence"]["RW"]:
        raise RuntimeError("source/evidence mount permissions mismatch: " + app["name"])
    return {"name": app["name"], "containerId": app["id"], "pid": app["ready"].split()[0],
            "health": "UP", "sourceReadOnly": True, "evidenceWritable": True}


def signed_notification(port, event):
    raw = json.dumps(event, separators=(",", ":")).encode()
    signature = hmac.new(os.environ["V2_PAYMENT_SIGNING_SECRET"].encode(), raw, hashlib.sha256).hexdigest()
    req = urllib.request.Request("http://127.0.0.1:%d/api/v2/payment/notifications/sandbox" % port,
                                 data=raw, headers={"Content-Type": "application/json",
                                                    "x-sandbox-signature": signature})
    try:
        with urllib.request.urlopen(req, timeout=7) as response:
            return {"status": response.status, "body": response.read().decode(), "rawSha256": hashlib.sha256(raw).hexdigest()}
    except urllib.error.HTTPError as error:
        return {"status": error.code, "body": error.read().decode(), "rawSha256": hashlib.sha256(raw).hexdigest()}


def run_correctness_population(seed, app1, app2, evidence, tokens):
    case = evidence / "correctness-population"
    case.mkdir()
    users = list(tokens)
    if len(users) < 3:
        raise ValueError("correctness population requires three isolated users")
    draw_user, sign_user, pay_user = users[:3]
    intents = [
        ("draw", app1, draw_user, "/api/v2/draws", {"requestId": "t8-correct-draw-" + uuid.uuid4().hex, "activityId": seed["activityId"]}),
        ("signin", app2, sign_user, "/api/v2/rebates/sign-ins", {"requestId": "t8-correct-sign-" + uuid.uuid4().hex,
                                                                   "targetDate": time.strftime("%Y-%m-%d", time.gmtime())}),
        ("payment", app1, pay_user, "/api/v2/payment/orders", {"requestId": "t8-correct-pay-" + uuid.uuid4().hex,
                                                                  "productId": "sku", "provider": "sandbox"}),
    ]
    responses = {}
    for kind, app, user, path, body in intents:
        write_json(case / (kind + "-intent.json"), {"user": user, "path": path, "body": body, "at": time.time()})
        response = request(app["port"], path, tokens[user], body)
        write_json(case / (kind + "-response.json"), response)
        if response["status"] not in (200, 202) or not isinstance(response["body"], dict):
            raise RuntimeError("correctness " + kind + " not accepted; original response retained")
        responses[kind] = response["body"]
    draw_order = responses["draw"].get("orderId")
    payment_order = responses["payment"].get("orderId")
    if not draw_order or not payment_order or responses["signin"].get("state") != "DELIVERED":
        raise RuntimeError("correctness draw/payment identity or signin delivery missing")
    control = seed["databases"][0]
    amount_currency = mysql(control, "SELECT amount,currency FROM payment_product WHERE product_id='sku'").split("\t")
    if len(amount_currency) != 2 or mysql(control, "SELECT COUNT(*) FROM sandbox_payment WHERE merchant_order_id=" + sql(payment_order)) != "1":
        raise RuntimeError("sandbox payment order was not persisted")
    event = {"provider": "sandbox", "providerAccount": os.environ["V2_PAYMENT_ACCOUNT"],
             "environment": "local-sandbox", "eventId": "t8-correct-paid-" + uuid.uuid4().hex,
             "transactionId": "t8-correct-tx-" + uuid.uuid4().hex, "merchantOrderId": payment_order,
             "type": "PAID", "amount": amount_currency[0], "currency": amount_currency[1], "userBinding": pay_user}
    raw = json.dumps(event, separators=(",", ":"))
    write_json(case / "sandbox-fact.json", event)
    mysql(control, "UPDATE sandbox_payment SET status='PAID',fact=" + sql(raw) + " WHERE merchant_order_id=" + sql(payment_order))
    notification = signed_notification(app2["port"], event)
    write_json(case / "signed-notification-result.json", notification)
    if notification["status"] != 200 or notification["body"] != "SUCCESS":
        raise RuntimeError("signed sandbox payment callback not accepted")
    def delivered(app, path, token):
        value = request(app["port"], path, token)
        return value if value["status"] == 200 and value["body"].get("state") == "DELIVERED" else None
    draw_final = wait_for(lambda: delivered(app2, "/api/v2/draws/" + draw_order, tokens[draw_user]),
                          120, "correctness draw did not deliver")
    pay_final = wait_for(lambda: delivered(app1, "/api/v2/payment/orders/" + payment_order, tokens[pay_user]),
                         120, "correctness payment did not deliver")
    write_json(case / "delivered.json", {"draw": draw_final, "payment": pay_final})
    return {"drawOrderId": draw_order, "paymentOrderId": payment_order,
            "signinUser": sign_user, "paymentEventId": event["eventId"]}


def rows(database, columns, statement):
    raw = mysql(database, statement)
    values = []
    for line in raw.splitlines():
        fields = line.split("\t")
        if len(fields) != len(columns):
            raise ValueError("SQL result width mismatch: " + database)
        values.append(dict(zip(columns, fields)))
    return values


def source_rows(seed, user, order):
    db_index, suffix = route(user)
    result = {}
    for index, db in enumerate(seed["databases"]):
        key = sql(order)
        result[db] = {
            "outbox": rows(db, ("eventId", "eventType", "state", "attempt", "leaseToken", "payloadHash", "publishedAt"),
                           "SELECT event_id,event_type,state,attempt,lease_token,payload_hash,COALESCE(DATE_FORMAT(published_at,'%%Y-%%m-%%dT%%H:%%i:%%s.%%f'),'UNPUBLISHED') FROM outbox WHERE aggregate_id=%s ORDER BY created_at,event_id" % key),
            "retry": rows(db, ("eventId", "eventType", "state", "attempt", "leaseToken", "failureCount"),
                          "SELECT event_id,event_type,state,attempt,lease_token,failure_count FROM retry_job WHERE aggregate_id=%s ORDER BY created_at,event_id" % key),
            "dispatch": rows(db, ("aggregateId", "lastEventId", "redriveCount"),
                             "SELECT aggregate_id,last_event_id,redrive_count FROM v2_recovery_dispatch WHERE aggregate_id=%s" % key),
        }
        if index == db_index:
            result[db]["order"] = rows(db, ("orderId", "state", "recoveryStatus", "version"),
                                        "SELECT order_id,state,recovery_status,version FROM v2_draw_order_%03d WHERE order_id=%s" % (suffix, key))
            result[db]["ledger"] = rows(db, ("bizId", "rows"),
                                         "SELECT biz_id,COUNT(*) FROM user_credit_ledger_%03d WHERE biz_type='DRAW_AWARD' AND biz_id=%s GROUP BY biz_id" % (suffix, key))
        if index == 0:
            result[db]["reservation"] = rows(db, ("orderId", "state", "recoveryStatus", "version"),
                                              "SELECT draw_order_id,state,recovery_status,version FROM award_reservation WHERE draw_order_id=%s" % key)
    return result


def event_row(snapshot, event_id=None):
    found = [(database, row) for database, tables in snapshot.items() for row in tables["outbox"]
             if row["eventType"] == "PrepareDraw" and (event_id is None or row["eventId"] == event_id)]
    if len(found) != 1:
        raise RuntimeError("expected exactly one original PrepareDraw row across three databases")
    return found[0]


def marker_fields(path):
    parts = path.read_text().strip().split("\t")
    if len(parts) != 6:
        raise RuntimeError("invalid precise after-confirm marker")
    java_instant_ns(parts[5])
    return {"pid": parts[0], "eventId": parts[1], "aggregateId": parts[2],
            "leaseToken": parts[3], "attempt": int(parts[4]), "at": parts[5]}


def verify_marker(marker, app, order, row):
    ready = app["ready"].split()
    if marker["pid"] != ready[0] or marker["eventId"] != row["eventId"] or marker["aggregateId"] != order:
        raise RuntimeError("precise after-confirm identity/PID mismatch")
    if marker["leaseToken"] != row["leaseToken"] or marker["attempt"] != int(row["attempt"]):
        raise RuntimeError("precise after-confirm lease/attempt mismatch")
    if row["state"] != "PROCESSING":
        raise RuntimeError("confirmed event is not fenced PROCESSING before settlement")
    if inspect(app["name"])["Id"] != app["id"]:
        raise RuntimeError("owned app ID changed before fault")


def verify_settle_connection(fields, app, event, source_db, process):
    if len(fields) != 5 or fields[0] != app["ready"].split()[0] or fields[1] != event:
        raise RuntimeError("settlement connection marker identity/PID mismatch")
    connection_id = int(fields[2])
    if connection_id < 1 or len(process) != 1 or process[0]["id"] != str(connection_id) or process[0]["database"] != source_db:
        raise RuntimeError("settlement connection is not bound to target database")
    return connection_id


def java_instant_ns(value):
    match = re.fullmatch(r"(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d)(?:\.(\d{3}|\d{6}|\d{9}))?Z", value)
    if not match:
        raise ValueError("invalid Java Instant marker")
    second = calendar.timegm(datetime.fromisoformat(match.group(1)).replace(tzinfo=timezone.utc).timetuple())
    fraction = match.group(2) or ""
    return second * 1_000_000_000 + int(fraction.ljust(9, "0") or "0")


def kill_connection_timed(connection_id):
    if not isinstance(connection_id, int) or connection_id < 1:
        raise ValueError("invalid target connection ID")
    clock = "TIMESTAMPDIFF(MICROSECOND,'1970-01-01',UTC_TIMESTAMP(6))"
    output = mysql(None, "SELECT %s; KILL CONNECTION %d; SELECT %s" % (clock, connection_id, clock))
    values = output.splitlines()
    if len(values) != 2 or not all(value.isdigit() for value in values):
        raise RuntimeError("MySQL KILL server clock receipt missing")
    before_us, after_us = map(int, values)
    if after_us < before_us:
        raise RuntimeError("MySQL KILL server clock went backwards")
    return before_us, after_us


def run_confirm_sigkill(seed, app1, start_survivor, evidence, tokens):
    case = evidence / "confirm-sigkill"
    case.mkdir()
    user, token = next(iter(tokens.items()))
    request_id = "task8-sigkill-" + uuid.uuid4().hex
    body = {"requestId": request_id, "activityId": seed["activityId"]}
    write_json(case / "request-intent.json", {"user": user, "requestId": request_id, "request": body,
                                                   "port": app1["port"], "at": time.time()})
    try:
        accepted = request(app1["port"], "/api/v2/draws", token, body)
        write_json(case / "request.json", {"user": user, "requestId": request_id, "response": accepted})
    except (OSError, urllib.error.URLError) as error:
        write_json(case / "request.json", {"user": user, "requestId": request_id,
                                            "outcome": "UNKNOWN", "errorType": type(error).__name__})
        raise
    order = accepted["body"].get("orderId") if isinstance(accepted["body"], dict) else None
    if not order:
        raise RuntimeError("draw response lacks orderId; raw 503/UNKNOWN retained")
    initial = source_rows(seed, user, order)
    write_json(case / "before-release.json", initial)
    original_db, pending = event_row(initial)
    if pending["state"] != "PENDING":
        raise RuntimeError("claim gate failed: original event already claimed")
    original_event, original_hash = pending["eventId"], pending["payloadHash"]
    (evidence / "app-1" / "arm").write_text(original_event + "\n")
    (evidence / "app-1" / "release-claims").write_text("release\n")
    signal = evidence / "app-1" / "confirmed-blocked"
    wait_for(lambda: signal.is_file(), 15, "no real publisher confirm signal")
    before = source_rows(seed, user, order)
    write_json(case / "before.json", before)
    before_db, first = event_row(before, original_event)
    first_marker = marker_fields(signal)
    verify_marker(first_marker, app1, order, first)
    if before_db != original_db or first["payloadHash"] != original_hash:
        raise RuntimeError("original event moved or payload hash changed before kill")
    (evidence / "app-2" / "arm").write_text(original_event + "\n")
    (evidence / "app-2" / "gate-claims").write_text("hold until app-1 SIGKILL is proven\n")
    app2 = start_survivor()
    killed = docker("kill", "--signal=KILL", app1["id"])
    if killed.stdout.strip() != app1["id"]:
        raise RuntimeError("actual SIGKILL target mismatch")
    exit_code = docker("wait", app1["id"]).stdout.strip()
    write_json(case / "kill.json", {"targetContainerId": app1["id"], "preciseMarker": first_marker,
                                    "signal": "KILL", "exitCode": exit_code, "at": time.time()})
    if exit_code != "137":
        raise RuntimeError("SIGKILL did not exit 137")
    (evidence / "app-2" / "release-claims").write_text("app-1 exited 137\n")
    second_signal = evidence / "app-2" / "confirmed-blocked"
    wait_for(lambda: second_signal.is_file(), 120, "survivor did not confirm original event")
    takeover = source_rows(seed, user, order)
    write_json(case / "takeover.json", takeover)
    second_db, second = event_row(takeover, original_event)
    second_marker = marker_fields(second_signal)
    verify_marker(second_marker, app2, order, second)
    if second_db != original_db or second["payloadHash"] != original_hash:
        raise RuntimeError("takeover changed original event identity or hash")
    if int(second["attempt"]) <= int(first["attempt"]) or second["leaseToken"] == first["leaseToken"]:
        raise RuntimeError("same original event lacks fenced lease takeover")
    write_json(case / "takeover-marker.json", second_marker)
    (evidence / "app-2" / "release-confirm").write_text("settle\n")
    def recovered():
        result = request(app2["port"], "/api/v2/draws/" + order, token)
        snapshot = source_rows(seed, user, order)
        _, final_event = event_row(snapshot, original_event)
        if result["status"] == 200 and result["body"].get("state") == "DELIVERED" and final_event["state"] == "PUBLISHED":
            return result
        return None
    final_response = wait_for(recovered, 120, "original draw did not converge after SIGKILL")
    after = source_rows(seed, user, order)
    write_json(case / "after.json", after)
    after_db, final_event = event_row(after, original_event)
    if after_db != original_db or final_event["payloadHash"] != original_hash or final_event["state"] != "PUBLISHED":
        raise RuntimeError("original event/hash not PUBLISHED")
    ledgers = [row for tables in after.values() for row in tables.get("ledger", []) if row["bizId"] == order]
    if len(ledgers) != 1 or ledgers[0]["rows"] != "1":
        raise RuntimeError("award ledger is not exactly one")
    write_json(case / "result.json", {"orderId": order, "originalEventId": original_event, "originalPayloadHash": original_hash,
                                        "firstAttempt": first["attempt"], "takeoverAttempt": second["attempt"],
                                        "leaseTokenChanged": True, "originalFinalState": final_event["state"],
                                        "awardLedgerRows": 1, "final": final_response})
    return {"orderId": order, "eventId": original_event, "payloadHash": original_hash}


def receipt_snapshot(seed, user, order, failure_dir=None):
    from reconcile_v2_run import rows as all_rows
    database_index, suffix = route(user)
    database = seed["databases"][database_index]
    def read_table(db, table):
        try:
            return all_rows(db, table)
        except json.JSONDecodeError as error:
            if failure_dir is not None:
                write_json(Path(failure_dir) / ("sql-json-error-" + uuid.uuid4().hex + ".json"),
                           {"database": db, "table": table, "rawSqlJson": error.doc,
                            "error": str(error), "at": time.time()})
            raise RuntimeError("nonempty SQL JSON invalid: " + db + "." + table) from error
    def matching(table, key, value):
        return [row for row in read_table(database, table) if row.get(key) == value]
    snapshot = source_rows(seed, user, order)
    return {"source": snapshot,
            "deferred": matching("deferred_fact", "aggregate_id", order),
            "cases": matching("recovery_case", "aggregate_id", order),
            "assets": {
                "credit": matching("user_credit_account", "user_id", user),
                "quota": matching("raffle_activity_account", "user_id", user),
                "quotaDay": matching("raffle_activity_account_day", "user_id", user),
                "quotaMonth": matching("raffle_activity_account_month", "user_id", user),
                "creditLedger": matching("user_credit_ledger_%03d" % suffix, "user_id", user),
                "quotaLedger": matching("user_quota_ledger_%03d" % suffix, "user_id", user),
                "stock": read_table(seed["databases"][0], "award_stock"),
                "reservation": [row for row in read_table(seed["databases"][0], "award_reservation")
                                if row.get("draw_order_id") == order]}}


def owned_broker_snapshot(owned, prefix):
    state = inspect(RABBIT)
    if state["Id"] not in owned or NETWORK not in state["NetworkSettings"]["Networks"]:
        raise RuntimeError("fresh Rabbit identity not owned")
    result = {"id": state["Id"], "image": state["Image"], "running": state["State"]["Running"],
              "volumes": [{"name": row.get("Name"), "destination": row["Destination"]}
                          for row in state["Mounts"] if row["Type"] == "volume"]}
    if not result["running"]:
        return {**result, "queues": None, "missingReason": "owned broker stopped"}
    raw = docker("exec", "--user", "rabbitmq", state["Id"], "rabbitmqctl", "-q", "list_queues",
                 "name", "type", "durable", "messages_ready", "messages_unacknowledged").stdout
    found = [line.split("\t") for line in raw.splitlines() if line.startswith(prefix + ".")]
    if {row[0] for row in found} != {prefix + ".workflow", prefix + ".notifications"} or any(len(row) != 5 for row in found):
        raise RuntimeError("fresh broker queues do not match owned prefix")
    return {**result, "queues": found, "raw": raw, "oldestAgeSeconds": None,
            "ageMissingReason": "Rabbit queue counters do not expose oldest message enqueue timestamp"}


def task_inventory(seed):
    from reconcile_v2_run import rows as all_rows
    return {db: {table: all_rows(db, table) for table in ("outbox", "retry_job", "v2_recovery_dispatch")}
            for db in seed["databases"]}


def verify_preserved_tasks(before, after):
    for db, tables in before.items():
        for table, originals in tables.items():
            keys = ("aggregate_type", "aggregate_id") if table == "v2_recovery_dispatch" else ("event_id",)
            actual = {tuple(row[key] for key in keys): row for row in after[db][table]}
            for row in originals:
                key = tuple(row[field] for field in keys)
                if key not in actual:
                    raise RuntimeError("rollback deleted original " + table)
                current = actual[key]
                if table == "v2_recovery_dispatch":
                    if current["redrive_count"] < row["redrive_count"]:
                        raise RuntimeError("dispatch count decreased; requires separate phase-progress audit, not an automatic pass")
                elif current["payload_hash"] != row["payload_hash"] or current["attempt"] < row["attempt"]:
                    raise RuntimeError("rollback changed event hash or reset attempt")


def verify_graceful_stops(stops):
    for row in stops:
        if (row["running"] or row["exitCode"] not in (0, 143) or row["oomKilled"] or row["stateError"]
                or row["inspectedId"] != row["id"] or row["elapsedSeconds"] > 30
                or row["stopOutput"] != row["id"]):
            raise RuntimeError("bounded workers did not gracefully exit within common deadline")


def run_rollback(seed, app1, app2, evidence, tokens, cp, bound_endpoints, owned, prefix):
    from concurrent.futures import ThreadPoolExecutor
    case = evidence / "rollback"
    case.mkdir()
    for number in (1, 2):
        folder = evidence / ("app-%d" % number)
        write_json(folder / "command-stop-consumers.tmp", {"runId": seed["runId"]})
        (folder / "command-stop-consumers.tmp").replace(folder / "command-stop-consumers.json")
        wait_for(lambda: (folder / "stop-consumers-result.json").is_file(), 15, "actual consumers did not stop")
    user, token = next(iter(tokens.items()))
    body = {"requestId": "task8-rollback-" + uuid.uuid4().hex, "activityId": seed["activityId"]}
    write_json(case / "request-intent.json", {"user": user, "body": body, "at": time.time()})
    try:
        accepted = request(app1["port"], "/api/v2/draws", token, body)
        write_json(case / "request.json", accepted)
    except (OSError, urllib.error.URLError) as error:
        write_json(case / "request.json", {"outcome": "UNKNOWN", "errorType": type(error).__name__})
        raise
    order = accepted["body"].get("orderId") if isinstance(accepted["body"], dict) else None
    if not order:
        raise RuntimeError("rollback lacks accepted original order")
    before_release = source_rows(seed, user, order)
    write_json(case / "before-release.json", before_release)
    _, original = event_row(before_release)
    for number in (1, 2):
        (evidence / ("app-%d" % number) / "release-claims").write_text("real original event may publish\n")
    def dispatched():
        snapshot = source_rows(seed, user, order)
        if any(int(row["redriveCount"]) > 0 for tables in snapshot.values() for row in tables["dispatch"]):
            return snapshot
        return None
    actual_dispatch = wait_for(dispatched, 100, "production guarded recovery did not emit real dispatch")
    write_json(case / "real-dispatch.json", actual_dispatch)
    bounded = []
    for app in (app1, app2):
        state = inspect(app["name"])
        args = state["Config"]["Cmd"]
        bindings = [entry for entry in state["Config"]["Env"] if entry.startswith(("V2_DB00=", "V2_DB01=", "V2_DB02="))]
        if (state["Id"] != app["id"] or not state["State"]["Running"]
                or "--bm.v2.messaging.relay.mode=bounded" not in args
                or "--bm.v2.recovery.redrive.mode=guarded" not in args
                or set(bindings) != {"V2_DB%02d=%s" % (i, db) for i, db in enumerate(seed["databases"])}):
            raise RuntimeError("bounded writer mode/ID/database binding not proven")
        bounded.append({"id": app["id"], "name": app["name"], "mode": "bounded", "recovery": "guarded", "databases": bindings})
    try:
        writer_inventory(seed)
    except ValueError as error:
        if "running container refers" not in str(error):
            raise
        write_json(case / "old-writer-rejected.json", {"rejected": True, "reason": str(error), "knownWriters": bounded})
    else:
        raise RuntimeError("guarded switch failed to reject live old writers")
    broker_before = owned_broker_snapshot(owned, prefix)
    write_json(case / "broker-before.json", broker_before)
    before = task_inventory(seed)
    assets_before = receipt_snapshot(seed, user, order, case)
    write_json(case / "before-stop-tasks.json", before)
    write_json(case / "before-stop-assets.json", assets_before)
    start = time.monotonic()
    common_end = start + 30
    def stop(app):
        remaining = max(1, int(common_end - time.monotonic()))
        receipt = docker("stop", "--timeout", str(remaining), app["id"])
        state = inspect(app["name"])
        return {"id": app["id"], "stopOutput": receipt.stdout.strip(), "exitCode": state["State"]["ExitCode"],
                "running": state["State"]["Running"], "elapsedSeconds": time.monotonic() - start,
                "inspectedId": state["Id"], "oomKilled": state["State"]["OOMKilled"], "stateError": state["State"]["Error"]}
    with ThreadPoolExecutor(max_workers=2) as pool:
        stops = list(pool.map(stop, (app1, app2)))
    write_json(case / "shared-stop.json", {"sharedDeadlineSeconds": 30, "receipts": stops})
    verify_graceful_stops(stops)
    inventory = writer_inventory(seed)
    names = ",".join(sql(db) for db in seed["databases"])
    connections = int(mysql(None, "SELECT COUNT(*) FROM information_schema.processlist WHERE DB IN (" + names + ")"))
    if connections:
        raise RuntimeError("old target database connections remain before legacy start")
    stopped = task_inventory(seed)
    write_json(case / "stopped-tasks.json", stopped)
    verify_preserved_tasks(before, stopped)
    stopped_assets = receipt_snapshot(seed, user, order, case)
    write_json(case / "stopped-assets.json", stopped_assets)
    if stopped_assets["assets"] != assets_before["assets"]:
        raise RuntimeError("stopping bounded changed assets")
    write_json(case / "stopped-writer-inventory.json", {"knownStopped": bounded, "runningInventory": inventory, "targetConnections": connections})
    for app in (app1, app2):
        log = docker("logs", app["id"], check=False)
        (case / ("bounded-" + app["id"][:12] + ".log")).write_text(log.stdout + log.stderr)
        removed = docker("rm", app["id"])
        if removed.stdout.strip() != app["id"] or docker("inspect", app["id"], check=False).returncode == 0:
            raise RuntimeError("stopped owned app removal failed")
        owned.remove(app["id"])
        write_json(case / ("removed-" + app["id"][:12] + ".json"), {"id": app["id"], "absent": True})
    legacy_dir = evidence / "legacy"
    for number in (1, 2):
        (legacy_dir / ("app-%d" % number)).mkdir(parents=True)
    legacy1 = start_app(1, seed, legacy_dir, prefix, cp, bound_endpoints, owned, relay_mode="legacy")
    legacy2 = start_app(2, seed, legacy_dir, prefix, cp, bound_endpoints, owned, relay_mode="legacy")
    write_json(case / "legacy-apps.json", {"apps": [legacy1, legacy2], "relayMode": "legacy", "recoveryMode": "guarded"})
    def delivered():
        response = request(legacy1["port"], "/api/v2/draws/" + order, token)
        return response if response["status"] == 200 and response["body"].get("state") == "DELIVERED" else None
    final = wait_for(delivered, 120, "legacy relay plus guarded did not converge original order")
    wait_owned_tasks_drained(seed)
    broker_after = owned_broker_snapshot(owned, prefix)
    write_json(case / "broker-after.json", broker_after)
    if any(broker_after[key] != broker_before[key] for key in ("id", "image", "volumes")):
        raise RuntimeError("rollback changed broker identity or persistent volume")
    after = task_inventory(seed)
    write_json(case / "after-tasks.json", after)
    verify_preserved_tasks(stopped, after)
    assets_after = receipt_snapshot(seed, user, order, case)
    write_json(case / "after-assets.json", assets_after)
    _, original_final = event_row(assets_after["source"], original["eventId"])
    if original_final["payloadHash"] != original["payloadHash"] or original_final["state"] != "PUBLISHED":
        raise RuntimeError("rollback original identity lost")
    awards = [row for row in assets_after["assets"]["creditLedger"] if row["biz_id"] == order and row["biz_type"] == "DRAW_AWARD"]
    if len(awards) != 1:
        raise RuntimeError("rollback ledger is not once")
    result = {"orderId": order, "originalEventId": original["eventId"], "originalPayloadHash": original["payloadHash"],
              "originalDispatchRetained": True, "liveOldWriterRejected": True, "guardedPreserved": True,
              "awardLedgerRows": 1, "final": final}
    write_json(case / "result.json", result)
    return result


def wait_owned_tasks_drained(seed, seconds=60):
    def drained():
        count = sum(int(mysql(db, "SELECT COUNT(*) FROM outbox WHERE state<>'PUBLISHED'"))
                    + int(mysql(db, "SELECT COUNT(*) FROM retry_job WHERE state NOT IN ('PUBLISHED','DONE')"))
                    for db in seed["databases"])
        return count == 0
    wait_for(drained, seconds, "owned tasks did not settle after business completion")


def run_resume_paused(seed, app1, app2, evidence, tokens, user, order):
    if user not in tokens:
        raise ValueError("paused recovery user not in seed")
    case = evidence / "paused-remedial"
    case.mkdir()
    before = receipt_snapshot(seed, user, order, case)
    write_json(case / "before.json", before)
    db_index, suffix = route(user)
    original = before["source"][seed["databases"][db_index]]["order"][0]
    if original["recoveryStatus"] != "REVIEW" or not before["deferred"]:
        raise RuntimeError("remedial order is not paused with real deferred fact")
    command = {"runId": seed["runId"], "dbKey": "%02d" % db_index, "tableKey": "%03d" % suffix,
               "orderId": order, "expectedVersion": int(original["version"]),
               "operator": "task8-isolated-operator", "reason": "verified original late ReservationHeld fact"}
    write_json(evidence / "app-1/command-resume.tmp", command)
    (evidence / "app-1/command-resume.tmp").replace(evidence / "app-1/command-resume.json")
    wait_for(lambda: (evidence / "app-1/resume-result.json").is_file(), 10, "remedial audited resume failed")
    for number in (1, 2):
        (evidence / ("app-%d" % number) / "release-claims").write_text("gated before and audited resume saved\n")
    def delivered():
        response = request(app1["port"], "/api/v2/draws/" + order, tokens[user])
        return response if response["status"] == 200 and response["body"].get("state") == "DELIVERED" else None
    final = wait_for(delivered, 120, "original paused remedial order did not converge")
    wait_owned_tasks_drained(seed)
    after = receipt_snapshot(seed, user, order, case)
    write_json(case / "after.json", after)
    if any(row["closed_at"] is None or row["operator_id"] != "task8-isolated-operator" for row in after["cases"]):
        raise RuntimeError("remedial resume missing audit")
    awards = [row for row in after["assets"]["creditLedger"] if row["biz_id"] == order and row["biz_type"] == "DRAW_AWARD"]
    if len(awards) != 1:
        raise RuntimeError("remedial original order ledger is not one")
    return {"orderId": order, "final": final, "awardLedgerRows": 1, "notFaultProof": True}


def run_paused_receipt(seed, app1, app2, evidence, tokens):
    case = evidence / "paused-receipt"
    case.mkdir()
    user, token = next(iter(tokens.items()))
    request_id = "task8-paused-" + uuid.uuid4().hex
    body = {"requestId": request_id, "activityId": seed["activityId"]}
    write_json(case / "request-intent.json", {"user": user, "request": body, "at": time.time()})
    try:
        accepted = request(app1["port"], "/api/v2/draws", token, body)
        write_json(case / "request.json", accepted)
    except (OSError, urllib.error.URLError) as error:
        write_json(case / "request.json", {"outcome": "UNKNOWN", "errorType": type(error).__name__})
        raise
    order = accepted["body"].get("orderId") if isinstance(accepted["body"], dict) else None
    if not order:
        raise RuntimeError("paused receipt response missing original order")
    for number in (1, 2):
        (evidence / ("app-%d" % number) / "release-claims").write_text("original accepted\n")
    def receipt_marker():
        for number in (1, 2):
            path = evidence / ("app-%d" % number) / "receipt-blocked.json"
            if path.exists():
                return number, json.loads(path.read_text())
        return None
    emitter, marker = wait_for(receipt_marker, 20, "no actual ReservationHeld transport blocked")
    event = marker["event"]
    import base64
    wire = base64.b64decode(marker["bodyBase64"], validate=True)
    if (event["eventType"] != "ReservationHeld" or event["aggregateId"] != order
            or event["userId"] != user or marker["messageId"] != event["eventId"]
            or hashlib.sha256(wire).hexdigest() != marker["wireSha256"]
            or json.loads(wire) != event or marker["headers"].get("x-v2-failure-count", 0) != 0
            or marker["pid"] != int((app1 if emitter == 1 else app2)["ready"].split()[0])):
        raise RuntimeError("late receipt original transport identity mismatch")
    initial = receipt_snapshot(seed, user, order, case)
    write_json(case / "before-pause.json", initial)
    held = initial["assets"]["reservation"]
    if len(held) != 1 or held[0]["state"] != "HELD":
        raise RuntimeError("late receipt must originate from real HELD reservation")
    original_rows = [row for tables in initial["source"].values() for row in tables["outbox"]
                     if row["eventId"] == event["eventId"]]
    if len(original_rows) != 1:
        raise RuntimeError("late receipt lacks original outbox identity")
    write_json(case / "transport-identity.json", {"marker": marker, "outbox": original_rows[0],
                 "hashNamespaces": {"wireSha256": "raw broker message bytes", "payloadHash": "persisted EVENT hasher"}})
    db_index, suffix = route(user)
    source_db = seed["databases"][db_index]
    original_order = initial["source"][source_db]["order"][0]
    command = {"runId": seed["runId"], "dbKey": "%02d" % db_index, "tableKey": "%03d" % suffix,
               "orderId": order, "expectedVersion": int(original_order["version"]),
               "operator": "task8-isolated-operator", "reason": "verified original late ReservationHeld fact"}
    write_json(evidence / "app-1/command-pause.tmp", command)
    (evidence / "app-1/command-pause.tmp").replace(evidence / "app-1/command-pause.json")
    wait_for(lambda: (evidence / "app-1/pause-result.json").is_file(), 10, "production RecoveryService.pause failed")
    paused = receipt_snapshot(seed, user, order, case)
    write_json(case / "paused-before-release.json", paused)
    paused_order = paused["source"][source_db]["order"][0]
    if paused_order["recoveryStatus"] != "REVIEW" or paused["assets"] != initial["assets"]:
        raise RuntimeError("pause changed assets or did not persist REVIEW")
    for number in (1, 2):
        (evidence / ("app-%d" % number) / "release-receipt").write_text("pause persisted; deliver original bytes\n")
    def deferred():
        snapshot = receipt_snapshot(seed, user, order, case)
        return snapshot if any(row["event_id"] == event["eventId"] for row in snapshot["deferred"]) else None
    late = wait_for(deferred, 20, "actual consumer did not persist original late receipt")
    write_json(case / "deferred-after-late.json", late)
    original_deferred = [row for row in late["deferred"] if row["event_id"] == event["eventId"]]
    if (len(original_deferred) != 1 or original_deferred[0]["payload"] != event
            or original_deferred[0]["consumer_name"] != "workflow-v2"
            or original_deferred[0]["replayed_event_id"] is not None):
        raise RuntimeError("late consumer did not defer exact original event")
    if late["assets"] != paused["assets"] or late["source"][source_db]["order"] != paused["source"][source_db]["order"]:
        raise RuntimeError("paused late receipt changed assets/HELD/order")
    command["expectedVersion"] = int(paused_order["version"])
    write_json(evidence / "app-1/command-resume.tmp", command)
    (evidence / "app-1/command-resume.tmp").replace(evidence / "app-1/command-resume.json")
    wait_for(lambda: (evidence / "app-1/resume-result.json").is_file(), 10, "audited production resume failed")
    def delivered():
        response = request(app1["port"], "/api/v2/draws/" + order, token)
        return response if response["status"] == 200 and response["body"].get("state") == "DELIVERED" else None
    final = wait_for(delivered, 60, "audited resumed original order did not converge")
    wait_owned_tasks_drained(seed)
    after = receipt_snapshot(seed, user, order, case)
    write_json(case / "after-resume.json", after)
    if (not after["cases"] or any(row.get("closed_at") is None
            or row.get("operator_id") != "task8-isolated-operator"
            or row.get("resolution_reason") != "verified original late ReservationHeld fact"
            for row in after["cases"])):
        raise RuntimeError("recovery case lacks audited resume")
    if not any(row["event_id"] == event["eventId"] and row["replayed_event_id"]
               for row in after["deferred"]):
        raise RuntimeError("original deferred fact lacks persistent replay mapping")
    ledgers = [row for row in after["assets"]["creditLedger"] if row["biz_id"] == order and row["biz_type"] == "DRAW_AWARD"]
    if len(ledgers) != 1:
        raise RuntimeError("resumed original order award ledger not one")
    result = {"orderId": order, "lateEventId": event["eventId"], "wireSha256": marker["wireSha256"],
              "originalPayloadHash": original_rows[0]["payloadHash"], "assetsUnchangedWhilePaused": True,
              "persistentDefer": True, "auditedResume": True, "awardLedgerRows": 1, "final": final}
    write_json(case / "result.json", result)
    return result


def run_old_token(seed, app1, app2, evidence, tokens):
    case = evidence / "old-token"
    case.mkdir()
    user, token = next(iter(tokens.items()))
    request_id = "task8-oldtoken-" + uuid.uuid4().hex
    body = {"requestId": request_id, "activityId": seed["activityId"]}
    write_json(case / "request-intent.json", {"user": user, "requestId": request_id, "request": body,
                                                   "port": app1["port"], "at": time.time()})
    try:
        accepted = request(app1["port"], "/api/v2/draws", token, body)
        write_json(case / "request.json", {"user": user, "requestId": request_id, "response": accepted})
    except (OSError, urllib.error.URLError) as error:
        write_json(case / "request.json", {"user": user, "requestId": request_id,
                                            "outcome": "UNKNOWN", "errorType": type(error).__name__})
        raise
    order = accepted["body"].get("orderId") if isinstance(accepted["body"], dict) else None
    if not order:
        raise RuntimeError("draw response lacks orderId; raw 503/UNKNOWN retained")
    initial = source_rows(seed, user, order)
    write_json(case / "before-release.json", initial)
    original_db, pending = event_row(initial)
    if pending["state"] != "PENDING":
        raise RuntimeError("claim gate failed: original event already claimed")
    original_event, original_hash = pending["eventId"], pending["payloadHash"]
    (evidence / "app-1" / "arm-late").write_text(original_event + "\n")
    (evidence / "app-1" / "arm").write_text(original_event + "\n")
    (evidence / "app-1" / "release-claims").write_text("release\n")
    signal = evidence / "app-1" / "confirmed-blocked"
    wait_for(lambda: signal.is_file(), 15, "no real publisher confirm signal")
    before = source_rows(seed, user, order)
    write_json(case / "before.json", before)
    before_db, first = event_row(before, original_event)
    first_marker = marker_fields(signal)
    verify_marker(first_marker, app1, order, first)
    if before_db != original_db or first["payloadHash"] != original_hash:
        raise RuntimeError("original event moved or payload hash changed before kill")
    (evidence / "app-2" / "arm").write_text(original_event + "\n")
    (evidence / "app-2" / "release-claims").write_text("take over naturally expired original lease\n")
    second_signal = evidence / "app-2" / "confirmed-blocked"
    wait_for(lambda: second_signal.is_file(), 120, "survivor did not confirm original event")
    takeover = source_rows(seed, user, order)
    write_json(case / "takeover.json", takeover)
    second_db, second = event_row(takeover, original_event)
    second_marker = marker_fields(second_signal)
    verify_marker(second_marker, app2, order, second)
    if second_db != original_db or second["payloadHash"] != original_hash:
        raise RuntimeError("takeover changed original event identity or hash")
    if int(second["attempt"]) <= int(first["attempt"]) or second["leaseToken"] == first["leaseToken"]:
        raise RuntimeError("same original event lacks fenced lease takeover")
    write_json(case / "takeover-marker.json", second_marker)
    # 新token仍PROCESSING时晚投递真实旧claim；不改原runOnce批次截止。
    (evidence / "app-1" / "submit-late").write_text("submit captured original object\n")
    late_path = evidence / "app-1" / "late-settle-result.json"
    wait_for(lambda: late_path.is_file(), 5, "late repository settlement did not complete")
    late = json.loads(late_path.read_text())
    claim, report = late["originalClaim"], late["report"]
    if (claim["eventId"] != original_event or claim["leaseToken"] != first["leaseToken"]
            or claim["attempt"] != int(first["attempt"]) or claim["payloadHash"] != original_hash
            or report["completed"] or report["retried"] or report["dead"]
            or report["lost"] != [original_event]):
        raise RuntimeError("late original claim was not fenced as lost")
    after_late = source_rows(seed, user, order)
    write_json(case / "after-late-before-new-settle.json", after_late)
    _, preserved = event_row(after_late, original_event)
    if preserved != second or preserved["state"] != "PROCESSING":
        raise RuntimeError("old token changed new owner's PROCESSING state")
    # 新owner必须在自己的20s批次内settle。旧runOnce释放后按原截止拒绝。
    (evidence / "app-2" / "release-confirm").write_text("settle new owner\n")
    (evidence / "app-1" / "release-confirm").write_text("original runOnce keeps expired deadline\n")
    def recovered():
        result = request(app2["port"], "/api/v2/draws/" + order, token)
        snapshot = source_rows(seed, user, order)
        _, final_event = event_row(snapshot, original_event)
        if result["status"] == 200 and result["body"].get("state") == "DELIVERED" and final_event["state"] == "PUBLISHED":
            return result
        return None
    final_response = wait_for(recovered, 120, "original draw did not converge after old token rejection")
    after = source_rows(seed, user, order)
    write_json(case / "after.json", after)
    after_db, final_event = event_row(after, original_event)
    if after_db != original_db or final_event["payloadHash"] != original_hash or final_event["state"] != "PUBLISHED":
        raise RuntimeError("original event/hash not PUBLISHED")
    ledgers = [row for tables in after.values() for row in tables.get("ledger", []) if row["bizId"] == order]
    if len(ledgers) != 1 or ledgers[0]["rows"] != "1":
        raise RuntimeError("award ledger is not exactly one")
    write_json(case / "result.json", {"orderId": order, "originalEventId": original_event, "originalPayloadHash": original_hash,
                                        "firstAttempt": first["attempt"], "takeoverAttempt": second["attempt"],
                                        "leaseTokenChanged": True, "originalFinalState": final_event["state"],
                                        "awardLedgerRows": 1, "final": final_response})
    return {"orderId": order, "eventId": original_event, "payloadHash": original_hash}


def run_remedial_recovery(seed, app, evidence, tokens, user, order, original_event=None, original_hash=None):
    if user not in tokens:
        raise ValueError("recovery user not in owned seed")
    case = evidence / "remedial-recovery"
    case.mkdir()
    before = source_rows(seed, user, order)
    write_json(case / "before.json", before)
    database, original = event_row(before, original_event)
    if original_hash is not None and original["payloadHash"] != original_hash:
        raise RuntimeError("remedial frozen original payload hash mismatch")
    if original["state"] not in ("PENDING", "PROCESSING", "PUBLISHED"):
        raise RuntimeError("remedial original event is not recoverable")
    db_index, _ = route(user)
    orders = before[seed["databases"][db_index]].get("order", [])
    if len(orders) != 1 or orders[0]["state"] in ("DELIVERED", "CANCELLED", "REJECTED"):
        raise RuntimeError("remedial original business order is not unfinished")
    for number in (1, 2):
        (evidence / ("app-%d" % number) / "release-claims").write_text("remedial before snapshot saved\n")
    def converged():
        response = request(app["port"], "/api/v2/draws/" + order, tokens[user])
        snapshot = source_rows(seed, user, order)
        _, row = event_row(snapshot, original["eventId"])
        return response if response["status"] == 200 and response["body"].get("state") == "DELIVERED" and row["state"] == "PUBLISHED" else None
    response = wait_for(converged, 120, "remedial original event did not converge")
    wait_owned_tasks_drained(seed)
    after = source_rows(seed, user, order)
    write_json(case / "after.json", after)
    final_database, final = event_row(after, original["eventId"])
    ledgers = [row for tables in after.values() for row in tables.get("ledger", []) if row["bizId"] == order]
    if final_database != database or final["payloadHash"] != original["payloadHash"] or final["state"] != "PUBLISHED":
        raise RuntimeError("remedial recovery changed original identity or failed to publish")
    if len(ledgers) != 1 or ledgers[0]["rows"] != "1":
        raise RuntimeError("remedial recovery award ledger not exactly once")
    result = {"orderId": order, "eventId": original["eventId"], "payloadHash": original["payloadHash"],
              "beforeState": original["state"], "beforeAttempt": original["attempt"],
              "afterState": final["state"], "afterAttempt": final["attempt"],
              "awardLedgerRows": 1, "response": response, "notFaultProof": True}
    write_json(case / "result.json", result)
    return result


def run_mysql_settle_kill(seed, app1, app2, evidence, tokens):
    case = evidence / "mysql-settle-connection-kill"
    case.mkdir()
    user, token = next(iter(tokens.items()))
    request_id = "task8-mysql-kill-" + uuid.uuid4().hex
    body = {"requestId": request_id, "activityId": seed["activityId"]}
    write_json(case / "request-intent.json", {"user": user, "requestId": request_id, "body": body, "at": time.time()})
    try:
        accepted = request(app1["port"], "/api/v2/draws", token, body)
    except (OSError, urllib.error.URLError) as error:
        write_json(case / "request.json", {"requestId": request_id, "outcome": "UNKNOWN",
                                             "errorType": type(error).__name__})
        raise
    write_json(case / "request.json", accepted)
    order = accepted["body"].get("orderId") if isinstance(accepted["body"], dict) else None
    if not order:
        raise RuntimeError("MySQL KILL draw response lacks orderId")
    initial = source_rows(seed, user, order)
    write_json(case / "before-release.json", initial)
    source_db, pending = event_row(initial)
    if pending["state"] != "PENDING":
        raise RuntimeError("MySQL KILL claim gate failed")
    event, payload_hash = pending["eventId"], pending["payloadHash"]
    (evidence / "app-1" / "arm-settle").write_text(event + "\n")
    (evidence / "app-2" / "arm").write_text(event + "\n")
    (evidence / "app-1" / "release-claims").write_text("target known\n")
    marker_path = evidence / "app-1" / "settle-connection"
    wait_for_file_fast(marker_path, 15, "no actual settlement transaction connection marker")
    fields = marker_path.read_text().strip().split("\t")
    if len(fields) != 5 or not fields[2].isdigit():
        raise RuntimeError("invalid settlement connection marker")
    connection_id = int(fields[2])
    marker_at = java_instant_ns(fields[3])
    deadline_at = java_instant_ns(fields[4])
    if deadline_at <= marker_at or deadline_at - marker_at > 1_000_000_000:
        raise RuntimeError("settlement marker has invalid original deadline")
    # The app/container ID is pinned before releasing claims. Keep the critical path
    # to one processlist query and the real KILL; whole-run snapshots come later.
    process = rows(None, ("id", "database", "command"),
                   "SELECT ID,DB,COMMAND FROM information_schema.processlist WHERE ID=%d" % connection_id)
    verify_settle_connection(fields, app1, event, source_db, process)
    write_json(case / "kill-intent.json", {"connectionId": connection_id, "targetDatabase": source_db,
                                           "eventId": event, "atEpochNs": time.time_ns(), "markerEpochNs": marker_at,
                                           "settleDeadlineEpochNs": deadline_at, "processlist": process})
    if time.time_ns() >= deadline_at:
        raise RuntimeError("settlement connection KILL missed original deadline before command")
    server_before_us, server_after_us = kill_connection_timed(connection_id)
    kill_at = time.time_ns()
    absent = mysql(None, "SELECT COUNT(*) FROM information_schema.processlist WHERE ID=%d" % connection_id)
    write_json(case / "kill-result.json", {"connectionId": connection_id, "mysqlCommandExit": 0,
                                           "targetAbsent": absent == "0", "atEpochNs": kill_at,
                                           "markerToKillMs": round((kill_at - marker_at) / 1_000_000, 3),
                                           "mysqlServerBeforeEpochUs": server_before_us,
                                           "mysqlServerAfterEpochUs": server_after_us,
                                           "mysqlServerUpperEpochNs": server_after_us * 1000 + 999,
                                           "settleDeadlineEpochNs": deadline_at,
                                           "remainingDeadlineMs": round((deadline_at - kill_at) / 1_000_000, 3)})
    if absent != "0" or kill_at >= deadline_at or server_after_us * 1000 + 999 >= deadline_at:
        raise RuntimeError("MySQL KILL absent/deadline verification failed")
    (evidence / "app-1" / "release-claims").unlink()
    before = source_rows(seed, user, order)
    write_json(case / "after-kill-before-release.json", before)
    _, first = event_row(before, event)
    if first["state"] != "PROCESSING" or first["payloadHash"] != payload_hash or not first["leaseToken"]:
        raise RuntimeError("post-KILL original PROCESSING identity missing")
    (evidence / "app-1" / "release-settle").write_text("connection killed\n")
    (evidence / "app-2" / "release-claims").write_text("connection killed\n")
    successor_marker = evidence / "app-2" / "confirmed-blocked"
    wait_for(lambda: successor_marker.is_file(), 120, "survivor did not confirm original after MySQL KILL")
    takeover = source_rows(seed, user, order)
    write_json(case / "takeover.json", takeover)
    _, second = event_row(takeover, event)
    marker = marker_fields(successor_marker)
    verify_marker(marker, app2, order, second)
    if second["payloadHash"] != payload_hash or int(second["attempt"]) <= int(first["attempt"]) or second["leaseToken"] == first["leaseToken"]:
        raise RuntimeError("MySQL KILL original event lacks fenced takeover")
    write_json(case / "takeover-marker.json", marker)
    (evidence / "app-2" / "release-confirm").write_text("settle original\n")
    def converged():
        response = request(app2["port"], "/api/v2/draws/" + order, token)
        snapshot = source_rows(seed, user, order)
        _, item = event_row(snapshot, event)
        return response if response["status"] == 200 and response["body"].get("state") == "DELIVERED" and item["state"] == "PUBLISHED" else None
    final_response = wait_for(converged, 120, "MySQL KILL original event did not converge")
    after = source_rows(seed, user, order)
    write_json(case / "after.json", after)
    final_db, final = event_row(after, event)
    ledgers = [row for tables in after.values() for row in tables.get("ledger", []) if row["bizId"] == order]
    if final_db != source_db or final["payloadHash"] != payload_hash or len(ledgers) != 1 or ledgers[0]["rows"] != "1":
        raise RuntimeError("MySQL KILL final original identity or ledger mismatch")
    result = {"orderId": order, "eventId": event, "payloadHash": payload_hash, "connectionId": connection_id,
              "mysqlKillSucceeded": True, "firstAttempt": first["attempt"], "takeoverAttempt": second["attempt"],
              "leaseTokenChanged": True, "finalState": final["state"], "awardLedgerRows": 1,
              "finalResponse": final_response}
    write_json(case / "result.json", result)
    return result


def run_rabbit_recovery(seed, apps, evidence, tokens, owned, prefix, smoke=False):
    import threading
    import sys
    from sample_relay_optimization import Sampler, aligned_window, validate_final_sample, validate_continuous_arrivals
    case = evidence / "rabbit-recovery"
    case.mkdir()
    broker = inspect(RABBIT)
    if broker["Id"] not in owned:
        raise ValueError("Rabbit fault requires owned fresh broker")
    sampler = Sampler(sys.modules[__name__], seed, apps, broker["Id"], owned, prefix, case / "samples")
    for number in (1, 2):
        wait_for(lambda number=number: (evidence / ("app-%d" % number) / "metrics.prom").is_file(),
                 10, "test-only local metrics registry did not export")
    for number in (1, 2):
        (evidence / ("app-%d" % number) / "release-claims").write_text("continuous real HTTP arrivals\n")
    write_json(case / "broker-before.json", owned_broker_snapshot(owned, prefix))
    before = sampler.sample("baseline", 0)
    if before["orders"] or before["domains"]["OUTBOX"]["count"] or before["domains"]["RETRY"]["count"]:
        raise RuntimeError("fresh Rabbit case must start with zero inventory")
    stop = threading.Event()
    records = []
    errors = []
    users = list(tokens)
    def send():
        sequence = 0
        while not stop.is_set():
            started = time.time()
            user = users[sequence % len(users)]
            app = apps[sequence % len(apps)]
            body = {"requestId": "t8-rabbit-" + uuid.uuid4().hex, "activityId": seed["activityId"]}
            record = {"sequence": sequence, "startedAt": started, "monotonicStartedNs": time.monotonic_ns(),
                      "user": user, "body": body}
            write_json(case / ("intent-%04d.json" % sequence), record)
            try:
                record["response"] = request(app["port"], "/api/v2/draws", tokens[user], body)
            except (OSError, urllib.error.URLError) as error:
                record["response"] = {"outcome": "UNKNOWN", "errorType": type(error).__name__}
            except Exception as error:
                errors.append(type(error).__name__)
                stop.set()
            finally:
                record["endedAt"] = time.time()
                record["monotonicEndedNs"] = time.monotonic_ns()
                write_json(case / ("response-%04d.json" % sequence), record)
                records.append(record)
            sequence += 1
            stop.wait(max(0, 1 - (time.time() - started)))
    sender = threading.Thread(target=send, name="task8-http-sender")
    samples = [before]
    def sample_for(phase, seconds):
        end = time.monotonic() + seconds
        while time.monotonic() < end:
            started = time.monotonic()
            samples.append(sampler.sample(phase, len(samples)))
            if errors:
                raise RuntimeError("HTTP sender failed: " + errors[0])
            time.sleep(max(0, 1 - (time.monotonic() - started)))
    sender.start()
    try:
        sample_for("baseline", 6 if not smoke else 4)
        if not smoke:
            state = inspect(RABBIT)
            if state["Id"] != broker["Id"]:
                raise ValueError("owned Rabbit identity changed before STOP")
            started = time.time()
            reply = docker("stop", "--timeout", "10", broker["Id"])
            stopped = inspect(RABBIT)
            write_json(case / "rabbit-stop.json", {"id": broker["Id"], "startedAt": started,
                       "endedAt": time.time(), "receipt": reply.stdout.strip(), "running": stopped["State"]["Running"]})
            if stopped["State"]["Running"] or reply.stdout.strip() != broker["Id"]:
                raise RuntimeError("owned Rabbit did not stop")
            sample_for("rabbit_down", 12)
            started = time.time()
            reply = docker("start", broker["Id"])
            write_json(case / "rabbit-start.json", {"id": broker["Id"], "startedAt": started,
                       "endedAt": time.time(), "receipt": reply.stdout.strip()})
            if reply.stdout.strip() != broker["Id"]:
                raise RuntimeError("owned Rabbit start receipt mismatch")
            sample_for("recovery_with_arrivals", 45)
    finally:
        stop.set()
        sender.join(10)
        if sender.is_alive():
            raise RuntimeError("HTTP sender did not stop before cleanup")
        write_json(case / "requests.json", records)
    sample_for("drain", 2)
    wait_owned_tasks_drained(seed, seconds=120)
    sample_for("drain", 2)
    final = samples[-1]
    write_json(case / "broker-after.json", owned_broker_snapshot(owned, prefix))
    inventory = task_inventory(seed)
    write_json(case / "final-tasks.json", inventory)
    validate_final_sample(final)
    # 对所有原HTTP intent核精确request receipt；不丢503或网络UNKNOWN，不新建替代请求。
    from reconcile_v2_run import rows
    receipts = [r for db in seed["databases"][1:] for suffix in range(4)
                for r in rows(db, "user_request_receipt_%03d" % suffix)]
    write_json(case / "final-request-receipts.json", receipts)
    resolution = []
    for record in records:
        matches = [r for r in receipts if r["user_id"] == record["user"]
                   and r["request_id"] == record["body"]["requestId"]]
        status = record.get("response", {}).get("status")
        if len(matches) > 1 or (not matches and (status is None or status == 503 or 200 <= status < 300)):
            raise RuntimeError("original HTTP intent remains UNKNOWN")
        resolution.append({"requestId": record["body"]["requestId"], "httpStatus": status,
                           "receipt": matches[0] if matches else None})
    write_json(case / "intent-resolution.json", resolution)
    recovery = [s for s in samples if s["phase"] == "recovery_with_arrivals"]
    window = aligned_window(recovery[0], recovery[-1]) if len(recovery) >= 2 else None
    continuity = None
    if not smoke:
        continuity = validate_continuous_arrivals(records, recovery[0]["observedAt"], recovery[-1]["observedAt"])
        validate_continuous_arrivals(records, records[0]["startedAt"], records[-1]["startedAt"])
        if any(abs(s["elapsedSeconds"] - (s["monotonicEndedNs"] - s["monotonicStartedNs"]) / 1e9) > 2 for s in samples):
            raise RuntimeError("wall/monotonic sample discrepancy; suspended or clock-shifted experiment")
        if not window or not (window["completedPerSecond"] > window["acceptedPerSecond"]
                              and window["openBefore"] > window["openAfter"]):
            raise RuntimeError("continuous-arrival recovery throughput/backlog criterion not proven")
        arrivals = [r for r in records if recovery[0]["observedAt"] <= r["startedAt"] <= recovery[-1]["observedAt"]]
        if len(arrivals) < 10 or not window["acceptedIds"]:
            raise RuntimeError("recovery lacked sustained fresh arrivals")
    six_types = {"PrepareDraw", "ReserveAward", "ReservationHeld", "ConfirmReservation", "ReservationConfirmed", "AwardDelivery"}
    event_counts = {kind: sum(1 for tables in inventory.values() for row in tables["outbox"] if row["event_type"] == kind) for kind in six_types}
    if smoke and (any(value != len(final["orders"]) for value in event_counts.values())
                  or any(tables["v2_recovery_dispatch"] for tables in inventory.values())):
        raise RuntimeError("normal fresh draw amplification/zero-redrive criterion failed")
    result = {"smoke": smoke, "notCapacityBenchmark": True, "sixTypeEventCounts": event_counts, "requestCount": len(records),
              "finalOrders": len(final["orders"]), "recoveryWindow": window, "arrivalContinuity": continuity,
              "sampleIntervalsSeconds": [s["elapsedSeconds"] for s in samples],
              "brokerExactAgeAvailable": False, "task8Complete": False}
    write_json(case / "result.json", result)
    return result


def hash_tree(directory):
    hashes = {}
    for path in sorted(Path(directory).rglob("*")):
        if path.is_file() and path.name != "SHA256SUMS.json":
            hashes[str(path.relative_to(directory))] = sha256(path)
    write_json(Path(directory) / "SHA256SUMS.json", hashes)


def cleanup_owned(out, owned, owned_volumes, states):
    cleanup = []
    for cid in reversed(owned):
        state = docker("inspect", cid, check=False)
        if state.returncode == 0:
            (out / ("container-%s.log" % cid[:12])).write_text(docker("logs", cid, check=False).stdout)
            removed = docker("rm", "-f", cid, check=False)
            cleanup.append({"containerId": cid, "removeExit": removed.returncode,
                            "absentAfter": docker("inspect", cid, check=False).returncode != 0})
    for volume in owned_volumes:
        present = docker("volume", "inspect", volume, check=False).returncode == 0
        cleanup.append({"volume": volume, "retainedForRecovery": present,
                        "mount": "/var/lib/rabbitmq", "restoreCommand":
                        "docker run --network " + NETWORK + " --hostname task8rabbit --mount type=volume,src="
                        + volume + ",dst=/var/lib/rabbitmq -e RABBITMQ_DEFAULT_USER -e RABBITMQ_DEFAULT_PASS "
                        + states[DEPENDENCIES["V2_RABBIT_CONTAINER"]]["image"]})
    write_json(out / "cleanup.json", cleanup)
    return cleanup


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--env", required=True, choices=["v2-it"])
    parser.add_argument("--seed", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--smoke", action="store_true", help="两应用零业务请求、claim gate启动验证")
    parser.add_argument("--payment-smoke", action="store_true", help="smoke同时核沙盒付款profile启动")
    parser.add_argument("--mysql-smoke", action="store_true", help="smoke使用MySQL KILL相同1秒settle参数")
    parser.add_argument("--correctness", action="store_true", help="独立六域非零正确性人口")
    parser.add_argument("--rabbit-recovery", action="store_true")
    parser.add_argument("--sampler-smoke", action="store_true")
    parser.add_argument("--rollback", action="store_true", help="完整停旧writer后bounded退legacy且guarded保留")
    parser.add_argument("--paused-receipt", action="store_true", help="真实暂停后晚ReservationHeld与审计恢复")
    parser.add_argument("--old-token", action="store_true", help="真实旧claim在新token PROCESSING时晚结算")
    parser.add_argument("--mysql-kill", action="store_true", help="真实settle事务连接KILL与同身份接管")
    parser.add_argument("--resume-paused-order", help="仅审计resume先前失败case的原暂停订单")
    parser.add_argument("--recover-order", help="单独收敛先前失败运行遗留的原订单")
    parser.add_argument("--recover-event", help="既有失败raw里的精确原eventId")
    parser.add_argument("--recover-hash", help="既有失败raw里的原payloadHash")
    parser.add_argument("--recover-user", help="原订单所属seed用户")
    args = parser.parse_args()
    if bool(args.recover_order or args.resume_paused_order) != bool(args.recover_user) or sum((args.smoke, args.correctness, args.mysql_kill, args.old_token, args.paused_receipt, args.rollback, args.rabbit_recovery, args.sampler_smoke, bool(args.recover_order), bool(args.resume_paused_order))) > 1:
        parser.error("smoke, correctness, mysql-kill and recovery are separate; recover-order/user required together")
    if bool(args.recover_event) != bool(args.recover_hash) or (args.recover_event and not args.recover_order):
        parser.error("recover-event/hash must be paired and require recover-order")
    if args.payment_smoke and not args.smoke:
        parser.error("payment-smoke requires smoke")
    if args.mysql_smoke and not args.smoke:
        parser.error("mysql-smoke requires smoke")
    seed = json.loads(Path(args.seed).read_text())
    validate_seed_and_names(seed)
    out = Path(args.out).resolve()
    if out == ROOT or ROOT in out.parents or out.exists():
        raise ValueError("fault output must be a new external directory")
    states, bound_endpoints, writers = preflight(seed)
    cp = classpath()
    out.mkdir(mode=0o700, parents=True)
    for index in (1, 2):
        (out / ("app-%d" % index)).mkdir()
    if not args.correctness:
        (out / "app-1" / "gate-claims").write_text("hold until target identity is known\n")
    if args.smoke or args.rabbit_recovery or args.sampler_smoke:
        (out / "app-2" / "gate-claims").write_text("smoke holds all claims\n")
    if args.rabbit_recovery or args.sampler_smoke:
        for index in (1, 2):
            (out / ("app-%d" % index) / "arm-sampler").write_text("test-only local registry scrape\n")
    if args.rollback:
        for index in (1, 2):
            (out / ("app-%d" % index) / "arm-admin").write_text("test-only listener stop\n")
    if args.paused_receipt or args.resume_paused_order:
        for index in (1, 2):
            (out / ("app-%d" % index) / "arm-receipt").write_text("ReservationHeld only\n")
            if args.resume_paused_order:
                (out / ("app-%d" % index) / "release-receipt").write_text("remedial no transport fault\n")
    if args.mysql_kill or args.old_token or args.paused_receipt or args.rollback:
        (out / "app-2" / "gate-claims").write_text("hold until actual MySQL KILL\n")
    if args.recover_order or args.resume_paused_order:
        (out / "app-2" / "gate-claims").write_text("hold until remedial before snapshot\n")
    frozen = artifact_fingerprint()
    snapshot_artifact_bytes(out)
    app_image_id = docker("image", "inspect", "--format", "{{.Id}}", IMAGE).stdout.strip()
    write_json(out / "preflight.json", {"runId": seed["runId"], "databases": seed["databases"], "containers": states,
                                         "boundEndpoints": bound_endpoints, "writerInventory": writers,
                                         "artifactFingerprint": frozen, "appImageId": app_image_id,
                                         "classpathBinding": {"entries": len(cp.split(":")),
                                                              "repositoryMount": "/root/.m2/repository/ -> /m2/repository/",
                                                              "allEntriesVerified": True},
                                         "sourceHash": sha256(ROOT / "bm-domain/src/main/java/com/jay/domain/task/service/BoundedOutboxRelay.java"),
                                         "jarHash": sha256(ROOT / "bm-app/target/bm-app.jar"), "probeClassHash": sha256(ROOT / "bm-app/target/test-classes/com/jay/test/v2/RelayFaultProbe.class")})
    owned = []
    owned_volumes = []
    try:
        start_rabbit(owned, owned_volumes, seed["runId"])
        prefix = "bromark.it.task8." + seed["runId"]
        app1 = start_app(1, seed, out, prefix, cp, bound_endpoints, owned, payment=args.correctness or args.payment_smoke)
        tokens = json.loads(Path(seed["tokenFile"]).read_text())
        if args.rabbit_recovery or args.sampler_smoke:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned)
            result = run_rabbit_recovery(seed, [app1, app2], out, tokens, owned, prefix, smoke=args.sampler_smoke)
            write_json(out / "rabbit-result.json", result)
        elif args.rollback:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned)
            result = run_rollback(seed, app1, app2, out, tokens, cp, bound_endpoints, owned, prefix)
            write_json(out / "rollback-result.json", {"rollbackPassed": True, "task8Complete": False, "original": result})
        elif args.resume_paused_order:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned)
            result = run_resume_paused(seed, app1, app2, out, tokens, args.recover_user, args.resume_paused_order)
            write_json(out / "paused-remedial-result.json", {"remedialResumePassed": True,
                       "task8Complete": False, "notFaultProof": True, "original": result})
        elif args.paused_receipt:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned)
            result = run_paused_receipt(seed, app1, app2, out, tokens)
            write_json(out / "paused-receipt-result.json", {"pausedReceiptPassed": True, "task8Complete": False,
                                                            "original": result})
        elif args.old_token:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned)
            result = run_old_token(seed, app1, app2, out, tokens)
            write_json(out / "old-token-result.json", {"oldTokenFencingPassed": True, "task8Complete": False,
                                                       "original": result})
        elif args.mysql_kill:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned)
            result = run_mysql_settle_kill(seed, app1, app2, out, tokens)
            write_json(out / "mysql-kill-result.json", {"mysqlSettleKillPassed": True, "task8Complete": False,
                                                        "original": result})
        elif args.correctness:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned, payment=True)
            result = run_correctness_population(seed, app1, app2, out, tokens)
            write_json(out / "correctness-live.json", {"livePopulationAccepted": True,
                                                       "task8Complete": False, "identities": result})
        elif args.recover_order:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned)
            result = run_remedial_recovery(seed, app2, out, tokens, args.recover_user, args.recover_order, args.recover_event, args.recover_hash)
            write_json(out / "remedial-result.json", {"remedialRecoveryPassed": True, "task8Complete": False,
                                                       "notFaultProof": True, "original": result})
        elif args.smoke:
            app2 = start_app(2, seed, out, prefix, cp, bound_endpoints, owned, payment=args.payment_smoke)
            health = [smoke_health(app) for app in (app1, app2)]
            user = next(iter(tokens))
            empty = source_rows(seed, user, "task8-smoke-nonexistent-order")
            if any(rows for tables in empty.values() for rows in tables.values()):
                raise RuntimeError("smoke nonexistent order unexpectedly populated")
            names = ",".join(sql(db) for db in seed["databases"])
            connections = int(mysql(None, "SELECT COUNT(*) FROM information_schema.processlist WHERE DB IN (" + names + ")"))
            if connections < 1:
                raise RuntimeError("smoke found no actual target DB app connection")
            write_json(out / "smoke-result.json", {"smokePassed": True, "task8Complete": False,
                                                     "apps": health, "targetDbConnections": connections,
                                                     "businessRequests": 0, "paymentProfile": args.payment_smoke,
                                                     "mysqlKillParameters": args.mysql_smoke,
                                                     "settleDeadlineSeconds": 1})
        else:
            result = run_confirm_sigkill(seed, app1,
                                         lambda: start_app(2, seed, out, prefix, cp, bound_endpoints, owned), out, tokens)
            write_json(out / "result.json", {"confirmSigkillPassed": True, "confirmSigkill": result,
                                             "task8Complete": False, "otherCases": "NOT_RUN"})
    except BaseException as error:
        write_json(out / "failure.json", {"passed": False, "errorType": type(error).__name__, "message": str(error), "at": time.time()})
        raise
    finally:
        cleanup = cleanup_owned(out, owned, owned_volumes, states)
        postflight = artifact_fingerprint()
        unchanged = postflight == frozen
        write_json(out / "artifact-postflight.json", {"fingerprint": postflight, "unchanged": unchanged})
        if any((item.get("removeExit") != 0 or not item.get("absentAfter")) if "containerId" in item
               else not item["retainedForRecovery"] for item in cleanup):
            write_json(out / "cleanup-failure.json", {"passed": False, "reason": "owned resource cleanup failed"})
            (out / "result.json").unlink(missing_ok=True)
            (out / "smoke-result.json").unlink(missing_ok=True)
            (out / "remedial-result.json").unlink(missing_ok=True)
            (out / "correctness-live.json").unlink(missing_ok=True)
            (out / "mysql-kill-result.json").unlink(missing_ok=True)
            (out / "old-token-result.json").unlink(missing_ok=True)
            (out / "paused-receipt-result.json").unlink(missing_ok=True)
            (out / "paused-remedial-result.json").unlink(missing_ok=True)
            (out / "rollback-result.json").unlink(missing_ok=True)
            (out / "rabbit-result.json").unlink(missing_ok=True)
        if not unchanged:
            write_json(out / "artifact-change-failure.json", {"passed": False, "reason": "source or build artifacts changed"})
            (out / "result.json").unlink(missing_ok=True)
            (out / "smoke-result.json").unlink(missing_ok=True)
            (out / "remedial-result.json").unlink(missing_ok=True)
            (out / "correctness-live.json").unlink(missing_ok=True)
            (out / "mysql-kill-result.json").unlink(missing_ok=True)
            (out / "old-token-result.json").unlink(missing_ok=True)
            (out / "paused-receipt-result.json").unlink(missing_ok=True)
            (out / "paused-remedial-result.json").unlink(missing_ok=True)
            (out / "rollback-result.json").unlink(missing_ok=True)
            (out / "rabbit-result.json").unlink(missing_ok=True)
        hash_tree(out)
    if args.correctness and (out / "correctness-live.json").is_file():
        from reconcile_v2_run import reconcile
        wait_for(lambda: preflight(seed), 30, "writers did not fully stop before correctness reconcile")
        report = reconcile(seed)
        populations = {
            "draws": sum(slot["draws"] for db in report["counts"].values() for slot in db.values()),
            "signins": sum(slot["signins"] for db in report["counts"].values() for slot in db.values()),
            "payments": int(mysql(seed["databases"][0], "SELECT COUNT(*) FROM payment_order")),
            "stockReservations": int(mysql(seed["databases"][0], "SELECT COUNT(*) FROM award_reservation")),
            "creditLedgers": sum(slot["creditLedgers"] for db in report["counts"].values() for slot in db.values()),
            "deliveries": sum(int(mysql(db, "SELECT COUNT(*) FROM v2_award_delivery_%03d" % suffix))
                              for db in seed["databases"][1:] for suffix in range(4)),
        }
        write_json(out / "correctness-reconcile.json", report)
        write_json(out / "correctness-populations.json", populations)
        if not report["passed"] or any(value < 1 for value in populations.values()):
            write_json(out / "correctness-failure.json", {"passed": False, "reason": "six-scope or nonzero population failed"})
            hash_tree(out)
            raise RuntimeError("correctness reconciliation or population failed")
        write_json(out / "correctness-result.json", {"correctnessPassed": True, "task8Complete": False,
                                                     "sixScopes": report["checkedScopes"], "populations": populations})
        hash_tree(out)


if __name__ == "__main__":
    main()
