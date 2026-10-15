# BroMark 阶段4 抽奖库存闭环 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 实现固定候选、共享库存池、确认/取消互斥和可恢复的积分发奖。

**Architecture:** 将v2抽奖应用服务与旧责任链入口分开，复用Alias算法但仅使用冻结输入。库存域在db00独立提交，用户域按状态机接受回执，Outbox/Inbox处理运输重复。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MyBatis、MySQL 8.0 InnoDB、Redis、RabbitMQ、JUnit 5＋Vintage（兼容JUnit 4）、Maven。

**Spec:** [高并发积分支付与抽奖设计](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)。

## Global Constraints

- 阶段0完成后编译、测试、应用与容器统一Java 25，release=25；使用Boot 3.5.16及Jakarta体系，保留javax.sql等JDK API。
- 执行环境采用 MySQL 8.0 InnoDB，业务短事务使用 READ COMMITTED，关键读取显式加锁。
- 业务时区固定为 Asia/Shanghai。
- 首版积分使用 BigDecimal 与数据库定点小数，拒绝超过两位的小数或溢出，不使用 float、double，不在写入时静默舍入。
- 首版次数 SKU 不限库存，积分兑换在同一用户分库事务内完成扣积分与增加次数；签到奖励统一使用新资产内核。
- 首版奖品只支持积分与无权益结果；Redis 不保存唯一余额或库存事实，账户事务不调用 MQ、支付渠道或远程发货。
- 本阶段已完成开发及隔离验收；结果和已知限制见[验收报告](../../verification/2026-10-16-raffle-stock/README.md)。没有生产部署或数据切换。
- 路径相对 BroMark 仓库根目录；先读[实施总览](2026-10-15-credit-payment-raffle-implementation-index.md)的共享契约、测试环境和迁移规则。依赖：[阶段3](2026-10-15-phase-3-reliable-messaging.md)及[资产内核](2026-10-15-phase-1-2-assets-signin-exchange.md)。

## Review Focus

- 概率换版复制同批库存：4.1、4.2按stockPoolId共同守恒。
- 排队跨日或实时规则变化：4.3只用原快照。
- 取消先到、Held晚到：4.2墓碑与4.4单分支CAS。
- 用户库不可达且HELD超时：4.4保守占用，不当作取消。
- quantity=0或发奖重复：4.2、4.5不修改库存或重复积分。

## File Structure

strategy负责不可变发布与纯选奖；stock负责db00名额；raffle负责用户订单与确认；AwardDeliveryService负责本地交付。004在db00创建版本/池/预留，在用户四张表创建抽奖/中奖结构及恢复索引。

## Tasks

