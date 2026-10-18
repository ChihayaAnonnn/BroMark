# PostgreSQL Runtime Switch and MySQL→PostgreSQL Data Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run the P0 BroMark stack on PostgreSQL 17 for all three logical databases, then migrate the existing MySQL V2 data into PostgreSQL without changing V2 business identities or losing data.

**Architecture:** `bm-router` already detects `jdbc:postgresql:` URLs and wraps each connection with `PostgresConnection`; `PostgresSql` translates the MySQL-shaped V2 SQL (`ON DUPLICATE KEY`, `UTC_TIMESTAMP(6)`, `JSON_EXTRACT`, `IF`, index hints) to PostgreSQL. The main work is schema parity, PG test/runtime configuration, translator gap closure, and a freeze-window data copy with sequence/checksum verification. MySQL remains the rollback snapshot until PostgreSQL verification passes; no dual-write.

**Tech Stack:** Java 25 / Spring Boot, `bm-router` PostgreSQL compatibility layer, PostgreSQL 17, MySQL 8 (source only during migration), Python 3 migration tooling, Docker.

**Spec:** `docs/superpowers/specs/2026-10-18-p0-delivery-optimization-design.md` and the existing PG contract: `docs/dev-ops/postgresql/v2-schema.sql`, `docs/dev-ops/postgresql/v2-schema-check.sql`, `bm-router/src/main/java/cn/bugstack/middleware/db/router/dynamic/PostgresSql.java`.

## Global Constraints

- Target database is PostgreSQL 17; all three logical DBs (`pg_control`, `pg_one`, `pg_two`) use the same dialect.
- Preserve 2 logical user DBs × 4 shard tables and the `db00/db01/db02` routing layout.
- PG schema must match MySQL V2 migrations `001`–`007` plus P0 `004_user_draw.sql` (`reserved_at`, `draw_backlog` index).
- Timestamps are UTC `timestamp(6) without time zone`; JSON is `jsonb`; raw message bodies are `bytea`; hashes remain `char(64)` and must not be recomputed from re-serialized JSON.
- Business identities (`order_id`, `event_id`, `request_hash`, `identity_hash`, `stock_pool_id`) are copied verbatim.
- No data loss: cutover runs with writers stopped, source snapshot retained, row counts and money/quota sums verified before switching traffic.
- MySQL data migration scope is the V2 tables in `router_control` / `router_one` / `router_two`; V1/legacy tables are out of scope unless a later explicit request includes them.
- No measured throughput/P99 claim without a run on the target M5 + 16GB Mac setup.
- Do not commit secrets, seed tokens, or private keys.

## Review Focus

- MySQL `datetime(6)` values must remain the same UTC instant in PG; a non-UTC JVM must not shift them.
- `ON DUPLICATE KEY UPDATE` semantics must remain exactly once for outbox/retry/inbox/stock, not “insert then duplicate error”.
- `jsonb` must preserve the V2 snapshot fields used by delivery/reconciliation (amount, type, configHash); no re-serialization drift.
- Identity sequences after explicit-ID import must be greater than the maximum imported ID, or the first new order fails.
- A failed cutover must be able to fall back to the stopped MySQL snapshot without partial PG writes.

---

## Task 1: PostgreSQL schema parity with P0

**Files:**
- Modify: `docs/dev-ops/postgresql/v2-schema.sql`
- Modify: `docs/dev-ops/postgresql/v2-schema-check.sql`
- Test: `bm-app/src/test/java/com/jay/test/v2/PostgresSchemaParityIT.java` (new)

**Interfaces:**
- Adds `reserved_at timestamp(6) without time zone` to `v2_draw_order_000..003`.
- Adds `CREATE INDEX ..._draw_backlog ON v2_draw_order_XXX (state,last_progress_at,order_id)` for each shard.
- Schema check expected counts become 74 tables, 120 indexes, 130 constraints, 5 sequences.

- [ ] **Step 1: Write the failing parity test**

