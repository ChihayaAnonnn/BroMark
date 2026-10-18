# P0 异步交付链路优化设计（8C16G 单机口径）

日期：2026-10-18

状态：待评审；尚未实施，未产生新的性能数字

适用范围：BroMark V2 抽奖/兑换的异步交付链路（P0）

目标环境：单机 8C16G 全栈，应用 4C8G、MySQL 3C6G、RabbitMQ+Redis 1C2G，k6 外置

目标 workload：2,000 用户、10 个活动、10 个奖，query/result/exchange/draw = 7:1:1:1

## 1. 目的与结论

当前 P0 的问题不是“消费线程少”，而是**每笔抽奖被拆成 6 段串行消费、8 个共享线程、单队列，叠加 8 个 DB 事务和 4 次跨库跳转**。在 8C4G、200 用户、单活动单奖的实验里，600 VU 受理约 164 draw/s，交付事件只有 0.8/s；即使 5 VU 也只有 7.5/s。

本设计把 P0 收敛为**三段事务、两个事件、两次跨库**：

1. **user 受理事务**：锁账户、扣额度、选奖、落订单、写 `ReserveAward` outbox；
2. **control 库存事务**：领取 `ReserveAward`，一次事务完成 reserve + confirm，写 `ReservationConfirmed` outbox；
3. **user 交付事务**：领取 `ReservationConfirmed`，加积分、写交付记录、订单置 `DELIVERED`。

目标是在同一台 8C16G 机器上，用更少的事务和更独立的消费池，把**可持续 `DELIVERED/s`** 推到当前资源下的最高值；入口 QPS、P99、错误率和资源利用率作为同期观测，不用入口 QPS 代替完成吞吐。

本文是设计文档，不是实施结果。所有性能数字都必须按第 9 节口径实测后才能写进简历。

## 2. 目标、非目标、反目标

### 2.1 目标

- 把每笔抽奖的稳态 DB 事务数从 **8** 降到 **3**，事件数从 **6** 降到 **2**，跨库跳转从 **4** 降到 **2**。
- 把消费端从“一个 workflow 队列 + 8 个共享线程”改成“按阶段独立队列 + 独立消费池 + 后台 DB 预算”。
- 在不关闭 `fsync`/`sync_binlog`、不降低资产正确性的前提下，提高完成吞吐。
- 给入口加背压，使“最高 QPS”是积压可控的可持续值，而不是无限收单。
- 形成可复现的 8C16G 全栈基准：入口 QPS、`DELIVERED/s`、P50/P95/P99、错误率、应用/MySQL 资源利用率。
- 产出可用于简历的实测口径：同资源、同 workload、同脚本、至少三轮，报告中位数和原始证据。

### 2.2 非目标

- 不做生产迁移，不兼容旧事件格式，不保留旧消息，不迁移旧数据。
- 不把 MySQL 换成 PostgreSQL，不引入 Kafka，不关闭持久化。
- 不在本期做库存分桶、账户余额原子更新、control/user 分实例、查询只读副本。
- 不改变分库路由模型（3 个逻辑库 + 2 个用户库 × 4 张分表）。
- 不改前端、支付、签到、兑换的业务语义；兑换/签到的通知仍走独立 `p0.notify` 队列，语义不变。
- 不把“入口受理 QPS”当作简历主指标。

### 2.3 反目标

- 入口 QPS 很高但积压无限增长——这不是容量提升。
- 只加消费线程、连接池或 CPU，不减少事务/跨库——这会把瓶颈推到 DB 后再次饱和。
- 为了数字关闭 `innodb_flush_log_at_trx_commit`、`sync_binlog` 或降低幂等/对账要求。
- 用一次单轮 A/B 的小差异写“提升 N 倍”。
- 在未实测前把设计目标写成简历数字。

## 3. 资源与 workload 口径

### 3.1 资源

| 组件 | 配额 | 说明 |
|---|---|---|
| 应用 | 4C / 8G，`-Xmx4g` | 单实例；HTTP + 消费线程共享 |
| MySQL | 3C / 6G，buffer pool 3G | 3 个逻辑库同一实例；`max_connections=300` |
| RabbitMQ + Redis | 1C / 2G | 共享剩余资源 |
| OS / Docker | 共享，不单独配额 | 8C16G 已被上面三项占满；监控 pressure/steal，若不稳定从应用或 MySQL 回收 0.5C/1G |
| k6 | 优先外置；只有一台 Mac 时同机独立 cgroup | 外置最干净；同机时压测端会占宿主资源，报告必须标注 |

要求：8C16G 是**整机/VM 的真实配额**，不是 16G Mac 上 Docker Desktop 的虚配；压测前用 `docker inspect`、cgroup `cpu.max`、`memory.max` 核验。

