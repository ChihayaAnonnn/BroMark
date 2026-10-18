# P0 异步交付链路优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把每笔抽奖从 8 个 DB 事务 / 6 个事件压到 3 个事务 / 2 个事件，并在 8C16G 全栈、2,000 用户 / 10 活动 / 10 奖的 workload 下提高可持续 `DELIVERED/s`。

**Architecture:** 受理事务完成选奖、扣额度、落 `PENDING_RESERVATION` 和 `ReserveAward` outbox；control 消费一次完成库存 reserve+confirm，发 `ReservationConfirmed`；user 消费一次完成加积分/`DELIVERED`。积分奖一律有库存，只有 `NONE`（谢谢参与）无库存且是固定 fallback；不支持取消。

**Tech Stack:** Java 25、Spring Boot、MySQL 8（InnoDB、`fsync=1`、`sync_binlog=1`）、RabbitMQ、JUnit 4 + AssertJ、Maven、k6。

**Spec:** `docs/superpowers/specs/2026-10-18-p0-delivery-optimization-design.md`

## Global Constraints

- 目标环境：单机 8C16G 全栈；应用 4C8G（`-Xmx4g`）、MySQL 3C6G（buffer pool 3G）、RabbitMQ+Redis 1C2G；k6 优先外置。
- Workload：2,000 用户、10 活动、10 奖、query/result/exchange/draw = 7:1:1:1；奖品均匀命中。
- 所有 `CREDIT` 奖必须有独立 `stockPoolId`；只有 `NONE` 无库存；`fallback` 固定 `NONE`；发布策略时校验。
- 不允许取消：没有 `CANCELLED` 状态、没有取消事件、没有 cancel-from-CONFIRMED 补偿；`/cancel` 返回 405/409。
- 主链路 3 个 DB 事务、2 个事件（`ReserveAward`、`ReservationConfirmed`）、2 次跨库。
- 不做旧事件兼容、不迁移旧数据；切换时删除旧队列/旧 outbox/旧状态，空库重建。
- 不关闭 `innodb_flush_log_at_trx_commit`、`sync_binlog`；不改变分库路由（3 个逻辑库、2 个用户库 × 4 张分表）。
- 连接池 72/库；总许可 65（TRADE 24 / WORKER 28 / QUERY 8 / RELAY 2 / RECOVERY 2 / CALLBACK 1）；`physical-connection-limits=240`。
- 本计划以 MySQL 为 P0 运行目标；PostgreSQL profile 只需保持编译/现有测试不回归，不纳入 P0 验收。
- 每个任务结束必须提交；提交信息用 `feat(p0): ...` / `test(p0): ...` / `docs(p0): ...`。

## Review Focus

1. **库存耗尽**：candidate 无库存时必须落 `NONE`，订单仍 `DELIVERED`，不能出现“无奖可发”或扣了库存却不发奖。
2. **重复消息**：重复 `ReserveAward` 不能二次扣库存；重复 `ReservationConfirmed` 不能二次加积分。
3. **批量失败**：一批事件里任一条失败必须整批回滚、不 ack，不能部分成功却全部确认。
4. **背压/幂等竞态**：同一 `requestId` 重试必须返回原订单；不同新请求才受在途/积压限制。
5. **旧事件残留**：旧 `PrepareDraw`/`ReservationHeld`/`ConfirmReservation`/`AwardDelivery` 必须进入 DLQ/隔离，不能被静默 ack 成成功。

---

## File Structure

| 文件 | 责任 |
|---|---|
| `bm-domain/src/main/java/com/jay/domain/task/model/EventEnvelope.java` | 允许 schemaVersion 2 |
| `bm-domain/src/main/java/com/jay/domain/task/service/EventRouting.java` | 只路由 `ReserveAward`/`ReservationConfirmed` + 通知 |
| `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java` | 受理事务：选奖、额度、`PENDING_RESERVATION`、`ReserveAward` |
| `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawDeliveryService.java` | 交付事务：加积分/`NONE`、`DELIVERED` |
| `bm-domain/src/main/java/com/jay/domain/raffle/service/P0AdmissionGate.java` | 背压接口：快照判断 + 单用户在途 |
| `bm-domain/src/main/java/com/jay/domain/stock/service/StockReservationService.java` | `confirmOnReserve`：reserve+confirm 一次事务 |
| `bm-domain/src/main/java/com/jay/domain/strategy/service/StrategyPublicationService.java` | 发布校验：CREDIT 必须有 stockPoolId；fallback 必须 NONE 且无库存 |
| `bm-domain/src/main/java/com/jay/domain/stock/repository/StockRepository.java` | 新增 `commit(pool, order)` |
| `bm-infrastructure/src/main/java/com/jay/infrastructure/cache/P0AdmissionCache.java` | 策略/黑名单 1～3 秒 TTL 缓存 |
| `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlStockRepository.java` | 条件 UPDATE `available-1, allocated+1` |
| `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlOutboxWriter.java` | affected rows 判断，重复才 SELECT |
| `bm-trigger/src/main/java/com/jay/trigger/listener/v2/StockCommandListener.java` | 只处理 `ReserveAward`，输出 `ReservationConfirmed` |
| `bm-trigger/src/main/java/com/jay/trigger/listener/v2/P0BatchConsumer.java` | 批量消费，一批一事务、提交后统一 ack |
| `bm-domain/src/main/java/com/jay/domain/raffle/service/P0RecoveryService.java` | 重发 `PENDING_RESERVATION` 的 `ReserveAward` |
| `bm-trigger/src/main/java/com/jay/trigger/job/v2/P0RecoveryJob.java` | 扫描 `PENDING_RESERVATION` 兜底重发 |
| `bm-app/src/main/java/com/jay/config/V2RabbitConfig.java` | `p0.reserve`/`p0.deliver`/`p0.notify` 拓扑与监听器 |
| `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java` | 新 handler 装配，删除旧六段 beans |
| `bm-app/src/main/java/com/jay/config/V2BudgetProperties.java` | 新预算默认值 |
| `bm-app/src/main/resources/application-v2-p0.yml` | pool 72、预算 65、队列前缀 |
| `docs/dev-ops/mysql/migrations/004_user_draw.sql` | `reserved_at` + 积压索引 |
| `bm-app/src/test/java/com/jay/test/v2/P0*.java` | 新链路单元/集成测试 |
| `docs/performance/p0-load.js` | 可移植 k6 脚本 |
| `docs/performance/p0_seed.py` | 2,000/10/10 种子 |
| `docs/performance/p0-runbook.md` | 两 Mac + M5 8C16G 执行步骤 |