```java
@Test public void p0ColumnsAndBacklogIndexesExist() {
  // connect V2_PG_HOST / V2_DB00, assert information_schema has reserved_at and
  // pg_indexes has v2_draw_order_000_draw_backlog ... 003
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `V2_PG_HOST=... V2_PG_PASSWORD=... mvn -pl bm-app -Dtest=PostgresSchemaParityIT test`
Expected: FAIL, missing `reserved_at` / `draw_backlog`.

- [ ] **Step 3: Add the column and indexes to the PG schema**

Edit each `v2_draw_order_00X` block; keep UTC default and C collation. Update `v2-schema-check.sql` counts to 120 indexes.

- [ ] **Step 4: Apply twice and verify**

Run `psql -f docs/dev-ops/postgresql/v2-schema.sql` twice on a fresh DB, then `psql -f docs/dev-ops/postgresql/v2-schema-check.sql`.
Expected: both succeed; parity test PASS; second apply is a no-op.

- [ ] **Step 5: Commit**

```bash
git add docs/dev-ops/postgresql/v2-schema.sql docs/dev-ops/postgresql/v2-schema-check.sql bm-app/src/test/java/com/jay/test/v2/PostgresSchemaParityIT.java
git commit -m "feat(pg): add P0 draw columns and backlog indexes"
```

---

## Task 2: PostgreSQL test harness for P0 suites

**Files:**
- Modify: `bm-app/src/test/java/com/jay/test/v2/AssetTestSupport.java`
- Create: `bm-app/src/test/java/com/jay/test/v2/PostgresAssetTestSupport.java`
- Modify: `bm-app/src/test/java/com/jay/test/v2/Java25RouterIntegrationTest.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0PostgresSmokeV2Test.java`

**Interfaces:**
- `PostgresAssetTestSupport.run(CheckedConsumer)` builds a PG `ApplicationContextRunner` with `mini-db-router...url=jdbc:postgresql://${V2_PG_HOST}/${V2_DB0X}`, user `postgres`, password `V2_PG_PASSWORD`, driver `org.postgresql.Driver`.
- `AssetTestSupport` schema setup chooses PG schema when the datasource is PostgreSQL; no MySQL migration files are executed against PG.
- P0 tests can run with `-Dp0.dialect=postgres`.

- [ ] **Step 1: Write the failing smoke test**

Run the admission→confirm→delivery CREDIT flow through `PostgresAssetTestSupport`; assert `DELIVERED` and balance credited.

- [ ] **Step 2: Run and confirm failure**

Run against the PG container; expected: missing PG harness/context.

- [ ] **Step 3: Implement the PG harness and dialect switch**

Use `PostgresSqlContractIT.Database` as the minimal JDBC pattern; build routed Hikari pools with `org.postgresql.Driver`, `SET TIME ZONE 'UTC'`, and `PostgresConnection` wrapping through the router (do not wrap manually if `DynamicDataSource` already does it). Load `v2-schema.sql` into all three DBs before tests.

- [ ] **Step 4: Run the P0 smoke and one MySQL P0 suite**

Run: `mvn -pl bm-app -Dtest=P0PostgresSmokeV2Test test` with `V2_PG_*`.
Expected: PASS. Confirm existing MySQL P0 suites still pass.

- [ ] **Step 5: Commit**

---

## Task 3: P0 runtime profile on PostgreSQL

**Files:**
- Modify: `bm-app/src/main/resources/application-v2-p0.yml`
- Modify: `docs/performance/p0-runbook.md`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0ProfilePostgresV2Test.java`

**Interfaces:**
- `mini-db-router.jdbc.datasource.global.driver-class-name=org.postgresql.Driver`
- URLs: `jdbc:postgresql://${V2_PG_HOST}/${V2_DB00:pg_control}` etc.
- Username `postgres`; password `${V2_PG_PASSWORD}`.
- Keep 72 pool / 65 permits / 240 physical / `prefix: p0`.
- Remove MySQL-only `V2_MYSQL_*` from the P0 profile.

- [ ] **Step 1: Write a failing binding test**

Assert the P0 profile binds `org.postgresql.Driver` and the three `jdbc:postgresql:` URLs, and `P0BudgetV2Test` still validates 65 < 72.

- [ ] **Step 2: Run and confirm failure**

- [ ] **Step 3: Switch the profile and runbook Docker/env examples to PG**

The app container uses `V2_PG_HOST=bromark-p0-pg:5432`; host tests use `127.0.0.1:15432`.

- [ ] **Step 4: Boot the P0 profile against PG and run a health/admission smoke**

- [ ] **Step 5: Commit**

---

## Task 4: Close PostgreSQL translator gaps found by P0 suites

**Files:**
- Modify: `bm-router/src/main/java/cn/bugstack/middleware/db/router/dynamic/PostgresSql.java` and/or `PostgresConnection.java` only where a P0 suite fails
- Test: `bm-router/src/test/java/cn/bugstack/middleware/db/router/dynamic/PostgresSqlP0Test.java` (new focused cases)

**Interfaces:**
- No new public API; fixes are translation cases for the exact SQL that P0 repositories emit.

