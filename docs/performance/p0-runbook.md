# P0 两机 benchmark runbook（M5 32GB SUT + 16GB Mac k6）

本文只描述**当前 P0 代码与 P0 profile** 在“M5 32GB 做 SUT、16GB Mac 原生 k6 做负载”的两机口径下如何准备、执行、取证和重置。它不是容量结论，也不承诺任何吞吐数字。

## 0. 硬规则：只用当前资源、当前实测数字

写报告/简历/复盘时，必须满足：

1. **资源以实测为准。** 先记录 `docker info` 的 CPU/内存、每个容器的 `--cpus/--memory`、`docker inspect` 的 `NanoCpus/Memory`。如果 Docker VM 实际只有 8C8G，就写 **8C8G**；不能写 8C16G，也不能把 8C16G 的配额套到 8C8G 的测量上。
2. **workload 以 seed 为准。** 正式口径必须是 2,000 用户 / 10 活动 / 10 奖 / `query:result:exchange:draw = 7:1:1:1`；混入其它比例、其它用户数或旧 seed 的结果不能与本轮对比。
3. **只写本轮实测。** 每个数字必须能追溯到本轮 `requests.jsonl`、`k6-summary.json`、MySQL/Rabbit/容器快照和 SHA256；缺失就写“未采集/未执行”，不能补零。
4. **不混用历史数字。** 旧 8C4G / 200 用户 / 单活动单奖的结果只能作为历史对照，不能当成本轮 P0 结果，也不能用入口 QPS 冒充 `DELIVERED/s`。
5. **smoke 不是容量。** 1 VU 或单轮短跑只能标成 smoke/验证，不能写成“可持续容量”；正式结论至少 3 轮同口径，取中位数并保留原始证据。
6. **网络条件必须标注。** 同 Wi-Fi 与有线是不同口径；Wi-Fi 必须在报告中写明，不能用 Wi-Fi 的 P99 代表生产内网延迟。

## 1. 角色、地址和网络

| 机器 | 角色 | 地址/备注 |
| --- | --- | --- |
| M5 32GB | SUT：Docker VM 8C16G，应用/MySQL/Rabbit/Redis | `M5_IP`，用 `ipconfig getifaddr en0`（Wi-Fi）或 `en1`（有线）取得 |
| 16GB Mac | 原生 k6 负载机 | `LOAD_IP`，与 M5 同一交换机/同一 Wi-Fi |

```sh
# M5：确认监听地址
ipconfig getifaddr en0    # Wi-Fi
ipconfig getifaddr en1    # 有线/雷雳网卡（名称以本机为准）
lsof -nP -iTCP:8091 -sTCP:LISTEN
```

网络要求：

- 优先**有线**；只能用 Wi-Fi 时使用同一 AP 的 5GHz，关闭 VPN、代理、iCloud 大流量同步和系统更新。
- 负载机与 SUT 在同一子网，`ping` 不应有明显的毫秒级抖动；正式跑之前用 `curl` 连续取 health 100 次，观察 P99。
- macOS 防火墙若开启，需允许 Docker Desktop/Java 的入站 8091；或者只在隔离的实验网段临时放行。不要把 8091/13316 暴露到公网。
- Docker Desktop 的 `host.docker.internal` 只用于本机容器访问宿主；两机之间必须用 `M5_IP`。k6 正式跑用**原生**，不要用 Docker k6 绕 NAT。

两台机器都建议：

```sh
caffeinate -dimsu &          # 防止 App Nap/睡眠干扰
sudo pmset -a sleep 0        # 如允许修改电源策略
ulimit -n 65535              # 负载机尤其重要
```

## 2. M5 Docker VM 资源切分

目标：Docker VM 真实配额 **8C16G**，容器配额合计不超过 VM；P0 profile 是 3 个逻辑库、每库 72 连接、总许可 65（含 CALLBACK 1）。

| 容器 | CPU | 内存 | 关键参数 |
| --- | --- | --- | --- |
| `bromark-p0-app` | 4 | 8G | `-Xms4g -Xmx4g`，端口 8091 |
| `bromark-p0-mysql` | 3 | 6G | `--max-connections=300`，`--innodb-buffer-pool-size=3G`，端口 13316 |
| `bromark-p0-rabbit` | 0.5 | 1G | 与 Redis 合计约 1C/2G；以实际配额记录 |
| `bromark-p0-redis` | 0.5 | 1G | 仅隔离实例，可 `FLUSHDB` |

