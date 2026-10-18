# P0 PostgreSQL 运行与压测 Runbook

> 当前 P0 运行路径只使用 PostgreSQL；旧 `p0-runbook.md` 是 MySQL 历史版本，不再用于新实验。

## 1. 本地依赖

| 服务 | 容器 | 端口 | 凭据 |
| --- | --- | --- | --- |
| PostgreSQL 17 | `bromark-p0-pg` | `15432` | `postgres` / `p0test`；`pg_control`、`pg_one`、`pg_two` |
| Redis 7 | `bromark-p0-redis` | `16386` | 无密码 |
| RabbitMQ 3.13 | `bromark-p0-rabbit` | `15686` | `bromark_it` / `p0test` |

```sh
docker run -d --name bromark-p0-pg -e POSTGRES_PASSWORD=p0test -p 15432:5432 postgres:17
docker run -d --name bromark-p0-redis -p 16386:6379 redis:7 --appendonly no
docker run -d --name bromark-p0-rabbit \
  --tmpfs /tmp:rw,mode=1777 --tmpfs /var/lib/rabbitmq:rw,mode=1777 \
  -e HOME=/tmp -e RABBITMQ_MNESIA_BASE=/tmp/rabbitmq/mnesia \
  -e RABBITMQ_LOG_BASE=/tmp/rabbitmq/log \
  -e RABBITMQ_DEFAULT_USER=bromark_it -e RABBITMQ_DEFAULT_PASS=p0test \
  -p 15686:5672 rabbitmq:3.13

for db in pg_control pg_one pg_two; do
  docker exec bromark-p0-pg psql -U postgres -c "CREATE DATABASE $db;" || true
  docker exec -i bromark-p0-pg psql -U postgres -d "$db" -v ON_ERROR_STOP=1 \
    -f - < docs/dev-ops/postgresql/v2-schema.sql
done
docker exec bromark-p0-pg psql -U postgres -c 'ALTER SYSTEM SET max_connections=300;'
docker restart bromark-p0-pg
```

## 2. 环境变量

```sh
export V2_PG_HOST=127.0.0.1:15432
export V2_PG_PASSWORD=p0test
export V2_DB00=pg_control
export V2_DB01=pg_one
export V2_DB02=pg_two
export V2_RABBIT_HOST=127.0.0.1
export V2_RABBIT_PORT=15686
export V2_RABBIT_PASSWORD=p0test
export V2_REDIS_HOST=127.0.0.1
export V2_REDIS_PORT=16386
```

容器内测试把 `127.0.0.1` 换成 `host.docker.internal`。

## 3. Seed（2,000 用户 / 10 活动 / 10 奖）

```sh
PGPASSWORD="$V2_PG_PASSWORD" python3 docs/performance/p0_seed.py \
  --dialect postgres \
  --pg-host "$V2_PG_HOST" \
  --run-id p0run1 \
  --users 2000 --activities 10 --prizes 10 \
  --out /tmp/bromark-p0-p0run1

export V2_DB00=router_p0_p0run1_control
export V2_DB01=router_p0_p0run1_one
export V2_DB02=router_p0_p0run1_two
export BM_V2_AUTH_PUBLIC_KEY="$(python3 -c 'import json;print(json.load(open("/tmp/bromark-p0-p0run1/seed.json"))["publicKey"])')"
export BM_V2_AUTH_ISSUER=https://issuer.test
export BM_V2_AUTH_AUDIENCE=bromark
```

脚本会建 `router_p0_<run>_{control,one,two}` 三个库、按 `docs/dev-ops/postgresql/v2-schema.sql` 建表、写入 10 个库存池/策略/CREDIT 奖/fallback NONE、2,000 用户和全活动额度账户，并把 seed/tokens/private key 写到仓库外。

## 4. 启动应用

```sh
java -jar bm-app/target/bm-app.jar \
  --spring.profiles.active=v2-p0 \
  --bm.v2.raffle.enabled=true
```

P0 profile 已包含 PG driver/URL、`asset-write-mode=V2`、admission/cancel 配置、pool 72、65 许可、prefetch 8。

## 5. k6

```sh
BASE_URL=http://<m5-ip>:8091 \
SEED_DIR=/tmp/bromark-p0-p0run1 \
VUS=100 DURATION=3m \
k6 run --log-format raw --console-output=/tmp/p0-requests.jsonl \
  docs/performance/p0-load.js
```

开环：`RATE=<总请求/s>`，`draw/s = RATE / 10`（7:1:1:1）。

## 6. 证据

- `p0_delivery_total{result="delivered"}` 的窗口差值 / 秒数 = `DELIVERED/s`。
- `p0_admission_total`、`p0_delivery_total`、`p0_stage_seconds`、`p0_backlog_orders`、`p0_oldest_age_seconds`、`p0_inflight_orders`。
- 端到端完成延迟：`ReserveAward` 的 `outbox.created_at` → `v2_draw_order_*.delivered_at`。
- 只写本轮实测；没有 M5 实测就不要写吞吐/P99 结论。

## 7. 数据迁移

现有 MySQL V2 数据迁移使用 `docs/dev-ops/postgresql/migrate_mysql_to_pg.py`，步骤见 `cutover-runbook.md`。迁移期间停写、保留 MySQL 只读快照；V1 旧业务不迁移。