- [ ] **Step 1: Run the P0 suites against PG and capture the first failing SQL**

Run: `mvn -pl bm-app -Dtest='P0*Test' -Dp0.dialect=postgres test` with PG env.
Expected: red only on translator/SQL edges; record each failing repository SQL.

- [ ] **Step 2: Add one failing translator test per gap**

Cover at minimum: `v2_draw_order` backlog query, `award_stock` upsert/row lock, outbox duplicate, `retry_job` `IF/GREATEST`, `json_extract/json_unquote` in reconciliation.

- [ ] **Step 3: Fix translation minimally and re-run PG tests**

- [ ] **Step 4: Re-run the MySQL P0 suites to prove no regression**

- [ ] **Step 5: Commit**

---

## Task 5: Data migration tool and dry run

**Files:**
- Create: `docs/dev-ops/postgresql/migrate_mysql_to_pg.py`
- Test: `docs/dev-ops/postgresql/tests/test_migrate_mysql_to_pg.py`
- Create: `docs/dev-ops/postgresql/migration-manifest.json`

**Interfaces:**
- CLI: `migrate_mysql_to_pg.py --source-host --source-port --source-password --target-host --target-port --target-password --run-id --dry-run|--execute --out`.
- Copies every V2 table in `router_control/router_one/router_two`; maps MySQL `json`→`jsonb`, `datetime(6)`→UTC timestamp, `tinyint(1)`→boolean/smallint per PG schema, `longblob`→`bytea`.
- After import: `setval(pg_get_serial_sequence(table,column), max(column))` for every identity sequence; verify source/target row counts, `SUM(delta)` for credit ledgers, `SUM(available/reserved/allocated)` for `award_stock`, and max IDs.
- `--dry-run` reports counts/checksums without writing; `--execute` requires a stopped-writer assertion and an empty PG target.

- [ ] **Step 1: Write failing tests for row count / sequence / checksum helpers**

Use fixture rows with Chinese JSON, UTC microseconds, and bytea; assert no drift.

- [ ] **Step 2: Implement copy + verification**

Use `mysql`/`psql` CLI or Python DB drivers; disable triggers/FKs on target during load, then re-enable and validate constraints.

- [ ] **Step 3: Dry-run against a small MySQL snapshot**

Create a scratch MySQL DB with 20 users/2 activities/2 prizes; dry-run; then execute into fresh PG; assert counts and sequences.

- [ ] **Step 4: Rehearse full-size migration with writers stopped**

Use the P0 seed 2,000/10/10 source; measure copy time; verify checksums; record exact commands.

- [ ] **Step 5: Commit**

---

## Task 6: Cutover and rollback rehearsal

**Files:**
- Create: `docs/dev-ops/postgresql/cutover-runbook.md`
- Modify: `docs/performance/p0-runbook.md`

**Interfaces:**
- Steps: stop app/relay/consumers → snapshot MySQL → run migration `--execute` → verify checksums/sequences → start app on PG → smoke `create→confirm→deliver` → keep MySQL read-only for rollback.
- Rollback: stop PG app, revert `application-v2-p0.yml` to the stopped MySQL snapshot, restart, verify one order/ledger; no dual-write.

- [ ] **Step 1: Write the runbook with exact commands and go/no-go checks**
- [ ] **Step 2: Rehearse on a scratch copy; record timings and failures**
- [ ] **Step 3: Commit**

---

## Task 7: PostgreSQL benchmark and evidence

**Files:**
- Modify: `docs/performance/p0-runbook.md`
- Modify: `docs/performance/p0_seed.py` (or add `p0_seed_pg.py`)

**Interfaces:**
- Seed and load must point at PG; no MySQL dependency in the benchmark path.
- Collect same metrics as MySQL run; only measured numbers.

- [ ] **Step 1: Seed PG with 2,000/10/10**
- [ ] **Step 2: Run k6 CLOSED at 100/300/600 VU and OPEN at 20–160 draw/s**
- [ ] **Step 3: Record DELIVERED/s, entry QPS, P99, error rate, CPU/mem, gate/backlog**
- [ ] **Step 4: Compare to MySQL baseline only when both are measured on the same resource envelope**
- [ ] **Step 5: Commit the runbook/results**

---

## Self-Review

- Spec coverage: PG schema parity, runtime, tests, seed, data migration, cutover, benchmark all have tasks.
- No task changes V2 business identities or re-hashes JSON.
- MySQL remains rollback-only after cutover; no dual-write.
- Data migration scope is V2 tables; V1 is explicitly out of scope.
- The plan does not claim any throughput number without a measured PG run.