先设置 Docker Desktop 的 VM 为 8 CPU / 16GB，再启动容器：

```sh
docker network create bromark-p0 2>/dev/null || true

docker run -d --name bromark-p0-mysql --network bromark-p0 \
  --cpus 3 --memory 6g --memory-swap 6g \
  -e MYSQL_ROOT_PASSWORD="$V2_MYSQL_PASSWORD" \
  -p 13316:3306 -v bromark-p0-mysql-data:/var/lib/mysql \
  mysql:8.0 \
  --max-connections=300 --innodb-buffer-pool-size=3G \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_bin

docker run -d --name bromark-p0-rabbit --network bromark-p0 \
  --cpus 0.5 --memory 1g \
  -e RABBITMQ_DEFAULT_USER=bromark_it \
  -e RABBITMQ_DEFAULT_PASS="$V2_RABBIT_PASSWORD" \
  -p 15686:5672 -p 15672:15672 rabbitmq:3.13-management

docker run -d --name bromark-p0-redis --network bromark-p0 \
  --cpus 0.5 --memory 1g \
  -p 16386:6379 redis:7 --appendonly no
```

> Rabbit 与 Redis 的 0.5C/1G 是“合计约 1C/2G”的近似切分；如果实际只能给到别的值，在报告里写实际值，不要照抄本表。

核验（把输出原样保存到取证目录）：

```sh
docker info --format 'CPUs={{.NCPU}} Memory={{.MemTotal}}'
docker inspect bromark-p0-app bromark-p0-mysql bromark-p0-rabbit bromark-p0-redis \
  --format '{{.Name}} NanoCpus={{.HostConfig.NanoCpus}} Memory={{.HostConfig.Memory}}'
docker stats --no-stream
```

如果 Docker VM 不是 8C16G，**不要继续宣称 8C16G**；按第 0 节规则在报告中写实际配额，或者换到真正的 8C16G 机器复测。

## 3. 前置条件与秘密

- M5 仓库路径：`/path/to/BroMark-p0-impl`（下称 `$REPO`）。
- M5 需要 `mysql` CLI（`brew install mysql-client` 或系统已有）；p0_seed 也接受 `--mysql-bin` 指定绝对路径。
- 负载机需要原生 k6：

```sh
brew install k6
k6 version    # 建议 1.3.x；本脚本在 1.3.0 上验证
```

- 密码只放在 shell 环境变量里，不写入仓库、日志或截图：

```sh
export V2_MYSQL_PASSWORD='<isolated-mysql-password>'
export V2_RABBIT_PASSWORD='<isolated-rabbit-password>'
```

- seed 输出目录必须在 Git 仓库外（脚本会拒绝仓库内路径），例如 `~/bromark-seed/$RUN` 或 `/tmp/bromark-seed/$RUN`。目录权限 700、`tokens.json`/`private.pem` 600。
- 负载机只需要 `seed.json` + `tokens.json`；**不需要也不应该复制 `private.pem`**。tokens 是 bearer 凭据，传到负载机后同样放在仓库外、权限 600。

## 4. 空库重置（顺序不能反）

每次正式轮次前都从空库开始；不要在上一次 run 的库上叠加。

### 4.1 停应用和消费者

```sh
docker stop bromark-p0-app 2>/dev/null || true
# 确认没有应用在写 MySQL；正式环境还要确认 relay/消费者已停。
```

### 4.2 删除旧 Rabbit 队列和 exchange

P0 profile 的 prefix 是 `p0`，声明的 exchange/queue 是：

- exchange：`p0.events`
- queues：`p0.reserve`、`p0.deliver`、`p0.notify`

```sh
RABBIT=bromark-p0-rabbit
for q in p0.reserve p0.deliver p0.notify; do
  docker exec "$RABBIT" rabbitmqctl delete_queue "$q" 2>/dev/null || true
done
docker exec "$RABBIT" rabbitmqctl delete_exchange p0.events 2>/dev/null || true

# 旧前缀如果存在也一并清掉，避免旧绑定/旧消费者污染本轮：
for q in bromark.v2.reserve bromark.v2.deliver bromark.v2.notify; do
  docker exec "$RABBIT" rabbitmqctl delete_queue "$q" 2>/dev/null || true
done
docker exec "$RABBIT" rabbitmqctl delete_exchange bromark.v2.events 2>/dev/null || true
```

### 4.3 删除三个逻辑库

`p0_seed.py` 只接受全新库；先显式 DROP。drop 三个库同时清掉 outbox、retry_job、consumer_inbox、receipt、旧订单等所有残留，不需要逐表 DELETE。

