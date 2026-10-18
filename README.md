# BroMark



## P0 PostgreSQL 本地复现（另一台 Mac 可直接执行）

当前 P0 运行路径**只使用 PostgreSQL**，不再使用 MySQL；V1 旧 MyBatis 业务不在迁移范围内。测试数据不提交 Git（包含私钥/token），由下面的 seed 命令在目标机器重新生成，或从另一台机器安全拷贝 seed 目录/`pg_dump`。

### 1. 克隆代码

```bash
git clone https://github.com/ChihayaAnonnn/BroMark.git
cd BroMark
git checkout master   # PostgreSQL P0 已合并
```

要求：Docker Desktop、Java 25、Maven、原生 k6、Python 3、`psql`（`brew install postgresql@17` 或使用 `postgres:17` 容器）。

### 2. 启动 PostgreSQL / Redis / RabbitMQ（不要启动 MySQL）

```bash
docker run -d --name bromark-p0-pg \
  -e POSTGRES_PASSWORD=p0test -p 15432:5432 \
  postgres:17 -c max_connections=300

docker run -d --name bromark-p0-redis -p 16386:6379 redis:7 --appendonly no

docker run -d --name bromark-p0-rabbit \
  --tmpfs /tmp:rw,mode=1777 --tmpfs /var/lib/rabbitmq:rw,mode=1777 \
  -e HOME=/tmp \
  -e RABBITMQ_MNESIA_BASE=/tmp/rabbitmq/mnesia \
  -e RABBITMQ_LOG_BASE=/tmp/rabbitmq/log \
  -e RABBITMQ_DEFAULT_USER=bromark_it \
  -e RABBITMQ_DEFAULT_PASS=p0test \
  -p 15686:5672 rabbitmq:3.13
```

等待 `pg_isready`、`redis-cli ping`、RabbitMQ AMQP 端口 15686 可用。Rabbit 的 `--tmpfs`/`HOME=/tmp` 是 macOS Docker Desktop 上规避 `.erlang.cookie: eacces` 的已验证 workaround。

### 3. 生成 2,000 用户 / 10 活动 / 10 奖的测试数据

```bash
export V2_PG_HOST=127.0.0.1:15432
export V2_PG_PASSWORD=p0test
export V2_RABBIT_HOST=127.0.0.1
export V2_RABBIT_PORT=15686
export V2_RABBIT_PASSWORD=p0test
export V2_REDIS_HOST=127.0.0.1
export V2_REDIS_PORT=16386

PGPASSWORD="$V2_PG_PASSWORD" python3 docs/performance/p0_seed.py \
  --dialect postgres \
  --pg-host "$V2_PG_HOST" \
  --run-id p0run1 \
  --users 2000 --activities 10 --prizes 10 \
  --out /tmp/bromark-p0-p0run1
```

seed 会创建 `router_p0_p0run1_control/one/two` 三个 PG 库，按 `docs/dev-ops/postgresql/v2-schema.sql` 建表，写入 10 个 CREDIT 奖/10 个库存池/fallback NONE/2,000 用户/全活动额度/2,000 条结果订单。`seed.json`、`tokens.json`、`private.pem`、`public.der` 只写到 `/tmp` 下，**不要提交 Git**。

如果需要另一台机器已有的精确数据，可安全拷贝整个 seed 目录，并用 `pg_dump -Fc`/`pg_restore` 拷贝 `router_p0_p0run1_*` 三库；只拷 Git 不会包含数据库行或 token。

### 4. 启动应用

```bash
export V2_DB00=router_p0_p0run1_control
export V2_DB01=router_p0_p0run1_one
export V2_DB02=router_p0_p0run1_two
export BM_V2_AUTH_PUBLIC_KEY="$(python3 -c 'import json;print(json.load(open("/tmp/bromark-p0-p0run1/seed.json"))["publicKey"])')"
export BM_V2_AUTH_ISSUER=https://issuer.test
export BM_V2_AUTH_AUDIENCE=bromark

mvn -q -DskipTests package
java -jar bm-app/target/bm-app.jar --spring.profiles.active=v2-p0
```

验证：

```bash
curl -fsS http://127.0.0.1:8091/actuator/health
curl -fsS http://127.0.0.1:8091/actuator/prometheus | grep '^p0_'
```

`application-v2-p0.yml` 已配置 PG driver/URL、`pg-it` 预算（pool 72、65 许可）、PG 秒级超时、Rabbit channel cache 64、Prometheus 和 admission 配置；不要再启动 MySQL。

### 5. 跑 k6 smoke / 正式负载

```bash
BASE_URL=http://127.0.0.1:8091 \
SEED_DIR=/tmp/bromark-p0-p0run1 \
VUS=1 DURATION=5s \
k6 run docs/performance/p0-load.js
```

正式负载示例：

```bash
BASE_URL=http://127.0.0.1:8091 \
SEED_DIR=/tmp/bromark-p0-p0run1 \
VUS=100 DURATION=3m \
k6 run --log-format raw --console-output=/tmp/p0-requests.jsonl \
  docs/performance/p0-load.js
```

开环用 `RATE=<总请求/s>`，`draw/s = RATE / 10`。只写本轮实测的 `DELIVERED/s`、P99、错误率；没有 M5 实测数字就不要写容量结论。指标解释见 `docs/performance/p0-postgres-runbook.md`。

### 6. 回归测试（PostgreSQL 模式）

```bash
P0_DB_DIALECT=postgres \
V2_PG_HOST=127.0.0.1:15432 V2_PG_PASSWORD=p0test \
V2_DB00=pg_control V2_DB01=pg_one V2_DB02=pg_two \
mvn -q -pl bm-app -am \
  -Dtest='P0*Test,PostgresV2IntegrationIT,PostgresSqlContractIT' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

测试直接复用 seed 创建的 `router_p0_p0run1_*` 三库（已含 schema）；成功标准是 `0 failures / 0 errors`。

### 7. 迁移已有 MySQL V2 数据

工具：`docs/dev-ops/postgresql/migrate_mysql_to_pg.py`；步骤：`docs/dev-ops/postgresql/cutover-runbook.md`。迁移只覆盖 V2 三库，停写后执行，MySQL 保留为只读回滚快照；V1 不迁移。