### Task 4.1: 不可变策略发布与有界本地缓存

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/model/FrozenStrategy.java`
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/model/AwardSnapshot.java`
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/model/RuleInputSnapshot.java`
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/model/PublishStrategyCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/service/StrategyPublicationService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/service/FrozenRaffleEngine.java`
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/repository/StrategyVersionRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlStrategyVersionRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/cache/StrategyVersionCache.java`
- Create: `bm-domain/src/main/java/com/jay/domain/activity/service/PublicActivityQueryService.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/PublicActivityQueryController.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/StrategyVersionDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/strategy_version_mapper.xml`
- Create: `docs/dev-ops/mysql/migrations/004_strategy_stock_draw.sql`
- Modify: `docs/dev-ops/mysql/migrations/manifest.json`
- Modify: `bm-domain/src/main/java/com/jay/domain/activity/service/AdmissionService.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/FrozenStrategyV2Test.java`

**Interfaces:**
- Consumes: 已有AliasMethodAlgorithm.build(List<StrategyAwardEntity>)与dispatch(AliasTable,Random)；2.1AdmissionSnapshot。
- Produces: publish(PublishStrategyCommand):String version；load(long strategyId,String version):FrozenStrategy；FrozenRaffleEngine.choose(FrozenStrategy,RuleInputSnapshot,Random):AwardSnapshot。AwardSnapshot(awardId,type,amount,stockPoolId,quantity,configHash)；RuleInputSnapshot(businessTime,totalEffectiveCount,dayEffectiveCount,blacklisted,blacklistVersion)。PublicActivityQueryService.current(long activityId):FrozenStrategy；公开GET /api/v2/activities/{id}及/{id}/awards只返回活动与奖品展示字段，不暴露内部规则或完整Alias表。

- [x] **Step 1 — 写失败测试。** 在 `FrozenStrategyV2Test` 中发布v1后修改原列表/数组与当前版本，淘汰本地和Redis后恢复旧版本，另测无效奖品与兜底，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(v1Amount, reloadedV1.getAward(101).getAmount());
assertEquals(v1PoolId, reloadedV1.getAward(101).getStockPoolId());
assertThrows(IllegalArgumentException.class, unsupportedPrizePublish);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=FrozenStrategyV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 004先建立strategy_version、strategy_stock_binding和award_stock骨架，测试可直接初始化名额；4.2补齐预留与库存变更协议。MySQL保存完整奖池、默认/权重Alias表、规则树与权益快照；同一控制库事务发布并切活动指针，不能初始化已有库存池。发布校验概率非负且总和>0、重复奖品/规则缺失、仅积分或无权益、无限库存兜底无循环。冻结对象防御性复制现有Alias可变数组，金额抽取后落AwardSnapshot不重随机。本地Guava有界缓存按(strategyId,version)，加载合并，Redis仅重建缓存，MySQL兜底；配置cache.maxVersions=128作为起点，记录实际内存。
- [x] **Step 3a — 实现公开查询缓存。** 活动当前指针缓存TTL初始3秒，版本内容不可变；按活动合并回源，并使用独立QUERY许可及有界回源并发。策略发布提交后预热，预热失败不回滚发布事实；Redis不可用从MySQL重建。公开响应标记配置版本、仅显示缓存库存，交易仍使用权威配置/库存裁决。测试冷缓存100并发只有一次同键加载、过期刷新失败不无限回源、旧订单加载旧版本与事务准入不使用展示缓存。
- [x] **Step 3b — 固定随机积分。** 积分权益必须为正、两位以内且不溢出；随机区间在版本中冻结，以整数分采样后转BigDecimal，禁止用double计算积分金额。测试固定随机源覆盖区间两端及单值区间；具体金额在候选CAS时持久化，重试不再次随机。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。默认池、权重池、黑名单、解锁、兜底分别验证统一返回AwardSnapshot；引擎不得调用旧queryTodayUserRaffleCount或实时积分查询。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: publish immutable raffle strategies"`；不提交本地配置、凭据或原始用户数据。

### Task 4.2: 库存池、预留确认与取消墓碑

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/stock/model/ReserveCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/stock/model/ConfirmCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/stock/model/CancelCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/stock/model/ReservationResult.java`
- Create: `bm-domain/src/main/java/com/jay/domain/stock/service/StockReservationService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/stock/repository/StockRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlStockRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/StockDao.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/ReservationDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/stock_mapper.xml`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/reservation_mapper.xml`
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/StockCommandListener.java`
- Modify: `docs/dev-ops/mysql/migrations/004_strategy_stock_draw.sql`
- Test: `bm-app/src/test/java/com/jay/test/v2/StockProtocolV2Test.java`

**Interfaces:**
- Consumes: 4.1版本绑定与AwardSnapshot；3.3消费事务；1.4Outbox。
- Produces: StockReservationService.reserve(ReserveCommand):ReservationResult；confirm(ConfirmCommand):ReservationResult；cancel(CancelCommand):ReservationResult。Reserve包含drawOrderId/userId/identityHash/requestHash/version/candidate/fallback；Confirm包含原身份及finalHash；Cancel只需不可变订单身份。Result包含state、finalSnapshot、quantity、原摘要。

- [x] **Step 1 — 写失败测试。** 在 `StockProtocolV2Test` 中池总量100、allocated30、reserved10、available60，v1/v2并发争抢；取消先到后预留；quantity=0确认，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(100L, available + reserved + allocated);
assertEquals("CANCELLED", reserveAfterTombstone.getState());
assertEquals(0L, zeroQuantityCounterChanges);
assertEquals(firstResult, duplicateReserveResult);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StockProtocolV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 004新增award_stock/award_reservation/stock_change_log，先唯一建立预留身份并锁预留再锁池。预留核对发布绑定与摘要；可售不足固定兜底，绝不重抽。HELD→CONFIRMED按quantity搬reserved到allocated；HELD→CANCELLED按quantity归还available；quantity=0不改计数。无预留取消插墓碑，CONFIRMED/CANCELLED不可互转。每次计数变化与日志和对应ReservationHeld/Confirmed/Cancelled响应Outbox同事务；重复业务新eventId返回原事实响应，摘要冲突核查。补货/核减addStock(stockPoolId,bizId,delta,reason)同事务日志，核减不可侵占reserved/allocated。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测库存行热点、预留后宕机、重复确认、同ID不同候选、篡改stockPoolId、0库存及不限库存；HELD超时不得释放。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: reserve confirm and cancel shared stock"`；不提交本地配置、凭据或原始用户数据。