```sh
RUN=p0run1
for role in control one two; do
  db="router_p0_${RUN}_${role}"
  MYSQL_PWD="$V2_MYSQL_PASSWORD" mysql -h 127.0.0.1 -P 13316 -uroot \
    -e "DROP DATABASE IF EXISTS \`$db\`"
done
```

### 4.4 清 Redis（仅隔离实例）

```sh
docker exec bromark-p0-redis redis-cli FLUSHDB
```

### 4.5 重新 seed

在 M5 的仓库根目录执行（全量 2,000/10/10）：

```sh
RUN=p0run1
SEED_DIR="$HOME/bromark-seed/$RUN"

python3 docs/performance/p0_seed.py \
  --run-id "$RUN" \
  --users 2000 --activities 10 --prizes 10 \
  --out "$SEED_DIR" \
  --mysql-host 127.0.0.1 --mysql-port 13316 \
  --mysql-password "$V2_MYSQL_PASSWORD"
```

如果 M5 的 MySQL 在 Docker 网络内且你就在该网络里执行，可改成 `--mysql-host bromark-p0-mysql --mysql-port 3306`。脚本不绑定容器名，也不会自动 `docker exec`。

快速 smoke 可以先用小规模（**不能**当作正式容量）：

```sh
python3 docs/performance/p0_seed.py \
  --run-id p0smoke1 --users 20 --activities 2 --prizes 2 \
  --out /tmp/p0-smoke-seed \
  --mysql-host 127.0.0.1 --mysql-port 13316 \
  --mysql-password "$V2_MYSQL_PASSWORD"
```

脚本会打印摘要（不含 token）：

- `users=2000`、`activities=10`、`prizes=10`、`creditPrizes=10`、`stockPools=10`、`resultOrders=2000`、`fallback=NONE`。
- `seed.json` 的 `activities[*].activityId`、`prizes[*].stockPoolId`、`resultOrders` 供 k6 使用；`publicKey` 供应用启动。
- `tokens.json` 是 2,000 个 RS256 JWT；`private.pem` 只留在 M5 的 seed 目录，不复制到负载机。

seed 后核对（示例，数据库名按 `$RUN` 替换）：

```sh
MYSQL_PWD="$V2_MYSQL_PASSWORD" mysql -h 127.0.0.1 -P 13316 -uroot --batch \
  -D "router_p0_${RUN}_control" -e "
SELECT 'strategies',COUNT(*) FROM strategy_version
UNION ALL SELECT 'stock',COUNT(*) FROM award_stock
UNION ALL SELECT 'bindings',COUNT(*) FROM strategy_stock_binding
UNION ALL SELECT 'pointers',COUNT(*) FROM v2_activity_pointer
UNION ALL SELECT 'fallback_none',COUNT(*)
  FROM strategy_version
  WHERE JSON_UNQUOTE(JSON_EXTRACT(snapshot,'\$.fallback.type'))='NONE';"

# 两个 user 库分别检查；合计应为 2000 个 credit 账户、20000 个活动账户、2000 条预置结果订单。
for role in one two; do
  MYSQL_PWD="$V2_MYSQL_PASSWORD" mysql -h 127.0.0.1 -P 13316 -uroot --batch \
    -D "router_p0_${RUN}_${role}" -e "
  SELECT '$role credit',COUNT(*) FROM user_credit_account
  UNION ALL SELECT '$role activity',COUNT(*) FROM raffle_activity_account
  UNION ALL SELECT '$role draws',
    (SELECT COUNT(*) FROM v2_draw_order_000)+(SELECT COUNT(*) FROM v2_draw_order_001)
    +(SELECT COUNT(*) FROM v2_draw_order_002)+(SELECT COUNT(*) FROM v2_draw_order_003);"
done
```

### 4.6 启动应用

先按仓库现有 Java 25 流程构建 jar 和镜像（如宿主没有 JDK 25，使用现有的 Maven Docker 流程）：

```sh
mvn -q -pl bm-app -am -DskipTests package
docker build -t bromark-p0-app:local bm-app
```

读取 seed 的公钥：

```sh
AUTH_PUBLIC_KEY=$(python3 -c \
  'import json,sys; print(json.load(open(sys.argv[1]))["publicKey"])' \
  "$SEED_DIR/seed.json")
```

启动（应用与 MySQL/Rabbit/Redis 同 Docker 网络，走容器 DNS，避免 host 端口转发）：