**删除/替换**：`DrawProgressService`、`AwardDeliveryService`、`CandidateWorker`、`DrawRecoveryService`、`StockRecoveryJob`，以及只验证六段/取消的测试（`DrawCancellationV2Test`、`RaffleDeliveryV2Test`、`GuardedWorkflowRecoveryV2Test` 等）。删除前先 `grep` 所有引用，避免编译残留。

---

## Task 1: Event v2 契约与 P0 schema

**Files:**
- Modify: `bm-domain/src/main/java/com/jay/domain/task/model/EventEnvelope.java`
- Modify: `docs/dev-ops/mysql/migrations/004_user_draw.sql`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0EventContractV2Test.java`

**Interfaces:**
- Consumes: 现有 `EventEnvelope`、`ReservationResult`、`AwardSnapshot`。
- Produces: `EventEnvelope` 接受 `schemaVersion == 1 || == 2`；`v2_draw_order` 新增 `reserved_at` 与 `draw_backlog(state,last_progress_at,order_id)`。

- [ ] **Step 1: 写失败测试**

```java
@Test public void acceptsSchemaTwoAndRejectsUnknown() {
  assertThat(new EventEnvelope("e1","ReserveAward","o1",
      new BusinessId("DRAW","o1","a".repeat(64)),"u1",RouteSlot.control(),
      "a".repeat(64),1,2,"{}")).isNotNull();
  assertThatThrownBy(() -> new EventEnvelope("e2","ReserveAward","o1",
      new BusinessId("DRAW","o1","a".repeat(64)),"u1",RouteSlot.control(),
      "a".repeat(64),1,3,"{}")).isInstanceOf(IllegalArgumentException.class);
}