### Task 4.3: 原子扣次受理与唯一候选

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/model/DrawCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/model/DrawView.java`
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/CandidateWorker.java`
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/repository/DrawRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlDrawRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/DrawOrderDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/draw_order_mapper.xml`
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/PrepareDrawListener.java`
- Modify: `docs/dev-ops/mysql/migrations/004_strategy_stock_draw.sql`
- Modify: `bm-domain/src/main/java/com/jay/domain/activity/service/AdmissionService.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/DrawAcceptanceV2Test.java`

**Interfaces:**
- Consumes: 1.3QuotaKernel、4.1冻结策略、3.3消息事务。
- Produces: AdmissionService.forDraw(String,long,Deadline):AdmissionSnapshot；DrawService.create(DrawCommand):DrawView；Command(userId,requestId,activityId,deadline)；CandidateWorker.prepare(EventEnvelope):void；DrawView(orderId,state,recoveryStatus,finalAward)。

- [x] **Step 1 — 写失败测试。** 在 `DrawAcceptanceV2Test` 中同键100次扣次，两个Worker选出不同随机候选并发CAS，排队跨日且实时黑名单变化，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, quotaConsumptionCount);
assertEquals(1L, acceptedOrderCount);
assertEquals(1L, fixedCandidateCount);
assertEquals(originalRuleInput, storedRuleInputAfterMidnight);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DrawAcceptanceV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 首查请求回执，独立取db00准入，再用户事务锁账户/身份/订单/月/日，原子扣次、记录business_at及扣次后有效计数快照、写ACCEPTED和PrepareDraw。资格拒绝写REJECTED且无扣次。prepare只用原策略与RuleInputSnapshot，CAS ACCEPTED→CANDIDATE_FIXED保存候选、具体积分、兜底和ReserveAward Outbox；失败方丢弃自己的随机结果读胜者。候选计算可在事务外，最终CAS与Outbox必须原子；取消获胜时不能再写候选。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测余额总/月/日边界、技术回滚不发成功、固定候选后重启、无当前策略缓存及活动关闭；不按新日期或新黑名单重算。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: accept draws and freeze one candidate"`；不提交本地配置、凭据或原始用户数据。