```sh
docker run -d --name bromark-p0-app --network bromark-p0 \
  --cpus 4 --memory 8g --memory-swap 8g \
  -p 8091:8091 \
  -e JAVA_OPTS='-Xms4g -Xmx4g' \
  -e PARAMS='--spring.profiles.active=v2-p0 --bm.v2.raffle.enabled=true --server.port=8091' \
  -e V2_MYSQL_HOST=bromark-p0-mysql:3306 \
  -e V2_MYSQL_PASSWORD="$V2_MYSQL_PASSWORD" \
  -e V2_DB00="router_p0_${RUN}_control" \
  -e V2_DB01="router_p0_${RUN}_one" \
  -e V2_DB02="router_p0_${RUN}_two" \
  -e V2_RABBIT_HOST=bromark-p0-rabbit \
  -e V2_RABBIT_PORT=5672 \
  -e V2_RABBIT_PASSWORD="$V2_RABBIT_PASSWORD" \
  -e V2_REDIS_HOST=bromark-p0-redis \
  -e V2_REDIS_PORT=6379 \
  -e BM_V2_AUTH_PUBLIC_KEY="$AUTH_PUBLIC_KEY" \
  -e BM_V2_AUTH_ISSUER='https://issuer.test' \
  -e BM_V2_AUTH_AUDIENCE='bromark' \
  bromark-p0-app:local
```

说明：

- P0 profile 已设置 pool 72、预算 65、prefix `p0`、bounded relay；但**当前 profile 没有显式设置 `bm.v2.raffle.enabled`**，所以启动参数必须带 `--bm.v2.raffle.enabled=true`，否则抽奖/活动查询 controller 不会注册。
- `PARAMS` 是 bm-app Dockerfile 的入口参数；不要改成在镜像名后面追加 `--spring...`。
- 若不用 Docker 跑应用，去掉 `--network`/`-p`，把 `V2_MYSQL_HOST` 改为 `127.0.0.1:13316`，并按实际进程资源记录；资源口径会与 4C8G 容器不同。

启动后检查：

```sh
curl -fsS "http://$M5_IP:8091/actuator/health"
docker logs --tail 200 bromark-p0-app
docker exec bromark-p0-rabbit rabbitmqctl list_queues name messages consumers
```

确认：health 为 `UP`；`p0.reserve`/`p0.deliver`/`p0.notify` 已声明且有消费者；日志里是 `p0` 前缀，不是旧 `bromark.v2`；没有启动期预算/连接池校验错误。

## 5. 负载机准备与 k6 命令

### 5.1 复制 seed 与建立输出目录

```sh
REPO="$HOME/BroMark-p0-impl"   # 负载机上的仓库路径，按实际修改
RUN=p0run1
mkdir -p "$HOME/bromark-runs/$RUN" "$HOME/bromark-seed/$RUN"
scp "m5:bromark-seed/$RUN/seed.json"   "$HOME/bromark-seed/$RUN/"
scp "m5:bromark-seed/$RUN/tokens.json" "$HOME/bromark-seed/$RUN/"
chmod 600 "$HOME/bromark-seed/$RUN/tokens.json"
```

负载机上也要有本仓库的 `docs/performance/p0-load.js`（`git archive`/`scp` 均可）。输出目录不要放在仓库里。

### 5.2 smoke（1 VU，60 秒）

```sh
M5_IP=<M5-address>
RUN=p0run1
OUT="$HOME/bromark-runs/$RUN/smoke"
mkdir -p "$OUT"

k6 run \
  -e BASE_URL="http://$M5_IP:8091" \
  -e SEED_DIR="$HOME/bromark-seed/$RUN" \
  -e RUN_ID="$RUN-smoke-$(date +%s)" \
  --vus 1 --duration 60s \
  --log-format raw \
  --console-output "$OUT/requests.jsonl" \
  --summary-export "$OUT/k6-summary.json" \
  "$REPO/docs/performance/p0-load.js" 2>&1 | tee "$OUT/k6.log"
```

smoke 通过的最低检查：

- `requests.jsonl` 每行都有 `requestId/user/op/status/latency/orderId/state` 字段；`op` 比例为 7:1:1:1。
- 至少出现 `query/result/exchange/draw` 四类请求；`result` 的 `targetOrderId` 来自 `seed.json.resultOrders`。
- 没有 401/403（token/issuer/audience 配置错误）、没有连接拒绝；5xx 和超时单独统计，不能混入业务拒绝。

### 5.3 预热（1 VU，60 秒，不计入正式分母）