现实约束：如果只有一台 16GB Mac 的 Docker，Docker VM 通常只能分到 8GB 左右，无法干净地跑 8C16G 全栈。此时按**实际可用配额**（例如 8C8G）测量并在报告/简历写实际值，不把 8C8G 写成 8C16G；云 8C16G 作为可选复测。资源口径必须在结果中显式标注，不能混用。

### 3.2 workload

- 2,000 用户，10 个活动，10 个奖；不是单活动单奖。奖品均匀命中，避免单奖 100% 的合成热点。
- 10 个奖均为 `CREDIT`，每个奖有独立 `stockPoolId`；只有 `NONE`（谢谢参与）无库存。`fallback` 固定为 `NONE`。
- 请求配比固定 `query : result : exchange : draw = 7 : 1 : 1 : 1`。
- 每用户期初额度/积分充足；预置 2,000 笔已完成结果订单，供 result 查询使用。
- 预热 1 VU 60 秒，不计入正式统计；正式阶段每档 3～5 分钟。
- 主容量曲线用**开环固定到达率**（20/40/80/120/160 draw/s 等价混合），不用闭环 VU 反推容量；闭环 5/100/300/600 VU 只用于和旧实验对照。

### 3.3 主指标

| 指标 | 定义 | 用途 |
|---|---|---|
| **可持续 `DELIVERED/s`** | 稳态下每秒进入 `DELIVERED` 的订单数 | 主指标 |
| 入口 QPS | 2xx 且非业务 REJECTED 的请求/秒；202 只算受理 | 辅助 |
| 写受理 QPS | draw + exchange 的成功受理/秒 | 辅助 |
| 端到端 P50/P95/P99 | 请求发出到订单 `DELIVERED`，或 HTTP 成功延迟（分开报） | 护栏 |
| 错误率 | 5xx/超时/池超时/业务拒绝，分开列 | 护栏 |
| 积压 | `PENDING_RESERVATION` 数量、最老年龄、outbox pending | 可持续性判据 |
| 资源 | 应用 CPU/内存、MySQL CPU/IO、Hikari wait、GC | 解释瓶颈 |

“可持续”判据：在某一到达率下连续运行至少 3 分钟，积压斜率约 0（允许启动瞬态），错误率不超护栏，P99 不超护栏；再高一档若积压持续增长，则上一档就是当前资源的最大可持续吞吐。

## 4. 现状与设计约束

### 4.1 现状链路

```
HTTP 受理（control 查配置 + user 扣额度/落单/outbox）
→ PrepareDraw（user：选奖、CANDIDATE_FIXED、outbox ReserveAward）
→ ReserveAward（control：hold 库存、outbox ReservationHeld）
→ ReservationHeld（user：RESULT_RECORDED、outbox ConfirmReservation）
→ ConfirmReservation（control：move 库存、outbox ReservationConfirmed）
→ ReservationConfirmed（user：COMMITTED、outbox AwardDelivery）
→ AwardDelivery（user：加积分、DELIVERED）
```

每段是一个独立的 Rabbit 消息、独立 `ConsumerUnitOfWork` 事务。消费端一个 `workflow` 队列、8 个消费者；每段都经过 inbox、receipt、业务 SQL、outbox insert、outbox hash select、commit。

### 4.2 必须保持的不变量

1. **不超卖**：`available + reserved + allocated` 与实际库存一致；任何扣减/释放都以条件更新和影响行数为准。
2. **不重复发奖**：同一 `(user, bizId)` 的积分流水唯一；交付事务重复执行只返回已存在结果。
3. **不丢单**：订单与 `ReserveAward` outbox 同一 user 事务提交；control 确认与 `ReservationConfirmed` outbox 同一 control 事务提交。
4. **不提前 ack**：消息只在本地事务提交后 ack；失败进入重试/DLQ，不丢消息。
5. **锁顺序固定**：user 账户 → 活动账户 → 订单；control 库存行单独加锁；批量时按 user/stockPoolId 排序。
6. **事件身份不可变**：`eventId`、`requestHash`、`aggregateVersion` 在重试中保持不变；消费端校验 payload hash。
7. **受理与交付解耦但可观测**：入口 202 只代表受理；完成以 `DELIVERED` 为准。
8. **背压不拒绝已受理订单**：只限制新受理，不取消在途。

### 4.3 设计约束

- 不改变分库路由：control 库 + 2 个 user 库；user 库 4 张分表。
- 跨库没有 XA：三段事务仍是最终一致，靠 outbox + inbox + 状态 CAS。
- 不做旧格式兼容：可以删除旧队列、旧事件、旧状态；数据库可重建。
- 单实例部署：per-user 内存限流/信号量可用；如果未来多实例，需要改为 Redis/DB 计数。

## 5. 目标架构