@Test public void reservationReceiptRoundTrips() {
  var r = new ReservationResult("o1","u1","a".repeat(64),"b".repeat(64),
      "CONFIRMED", new AwardSnapshot(0,"NONE",null,null,0,"c".repeat(64)), 0);
  assertThat(ReceiptJson.read(ReceiptJson.write(r), ReservationResult.class)).isEqualTo(r);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0EventContractV2Test -DfailIfNoTests=false test`
Expected: FAIL，`EventEnvelope` 对 schemaVersion=2 抛异常。

- [ ] **Step 3: 实现**

- `EventEnvelope` 校验改为 `schemaVersion != 1 && schemaVersion != 2` 时抛异常。
- `004_user_draw.sql` 的 `v2_draw_order` 在 `confirmed_at` 后加 `reserved_at datetime(6)`，在 `draw_user` 后加 `INDEX draw_backlog(state,last_progress_at,order_id)`。
- 不新增 008 migration；空库重建使用 004。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-domain/src/main/java/com/jay/domain/task/model/EventEnvelope.java docs/dev-ops/mysql/migrations/004_user_draw.sql bm-app/src/test/java/com/jay/test/v2/P0EventContractV2Test.java
git commit -m "feat(p0): allow event schema v2 and add backlog index"
```

---

## Task 2: 受理事务与 admission 缓存

**Files:**
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/cache/P0AdmissionCache.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlOutboxWriter.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java:60-90`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0AdmissionV2Test.java`

**Interfaces:**
- `P0AdmissionCache(UserTransactionRunner tx, StrategyVersionRepository repo, Duration ttl)`
- `StrategyVersionRepository.DrawAdmission admit(String user, long activity, Deadline deadline)`
- `DrawService(tx, assets, quota, accounts, repo, codec, ids, out, faults, observation, admission, engine)`
- `DrawService.create(DrawCommand) -> DrawView`
- 产出：订单状态 `PENDING_RESERVATION`；outbox 1 条 `ReserveAward`，schemaVersion=2，payload=`ReserveCommand`。

- [ ] **Step 1: 写失败测试**

```java
@Test public void admissionPersistsCandidateAndReserveAwardWithoutPrepareDraw() {
  AssetTestSupport.run(h -> {
    DrawAcceptanceV2Test.schema(h);
    long a = DrawAcceptanceV2Test.publish(h, "pool-" + UUID.randomUUID());
    String u = "p0-" + UUID.randomUUID();
    DrawAcceptanceV2Test.grant(h, u, a);
    var svc = DrawAcceptanceV2Test.service(h);
    var view = svc.create(new DrawCommand(u, "req-" + UUID.randomUUID(), a,
        new Deadline(Instant.now().plusSeconds(60))));
    var r = new MySqlDrawRepository(h.j.getDataSource())
        .find(view.orderId(), false);
    assertThat(r.state()).isEqualTo("PENDING_RESERVATION");
    assertThat(r.candidate()).isNotNull();
    assertThat(r.fallback().type()).isEqualTo("NONE");
    assertThat(h.j.queryForObject(
        "select count(*) from outbox where aggregate_id=? and event_type='ReserveAward'",
        Integer.class, view.orderId())).isOne();
    assertThat(h.j.queryForObject(
        "select count(*) from outbox where aggregate_id=? and event_type='PrepareDraw'",
        Integer.class, view.orderId())).isZero();
  });
}

@Test public void sameRequestReturnsSameOrderAndOneOutboxRow() { /* 两次 create，断言 orderId 相同、outbox 1 */ }

@Test public void admissionCacheAvoidsRepeatedConfigRead() { /* 同 user+activity 两次 admit，repo 只被调用一次 */ }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0AdmissionV2Test -DfailIfNoTests=false test`
Expected: FAIL，旧链路生成 `PrepareDraw`，状态为 `ACCEPTED`/`CANDIDATE_FIXED`。

- [ ] **Step 3: 实现**

- `P0AdmissionCache`：Guava `LoadingCache<String, DrawAdmission>`，key=`user + ":" + activity`，`expireAfterWrite(ttl)`；miss 时 `tx.inControl(() -> repo.admit(user, activity, deadline))`；`invalidate(user, activity)` 供发布/拉黑调用。
- `DrawService`：
  - 删除 `tx.inControl(strategies.admit(...))` 块；改为 `var admitted = admission.admit(...)`。
  - 在 `tx.inUser` 内、`repo.insert` 前调用 `engine.choose(f, input, new Random())` 和 `engine.fallback(f, new Random())`。
  - `new DrawRecord(..., "PENDING_RESERVATION", ..., candidate, fallback, null)`。
  - `out.append(event(r, "ReserveAward", RouteSlot.control(), eventId, ReceiptJson.write(new ReserveCommand(...))))`。
  - `DrawService` 不再依赖 `strategies`；`cancel` 方法暂时保留到 Task 6 删除，避免 `DrawController` 编译中断。
- `V2RaffleConfig` 新增 `@Bean P0AdmissionCache p0AdmissionCache(UserTransactionRunner tx, MySqlStrategyVersionRepository repo)`（TTL 2 秒）；`drawService` 注入 `P0AdmissionCache` 与 `FrozenRaffleEngine`；更新构造调用。
- 更新全部 `new DrawService` 调用点（`V2RaffleConfig`、`DrawAcceptanceV2Test.service`、`RaffleFaultV2Test`）；`grep -rn "new DrawService"` 确认无遗漏。
- `MySqlOutboxWriter.append`：`INSERT ... ON DUPLICATE KEY UPDATE` 用影响行数判断；新插入直接返回，重复时才 `SELECT payload_hash` 校验；测试覆盖新插入、重复同 hash、重复不同 hash 抛 `AssetConflictException`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-infrastructure/src/main/java/com/jay/infrastructure/cache/P0AdmissionCache.java bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java bm-app/src/main/java/com/jay/config/V2RaffleConfig.java bm-app/src/test/java/com/jay/test/v2/P0AdmissionV2Test.java
git commit -m "feat(p0): persist candidate and ReserveAward in admission"
```

---

## Task 3: control 侧 reserve+confirm 一次事务

**Files:**
- Modify: `bm-domain/src/main/java/com/jay/domain/stock/repository/StockRepository.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/stock/service/StockReservationService.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/strategy/service/StrategyPublicationService.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/cache/P0AdmissionCache.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlStockRepository.java`
- Modify: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/StockCommandListener.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0ReservationV2Test.java`

**Interfaces:**
- `StockRepository.commit(String pool, String order) -> boolean`
- `StockReservationService.confirmOnReserve(ReserveCommand c) -> ReservationResult`
- `StockCommandListener.apply(EventEnvelope e) -> List<EventEnvelope>`，只接受 `ReserveAward`
- `P0AdmissionCache.invalidateActivity(long activity)`：发布后清掉该活动所有 user:activity 缓存项。
- 复用 `StrategyPublicationService` 已有的 `Consumer<FrozenStrategy> warm` 钩子：`V2RaffleConfig.strategyPublicationService` 传入 `f -> { admissions.invalidateActivity(f.activityId()); cache.warm(f); }`，不新增构造器。
- 产出：`ReservationResult(state="CONFIRMED")`；`ReservationConfirmed` 事件 schema 2。

- [ ] **Step 1: 写失败测试**

```java
@Test public void commitMovesAvailableToAllocatedAndFallsBackToNone() {
  AssetTestSupport.run(h -> {
    FrozenStrategyV2Test.schema(h);
    var svc = StockProtocolV2Test.service(h);
    String pool = "p0-" + UUID.randomUUID();
    StockProtocolV2Test.control(h, () -> { svc.createPool(pool, "init", 1, "p0"); return null; });
    var engine = new FrozenRaffleEngine();
    // 构造带 CREDIT(candidate) + NONE(fallback) 的 ReserveCommand，调用 confirmOnReserve
    var first = svc.confirmOnReserve(cmd(h, pool, engine));
    assertThat(first.state()).isEqualTo("CONFIRMED");
    assertThat(first.finalSnapshot().type()).isEqualTo("CREDIT");
    assertThat(stock(h, pool)).containsEntry("available", 0L).containsEntry("allocated", 1L)
        .containsEntry("reserved", 0L);
    var second = svc.confirmOnReserve(cmd(h, pool, engine));
    assertThat(second.finalSnapshot().type()).isEqualTo("NONE");
    assertThat(stock(h, pool)).containsEntry("available", 0L).containsEntry("allocated", 1L);
  });
}

@Test public void duplicateConfirmOnReserveDoesNotAllocateTwice() { /* 同一命令两次，库存不变 */ }

@Test public void publicationRejectsInvalidStockAndFallback() {
  // fallback=CREDIT 或 CREDIT 无 stockPoolId -> StrategyPublicationService.publish 抛异常
}

@Test public void reserveAwardEmitsReservationConfirmedSchemaTwo() {
  var events = new StockCommandListener(svc, h.codec).apply(reserveEvent);
  assertThat(events).hasSize(1);
  assertThat(events.getFirst().eventType()).isEqualTo("ReservationConfirmed");
  assertThat(events.getFirst().schemaVersion()).isEqualTo(2);
}
```

测试文件内定义 `cmd(h,pool,engine)` 与 `stock(h,pool)` 两个 private helper；`stock` 直接查询 `award_stock` 的 available/reserved/allocated。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0ReservationV2Test -DfailIfNoTests=false test`
Expected: FAIL，`commit`/`confirmOnReserve` 不存在。

- [ ] **Step 3: 实现**

- `StockRepository.commit`；`MySqlStockRepository`：
  ```sql
  update award_stock set available=available-1,allocated=allocated+1,version=version+1
  where stock_pool_id=? and recovery_status='ACTIVE' and available>0
  ```
  影响行数为 1 才写 `stock_change_log` 并返回 true；0 返回 false。
- `StockReservationService.confirmOnReserve`：
  - 复用 `lockOrCreate`/`validate`；已 `CONFIRMED` 直接返回。
  - `candidate.quantity()==1` 时 `repo.commit(candidate.stockPoolId(), order)`；失败则 `final=fallback`。
  - 若 `final.type()!="NONE"` 或 `final.stockPoolId()!=null` 抛 `AssetConflictException`（发布校验兜底）。
  - `repo.save(new ReservationResult(..., "CONFIRMED", final, final.quantity()))`。
- `StockCommandListener`：只保留 `ReserveAward` 分支；输出 `ReservationConfirmed`，payload=`ReceiptJson.write(result)`，`schemaVersion=2`；删除 confirm/cancel 分支。
- `StrategyPublicationService.validate`：所有 `CREDIT` 奖 `stockPoolId != null`；`fallback.type == NONE && fallback.stockPoolId == null`；不满足拒绝发布。
- `P0AdmissionCache.invalidateActivity(long)` 用 `asMap().keySet().removeIf(k -> k.endsWith(":" + activity))`；`V2RaffleConfig` 的 publication warm 钩子先 invalidate 再 warm。黑名单没有 writer，接受 2 秒 TTL 延迟（recorded ruling）。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-domain/src/main/java/com/jay/domain/stock bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlStockRepository.java bm-trigger/src/main/java/com/jay/trigger/listener/v2/StockCommandListener.java bm-app/src/test/java/com/jay/test/v2/P0ReservationV2Test.java
git commit -m "feat(p0): confirm stock reservation in one control transaction"
```

---

## Task 4: user 侧交付事务

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawDeliveryService.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java:88-150`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0DeliveryV2Test.java`

**Interfaces:**
- `DrawDeliveryService(DrawRepository repo, AssetKernel assets)`
- `void deliver(EventEnvelope e)`
- 产出：订单 `DELIVERED`；`CREDIT` 加积分；`NONE` 不加积分；重复事件不二次加积分。

- [ ] **Step 1: 写失败测试**

```java
@Test public void confirmedCreditDeliversOnceAndNoneDoesNotCredit() {
  // 构造 PENDING_RESERVATION 订单 + ReservationConfirmed(CREDIT) 事件
  var view = svc.create(new DrawCommand(u, "req-" + UUID.randomUUID(), a,
      new Deadline(Instant.now().plusSeconds(60))));
  String o = view.orderId();
  delivery.deliver(eventFor(o));
  assertThat(h.balance(u)).isEqualByComparingTo("10.00");
  assertThat(new MySqlDrawRepository(h.j.getDataSource()).find(o, false).state()).isEqualTo("DELIVERED");
  delivery.deliver(eventFor(o)); // 重复
  assertThat(h.balance(u)).isEqualByComparingTo("10.00");

  // 构造 NONE 订单，deliver 后 DELIVERED 且余额不变
}

@Test public void finalAwardMismatchLeavesOrderPending() { /* 篡改 finalAward，断言 AssetConflictException 且状态 PENDING */ }
```

测试文件内定义 `svc`、`delivery`、`eventFor(orderId)` 三个 helper；`h.balance(u)` 来自 `AssetTestSupport`。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0DeliveryV2Test -DfailIfNoTests=false test`
Expected: FAIL，`DrawDeliveryService` 不存在。

- [ ] **Step 3: 实现**

- `deliver`：解析 `ReservationResult`；校验 `state=CONFIRMED`、order/user/hash；
- 读取订单（先非锁获取 user/activity），`assets.lockAccounts`，再 `repo.find(order,true)`；
- 校验 `finalAward` 等于 candidate 或 fallback，`configHash` 一致；
- `DELIVERED` 直接返回；非 `PENDING_RESERVATION` 抛冲突；
- `CREDIT` 调 `assets.credit`，`NONE` 跳过；`repo.delivered(r)` + `repo.update(r, r.transition("DELIVERED", ...))`。
- `V2RaffleConfig` 注册 `ReservationConfirmed` handler 指向 `deliver`；删除 `AwardDeliveryService` bean。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-domain/src/main/java/com/jay/domain/raffle/service/DrawDeliveryService.java bm-app/src/main/java/com/jay/config/V2RaffleConfig.java bm-app/src/test/java/com/jay/test/v2/P0DeliveryV2Test.java
git commit -m "feat(p0): deliver confirmed awards in one user transaction"
```

---

## Task 5: 事件路由与 P0 队列拓扑

**Files:**
- Modify: `bm-domain/src/main/java/com/jay/domain/task/service/EventRouting.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2RabbitConfig.java:100-560`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0RoutingV2Test.java`

**Interfaces:**
- `EventRouting.CONTROL = Set.of("ReserveAward", "CreditGranted")`（保留支付控制事件）
- `EventRouting.USER = Set.of("ReservationConfirmed", "GrantCredit")`（保留支付用户事件）
- `EventRouting.NOTIFICATIONS = Set.of("ExchangeCompleted","SignInCompleted")`
- `EventRouting.route` 接受 `schemaVersion == 1 || == 2`，拒绝 3/0/-1；删除现有 `schemaVersion != 1` 的硬门槛。
- 队列：`p0.reserve`（并发 8）、`p0.deliver`（16）、`p0.notify`（4）；exchange `p0.events`。
- `V2MessageListener` handlers：`ReserveAward`、`ReservationConfirmed`、通知。

- [ ] **Step 1: 写失败测试**

```java
@Test public void routesOnlyP0Events() {
  // 测试内定义 reserveAward()/reservationConfirmed()/prepareDraw() 三个 EventEnvelope helper
  var routing = new EventRouting(new RouteCodec(2,4));
  assertThat(routing.route(reserveAward())).isEqualTo(RouteSlot.control()); // schema 2
  assertThat(routing.route(reservationConfirmed())).isEqualTo(new RouteSlot("01","000")); // schema 2
  assertThat(routing.route(notificationSchemaOne())).isNotNull(); // old schema 1 still routed
  assertThatThrownBy(() -> routing.route(prepareDraw()))
      .isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(() -> routing.route(withSchema(reserveAward(), 3)))
      .isInstanceOf(IllegalArgumentException.class);
}

@Test public void declaresReserveDeliverNotifyQueues() {
  // 读取 V2RabbitConfig 的 Declarables，断言队列名/绑定
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0RoutingV2Test -DfailIfNoTests=false test`
Expected: FAIL，旧路由仍接受六段类型。

- [ ] **Step 3: 实现**

- `EventRouting` 只保留新集合；未知类型抛 `IllegalArgumentException`。
- 删除 `EventRouting.route` 中的 `if (e.schemaVersion() != 1) throw ...`，改为 `schemaVersion == 1 || schemaVersion == 2`；否则 Task 1 构造的 v2 事件无法路由。
- `V2RabbitConfig`：
  - 新 exchange `p0.events`、队列 `p0.reserve`/`p0.deliver`/`p0.notify`，routing key=eventType。不声明无人消费的 `p0.dlq`；DLQ 等价物是 `retry_job DEAD` + `raw_message` quarantine。
  - `p0.reserve` 绑定 `ReserveAward` + `CreditGranted`；`p0.deliver` 绑定 `ReservationConfirmed` + `GrantCredit`；支付事件继续可路由，语义不变。
  - `@RabbitListener(id="p0-reserve", queues="${...}.reserve", concurrency="8")` 等；`p0.deliver` 16、`p0.notify` 4。
  - 从 `v2MessageContainerFactory` 移除 `setConcurrentConsumers`/`setMaxConcurrentConsumers`，并发由 listener 注解控制，避免属性覆盖。
  - `v2MessageListener` 的 handlers map 注册 `ReserveAward`/`ReservationConfirmed` + 支付 `GrantCredit`/`CreditGranted` + 通知；不注册旧六段 draw 类型。
  - 删除旧 `workflow`/`notifications` 队列与监听方法。
- 保留 `V2MessageListener` 的 inbox/outbox/retry/ack/DLQ 语义。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-domain/src/main/java/com/jay/domain/task/service/EventRouting.java bm-app/src/main/java/com/jay/config/V2RabbitConfig.java bm-app/src/test/java/com/jay/test/v2/P0RoutingV2Test.java
git commit -m "feat(p0): add reserve/deliver/notify queue topology"
```

---

## Task 6: 装配新 handler，删除取消与旧六段

**Files:**
- Modify: `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java`
- Modify: `bm-trigger/src/main/java/com/jay/trigger/http/v2/DrawController.java:90-105`
- 本期只移除旧 beans/handler 与 cancel 入口；`DrawProgressService`/`AwardDeliveryService`/`CandidateWorker` 的类删除放到 Task 10，避免旧 recovery 仍引用它们导致编译中断。
- Test: `bm-app/src/test/java/com/jay/test/v2/P0WiringV2Test.java`

**Interfaces:**
- `@Bean(name="ReserveAward") EventHandler reserve(StockCommandListener listener)`
- `@Bean(name="ReservationConfirmed") EventHandler confirmed(DrawDeliveryService delivery)`
- `POST /api/v2/draws/{order}/cancel` 返回 405；无 `CANCELLED` 状态。

- [ ] **Step 1: 写失败测试**

```java
@Test public void cancelEndpointIsGone() {
  // MockMvc POST /api/v2/draws/o1/cancel -> 405
}

@Test public void contextHasOnlyP0Handlers() {
  // ApplicationContextRunner / Spring context:
  // handlers 含 ReserveAward/ReservationConfirmed，不含 PrepareDraw/ReservationHeld/...
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0WiringV2Test -DfailIfNoTests=false test`
Expected: FAIL，旧 handler 仍存在 / cancel 仍可用。

- [ ] **Step 3: 实现**

- `V2RaffleConfig` 删除 `prepare`/`held`/`confirmed(old)`/`cancelled`/`deliver(old)` 这些 event handler beans；新增两个 handler。`drawProgressService`/`awardDeliveryService`/`candidateWorker` 的 bean 与旧 recovery bean 留到 Task 10 一起删除，避免上下文缺依赖。
- `DrawController` 删除 cancel 方法或返回 `ResponseEntity.status(405).build()`；删除 `service.cancel` 调用。
- **不要删除 `DrawService.cancel` 方法或 `DrawProgressService`/`AwardDeliveryService`/`CandidateWorker` 类**：`DrawCancellationV2Test` 等旧测试仍引用它们，删除会编译失败。Task 10 删除旧 recovery 时一并删除类和测试。
- 不删除只验证六段/取消的测试；Task 10/11 统一替换。保留支付/签到/兑换测试。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat(p0): wire reserve/confirm handlers and remove cancellation"
```

---

## Task 7: 连接预算与 P0 profile

**Files:**
- Modify: `bm-app/src/main/java/com/jay/config/V2BudgetProperties.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2WorkerConfig.java`
- Create: `bm-app/src/main/resources/application-v2-p0.yml`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0BudgetV2Test.java`

**Interfaces:**
- 代码默认预算保持旧值（7 个许可），避免 `application-v2-it.yml` 的 pool 8 启动失败。
- P0 profile `application-v2-p0.yml` 覆盖：`TRADE=24, WORKER=28, QUERY=8, RELAY=2, RECOVERY=2, CALLBACK=1`；`physicalConnectionLimits.mysql-it=240`；`reservedConnections=20`；`prefetch=8`。
- P0 profile：3 个 Hikari pool `maximum-pool-size=72`，`minimum-idle=0`，`connection-timeout=1000`；`max_connections=300`。

- [ ] **Step 1: 写失败测试**

```java
@Test public void p0ProfileDeclaresPoolAndBudget() throws Exception {
  var yaml = new String(getClass().getClassLoader()
      .getResourceAsStream("application-v2-p0.yml").readAllBytes());
  assertThat(yaml).contains("maximum-pool-size: 72");
  assertThat(yaml).contains("TRADE: 24").contains("WORKER: 28")
      .contains("QUERY: 8").contains("RELAY: 2").contains("RECOVERY: 2")
      .contains("CALLBACK: 1");
  assertThat(yaml).contains("physical-connection-limits").contains("240");
  assertThat(yaml).contains("prefix: p0");
}

@Test public void acceptsP0PoolAndBudget() {
  var p = new V2BudgetProperties();
  p.getInstances().get("mysql-it").put(WorkClass.TRADE, 24);
  p.getInstances().get("mysql-it").put(WorkClass.WORKER, 28);
  p.getInstances().get("mysql-it").put(WorkClass.QUERY, 8);
  p.getInstances().get("mysql-it").put(WorkClass.RELAY, 2);
  p.getInstances().get("mysql-it").put(WorkClass.RECOVERY, 2);
  p.setPhysicalConnectionLimits(Map.of("mysql-it", 240));
  p.validateConnections(Map.of("00",72,"01",72,"02",72)); // 不抛
}

@Test public void rejectsBudgetNotBelowPool() {
  var p = new V2BudgetProperties();
  p.getInstances().get("mysql-it").put(WorkClass.TRADE, 72);
  assertThatThrownBy(() -> p.validateConnections(Map.of("00",72,"01",72,"02",72)))
      .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0BudgetV2Test -DfailIfNoTests=false test`
Expected: FAIL，`application-v2-p0.yml` 不存在或缺少 P0 值。

- [ ] **Step 3: 实现**

- **不要改 `V2BudgetProperties` 的代码默认值**：现有 `application-v2-it.yml` 是 pool 8，默认预算 65 会让 `validateConnections` 启动失败。默认值保持旧值，P0 的 65/240 只在 `application-v2-p0.yml` 覆盖。
- `application-v2-p0.yml` 复用 `application-v2-it.yml` 的 URL/凭证占位，pool 改 72，加入 `bm.v2.budget.*`（`instances.mysql-it.TRADE=24`、`WORKER=28`、`QUERY=8`、`RELAY=2`、`RECOVERY=2`；`physical-connection-limits.mysql-it=240`、`reserved-connections=20`、`prefetch=8`）、`bm.v2.messaging.prefix=p0`、`relay.mode=bounded`。
- 确保 `V2WorkerConfig.validateConnections` 在 72 池、65 许可、240 限制下通过。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-app/src/main/java/com/jay/config/V2BudgetProperties.java bm-app/src/main/java/com/jay/config/V2WorkerConfig.java bm-app/src/main/resources/application-v2-p0.yml bm-app/src/test/java/com/jay/test/v2/P0BudgetV2Test.java
git commit -m "feat(p0): configure pool 72 and 65-permit budget"
```

---

## Task 8: 批量消费（默认 1，目标 8）

**Files:**
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/P0BatchConsumer.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/task/service/ConsumerUnitOfWork.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2RabbitConfig.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0BatchConsumerV2Test.java`

**Interfaces:**
- `ConsumerUnitOfWork.applyAttemptInTransaction(String consumer, EventEnvelope e, int generation, RetryJobRepository retries, Supplier<List<EventEnvelope>> handler) -> ConsumeOutcome`（调用方已开启事务）
- `P0BatchConsumer(int batchSize, ConsumerUnitOfWork work, V2MessageListener listener); void onMessages(List<Message> messages, Channel channel)`
- 一批一事务；全部成功才 ack 最后一条；任一失败整批回滚、全部 nack/requeue。

- [ ] **Step 1: 写失败测试**

```java
@Test public void batchCommitsOnceAndAcksAll() { /* 3 条事件，1 次 commit，3 次 ack */ }

@Test public void batchFailureRollsBackAndDoesNotAck() {
  // 第 2 条抛异常 -> 第 1 条也不可见，ack 次数 0
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0BatchConsumerV2Test -DfailIfNoTests=false test`
Expected: FAIL，无批量 consumer。

- [ ] **Step 3: 实现**

- 把 `ConsumerUnitOfWork.applyGuarded` 拆成“事务外开启”和“事务内执行”两段；`applyAttempt` 保持原语义，新增 `applyAttemptInTransaction`。
- `P0BatchConsumer` 用 `tx.inSlot` 包住一批事件，逐条 `applyAttemptInTransaction`；全部成功后再 ack；异常时抛出让外层 nack。
- `V2RabbitConfig` 的 `p0.reserve`/`p0.deliver` 容器开启 batch（`setBatchSize`/`setConsumerBatchEnabled`），默认 `bm.v2.p0.batch-size=1`，最大 8；`p0.notify` 不批量。
- 批量仍逐条做 inbox/receipt；不能跳过幂等。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-trigger/src/main/java/com/jay/trigger/listener/v2/P0BatchConsumer.java bm-domain/src/main/java/com/jay/domain/task/service/ConsumerUnitOfWork.java bm-app/src/main/java/com/jay/config/V2RabbitConfig.java bm-app/src/test/java/com/jay/test/v2/P0BatchConsumerV2Test.java
git commit -m "feat(p0): add optional batch consumer with all-or-nothing ack"
```

---

## Task 9: 背压与 P0 指标

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/P0AdmissionGate.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/observability/P0BacklogProbe.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2MetricsConfig.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0BackpressureV2Test.java`

**Interfaces:**
- `record P0BacklogSnapshot(long pending, long oldestAgeSeconds)`
- `P0AdmissionGate.update(P0BacklogSnapshot)`；`boolean tryEnter(String user, long activity)`；`void leave(String user)`
- `P0BacklogProbe.snapshot() -> P0BacklogSnapshot`（每 1 秒刷新，失败保留上一次）
- 指标：`p0_admission_total`、`p0_delivery_total`、`p0_event_total`、`p0_stage_seconds`、`p0_backlog_orders`、`p0_oldest_age_seconds`、`p0_inflight_orders`。

- [ ] **Step 1: 写失败测试**

```java
@Test public void rejectsNewDrawWhenBacklogExceededButAllowsSameRequest() {
  gate.update(new P0BacklogSnapshot(20_001, 10));
  assertThat(gate.tryEnter("u1", 1)).isFalse();
  // 同一 requestId 的重复请求仍返回原订单（由 DrawService 幂等分支覆盖）
}

@Test public void perUserLimitReleasesAfterDelivery() { /* tryEnter 两次第二次 false；leave 后 true */ }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0BackpressureV2Test -DfailIfNoTests=false test`
Expected: FAIL，gate/probe 不存在。

- [ ] **Step 3: 实现**

- `P0BacklogProbe` 用 1 秒定时查询 8 张 `v2_draw_order_*` 的 `state='PENDING_RESERVATION'` 数量与最老 `last_progress_at`；走 `draw_backlog` 索引，不扫全表。
- `P0AdmissionGate` 持有 `AtomicReference<P0BacklogSnapshot>` + `ConcurrentHashMap<String,AtomicInteger>`；超过 20,000 或 120s 拒绝新 draw；同一 requestId 在 `DrawService` 幂等分支优先返回，不走 gate 拒绝。
- `DrawService.create` 先做同一 `requestId` 的幂等查询并直接返回已有订单；只有新请求才进入 gate。`try/finally` 在受理成功/失败后 `leave`，不能把重试请求当成新的在途请求拒绝。
- 黑名单按 spec 处理：`admitted.blacklisted()` 时落 `REJECTED`，不扣额度、不写 `ReserveAward`、不进入 gate；测试覆盖余额/额度不变且无 outbox。
- `V2MetricsConfig` 注册 gauge/counter；`DrawService`/`DrawDeliveryService` 记录 admission/delivery/stage。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add bm-domain/src/main/java/com/jay/domain/raffle/service/P0AdmissionGate.java bm-infrastructure/src/main/java/com/jay/infrastructure/observability/P0BacklogProbe.java bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java bm-app/src/main/java/com/jay/config/V2MetricsConfig.java bm-app/src/test/java/com/jay/test/v2/P0BackpressureV2Test.java
git commit -m "feat(p0): add backlog backpressure and delivery metrics"
```

---

## Task 10: 恢复兜底，删除旧六段恢复

**Files:**
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/P0RecoveryJob.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java`
- Delete: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawRecoveryService.java`
- Delete: `bm-trigger/src/main/java/com/jay/trigger/job/v2/StockRecoveryJob.java`
- Delete: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawProgressService.java`
- Delete: `bm-domain/src/main/java/com/jay/domain/raffle/service/AwardDeliveryService.java`
- Delete: `bm-domain/src/main/java/com/jay/domain/raffle/service/CandidateWorker.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/P0RecoveryV2Test.java`

**Interfaces:**
- 新建 `bm-domain/src/main/java/com/jay/domain/raffle/service/P0RecoveryService.java`
- `P0RecoveryService(UserTransactionRunner tx, DrawRepository repo, RouteCodec codec, OutboxWriter out, OrderIdGenerator ids)`
- `P0RecoveryService.reemit(String orderId) -> boolean`：同业务身份、新 eventId、同 `ReserveCommand` payload；订单非 `PENDING_RESERVATION` 返回 false。
- `P0RecoveryJob(P0RecoveryService service, DrawRepository repo); void tick()`：扫描 `PENDING_RESERVATION` 超过 5 分钟且无 PENDING outbox 的订单，调用 `reemit`。

- [ ] **Step 1: 写失败测试**

```java
@Test public void stalePendingOrderGetsReserveAwardAgain() {
  // 构造 PENDING_RESERVATION + last_progress_at 10 分钟前 + 无 outbox
  job.tick();
  assertThat(outboxCount(order, "ReserveAward")).isOne();
}

@Test public void deliveredOrderIsIgnored() { /* no new outbox */ }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0RecoveryV2Test -DfailIfNoTests=false test`
Expected: FAIL，旧 recovery 仍在处理六段状态。

- [ ] **Step 3: 实现**

- 删除 `DrawRecoveryService`/`StockRecoveryJob` 及其 beans；删除/替换引用 `DrawProgressService`/`AwardDeliveryService`/`CandidateWorker` 的旧测试（`DrawCancellationV2Test`、`RaffleDeliveryV2Test`、`GuardedWorkflowRecoveryV2Test` 等），避免 Task 10 删除类后编译失败。
- 支付/签到 recovery 不动。
- 新增 `P0RecoveryJob`，每 60 秒扫描最多 64 条，最多重发 8 条；事务内写 outbox；失败保留 DB 事实。
- 恢复不释放库存、不取消订单；只补 `ReserveAward`，由 control/user 消费者继续。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat(p0): replace six-stage recovery with pending-reservation replay"
```

---

## Task 11: 端到端与对账

**Files:**
- Test: `bm-app/src/test/java/com/jay/test/v2/P0EndToEndV2Test.java`
- Modify: `bm-app/src/test/java/com/jay/test/v2/RaffleIntegrationV2Test.java`（改成新链路或删除）
- Delete: 只验证六段/取消的旧测试

**Interfaces:**
- 使用 Spring 上下文/`AssetTestSupport` + 直接调用 `V2MessageListener` 或 Rabbit 容器，跑完整三段。
- 对账 SQL：订单状态、`available+reserved+allocated`、积分流水、交付记录。

- [ ] **Step 1: 写失败测试**

```java
@Test public void endToEndCreditThenNoneThenDuplicate() {
  // 1) 有库存：admission -> reserve -> delivery，订单 DELIVERED，余额 +10
  // 2) 无库存：admission -> reserve 落 NONE -> delivery，订单 DELIVERED，余额不变
  // 3) 重复 ReservationConfirmed：余额不变
  // 4) 对账：stock total=available+reserved+allocated，delivery 行存在，差异 0
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl bm-app -am -Dtest=P0EndToEndV2Test -DfailIfNoTests=false test`
Expected: FAIL，旧链路仍走六段/取消。

- [ ] **Step 3: 实现/清理**

- 更新/删除旧六段测试；保留支付/签到/兑换/资产测试。
- 确保 `V2MessageListener` 对旧事件类型返回异常/隔离，不静默 ack。
- 对账 SQL 覆盖 2 个 user 库 × 4 表 + control `award_stock`/`award_reservation`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上；再跑 `mvn -pl bm-app -am test`（需要 `V2_MYSQL_PASSWORD`、Rabbit）。
Expected: PASS；旧六段测试已删除/替换。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "test(p0): add end-to-end credit/none/duplicate reconciliation"
```

---

## Task 12: 两机 benchmark 脚本与 runbook

**Files:**
- Create: `docs/performance/p0-load.js`
- Create: `docs/performance/p0_seed.py`
- Create: `docs/performance/p0-runbook.md`
- Modify: `docs/performance/README.md`

**Interfaces:**
- `p0-load.js`：`__ENV.BASE_URL`、`__ENV.SEED_DIR`、`__ENV.RUN_ID`、`__ENV.VUS` 或 `__ENV.RATE`；7:1:1:1；输出 request JSONL。
- `p0_seed.py`：`--run-id`、`--users 2000`、`--activities 10`、`--prizes 10`；生成 seed/tokens；fallback NONE；所有 CREDIT 有 stockPoolId。
- `p0-runbook.md`：M5 32GB 做 SUT（Docker 8C16G）、16GB Mac 做 k6、同 Wi-Fi/有线、冒烟/正式/取证；包含“停应用→删旧队列/旧 outbox→重建 3 库 schema→seed 2,000/10/10→启动”的空库重置步骤。

- [ ] **Step 1: 写可执行 smoke**

- 在 M5 启动最小 stub 或应用 health；在 16GB Mac 跑：
  `k6 run --vus 1 --duration 1s docs/performance/p0-load.js`
- seed 脚本 dry-run/实际跑一次，断言 2,000 用户、10 活动、10 奖、fallback NONE。

- [ ] **Step 2: 运行确认失败**

Run: 上述命令。
Expected: FAIL，脚本不存在或路径/参数不匹配。

- [ ] **Step 3: 实现**

- 从 `BroMark-8c4g.../candidate/load.js` 脱敏复制，去掉 `/seed` 绝对路径与容器绑定；支持 native k6。
- `p0_seed.py` 复用 `seed_v2_testdata.py` 的 route/token，生成 10 个 CREDIT 奖 + 10 个 stock + fallback NONE；不写私有 token 到 Git。
- runbook 写清 M5 IP、Docker 资源、k6 安装、防火墙、`caffeinate`、`ulimit`、结果回传。

- [ ] **Step 4: 运行 smoke 确认通过**

Run: 1 VU 冒烟 + seed 计数校验。
Expected: PASS；输出文件包含 requestId/status/latency。

- [ ] **Step 5: 提交**

```bash
git add docs/performance/p0-load.js docs/performance/p0_seed.py docs/performance/p0-runbook.md docs/performance/README.md
git commit -m "docs(p0): add two-machine benchmark scripts and runbook"
```

---

## Self-Review

- **Spec coverage:** 3 段事务（Task 2/3/4）、NONE fallback（Task 3/4）、无取消（Task 6）、队列/预算（Task 5/7）、批量（Task 8）、背压/观测（Task 9）、恢复（Task 10）、验收/脚本（Task 11/12）。
- **Type consistency:** `ReservationResult` 同时用于 control 输出和 user 输入；`ReserveCommand` 只在 `ReserveAward` payload；`DrawDeliveryService.deliver(EventEnvelope)` 与 `V2MessageListener` handler 一致。
- **Review Focus:** NONE/库存耗尽在 Task 3/4/11；重复在 Task 3/4/11；批量失败在 Task 8；背压/幂等在 Task 2/9；旧事件隔离在 Task 5/11。
- **比例:** 12 个任务，每个 4～5 步；未展开完整方法体，只给签名、测试断言和关键 SQL。

## Execution Handoff

计划保存后请先 review。若确认，下一步可选择：

- **Subagent-driven**：每个 task 由独立 subagent 实现 + 独立 reviewer，适合这种跨 10+ 文件、接口耦合强的改造。
- **Native**：我在当前 session 按顺序实现，最后统一 reviewer；更快，但中途没有独立 gate。

我的建议是 **Subagent-driven**：Task 2/3/4 的接口必须严格对齐，独立 reviewer 能在每个 task 后拦住类型/状态机偏差。
