#!/usr/bin/env python3
"""Copy one V2 MySQL logical database into an empty PostgreSQL database.

The target schema must already exist (docs/dev-ops/postgresql/v2-schema.sql).
The source and target credentials are read from MYSQL_PWD / PGPASSWORD only.
The tool is intentionally conservative: it refuses a non-empty target unless
--truncate is given, preserves explicit identity values, then advances every
identity sequence above the imported max.
"""
import argparse
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SCHEMA = ROOT / "docs" / "dev-ops" / "postgresql" / "v2-schema.sql"
IDENT = re.compile(r"[a-z][a-z0-9_]*")
SOURCE_DB = re.compile(r"[a-z0-9_]{1,64}")
TARGET_DB = re.compile(r"[a-z0-9_]{1,64}")


def fail(msg):
    raise SystemExit("migrate_mysql_to_pg: " + msg)


def run(cmd, env=None, stdin=None):
    result = subprocess.run(cmd, input=stdin, text=True, capture_output=True, env=env)
    if result.returncode:
        raise RuntimeError("command failed: %s\n%s" % (" ".join(cmd[:4]), result.stderr.strip()))
    return result.stdout


def mysql(args, sql, db=None):
    cmd = ["mysql", "--xml", "--batch", "--skip-column-names", "-h", args.source_host,
           "-P", str(args.source_port), "-u", args.source_user]
    if db:
        cmd.append(db)
    env = dict(os.environ, MYSQL_PWD=os.environ.get("MYSQL_PWD", ""))
    return run(cmd + ["-e", sql], env=env)


def psql(args, sql, db):
    cmd = ["psql", "-X", "-q", "-A", "-t", "-v", "ON_ERROR_STOP=1",
           "-h", args.target_host, "-p", str(args.target_port), "-U", args.target_user, "-d", db]
    env = dict(os.environ, PGPASSWORD=os.environ.get("PGPASSWORD", ""))
    return run(cmd + ["-c", sql], env=env)


def tables():
    out = []
    for line in SCHEMA.read_text().splitlines():
        match = re.match(r"CREATE TABLE IF NOT EXISTS ([a-z0-9_]+)\s*\(", line)
        if match:
            out.append(match.group(1))
    if not out:
        fail("no tables found in " + str(SCHEMA))
    return out


def columns(args, table):
    target = psql(args, "select column_name,data_type,is_identity from information_schema.columns "
                       "where table_schema='public' and table_name=%s order by ordinal_position"
                       % quote(table), args.target_db)
    result = []
    for line in target.splitlines():
        name, data_type, identity = line.split("|")
        if not IDENT.fullmatch(name):
            fail("unsafe column " + name)
        result.append((name, data_type, identity == "YES"))
    if not result:
        fail("target table missing: " + table)
    return result


def quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def value_sql(value, data_type, source_type):
    if value is None:
        return "NULL"
    if data_type == "boolean":
        return "TRUE" if value.lower() in ("1", "true") else "FALSE"
    if data_type == "bytea":
        if not re.fullmatch(r"[0-9a-fA-F]*", value):
            fail("invalid hex for bytea")
        return "decode(%s,'hex')" % quote(value)
    if data_type in ("smallint", "integer", "bigint", "numeric", "real", "double precision"):
        return value
    if data_type == "jsonb" or data_type == "json":
        return quote(value) + "::jsonb"
    return quote(value)


def target_count(args, table):
    return int(psql(args, "select count(*) from " + table, args.target_db).strip())


def source_count(args, table):
    xml = mysql(args, "select count(*) as n from " + table, args.source_db)
    return int(re.search(r"<field name=\"n\">([0-9]+)</field>", xml).group(1))