### 5.1 三段事务

```
T1 user 受理
  lock account → 幂等检查 → 选奖(candidate/fallback) → 扣额度
  → insert order(PENDING_RESERVATION) → outbox ReserveAward → commit → 202

T2 control 库存
  inbox dedupe → lock stock row → reserve + confirm(一次事务)
  → award_reservation(CONFIRMED) → outbox ReservationConfirmed → commit → ack

T3 user 交付
  inbox dedupe → lock account → order FOR UPDATE
  → 幂等检查 → 加积分/写交付/award → order DELIVERED → commit → ack
```

稳态下每笔抽奖 3 个 DB 事务、2 个事件、2 次跨库、2 次账户锁（T1 + T3）。control 库存锁只在 T2。

### 5.2 状态机

只保留必要状态：

```
REJECTED              受理拒绝（额度不足/活动关闭/黑名单），不产生事件
PENDING_RESERVATION   已受理、候选已定、额度已扣、ReserveAward 已入 outbox
DELIVERED             已发奖/已交付，终态
```

删除 `ACCEPTED`、`CANDIDATE_FIXED`、`RESULT_RECORDED`、`COMMITTED`、`CANCELLING`、`CANCELLED` 这些中间或取消状态。原因是：

- 选奖在 T1 完成，不再需要 `ACCEPTED → CANDIDATE_FIXED`；
- 库存确认由 `ReservationConfirmed` 事件承载，T3 一次完成 `COMMITTED + DELIVERED`，不需要两个持久中间态；
- 业务不允许取消，订单受理后只走向 `DELIVERED`；技术异常进 DLQ/人工复核，不提供用户取消路径。

订单表保留 `created_at`、`reserved_at`、`delivered_at` 作为审计时间，不靠中间状态表达进度。

### 5.3 事件契约（schemaVersion = 2）

#### ReserveAward（user → control）

```json
{
  "schemaVersion": 2,
  "eventId": "evt-...",
  "eventType": "ReserveAward",
  "orderId": "DR-...",
  "userId": "u-...",
  "activityId": 1001,
  "requestHash": "...",
  "identityHash": "...",
  "strategyId": 2001,
  "strategyVersion": "v7",
  "candidate": {
    "awardId": 1,
    "type": "CREDIT",
    "amount": "10.00",
    "stockPoolId": "pool-1001",
    "quantity": 1,
    "configHash": "..."
  },
  "fallback": {
    "awardId": 9,
    "type": "NONE",
    "amount": null,
    "stockPoolId": null,
    "quantity": 0,
    "configHash": "..."
  },
  "occurredAt": "2026-10-18T03:00:00Z"
}
```

要求：

- 所有 `CREDIT` 奖必须有 `stockPoolId`（积分也走库存）；只有 `NONE`（谢谢参与）无库存。
- `fallback` **固定为 `NONE`**，因此 T2 在候选库存不足时总能落到谢谢参与，不存在“无奖可发”的正常分支。发布策略时校验 `fallback.type == NONE && fallback.stockPoolId == null`；不满足则拒绝发布。
- `candidate`/`fallback` 的 `configHash` 必须能通过 strategy version 校验。
- 事件体不含可变余额/库存快照；control 以库存事实为准。
- control 消费 `ReserveAward` 时必须重新校验 `strategyId + strategyVersion`、candidate/fallback 的 `configHash` 和 `requestHash`；校验失败进入重试/人工复核，不能按事件里的快照直接扣库存。
- user 消费 `ReservationConfirmed` 时必须校验 `requestHash`、`finalAward` 等于订单的 candidate 或 fallback、`configHash` 一致；不一致进入重试/复核，不能直接加积分。

#### ReservationConfirmed（control → user）

```json
{
  "schemaVersion": 2,
  "eventId": "evt-...",
  "eventType": "ReservationConfirmed",
  "orderId": "DR-...",
  "userId": "u-...",
  "requestHash": "...",
  "reservationId": "RS-...",
  "finalAward": {
    "awardId": 1,
    "type": "CREDIT",
    "amount": "10.00",
    "stockPoolId": "pool-1001",
    "quantity": 1,
    "configHash": "..."
  },
  "stockPoolId": "pool-1001",
  "quantity": 1,
  "confirmedAt": "2026-10-18T03:00:01Z"
}
```

本设计只有两个稳态事件。`ReservationConfirmed.finalAward` 可以是 candidate（`CREDIT`，带 stockPoolId）或 fallback `NONE`；`NONE` 时 user 只把订单置 `DELIVERED`，不加积分、不扣库存。

策略/库存结构不一致、消息损坏、库存池非 ACTIVE 等技术异常走 retry/DLQ/人工复核，不新增业务拒绝/取消事件。

