#!/usr/bin/env python3
"""P0 两机 benchmark 种子：可移植的 MySQL seed helper。

只对**全新、空**的三个逻辑库执行：从 docs/dev-ops/mysql/migrations 的 manifest
建库建表，然后灌入 N 用户 / A 活动 / P 个 CREDIT 奖、A 个策略、P 个库存池，
并为每个用户预置一条已完成结果订单（供 7:1:1:1 的 result 查询使用）。

设计约束：
- 不绑定任何 Docker 容器名；MySQL 走 mysql CLI，PostgreSQL 走 psql CLI。
- MySQL 密码只从 MYSQL_PWD 读取，PostgreSQL 密码只从 PGPASSWORD 读取；都不写入 argv。
- seed.json/tokens.json/private.pem 必须写在仓库外；脚本拒绝 Git 工作区路径。
- 已存在的数据库、非空输出目录一律拒绝，避免覆盖上一次运行。
"""
import argparse
import base64
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
MIGRATIONS = ROOT / "docs" / "dev-ops" / "mysql" / "migrations"
PG_SCHEMA = ROOT / "docs" / "dev-ops" / "postgresql" / "v2-schema.sql"
DIALECT = "mysql"
PG_HOST = "127.0.0.1"
PG_PORT = 5432
PG_USER = "postgres"
PG_PASSWORD = ""

RUN_RE = re.compile(r"[a-z0-9_]{1,24}")
DB_RE = re.compile(r"router_p0_[a-z0-9_]{1,24}_(?:control|one|two)")
ORDER_RE = re.compile(r"[A-Z][A-Z0-9_]*-0([12])\.(00[0-3])-[A-Za-z0-9]+-[1-9][0-9]*")
HEX64 = "0" * 64


def fail(message):
    raise SystemExit("p0_seed: " + message)


def log(message):
    print(message, file=sys.stderr, flush=True)


def sql(v):
    """把 Python 值转成当前方言的字面量；两种方言都不依赖调用方转义。"""
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (dict, list)):
        v = json.dumps(v, separators=(",", ":"), ensure_ascii=False)
    if DIALECT == "postgres":
        return "'" + str(v).replace("'", "''") + "'"
    return "CONVERT(X'" + str(v).encode("utf-8").hex() + "' USING utf8mb4)"


def db_now():
    return "(clock_timestamp() AT TIME ZONE 'UTC')" if DIALECT == "postgres" else "UTC_TIMESTAMP(6)"


MYSQL_BIN = "mysql"
MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASSWORD = ""


def mysql(statement, database=None):
    """执行 SQL；MySQL 用 mysql CLI，PostgreSQL 用 psql CLI；返回原始文本。"""
    if database is not None and not DB_RE.fullmatch(database):
        fail("refusing database outside the p0 seed namespace: " + str(database))
    if DIALECT == "postgres":
        cmd = [
            "psql",
            "-X",
            "-q",
            "-A",
            "-t",
            "-v",
            "ON_ERROR_STOP=1",
            "-h",
            str(PG_HOST),
            "-p",
            str(PG_PORT),
            "-U",
            PG_USER,
            "-d",
            database or "postgres",
        ]
        env = dict(os.environ)
        env["PGPASSWORD"] = PG_PASSWORD
        result = subprocess.run(cmd, input=statement, text=True, capture_output=True, env=env)
        if result.returncode != 0:
            target = database or "server"
            raise RuntimeError("psql failed on " + target + ": " + result.stderr.strip())
        return result.stdout.rstrip("\r\n")
    cmd = [
        MYSQL_BIN,
        "--batch",
        "--raw",
        "--skip-column-names",
        "--default-character-set=utf8mb4",
        "-h",
        str(MYSQL_HOST),
        "-P",
        str(MYSQL_PORT),
        "-u",
        MYSQL_USER,
    ]
    if database:
        cmd.append(database)
    env = dict(os.environ)
    env["MYSQL_PWD"] = MYSQL_PASSWORD
    result = subprocess.run(cmd, input=statement, text=True, capture_output=True, env=env)
    if result.returncode != 0:
        target = database or "server"
        raise RuntimeError("mysql failed on " + target + ": " + result.stderr.strip())
    return result.stdout.rstrip("\r\n")