def copy_table(args, table):
    target_columns = columns(args, table)
    names = [c[0] for c in target_columns]
    # Binary columns must be selected as HEX so XML remains text-safe.
    source_meta = mysql(
        args,
        "select column_name,data_type from information_schema.columns where table_schema=%s "
        "and table_name=%s order by ordinal_position" % (quote(args.source_db), quote(table)),
        None,
    )
    source_types = {}
    for row in ET.fromstring("<root>" + source_meta + "</root>").iter("row"):
        fields = {f.get("name"): (f.text or "") for f in row}
        source_types[fields.get("column_name")] = fields.get("data_type", "")
    select = ",".join(("HEX(" + n + ") AS " + n) if source_types.get(n) in ("blob", "longblob", "varbinary", "binary") else n for n in names)
    xml = mysql(args, "select " + select + " from " + table, args.source_db)
    root = ET.fromstring("<root>" + xml + "</root>")
    batches = []
    batch = []
    for row in root.iter("row"):
        values = []
        for field in row.findall("field"):
            nil = field.get("{http://www.w3.org/2001/XMLSchema-instance}nil") == "true"
            values.append((field.text or "", nil))
        if len(values) != len(names):
            fail("column count mismatch for " + table)
        sql_values = []
        for (name, data_type, _), (text, nil) in zip(target_columns, values):
            sql_values.append(value_sql(None if nil else text, data_type, source_types.get(name, "")))
        batch.append("(" + ",".join(sql_values) + ")")
        if len(batch) >= 100:
            batches.append(batch)
            batch = []
    if batch:
        batches.append(batch)
    for rows in batches:
        psql(args, "insert into " + table + "(" + ",".join(names) + ") values " + ",".join(rows), args.target_db)


def sync_sequences(args):
    rows = psql(args, "select table_name,column_name from information_schema.columns "
                      "where table_schema='public' and is_identity='YES'", args.target_db)
    for line in rows.splitlines():
        if not line.strip():
            continue
        table, column = line.split("|")
        psql(args, "select setval(pg_get_serial_sequence(%s,%s), coalesce((select max(%s) from %s),1), "
                   "(select max(%s) from %s) is not null)"
                   % (quote(table), quote(column), column, table, column, table), args.target_db)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-host", default=os.environ.get("V2_MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--source-port", type=int, default=3306)
    parser.add_argument("--source-user", default="root")
    parser.add_argument("--source-db", required=True)
    parser.add_argument("--target-host", default=os.environ.get("V2_PG_HOST", "127.0.0.1"))
    parser.add_argument("--target-port", type=int, default=5432)
    parser.add_argument("--target-user", default="postgres")
    parser.add_argument("--target-db", required=True)
    parser.add_argument("--truncate", action="store_true")
    parser.add_argument("--execute", action="store_true")
    args = parser.parse_args()
    if not SOURCE_DB.fullmatch(args.source_db) or not TARGET_DB.fullmatch(args.target_db):
        fail("invalid database name")
    if not os.environ.get("MYSQL_PWD") or not os.environ.get("PGPASSWORD"):
        fail("set MYSQL_PWD and PGPASSWORD in the environment")
    if args.source_db == args.target_db:
        fail("source and target must differ")
    if args.source_host.count(":") == 1 and args.source_port == 3306:
        host, port = args.source_host.rsplit(":", 1)
        args.source_host, args.source_port = host, int(port)
    if args.target_host.count(":") == 1 and args.target_port == 5432:
        host, port = args.target_host.rsplit(":", 1)
        args.target_host, args.target_port = host, int(port)

    available = set(
        line.strip()
        for line in mysql(
            args,
            "select table_name from information_schema.tables where table_schema=%s"
            % quote(args.source_db),
            None,
        ).splitlines()
        if line.strip()
    )
    all_tables = [t for t in tables() if t in available]
    if not all_tables:
        fail("no matching V2 tables in source database")
    existing = sum(target_count(args, t) for t in all_tables)
    if existing and not args.truncate:
        fail("target is not empty (%d rows); pass --truncate only after a verified backup" % existing)
    plan = {t: source_count(args, t) for t in all_tables}
    print(json.dumps({"source": args.source_db, "target": args.target_db, "rows": plan}, sort_keys=True))
    if not args.execute:
        print("dry-run only; pass --execute to copy")
        return
    if args.truncate:
        psql(args, "truncate " + ",".join(all_tables) + " restart identity cascade", args.target_db)
    psql(args, "set session_replication_role=replica", args.target_db)
    for table in all_tables:
        if plan[table] == 0:
            continue
        copy_table(args, table)
        got = target_count(args, table)
        if got != plan[table]:
            fail("row count mismatch for %s: source=%d target=%d" % (table, plan[table], got))
        print("copied %s rows=%d" % (table, got))
    sync_sequences(args)
    psql(args, "set session_replication_role=origin", args.target_db)
    for table in all_tables:
        if target_count(args, table) != plan[table]:
            fail("post-copy count mismatch for " + table)
    print("migration verified")


if __name__ == "__main__":
    main()