删除：`PrepareDraw`、`ReservationHeld`、`ConfirmReservation`、`AwardDelivery`、`CancelReservation`、`ReservationCancelled`、`ReservationRejected`。

### 5.4 队列与消费拓扑

```text
exchange: p0.events (direct, durable)

queue: p0.reserve
  binding: ReserveAward
  consumer: control 库；并发 8；prefetch 8；手动 ack

queue: p0.deliver
  binding: ReservationConfirmed
  consumer: user 库；并发 16；prefetch 8；手动 ack

queue: p0.notify
  binding: ExchangeCompleted, SignInCompleted
  consumer: user 库；并发 4；prefetch 8；手动 ack
  说明：兑换/签到通知不属于 P0 三段链路，本期只改队列名与拓扑，不改语义

```

要点：

- **按阶段拆队列**，`p0.reserve` 和 `p0.deliver` 各自独立容器、独立线程池、独立并发。下一段事件只可能在上段提交后产生，因此拆队列不会破坏同一订单的因果顺序；重复/重试由 inbox 和状态 CAS 吸收。
- `p0.deliver` 并发高于 `p0.reserve`：交付是 user 库、按用户分散；库存是 10 个奖/10 行，control 并发过高只会增加行锁。兑换/签到通知单独 `p0.notify`，避免和交付抢同一批消费者。
- 不再使用旧 `bromark.v2.workflow`；`notifications` 改为 `p0.notify`，语义不变；切换时删除旧队列和旧 exchange。
- 监听器统一走 `V2MessageListener`，保留“提交后 ack、失败进 retry、耗尽进 DLQ”的语义。
- 不单独声明 broker `p0.dlq`：P0 的重试耗尽/格式错误由 `retry_job DEAD` + `raw_message` quarantine 承担；声明一个无人消费的队列会让运维误判。
- 每个队列暴露 ready/unacked 指标；消费端按阶段记录耗时。

### 5.5 数据库与连接预算

连接池：

| 逻辑库 | Hikari max | minIdle | connectionTimeout |
|---|---:|---:|---:|
| control | 72 | 0 | 1s |
| user db01 | 72 | 0 | 1s |
| user db02 | 72 | 0 | 1s |

`BoundedDatabaseBudget` 初始预算（按物理实例合计，必须小于每个逻辑池 72）：

| WorkClass | 许可 | 用途 |
|---|---:|---|
| TRADE | 24 | HTTP 受理/兑换 |
| WORKER | 28 | `p0.reserve` + `p0.deliver` + `p0.notify` 消费 |
| CALLBACK | 1 | 支付回调等既有 CALLBACK 工作 |
| QUERY | 8 | 查询/结果 |
| RELAY | 2 | outbox/retry 发布 |
| RECOVERY | 2 | 恢复/对账 |
| **合计** | **65** | 65 < 72，留 7 个余量 |

说明：

- 这是**初始值**，不是最优值；实施后按 3C MySQL 的实际 CPU/锁等待调整，但必须满足 `total < 每个逻辑池 max`，否则 `V2WorkerConfig.validateConnections` 会拒绝启动。
- `WORKER=28` 对应 28 个消费线程（8 reserve + 16 deliver + 4 notify），线程与许可一一对应，不额外排队；`CALLBACK=1` 保留给既有支付回调，虽然 P0 主链路不用它。如果交付仍被许可卡住，再在总预算内提高 WORKER、降低 TRADE。
- 不为后台单独建第二套 Hikari（本期）；先用 `BoundedDatabaseBudget` 的按类信号量隔离，避免连接池被 HTTP 线程全部占用。若实测仍被池 FIFO 卡住，再单独设计后台池。
- MySQL 3C6G：buffer pool 3G，`innodb_flush_log_at_trx_commit=1`、`sync_binlog=1`、`log_bin=ON`、`flush_method=fsync` 不变；`max_connections=300`。
- `bm.v2.budget.physical-connection-limits.mysql-it=240`：3×72=216 个池连接 + 20 预留 ≤ 240；启动时由 `validateConnections` 校验。P0 profile 的总许可是 65（含 CALLBACK 1），不是 64。
- 三个逻辑库仍在一个 MySQL 实例；本设计不把 control/user 拆到不同物理机。

### 5.6 批处理与提交

当前 COMMIT 平均约 18ms，是最大单项之一。三段事务已经把每单提交从 8 次降到 3 次；批处理是第二层杠杆。

设计：