```sh
OUT="$HOME/bromark-runs/$RUN/warmup"
k6 run -e BASE_URL="http://$M5_IP:8091" -e SEED_DIR="$HOME/bromark-seed/$RUN" \
  -e RUN_ID="$RUN-warmup-$(date +%s)" --vus 1 --duration 60s \
  --log-format raw --console-output "$OUT/requests.jsonl" \
  --summary-export "$OUT/k6-summary.json" \
  "$REPO/docs/performance/p0-load.js" 2>&1 | tee "$OUT/k6.log"
```

预热目录、smoke 目录和正式目录必须分开；**只有正式目录的请求进入正式分母**。

### 5.4 闭环对照（5/100/300/600 VUs，45–60 秒）

```sh
for V in 5 100 300 600; do
  OUT="$HOME/bromark-runs/$RUN/closed-$V"
  mkdir -p "$OUT"
  k6 run -e BASE_URL="http://$M5_IP:8091" -e SEED_DIR="$HOME/bromark-seed/$RUN" \
    -e RUN_ID="$RUN-c$V-$(date +%s)" --vus "$V" --duration 60s \
    --log-format raw --console-output "$OUT/requests.jsonl" \
    --summary-export "$OUT/k6-summary.json" \
    "$REPO/docs/performance/p0-load.js" 2>&1 | tee "$OUT/k6.log"
done
```

闭环数字只用于和旧实验对照，不作为主容量曲线。

### 5.5 开环主曲线（固定到达率）

脚本的 `RATE` 是**混合请求总到达率**，因为配比是 7:1:1:1：

| 目标 draw/s | `RATE`（总请求/s） |
| --- | --- |
| 20 | 200 |
| 40 | 400 |
| 80 | 800 |
| 120 | 1200 |
| 160 | 1600 |

每档 3–5 分钟，至少 3 轮：

```sh
for DRAW_RATE in 20 40 80 120 160; do
  RATE=$((DRAW_RATE * 10))
  OUT="$HOME/bromark-runs/$RUN/open-$DRAW_RATE"
  mkdir -p "$OUT"
  k6 run \
    -e BASE_URL="http://$M5_IP:8091" \
    -e SEED_DIR="$HOME/bromark-seed/$RUN" \
    -e RUN_ID="$RUN-o$DRAW_RATE-$(date +%s)" \
    -e RATE="$RATE" -e VUS=300 -e DURATION=3m \
    --log-format raw \
    --console-output "$OUT/requests.jsonl" \
    --summary-export "$OUT/k6-summary.json" \
    "$REPO/docs/performance/p0-load.js" 2>&1 | tee "$OUT/k6.log"
done
```

> **不要**在开环命令里再传 `--vus`/`--duration`：k6 CLI 会整段覆盖脚本里的 scenario，`RATE` 会被忽略。开环只用 `-e RATE/-e VUS/-e DURATION`。闭环才用 `--vus/--duration`。
>
> 若 k6 报 `dropped_iterations`，说明 `VUS`（preAllocatedVUs/maxVUs）不足以支撑该到达率；先提高 `-e VUS`，不要把 dropped 当成业务拒绝或 0 延迟。

## 6. 结果取证

每个正式目录至少保存：

| 文件 | 采集方式 |
| --- | --- |
| `requests.jsonl` | k6 `--log-format raw --console-output`；纯 JSONL |
| `k6-summary.json` | k6 `--summary-export` |
| `k6.log` | k6 stdout/stderr（`tee`） |
| `app.log` | `docker logs bromark-p0-app` |
| `docker-stats.txt` | `docker stats --no-stream` |
| `docker-inspect.json` | 四个容器的 `NanoCpus/Memory/Image/Id` |
| `mysql-status.txt` | `SHOW GLOBAL STATUS` + `Threads_connected`/`Innodb_row_lock_*` |
| `rabbit-queues.txt` | `rabbitmqctl list_queues name messages messages_ready messages_unacknowledged consumers` |
| `backlog.txt` | 各 user 库 `PENDING_RESERVATION` 计数、`outbox` pending 计数 |
| `clock-before.json` / `clock-after.json` | 两台机器 `python3 -c 'import time;print(time.time())'` 与 `date -u` |
| `env.txt` | `git rev-parse HEAD`、`k6 version`、`docker info`、`uname -a`、seed 参数（不含密码/token） |
| `SHA256SUMS` | 对上述文件做 `shasum -a 256` |

采集示例：