def route(user):
    """必须与 Java RouteCodec.routeUser 完全一致：2 库 × 4 分表。"""
    h = 0
    units = user.encode("utf-16-be", errors="surrogatepass")
    for index in range(0, len(units), 2):
        h = (31 * h + int.from_bytes(units[index:index + 2], "big")) & 0xFFFFFFFF
    slot = (h ^ (h >> 16)) & 7
    return slot // 4 + 1, slot % 4


def result_order(user, run, tail):
    """生成 RouteCodec.routeOrder 可解析、且 slot 等于 routeUser(user) 的结果订单。"""
    db, table = route(user)
    node = run.replace("_", "") or "p0"
    order = f"P0RESULT-0{db}.{table:03d}-{node}r{tail}-1"
    match = ORDER_RE.fullmatch(order)
    if not match or f"0{match.group(1)}.{match.group(2)}" != f"0{db}.{table:03d}":
        fail("generated result order is not route-compatible: " + order)
    return order


def token(private, user, now, ttl):
    def b64(raw):
        return base64.urlsafe_b64encode(raw).decode().rstrip("=")

    payload = {
        "iss": "https://issuer.test",
        "aud": "bromark",
        "sub": user,
        "iat": now,
        "nbf": now - 1,
        "exp": now + ttl,
    }
    data = (
        b64(b'{"alg":"RS256","typ":"JWT"}')
        + "."
        + b64(json.dumps(payload, separators=(",", ":")).encode())
    )
    signature = subprocess.run(
        ["openssl", "dgst", "-sha256", "-sign", str(private)],
        input=data.encode(),
        capture_output=True,
        check=True,
    ).stdout
    return data + "." + b64(signature)


def award(award_id, pool, amount="1.00"):
    return {
        "awardId": award_id,
        "type": "CREDIT",
        "amount": amount,
        "stockPoolId": pool,
        "quantity": 1,
        "configHash": hashlib.sha256(("p0:" + str(award_id) + ":" + pool).encode()).hexdigest(),
    }


def fallback_award():
    return {
        "awardId": 0,
        "type": "NONE",
        "amount": None,
        "stockPoolId": None,
        "quantity": 0,
        "configHash": hashlib.sha256(b"p0:fallback:NONE").hexdigest(),
    }


def prize_config(award_id, pool):
    return {
        "awardId": award_id,
        "type": "CREDIT",
        "minAmount": "1.00",
        "maxAmount": "1.00",
        "stockPoolId": pool,
        "rate": "1.00",
        "unlockAfter": 0,
    }


def fallback_config():
    return {
        "awardId": 0,
        "type": "NONE",
        "minAmount": None,
        "maxAmount": None,
        "stockPoolId": None,
        "rate": "1.00",
        "unlockAfter": 0,
    }


def strategy_snapshot(run, activity, version, prize_indices, prizes):
    configs = [prize_config(prizes[i]["awardId"], prizes[i]["stockPoolId"]) for i in prize_indices]
    count = len(configs)
    return {
        "activityId": activity,
        "strategyId": activity,
        "version": version,
        "state": "OPEN",
        "beginsAt": "2020-01-01T00:00:00Z",
        "endsAt": "2099-01-01T00:00:00Z",
        "prizes": configs,
        "fallback": fallback_config(),
        "weightedPools": {},
        "blacklistAwardId": None,
        "blacklistVersion": "p0-" + run,
        "tables": {
            "default": {
                "ids": [c["awardId"] for c in configs],
                "probabilities": [1.0 / count] * count,
                "aliases": [0] * count,
            }
        },
    }