- `p0.reserve`：每批最多 8 条 `ReserveAward`，按 `stockPoolId` 排序，在一个 control 事务内完成库存更新/预约记录/outbox；提交后统一 ack。
- `p0.deliver`：每批最多 8 条 `ReservationConfirmed`，按 `userId, orderId` 排序，在一个 user 事务内完成加积分/订单更新；提交后统一 ack。排序是为了保持账户/订单锁顺序，避免同批多用户/多订单互相死锁。
- 批内任一条失败：整批回滚，降级为逐条重试；不允许“部分成功却全部 ack”。
- 批次大小是配置项，默认可以先用 1 保证正确性；正确性测试通过后开 8。批量越大，锁持有时间越长，必须用 8 而不是 64。
- 批量事务仍然每条事件做 inbox/receipt 校验；不能因为批量而跳过幂等。
- 如果批量导致死锁率上升，回退到单条；以 `DELIVERED/s` 和 P99 判断，不以“批更大”本身为目标。

### 5.7 入口背压

目标不是无限收单，而是让入口与交付能力对齐。

- 后台每 1 秒统计：`PENDING_RESERVATION` 数量、最老 `created_at` 年龄、outbox PENDING 数量，写入内存 gauge。
- 受理入口检查 gauge：
  - `pending > 20,000` 或 `oldestAge > 120s`：对新 draw 返回 429 + `Retry-After: 1`；query/result 不拒绝。
  - 恢复正常后自动放行，不需要人工。
- 单用户同时在途抽奖限制 1（单实例内存信号量；2,000 用户用 Caffeine/ConcurrentHashMap 维护），交付后释放。
- 单活动在途上限可配置（初始 5,000），防止单个活动打满库存行。
- 背压阈值是**保护线**，不是吞吐目标；调参时以“积压不持续增长”为准，不能为了入口 QPS 把阈值调到不触发。

### 5.8 观测

必须新增的指标：

| 指标 | 类型 | 标签 |
|---|---|---|
| `p0_admission_total` | counter | result=accepted/rejected/ratelimited |
| `p0_delivery_total` | counter | result=delivered/duplicate/retry/failed |
| `p0_event_total` | counter | type, result |
| `p0_stage_seconds` | timer | stage=reserve/deliver, result |
| `p0_batch_size` | histogram | stage |
| `p0_queue_ready` | gauge | queue |
| `p0_backlog_orders` | gauge | state |
| `p0_oldest_age_seconds` | gauge | state |
| `p0_inflight_orders` | gauge | — |
| `p0_db_commit_seconds` | timer | work |

日志：每条事件带 `eventId`、`orderId`、`userId`、`stage`、`attempt`；错误日志包含可重试/不可重试分类。

看板：入口 QPS vs `DELIVERED/s`、队列深度、积压年龄、Hikari wait、MySQL CPU/IO、应用 CPU/内存、GC。

## 6. 关键流程

### 6.1 正常抽奖

1. HTTP 受理：检查背压/用户在途；从缓存取策略与黑名单（1～3 秒 TTL，发布/拉黑主动失效；cache miss 才走短 control 读）；`tx.inUser` 锁账户、幂等检查、`engine.choose`、扣额度、插订单 `PENDING_RESERVATION`、写 outbox `ReserveAward`；提交；返回 202 + orderId。
2. `p0.reserve` 消费：inbox 去重；锁库存行；候选有库存则 `available-1, allocated+1`，否则用 fallback；写 `award_reservation(CONFIRMED)`、库存日志；写 outbox `ReservationConfirmed`；提交；ack。控制事务内不要复用两套 reserve/confirm 的两次 save；直接条件更新一次，写一次 reservation 快照和一次 stock log。
3. `p0.deliver` 消费：inbox 去重；锁账户；读订单（`FOR UPDATE`）；若已 `DELIVERED` 返回重复；加积分/写 receipt/交付记录/award；订单 CAS 到 `DELIVERED`；提交；ack。

### 6.2 重复/重试

- 重复 `ReserveAward`：inbox 返回 duplicate；若已有 responses，按原响应重放 `ReservationConfirmed`，不再扣库存。
- 重复 `ReservationConfirmed`：订单已 `DELIVERED`，直接返回；不重复加积分。
- 同一 `requestId` 的 HTTP 重试在背压/在途限制下仍返回原订单视图，不能被当成新的在途请求拒绝；只有不同 `requestId` 的新抽奖才受限。
- 消费失败：事务回滚，消息不 ack，进入 retry；复用现有 `RetryPolicy`/`retry_job` 的 12 次预算与指数退避，不新增无限重试；超过预算进 `retry_job DEAD` + `raw_message` quarantine（DLQ 等价物）/人工复核，不声明无人消费的 broker DLQ。
- 进程崩溃：订单/ outbox 已提交的会重发；未提交的 HTTP 请求失败，客户端按 requestId 重试，受理幂等。

### 6.3 库存不足/降级

