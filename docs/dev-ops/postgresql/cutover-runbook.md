# V2 MySQL → PostgreSQL Cutover Runbook

只迁移 V2 三个逻辑库；V1 旧 MyBatis 业务不迁移。切换期间停写，MySQL 保留为只读回滚快照。

## 1. 准备

```sh
# 目标 PG 三库；schema 必须已应用两遍并通过 v2-schema-check.sql
for db in pg_control pg_one pg_two; do
  docker exec bromark-p0-pg psql -U postgres -c "CREATE DATABASE $db;" || true
  docker exec -i bromark-p0-pg psql -U postgres -d "$db" -v ON_ERROR_STOP=1 \
    -f - < docs/dev-ops/postgresql/v2-schema.sql
done
```

停应用、relay、所有消费者；确认没有 MySQL 写入。记录源库行数、余额/额度汇总和 `award_stock` 三列汇总，作为迁移前证据。

## 2. Dry run

```sh
export MYSQL_PWD=... PGPASSWORD=...
python3 docs/dev-ops/postgresql/migrate_mysql_to_pg.py \
  --source-host 127.0.0.1:3306 --source-db router_control \
  --target-host 127.0.0.1:15432 --target-db pg_control
```

对 `router_one → pg_one`、`router_two → pg_two` 重复。Dry run 只输出每表行数，不写目标。

## 3. Execute

```sh
python3 docs/dev-ops/postgresql/migrate_mysql_to_pg.py \
  --source-host 127.0.0.1:3306 --source-db router_control \
  --target-host 127.0.0.1:15432 --target-db pg_control \
  --truncate --execute
```

三个库分别执行。工具会：
- 只复制源库实际存在的 V2 表；
- 保留显式 identity 值，并在复制后 `setval` 到 max+1；
- 逐表比较 source/target 行数，不一致立即失败；
- `--truncate` 只允许在已备份/已确认目标可重建时使用。

## 4. Go/no-go

- 三库所有表行数一致；
- `user_credit_ledger_*` 的 `SUM(delta)`、`user_credit_account.balance` 合计与源一致；
- `award_stock` 的 `available+reserved+allocated=total`，且与源一致；
- identity sequence 的 `last_value >= max(id)`；
- 启动 PG P0 app，健康检查、Prometheus、一次 `create→ReserveAward→ReservationConfirmed→DELIVERED` smoke 通过。

## 5. Rollback

停 PG app，保留 PG 数据不动，恢复 MySQL 停止快照并重启旧配置；不要双写。迁移失败不删除 MySQL，不重置 outbox/retry/ledger。

## 6. 限制

- 当前复制工具按表逐行 XML 读取，适合 V2 数据量与演练；超大表需改为分批游标。
- 迁移不搬 V1；V1 历史查询若需要，另立只读导出任务。
- PostgreSQL 与 MySQL 的 JSON/时间/空字符串比较差异以 `PostgresSqlContractIT` 和 `v2-schema-check.sql` 为准。