def admission_snapshot(activity, version, credit_cost="1.00"):
    return {
        "activityId": activity,
        "activityState": "OPEN",
        "version": version,
        "beginsAt": "2020-01-01T00:00:00Z",
        "endsAt": "2099-01-01T00:00:00Z",
        "readAt": "2026-10-18T00:00:00Z",
        "expiresAt": "2099-01-01T00:00:00Z",
        "blacklisted": False,
        "creditCost": credit_cost,
        "quota": {"totalDelta": 1000000, "dayLimitDelta": 1000000, "monthLimitDelta": 1000000},
        "rewards": [{"itemId": "credit", "activityId": 0, "credit": "2.00", "quota": None}],
    }


def result_record(user, order, activity, version, strategy, award_snapshot, now_iso):
    return {
        "orderId": order,
        "userId": user,
        "activityId": activity,
        "requestHash": HEX64,
        "identityHash": HEX64,
        "version": version,
        "time": {
            "instant": now_iso,
            "businessDate": now_iso[:10],
            "businessMonth": now_iso[:7],
        },
        "ruleInput": {
            "businessTime": now_iso,
            "totalEffectiveCount": 0,
            "dayEffectiveCount": 0,
            "blacklisted": False,
            "blacklistVersion": "p0",
        },
        "strategy": strategy,
        "state": "DELIVERED",
        "recoveryStatus": "ACTIVE",
        "objectVersion": 1,
        "candidate": award_snapshot,
        "fallback": fallback_award(),
        "finalAward": award_snapshot,
    }


def apply_migrations(dbs):
    if DIALECT == "postgres":
        raw = PG_SCHEMA.read_bytes()
        for db in dbs:
            mysql(raw.decode("utf-8"), db)
            log("postgres schema applied to " + db)
        return {"v2-schema.sql": hashlib.sha256(raw).hexdigest()}
    manifest_path = MIGRATIONS / "manifest.json"
    if not manifest_path.is_file():
        fail("migration manifest not found: " + str(manifest_path))
    manifest = json.loads(manifest_path.read_text())
    hashes = {}
    for entry in manifest["scripts"]:
        raw = (MIGRATIONS / entry["file"]).read_bytes()
        digest = hashlib.sha256(raw).hexdigest()
        if digest != entry["sha256"]:
            fail("migration checksum mismatch: " + entry["file"])
        hashes[entry["file"]] = digest
        text = raw.decode("utf-8")
        for index, db in enumerate(dbs):
            if entry["scope"] == "control" and index != 0:
                continue
            if entry["scope"] == "user" and index == 0:
                continue
            if entry["scope"] not in ("all", "control", "user"):
                fail("unknown migration scope: " + entry["scope"])
            for suffix in range(entry["shards"]):
                statements = [
                    s.strip()
                    for s in text.replace("{{suffix}}", f"{suffix:03d}").split(";")
                    if s.strip()
                ]
                if statements:
                    mysql(";\n".join(statements), db)
        log("migration " + entry["file"] + " applied")
    return hashes