- `candidate` 有库存：正常确认，`finalAward = candidate`，扣 stock。
- `candidate` 无库存：`finalAward = fallback = NONE`，不扣 stock，仍发 `ReservationConfirmed`；user 只把订单置 `DELIVERED`，不加积分。
- 因为 `fallback` 固定 `NONE`，正常流程不存在“无奖可发”；策略/库存结构不一致、库存池非 ACTIVE 属于技术异常，重试/DLQ/人工复核，不静默发奖、不自动释放已有库存。
- `NONE` 也走完 T3，保证订单有终态；结果查询能返回“谢谢参与”。

### 6.4 取消

不支持取消。

- 受理后的订单只走向 `DELIVERED`；没有 `CANCELLED` 状态，没有 `CancelReservation`/`ReservationCancelled` 事件。
- `/cancel` 端点移除，或明确返回 `405/409`；压测不包含取消请求。
- 库存确认后即使交付失败，也不自动释放库存；由人工复核处理，避免“已发奖但库存回滚”的不一致。
- 因此不需要 cancel-from-CONFIRMED 补偿，T2/T3 也没有取消竞态。

### 6.5 恢复/对账

- outbox relay 继续负责重发；本设计不改 relay 的 claim/publish/settle 结构。
- 新增低频扫描：`PENDING_RESERVATION` 超过 5 分钟且无 outbox PENDING 的订单，重新发 `ReserveAward`（同一 eventId 或新 eventId + 业务幂等）；仅作兜底，不进入热路径。
- 对账：订单状态、库存 `available+reserved+allocated`、积分流水、交付记录四方一致；任何差异进人工复核。

## 7. 代码改动面

> 具体方法签名和 DDL 由实施文档确定；这里只列必须动的模块，避免实施时漏边界。

| 模块 | 改动 |
|---|---|
| `DrawService` | 受理事务内完成选奖；直接插 `PENDING_RESERVATION`；写 `ReserveAward`；删除对 `PrepareDraw` 的依赖 |
| `CandidateWorker` | 选奖逻辑抽成可复用方法，供 `DrawService` 调用；旧 `PrepareDraw` handler 删除 |
| `DrawProgressService` | 删除 held/confirmed/cancel 分支；新增 `onReservationConfirmed` 的交付事务；`NONE` 只置 `DELIVERED` |
| `AwardDeliveryService` | 合并进 `DrawProgressService.onReservationConfirmed` 或抽成同一事务内方法；避免重复 `find`/重复锁 |
| `StockReservationService` | `reserve + confirm` 合并为一个 control 事务；候选无库存时落 `fallback=NONE`；删除 cancel/release |
| `StockCommandListener` | 只处理 `ReserveAward`；输出 `ReservationConfirmed`（candidate 或 NONE） |
| `EventRouting` | 删除旧类型与 cancel/reject 类型；只新增 `ReservationConfirmed` 路由 |
| `V2RaffleConfig` | 重新注册 `ReserveAward`/`ReservationConfirmed` handlers；删除 `PrepareDraw`/`ReservationHeld`/`ConfirmReservation`/`AwardDelivery`/cancel beans |
| `DrawRecoveryService` / `StockRecoveryJob` | 删除/重写旧六段状态恢复；只保留 `PENDING_RESERVATION` 兜底扫描和库存/额度对账；不改支付/签到 recovery |
| `MySqlAdmissionRepository` / `StrategyVersionCache` | 策略/黑名单 1～3 秒 TTL 缓存，发布/拉黑主动失效；cache miss 才走 control 读 |
| `StrategyPublicationService` | 发布校验：所有 `CREDIT` 奖必须有 `stockPoolId`；`fallback.type == NONE` 且 `stockPoolId == null`；不满足拒绝发布 |
| `V2RabbitConfig` | 新 exchange/queues/bindings；`p0.reserve`/`p0.deliver`/`p0.notify` 三个独立 listener 容器；并发/prefetch；删除旧 workflow 绑定 |
| `V2MessageListener` | 保留 inbox/outbox/retry/ack；按新事件类型注册 handler |
| `ConsumerUnitOfWork` | 保留；批量版本需要支持一批事件一个事务，或由独立 batch consumer 包装 |
| `MySqlOutboxWriter` | INSERT 影响行数判断，重复时才 SELECT 校验 hash；每事件少一次查询 |
| `MySqlAssetRepository` | 新链路没有 held/confirmed/cancel 阶段；只在受理、交付时锁账户；保持 account → activity → order 锁顺序 |
| `MySqlStockRepository` | reserve+confirm 合并；条件 UPDATE + 影响行数；库存日志同事务；可按 pool 排序批量 |
| `MySqlDrawRepository` | 状态 CAS 到 `PENDING_RESERVATION`/`DELIVERED`；删除旧中间态与取消状态；新增按状态/年龄扫描索引 |
| `V2BudgetProperties` | 新预算：TRADE 24 / WORKER 28 / QUERY 8 / RELAY 2 / RECOVERY 2；pool 72 |
| `V2WorkerConfig` | 保留 `validateConnections`；新增 `p0.reserve`/`p0.deliver`/`p0.notify` 三个消费容器的并发配置 |
| 观测 | 新增第 5.8 节指标；删除/保留旧 relay 指标按需 |