### Task 4.4: 结果确认、取消分界与状态恢复

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawProgressService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawRecoveryService.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/ReservationReceiptListener.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/DrawRecoveryJob.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/StockRecoveryJob.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/raffle/repository/DrawRepository.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/stock/repository/StockRepository.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlStockRepository.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlDrawRepository.java`
- Modify: `bm-app/src/main/resources/mybatis/mapper/v2/draw_order_mapper.xml`
- Test: `bm-app/src/test/java/com/jay/test/v2/DrawCancellationV2Test.java`

**Interfaces:**
- Consumes: 4.2库存事实；4.3订单；3.4暂停与恢复；1.3原桶refund。
- Produces: DrawProgressService.onHeld(EventEnvelope):void、onConfirmed(EventEnvelope):void、onCancelled(EventEnvelope):void；DrawService.cancel(String userId,String orderId):DrawView；DrawRecoveryService.inspect(String orderId):RecoveryDecision；DrawRepository和StockRepository接入3.4 RecoveryAggregatePort并注册DRAW/RESERVATION类型。

- [x] **Step 1 — 写失败测试。** 在 `DrawCancellationV2Test` 中用栅栏并发取消与ReservationHeld；确认回执丢失后重放；跨月取消原桶；暂停时晚到回执，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertTrue(resultRecorded ^ cancelling);
assertEquals(1L, refundLedgerCount);
assertEquals(currentDayBefore, currentDayAfterOldCancel);
assertEquals("COMMITTED", recoveredConfirmedOrder.getState());
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DrawCancellationV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** onHeld仅CANDIDATE_FIXED→RESULT_RECORDED，写最终快照/待确认中奖记录/ConfirmReservation；此提交禁止取消。取消仅ACCEPTED或CANDIDATE_FIXED→CANCELLING并写CancelReservation。onConfirmed仅RESULT_RECORDED→COMMITTED并写AwardDelivery，持久化confirmed_at用于端到端延迟；onCancelled仅CANCELLING退原总/月/日和有效次数后CANCELLED。晚到Held在CANCELLING只推进取消，重复回执按原事实响应。恢复逐库逐表扫ACTIVE；先事务外查询另一域权威事实，再本库核对版本CAS和原快照写恢复命令；不可达保留HELD告警，CONFIRMED重发原确认，不生成新候选或新订单。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。覆盖每个正常状态的重入、禁止跳转、RESULT_RECORDED后取消409、取消墓碑晚到、回执摘要不匹配转REVIEW；暂停恢复按3.4，不直接跨过ACTIVE检查。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: recover draw confirmation and cancellation"`；不提交本地配置、凭据或原始用户数据。

### Task 4.5: 幂等积分发奖、查询接口与库存对账

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/AwardDeliveryService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/raffle/service/RaffleReconciliationService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/ReconciliationReport.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/AwardDeliveryListener.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/RaffleDrawController.java`
- Create: `bm-api/src/main/java/com/jay/api/v2/DrawRequest.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/AwardRecordDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/award_record_mapper.xml`
- Create: `docs/operations/v2-raffle-reconciliation.sql`
- Modify: `docs/dev-ops/mysql/migrations/004_strategy_stock_draw.sql`
- Modify: `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/RaffleDeliveryV2Test.java`

**Interfaces:**
- Consumes: 4.4 COMMITTED与AwardDelivery；1.2AssetKernel；0.4身份；2.3 RequestAdmission对抽奖创建同样限流、取消与结果查询独立配额。
- Produces: AwardDeliveryService.deliver(EventEnvelope):void；RaffleReconciliationService.check(RouteSlot):ReconciliationReport；DrawService.get(String userId,String orderId):DrawView；设计的draws POST/GET/cancel接口。ReconciliationReport含creditMismatch、quotaMismatch、stockMismatch、duplicateDelivery、paymentMismatch、signinMismatch六个long及checkedScopes集合，未执行检查不能当零差异结论。

- [x] **Step 1 — 写失败测试。** 在 `RaffleDeliveryV2Test` 中同bizId不同eventId重复发奖、提交后Ack前中断、COMMITTED前查询、无权益兜底与跨用户查单，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, awardCreditLedgerCount);
assertEquals("DELIVERED", finalView.getState());
assertNull(beforeCommittedView.getFinalAward());
assertEquals(403, crossUserHttpStatus);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RaffleDeliveryV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 统一锁积分账户→活动账户→订单，只有COMMITTED可发积分；同事务写credit唯一流水、交付回执、中奖记录、DELIVERED及delivered_at，数量0只写无权益交付回执。GET统一200返回当前状态；POST新受理202、已有终态200；取消受理202、已取消200、不允许取消409。最终奖品仅COMMITTED/DELIVERED可见，其他状态禁止展示候选，所有查询返回恢复状态。校验池守恒、库存日志/预留计数、抽奖/取消流水、中奖与交付唯一性；记录实际checkedScopes，差异暂停相关池/账户并建核查单，不覆盖余额。v2不使用Redis库存同步。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测未确认先发奖、不支持类型进入WAITING_FIX、技术回滚无发奖流水、MQ中断后恢复与阶段各耗时指标。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: deliver raffle awards and reconcile outcomes"`；不提交本地配置、凭据或原始用户数据。

## 阶段退出条件

七步正常流程、所有取消竞态及跨版本库存测试通过；各库事实与流水一致。故障后继续原候选与权益；最终奖品只在确认后可见。保存真实故障记录和阶段耗时。