def main():
    parser = argparse.ArgumentParser(
        description="Seed a fresh P0 benchmark environment (PostgreSQL by default; no broker)."
    )
    parser.add_argument("--dialect", choices=["postgres", "mysql"], default="postgres")
    parser.add_argument("--run-id", required=True, help="lowercase [a-z0-9_]{1,24}")
    parser.add_argument("--users", type=int, default=2000)
    parser.add_argument("--activities", type=int, default=10)
    parser.add_argument("--prizes", type=int, default=10)
    parser.add_argument("--out", required=True, help="empty output dir outside the Git workspace")
    parser.add_argument("--stock", type=int, default=1000000, help="stock per prize pool")
    parser.add_argument("--activity-base", type=int, default=610000)
    parser.add_argument("--award-base", type=int, default=101)
    parser.add_argument("--batch-users", type=int, default=200)
    parser.add_argument("--token-ttl", type=int, default=86400, help="JWT lifetime in seconds")
    parser.add_argument("--mysql-host", default=os.environ.get("V2_MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--mysql-port", type=int, default=None)
    parser.add_argument("--mysql-user", default="root")
    parser.add_argument("--mysql-bin", default="mysql")
    parser.add_argument("--pg-host", default=os.environ.get("V2_PG_HOST", "127.0.0.1:15432"))
    parser.add_argument("--pg-port", type=int, default=None)
    parser.add_argument("--pg-user", default="postgres")
    args = parser.parse_args()

    global DIALECT, MYSQL_BIN, MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD
    global PG_HOST, PG_PORT, PG_USER, PG_PASSWORD

    run = args.run_id
    if not RUN_RE.fullmatch(run):
        parser.error("--run-id must match [a-z0-9_]{1,24}")
    if not 1 <= args.users <= 100000:
        parser.error("--users must be 1..100000")
    if not 1 <= args.activities <= 100:
        parser.error("--activities must be 1..100")
    if not args.activities <= args.prizes <= 1000:
        parser.error("--prizes must be >= --activities and <= 1000 (each activity needs a CREDIT prize)")
    if args.stock < 1:
        parser.error("--stock must be positive")
    if args.activity_base < 1 or args.award_base < 1:
        parser.error("activity/award bases must be positive")
    if args.batch_users < 1:
        parser.error("--batch-users must be positive")
    if not 60 <= args.token_ttl:
        parser.error("--token-ttl must be at least 60 seconds")

    out = Path(args.out).expanduser().resolve()
    if out == ROOT or ROOT in out.parents:
        parser.error("--out must stay outside the Git workspace (use /tmp or ~/bromark-seed)")
    if out.exists() and not out.is_dir():
        parser.error("--out exists and is not a directory")
    if out.exists() and any(out.iterdir()):
        parser.error("--out must be empty; refusing to overwrite " + str(out))

    DIALECT = args.dialect
    if DIALECT == "postgres":
        pg_host = args.pg_host
        pg_port = args.pg_port
        if pg_port is None:
            pg_port = int(os.environ.get("V2_PG_PORT", "5432"))
        if args.pg_port is None and pg_host.count(":") == 1 and pg_host.rsplit(":", 1)[1].isdigit():
            pg_host, embedded = pg_host.rsplit(":", 1)
            pg_port = int(embedded)
        PG_HOST = pg_host
        PG_PORT = pg_port
        PG_USER = args.pg_user
        PG_PASSWORD = os.environ.get("PGPASSWORD") or os.environ.get("V2_PG_PASSWORD", "")
        if not PG_PASSWORD:
            parser.error("set PGPASSWORD or V2_PG_PASSWORD; passwords are never accepted on argv")
        if shutil.which("psql") is None:
            fail("psql CLI not found in PATH; install postgresql-client")
        mysql_bin = "mysql"
    else:
        mysql_bin = args.mysql_bin
    if DIALECT == "mysql":
        if os.sep in mysql_bin:
            resolved = Path(mysql_bin)
            if not resolved.is_file():
                fail("mysql client not executable: " + str(resolved))
        elif shutil.which(mysql_bin) is None:
            fail(
                "mysql CLI not found in PATH; install mysql-client or set --mysql-bin"
                " (for example: brew install mysql-client)"
            )
    if shutil.which("openssl") is None:
        fail("openssl is required to generate the RSA key and RS256 tokens")

    MYSQL_BIN = mysql_bin
    host = args.mysql_host
    port = args.mysql_port
    if port is None:
        port = int(os.environ.get("V2_MYSQL_PORT", "3306"))
    # V2_MYSQL_HOST 在本仓库里经常写成 host:port；没有显式 --mysql-port 时拆开。
    if args.mysql_port is None and host.count(":") == 1 and host.rsplit(":", 1)[1].isdigit():
        host, embedded = host.rsplit(":", 1)
        port = int(embedded)
    MYSQL_HOST = host
    MYSQL_PORT = port
    MYSQL_USER = args.mysql_user
    MYSQL_PASSWORD = os.environ.get("MYSQL_PWD", "")
    if DIALECT == "mysql" and not MYSQL_PASSWORD:
        parser.error("set MYSQL_PWD in the environment; passwords are never accepted on argv")

    dbs = [f"router_p0_{run}_{part}" for part in ("control", "one", "two")]
    for db in dbs:
        if not DB_RE.fullmatch(db):
            fail("invalid database name: " + db)

    if DIALECT == "postgres":
        log("connecting to postgresql " + PG_HOST + ":" + str(PG_PORT))
        version = mysql("SHOW server_version")
        max_connections = int(mysql("SHOW max_connections"))
    else:
        log("connecting to mysql " + MYSQL_HOST + ":" + str(MYSQL_PORT))
        version = mysql("SELECT VERSION()")
        max_connections = int(mysql("SELECT @@max_connections"))
    if max_connections < 300:
        log(
            "WARNING: @@max_connections="
            + str(max_connections)
            + " < 300; the P0 profile (3 x pool 72) needs max_connections=300"
        )

    existing_sql = ",".join(sql(db) for db in dbs)
    if DIALECT == "postgres":
        existing = [
            row
            for row in mysql(
                "SELECT datname FROM pg_database WHERE datname IN (" + existing_sql + ")"
            ).splitlines()
            if row.strip()
        ]
    else:
        existing = [
            row
            for row in mysql(
                "SELECT SCHEMA_NAME FROM information_schema.schemata WHERE SCHEMA_NAME IN ("
                + existing_sql
                + ")"
            ).splitlines()
            if row.strip()
        ]
    if existing:
        fail("refusing existing databases: " + ", ".join(existing))

    old_umask = os.umask(0o077)
    try:
        out.mkdir(parents=True, exist_ok=True)
        out.chmod(0o700)

        for db in dbs:
            if DIALECT == "postgres":
                mysql("CREATE DATABASE " + db)
            else:
                mysql("CREATE DATABASE " + db + " CHARACTER SET utf8mb4 COLLATE utf8mb4_bin")
            log("created database " + db)
        hashes = apply_migrations(dbs)

        activity_ids = [args.activity_base + i for i in range(args.activities)]
        award_ids = [args.award_base + i for i in range(args.prizes)]
        pools = [f"p0_{run}_pool_{i:02d}" for i in range(args.prizes)]
        versions = [f"p0-{run}-a{i:02d}" for i in range(args.activities)]
        exchange_version = f"p0-{run}-exchange"
        sku = 61

        # 每个活动至少一个奖；10/10 时正好一活动一奖。多出的奖按轮转分配。
        prize_indices = [[] for _ in activity_ids]
        for index in range(args.prizes):
            prize_indices[index % args.activities].append(index)
        prizes = [
            {
                "awardId": award_ids[i],
                "type": "CREDIT",
                "stockPoolId": pools[i],
                "minAmount": "1.00",
                "maxAmount": "1.00",
            }
            for i in range(args.prizes)
        ]
        strategies = [
            strategy_snapshot(run, activity_ids[i], versions[i], prize_indices[i], prizes)
            for i in range(args.activities)
        ]

        control = []
        for i, pool in enumerate(pools):
            control.append(
                "INSERT INTO award_stock VALUES("
                + ",".join(
                    [
                        sql(pool),
                        str(args.stock),
                        str(args.stock),
                        "0",
                        "0",
                        "0",
                        sql("ACTIVE"),
                    ]
                )
                + ")"
            )
            control.append(
                "INSERT INTO stock_change_log(stock_pool_id,biz_id,kind,total_delta,"
                "available_delta,reserved_delta,allocated_delta,reason) VALUES("
                + ",".join(
                    [
                        sql(pool),
                        sql("seed"),
                        sql("INIT"),
                        str(args.stock),
                        str(args.stock),
                        "0",
                        "0",
                        sql("p0 benchmark synthetic audited budget"),
                    ]
                )
                + ")"
            )
        for i, activity in enumerate(activity_ids):
            control.append(
                "INSERT INTO strategy_version VALUES("
                + ",".join([str(activity), sql(versions[i]), sql(strategies[i])])
                + ")"
            )
            control.append(
                "INSERT INTO v2_activity_pointer VALUES("
                + ",".join([str(activity), str(activity), sql(versions[i])])
                + ")"
            )
            for prize_index in prize_indices[i]:
                control.append(
                    "INSERT INTO strategy_stock_binding VALUES("
                    + ",".join(
                        [
                            str(activity),
                            sql(versions[i]),
                            str(award_ids[prize_index]),
                            sql(pools[prize_index]),
                        ]
                    )
                    + ")"
                )
            control.append(
                "INSERT INTO v2_admission_config VALUES("
                + ",".join(
                    [
                        sql("ACTIVITY"),
                        sql(str(activity)),
                        sql(admission_snapshot(activity, versions[i])),
                    ]
                )
                + ")"
            )
        control.append(
            "INSERT INTO v2_admission_config VALUES("
            + ",".join(
                [
                    sql("EXCHANGE"),
                    sql(str(sku)),
                    sql(admission_snapshot(activity_ids[0], exchange_version)),
                ]
            )
            + ")"
        )
        control.append(
            "INSERT INTO payment_product VALUES("
            + ",".join([sql(str(sku)), "10.00", sql("CNY"), "100.00", "TRUE"])
            + ")"
        )
        mysql("START TRANSACTION;\n" + ";\n".join(control) + ";\nCOMMIT;", dbs[0])
        log("inserted pools/strategies/admission config")

        result_orders = {}
        mapping = {}
        now = int(time.time())
        now_iso = dt.datetime.fromtimestamp(now, dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        user_prefix = "p0-" + run + "-"
        for start in range(0, args.users, args.batch_users):
            stop = min(start + args.batch_users, args.users)
            batches = {db: [] for db in dbs[1:]}
            for n in range(start, stop):
                user = f"{user_prefix}{n}"
                db_index, suffix = route(user)
                db = dbs[db_index]
                statements = batches[db]
                statements.append(
                    "INSERT INTO user_credit_account(user_id,balance,opening_balance,status,"
                    "version,audit_source) VALUES("
                    + ",".join(
                        [sql(user), "1000000.00", "1000000.00", sql("ACTIVE"), "0", sql("p0 audited seed")]
                    )
                    + ")"
                )
                for i, activity in enumerate(activity_ids):
                    statements.append(
                        "INSERT INTO raffle_activity_account(user_id,activity_id,total_remaining,"
                        "day_limit,month_limit,effective_draw_count,opening_total,opening_day_limit,"
                        "opening_month_limit,opening_effective_count,audit_source) VALUES("
                        + ",".join(
                            [
                                sql(user),
                                str(activity),
                                "1000000",
                                "1000000",
                                "1000000",
                                "0",
                                "1000000",
                                "1000000",
                                "1000000",
                                "0",
                                sql("p0 audited seed"),
                            ]
                        )
                        + ")"
                    )
                prize_index = prize_indices[n % args.activities][0]
                snapshot_award = award(award_ids[prize_index], pools[prize_index])
                order = result_order(user, run, n + 1)
                record = result_record(
                    user,
                    order,
                    activity_ids[n % args.activities],
                    versions[n % args.activities],
                    strategies[n % args.activities],
                    snapshot_award,
                    now_iso,
                )
                result_orders[user] = order
                statements.append(
                    "INSERT INTO v2_draw_order_"
                    + f"{suffix:03d}"
                    + "(order_id,user_id,activity_id,state,snapshot,confirmed_at,delivered_at)"
                    + " VALUES("
                    + ",".join(
                        [
                            sql(order),
                            sql(user),
                            str(activity_ids[n % args.activities]),
                            sql("DELIVERED"),
                            sql(record),
                            db_now() + ",\n                            " + db_now(),
                        ]
                    )
                    + ")"
                )
                statements.append(
                    "INSERT INTO v2_award_record_"
                    + f"{suffix:03d}"
                    + "(order_id,user_id,state,snapshot) VALUES("
                    + ",".join([sql(order), sql(user), sql("DELIVERED"), sql(snapshot_award)])
                    + ")"
                )
                statements.append(
                    "INSERT INTO v2_award_delivery_"
                    + f"{suffix:03d}"
                    + "(order_id,user_id,award_hash,snapshot) VALUES("
                    + ",".join(
                        [sql(order), sql(user), sql(snapshot_award["configHash"]), sql(snapshot_award)]
                    )
                    + ")"
                )
                mapping[user] = None
            for db, statements in batches.items():
                if statements:
                    mysql("START TRANSACTION;\n" + ";\n".join(statements) + ";\nCOMMIT;", db)
            log(f"seeded users {start + 1}..{stop}")

        private = out / "private.pem"
        subprocess.run(
            [
                "openssl",
                "genpkey",
                "-algorithm",
                "RSA",
                "-pkeyopt",
                "rsa_keygen_bits:2048",
                "-out",
                str(private),
            ],
            check=True,
            capture_output=True,
        )
        private.chmod(0o600)
        public_der = subprocess.run(
            ["openssl", "pkey", "-in", str(private), "-pubout", "-outform", "DER"],
            check=True,
            capture_output=True,
        ).stdout
        (out / "public.der").write_bytes(public_der)

        for n, user in enumerate(mapping):
            mapping[user] = token(private, user, now, args.token_ttl)
            if (n + 1) % 250 == 0:
                log(f"signed tokens {n + 1}/{len(mapping)}")
        tokens = out / "tokens.json"
        tokens.write_text(json.dumps(mapping, separators=(",", ":")) + "\n")
        tokens.chmod(0o600)

        seed = {
            "runId": run,
            "users": args.users,
            "activities": [
                {
                    "activityId": activity_ids[i],
                    "strategyId": activity_ids[i],
                    "version": versions[i],
                    "prizeAwardIds": [award_ids[p] for p in prize_indices[i]],
                    "stockPoolIds": [pools[p] for p in prize_indices[i]],
                }
                for i in range(args.activities)
            ],
            "prizes": prizes,
            "fallback": fallback_config(),
            "stockPools": [
                {"stockPoolId": pools[i], "total": args.stock} for i in range(args.prizes)
            ],
            "databases": dbs,
            "databaseByRole": {"control": dbs[0], "one": dbs[1], "two": dbs[2]},
            "activityId": activity_ids[0],
            "sku": sku,
            "version": exchange_version,
            "exchangeVersion": exchange_version,
            "openingCreditPerUser": "1000000.00",
            "openingQuotaPerUser": 1000000,
            "stockPerPool": args.stock,
            "resultOrders": result_orders,
            "tokenFile": str(tokens),
            "publicKey": base64.b64encode(public_der).decode(),
            "migrationHashes": hashes,
            "dbVersion": version,
            "dbMaxConnections": max_connections,
            "generatedAt": now_iso,
        }
        (out / "seed.json").write_text(json.dumps(seed, indent=2, ensure_ascii=False) + "\n")
        log("wrote seed.json/tokens.json/private.pem under " + str(out))

        summary = {
            key: seed[key]
            for key in (
                "runId",
                "users",
                "activityId",
                "databases",
                "sku",
                "version",
                "stockPerPool",
                "dbVersion",
                "dbMaxConnections",
                "generatedAt",
            )
        }
        summary.update(
            {
                "activities": args.activities,
                "prizes": args.prizes,
                "creditPrizes": sum(1 for p in prizes if p["type"] == "CREDIT"),
                "stockPools": len(pools),
                "resultOrders": len(result_orders),
                "fallback": "NONE",
                "out": str(out),
                "tokenFile": str(tokens),
                "privateKey": str(private),
            }
        )
        print(json.dumps(summary, indent=2, ensure_ascii=False))
    finally:
        os.umask(old_umask)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError) as exc:
        fail(str(exc))