## 8. 重置与切换

不做兼容、不迁数据，切换按“重建环境”执行：

1. 停止应用、消费者、relay；确认无在途写入。
2. 删除旧 Rabbit exchange/queues（`bromark.v2.*`），删除旧 outbox/retry/consumer_inbox/receipt 数据。
3. 重建 3 个逻辑库 schema（新状态枚举、事件表、索引）；重建 2 个 user 库 × 4 分表。
4. 按 2,000 用户/10 活动/10 奖灌入种子；预置 2,000 笔已交付结果订单。
5. 启动新应用，确认新队列绑定、预算、连接池、背压 gauge。
6. 先跑小流量 smoke（1 VU、60 秒），再跑正式阶梯。

因为不是生产项目，不需要双写、回滚旧事件或数据迁移；但需要在文档/脚本里保留“如何从空库重建”的步骤，保证可复现。

## 9. 验收与简历取证

### 9.1 基线

- 用当前代码在**同一资源口径、同一 2,000/10/10 workload、同一 k6 脚本**上跑基线；资源口径优先 8C16G，若只有 Mac Docker 则用实际可用配额并显式标注；不能拿旧 8C4G/200 用户结果直接对比。
- 基线至少 3 轮，记录中位数、原始 k6、Prometheus、MySQL 状态、GC、Hikari、积压。
- 基线环境用当前代码 + 旧 schema/旧队列；优化环境用新 schema/新队列；两者都从空库重建，避免残留状态污染。

### 9.2 优化后

- 同一资源、workload、脚本、预热、阶段时长；至少 3 轮。
- 主曲线：开环到达率 20/40/80/120/160 draw/s 等价混合，每档 3～5 分钟；找积压不增长的最高档。
- 辅助曲线：闭环 5/100/300/600 VU 各 45～60 秒，和旧实验对照，但不作为主容量结论。
- 报告：入口 QPS、写受理 QPS、`DELIVERED/s`、P50/P95/P99、错误率、应用/MySQL CPU/内存、Hikari wait、队列深度、积压年龄。
- 时钟：HTTP P99 用 k6 自身计时；端到端交付延迟用同一时钟或在压测前后记录 client/server 偏差，偏差超过 10ms 时在报告中标注，不把跨时钟差值当精确值。

### 9.3 通过条件

- 3 个事务/2 个事件的结构在代码和指标上可验证（每单事务数、事件数）。
- 无超卖、无重复发奖、无丢单；四方对账差异 0。
- 在最高可持续档，积压斜率约 0；这是硬条件。
- 软护栏：同档 P99 不超过基线的 2 倍，技术错误率不超过基线 +0.5 个百分点；若为了吞吐突破软护栏，必须在报告中标注取舍，不能只报吞吐。
- 相对基线，`DELIVERED/s` 有可重复的明显提升；具体倍数以实测中位数为准，不在设计阶段承诺。
- 所有简历数字都能追溯到同一份原始证据；没有“入口 QPS 当完成吞吐”的表述。

### 9.4 简历可写/不可写

可写（实测后）：

- “单机 8C16G 全栈（应用 4C8G + MySQL 3C6G + RabbitMQ/Redis），2,000 用户/10 活动/10 奖，入口 QPS X、完成吞吐 Y/s、P99 Z、错误率 E。”
- “把单笔抽奖从 8 个 DB 事务/6 个事件压到 3 个事务/2 个事件，交付吞吐提升 N 倍（同资源同 workload，3 轮中位数）。”

不可写：

- 未实测的“支持 1.7k QPS”“提升 10 倍”。
- 只报入口 QPS，不报 `DELIVERED/s` 和积压。
- 把 8C4G/200 用户的旧数字套到 8C16G/2,000 用户口径。

## 10. 风险与开放项