```sh
OUT="$HOME/bromark-runs/$RUN/open-80"
docker logs bromark-p0-app > "$OUT/app.log" 2>&1
docker stats --no-stream > "$OUT/docker-stats.txt"
docker inspect bromark-p0-app bromark-p0-mysql bromark-p0-rabbit bromark-p0-redis \
  --format '{{json .}}' > "$OUT/docker-inspect.json"
docker exec bromark-p0-rabbit rabbitmqctl list_queues name messages messages_ready \
  messages_unacknowledged consumers > "$OUT/rabbit-queues.txt"

MYSQL_PWD="$V2_MYSQL_PASSWORD" mysql -h 127.0.0.1 -P 13316 -uroot --batch -e "
SHOW GLOBAL STATUS WHERE Variable_name IN
 ('Threads_connected','Threads_running','Innodb_row_lock_waits',
  'Innodb_row_lock_time_avg','Innodb_buffer_pool_read_requests',
  'Innodb_buffer_pool_reads','Aborted_connects');" > "$OUT/mysql-status.txt"

for role in one two; do
  MYSQL_PWD="$V2_MYSQL_PASSWORD" mysql -h 127.0.0.1 -P 13316 -uroot --batch \
    -D "router_p0_${RUN}_${role}" -e "
  SELECT '$role pending',
    (SELECT COUNT(*) FROM v2_draw_order_000 WHERE state='PENDING_RESERVATION')
   +(SELECT COUNT(*) FROM v2_draw_order_001 WHERE state='PENDING_RESERVATION')
   +(SELECT COUNT(*) FROM v2_draw_order_002 WHERE state='PENDING_RESERVATION')
   +(SELECT COUNT(*) FROM v2_draw_order_003 WHERE state='PENDING_RESERVATION') AS pending,
    (SELECT COUNT(*) FROM outbox WHERE state='PENDING') AS outbox_pending;"
done | tee "$OUT/backlog.txt"

shasum -a 256 "$OUT"/* > "$OUT/SHA256SUMS"
```

不要用 `k6` 客户端延迟代替服务端 `DELIVERED` 延迟；不要用入口 2xx 代替交付吞吐；不要把 seed 的 2,000 条预置结果订单算成本轮新交付。

## 7. 本轮跑完后的重置

```sh
docker stop bromark-p0-app 2>/dev/null || true
RABBIT=bromark-p0-rabbit
for q in p0.reserve p0.deliver p0.notify; do
  docker exec "$RABBIT" rabbitmqctl delete_queue "$q" 2>/dev/null || true
done
docker exec "$RABBIT" rabbitmqctl delete_exchange p0.events 2>/dev/null || true

RUN=p0run1
for role in control one two; do
  MYSQL_PWD="$V2_MYSQL_PASSWORD" mysql -h 127.0.0.1 -P 13316 -uroot \
    -e "DROP DATABASE IF EXISTS \`router_p0_${RUN}_${role}\`"
done
docker exec bromark-p0-redis redis-cli FLUSHDB
```

- 原始证据先复制到仓库外的归档目录，再删 seed/token；`tokens.json`/`private.pem` 不提交 Git。
- 下一次正式轮次从第 4 节重新开始，不要复用旧库或旧队列。

## 8. 已知边界 / 常见问题

- **P0 profile 未写 `bm.v2.raffle.enabled`**：必须在启动参数里加 `--bm.v2.raffle.enabled=true`，否则 `/api/v2/draws` 与 `/api/v2/activities/*` 不注册。本文不修改 profile，只记录运行要求。
- **max_connections**：P0 需要 MySQL `max_connections=300`；seed 在低于 300 时只警告，不阻止小规模 smoke。正式跑前必须核对。
- **host 端口转发**：应用与 MySQL 同 Docker 网络时优先用容器 DNS `bromark-p0-mysql:3306`；`host.docker.internal:13316` 会绕宿主端口，容量口径会变。
- **k6 场景覆盖**：开环不要传 `--vus/--duration`；闭环可以传，CLI 会覆盖脚本的 scenario。
- **Wi-Fi**：同 Wi-Fi 的 P99 受 AP/信道/邻居影响；正式结论优先有线，Wi-Fi 结果必须标注。
- **结果订单**：`seed.json.resultOrders` 是每个用户一条预置 DELIVERED 订单，供 result 查询；它不是本轮新交付，不能与服务端 `DELIVERED/s` 混算。
- **证据缺失**：没有 `requests.jsonl`/`k6-summary.json`/资源快照的运行，不能写成“通过”；缺项写“未采集”。