| 风险 | 影响 | 应对 |
|---|---|---|
| 选奖并入 HTTP 受理，`engine.choose` 慢 | 受理 P99 上升 | 先测单次选奖耗时；>5ms 时改为小 CPU 池/缓存，保持一个事务 |
| 3C MySQL 仍是瓶颈 | 交付上不去 | 先看 COMMIT/行锁；下一步做 stock 分桶/账户原子更新（后续设计） |
| `fallback` 被配置成 `CREDIT`/有库存 | T2 在库存耗尽时无法落到 NONE | 发布策略校验 `fallback.type == NONE && stockPoolId == null`；不满足拒绝发布 |
| 批量事务死锁/长锁 | P99 抖动 | 默认 batch=1；通过测试后开 8；按 user/stockPoolId 排序；死锁回退单条 |
| 背压阈值不当 | 误拒或积压 | 初始 20,000/120s；按“积压斜率 0”调；只拒新 draw |
| 单实例内存限流 | 多实例失效 | 本期单实例；多实例改 Redis/DB 计数 |
| 连接预算与池校验冲突 | 启动失败 | 遵守 `total < pool max`；65 < 72；启动时校验 |
| CREDIT 奖未配置 `stockPoolId` | 积分绕过库存，破坏“积分也有库存” | 发布策略校验所有 `CREDIT` 奖必须有 `stockPoolId`；不满足拒绝发布 |
| 旧事件/旧数据残留 | 消费失败 | 切换时删除旧队列/旧 outbox；空库重建 |

**开放项（实施文档决定）：**

- 新状态枚举/表 DDL 的最终字段与索引名。
- `engine.choose` 是否可直接在 HTTP 线程执行，还是需要小 CPU 池。
- `p0.reserve`/`p0.deliver` 的最终并发（初始 8/16）与 batch 大小（初始 1，目标 8）。
- 背压阈值、单用户/单活动在途上限的最终值。
- 是否保留 `v2_raffle_stage`/`v2_award_record` 作为审计，还是合并到订单 JSON。
- 恢复扫描的频率与“无 outbox PENDING”判定 SQL。
- 看板/告警的具体指标名与阈值。

## 11. 决策记录

### 11.1 用户已决定

- 目标资源：**单机 8C16G 全栈**，应用 4C8G、MySQL 3C6G、RabbitMQ+Redis 1C2G；k6 优先外置。只有一台 Mac Docker 时按实际可用配额（通常 8C8G）测量并在简历写实际值，不混用 8C16G。
- Workload：**2,000 用户 / 10 活动 / 10 奖 / 7:1:1:1**。
- 目标：同资源下把吞吐做到当前最优，主指标为可持续 `DELIVERED/s`，用于简历。
- 奖品模型：10 个 `CREDIT` 奖都有独立 `stockPoolId`；只有 `NONE`（谢谢参与）无库存；`fallback` 固定 `NONE`。
- **不允许取消**：没有 `CANCELLED` 状态、没有取消事件、没有 cancel-from-CONFIRMED 补偿。
- **不兼容旧事件、不迁移旧数据、旧消息可删**。
- 设计范围：P0 交付链 + 支撑它的队列/DB 预算/批量/观测/背压；库存分桶、账户原子更新、分库拆实例放后续。

### 11.2 本设计做出的调用（builder 可调整，但需记录理由）

- 采用 **3 段事务**：user 受理 → control reserve+confirm → user confirmed+deliver。
- 选奖并入受理事务，删除 `PrepareDraw`。
- 删除 `ReservationHeld`/`ConfirmReservation`/`AwardDelivery`，只新增 `ReservationConfirmed`；`finalAward` 为 candidate 或 `NONE`。
- 两个 P0 阶段队列 `p0.reserve`/`p0.deliver` + 一个通知队列 `p0.notify`，不用一个 workflow 队列。
- 初始预算 65（TRADE 24/WORKER 28/QUERY 8/RELAY 2/RECOVERY 2/CALLBACK 1），pool 72。
- 批处理默认 1，目标 8；先保证正确性。
- 背压初始 20,000 单/120 秒，只拒新 draw。
- 不实现取消路径；`/cancel` 移除或返回 `405/409`。

## 12. 验收清单（实施完成后逐项勾）

- [ ] 每笔抽奖稳态 3 个 DB 事务、2 个事件、2 次跨库，可在日志/指标中核对。
- [ ] `PrepareDraw`/`ReservationHeld`/`ConfirmReservation`/`AwardDelivery` 不再产生。
- [ ] `p0.reserve`/`p0.deliver`/`p0.notify` 独立队列、独立并发、独立指标。
- [ ] 连接预算启动校验通过；Hikari 池 72；总许可 65（含 CALLBACK 1）。
- [ ] 无超卖、无重复积分、无丢单；四方对账 0 差异。
- [ ] 重复事件、进程崩溃、库存不足/`NONE` 降级均有测试。
- [ ] `/cancel` 不可用（405/409），状态机不存在 `CANCELLED`。
- [ ] 所有 `CREDIT` 奖有 `stockPoolId`；`fallback.type == NONE` 且无库存。
- [ ] 背压触发/恢复有测试；只拒新 draw。
- [ ] 8C16G 全栈 3 轮基线/优化对比完成；原始证据归档。
- [ ] 简历数字来自实测中位数，标注资源、workload、脚本版本。
- [ ] 文档中的“目标”与“实测”分开，未把目标写成结果。
