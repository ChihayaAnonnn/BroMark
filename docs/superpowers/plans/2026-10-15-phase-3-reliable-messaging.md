# BroMark 阶段3 可靠消息与恢复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 建立业务提交不丢事件、消息重复不重复资产、暂停后可审计恢复的公共机制。

**Architecture:** 每个数据库独立Outbox与Inbox，Publisher先claim提交再发送，消费者本地事务提交后Ack。业务阶段与RecoveryStatus分离，恢复命令更换eventId但不更换bizId。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MyBatis、MySQL 8.0 InnoDB、Redis、RabbitMQ、JUnit 5＋Vintage（兼容JUnit 4）、Maven。

**Spec:** [高并发积分支付与抽奖设计](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)。

**执行状态（2026-10-15）：** 开发与隔离验收已完成，应用67项、Router28项、迁移4项通过，独立审查4项Important全部修复。见[验收记录](../../verification/2026-10-15-reliable-messaging/README.md)和[恢复手册](../../operations/v2-recovery-runbook.md)。完整旧套件仍有既有无限等待；未合并、推送或生产部署。沿用阶段1/2的JDBC仓储，不创建未使用的MyBatis包装文件；追加003_recovery_guard.sql用于协调身份与跨核查周期版本。

## Global Constraints

- 阶段0完成后编译、测试、应用与容器统一Java 25，release=25；使用Boot 3.5.16及Jakarta体系，保留javax.sql等JDK API。
- 执行环境采用 MySQL 8.0 InnoDB，业务短事务使用 READ COMMITTED，关键读取显式加锁。
- 业务时区固定为 Asia/Shanghai。
- 首版积分使用 BigDecimal 与数据库定点小数，拒绝超过两位的小数或溢出，不使用 float、double，不在写入时静默舍入。
- 首版次数 SKU 不限库存，积分兑换在同一用户分库事务内完成扣积分与增加次数；签到奖励统一使用新资产内核。
- 首版奖品只支持积分与无权益结果；Redis 不保存唯一余额或库存事实，账户事务不调用 MQ、支付渠道或远程发货。
- 本文任务已执行；实际结果、取舍与验收边界见上述记录，不代表生产部署或完整旧套件通过。
- 路径相对 BroMark 仓库根目录；先读[实施总览](2026-10-15-credit-payment-raffle-implementation-index.md)的共享契约、测试环境和迁移规则。依赖：[阶段1—2全部任务](2026-10-15-phase-1-2-assets-signin-exchange.md)。

## Review Focus

- 发布成功但状态未写：3.2允许重投，3.3吸收重复。
- 不可路由但收到broker Ack：3.2不能标发布成功。
- 旧租约恢复：3.1旧token不能覆盖。
- 新eventId重述完成业务：3.3返回原回执帮助源状态收敛。
- 核查暂停后晚到事实：3.4持久化、修复后重放，不静默丢弃。

## File Structure

domain/task定义消息契约、租约和恢复服务；infrastructure负责SQL与broker；trigger负责调度与监听。003只增加公共任务/消费/核查结构；后续业务状态字段由004/005迁移提供。

## Tasks

### Task 3.1: 可认领Outbox与代次租约

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/task/model/OutboxLease.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/service/OutboxRelay.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/repository/OutboxRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlOutboxRepository.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/OutboxRelayJob.java`
- Create: `docs/dev-ops/mysql/migrations/003_messaging_recovery.sql`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/OutboxDao.java`
- Modify: `bm-app/src/main/resources/mybatis/mapper/v2/outbox_mapper.xml`
- Modify: `docs/dev-ops/mysql/migrations/manifest.json`
- Test: `bm-app/src/test/java/com/jay/test/v2/OutboxLeaseV2Test.java`

**Interfaces:**
- Consumes: 1.4事件与Outbox；0.2控制/用户事务；0.3许可。
- Produces: OutboxRepository.claim(RouteSlot,int,Duration):List<OutboxLease>；complete(OutboxLease):boolean；retry(OutboxLease,Instant,String):boolean；Lease(ownerSlot,event,leaseToken,leaseUntil,attempt)，ownerSlot是源Outbox所在库，与event的目的槽分开。

- [x] **Step 1 — 写失败测试。** 在 `OutboxLeaseV2Test` 中两个实例同时认领，到期后新实例接管，旧执行者尝试完成，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(allPendingIds.size(), distinctClaimedIds.size());
assertFalse(repository.complete(oldLease));
assertTrue(repository.complete(currentLease));
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxLeaseV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 短事务SELECT FOR UPDATE SKIP LOCKED认领到时PENDING或过期PROCESSING，使用数据库时间和新token递增attempt。claim/complete/retry各自经inSlot开启源库短事务；完成/失败更新必须匹配eventId、PROCESSING、token，不按event目的槽更新任务。claim提交后才访问broker。每库一张Outbox，显式枚举db00/db01/db02，按类有界扫描；不把四张订单表数量误当Outbox张数。补充lease、next_attempt索引与枚举Repository方法实现。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测多实例、过期接管、旧token失败路径和扫描公平性；记录EXPLAIN避免无索引全表锁扫描。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: claim outbox work with fenced leases"`；不提交本地配置、凭据或原始用户数据。

### Task 3.2: 发布确认、Return与有界发送

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/task/service/ConfirmedEventPublisher.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/model/PublishOutcome.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/event/V2ConfirmedEventPublisher.java`
- Create: `bm-app/src/main/java/com/jay/config/V2RabbitConfig.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/task/service/OutboxRelay.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/PublisherConfirmV2Test.java`

**Interfaces:**
- Consumes: 3.1认领与Lease；1.4EventEnvelope。
- Produces: ConfirmedEventPublisher.publish(EventEnvelope,Deadline):CompletionStage<PublishOutcome>；Outcome=CONFIRMED/RETURNED/NACKED/UNKNOWN；OutboxRelay.runOnce(RouteSlot,int):int。

- [x] **Step 1 — 写失败测试。** 在 `PublisherConfirmV2Test` 中真实broker持久队列，分别使用正确绑定、错误routing key、丢失confirm连接和发送后未记完成的故障点，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(PublishOutcome.RETURNED, unroutableOutcome);
assertEquals("PENDING", returnedEventStateAfterRetrySchedule);
assertEquals("PUBLISHED", confirmedEventState);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PublisherConfirmV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 持久exchange/queue/message、mandatory=true、correlated publisher confirms及returns；CorrelationData按eventId和leaseToken区分本次尝试。只有确认成功且未Return才能complete；UNKNOWN退避重试，Return/Nack不能记PUBLISHED。异步或小批量等待Confirm，限制未确认数，提交claim后不占数据库连接等待。broker拓扑在v2测试配置中声明；本地可单节点durable作进程故障实验，生产/副本故障需独立quorum拓扑验收并记录磁盘/副本，不能宣称单节点容灾。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。旧EventPublisher只留迁移前旧路径，不再被v2业务调用；发布成功但DB状态未写时允许重复发送。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: publish outbox events with confirms and returns"`；不提交本地配置、凭据或原始用户数据。

### Task 3.3: 消费Inbox与可靠延迟重试

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/task/repository/ConsumerInbox.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/service/ConsumerUnitOfWork.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/service/RetryPolicy.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/model/ConsumeOutcome.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/service/EventRouting.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/repository/RetryJobRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlRetryJobRepository.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/retry_job_mapper.xml`
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/RetryPublishJob.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlConsumerInbox.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/ConsumerInboxDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/consumer_inbox_mapper.xml`
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/V2MessageListener.java`
- Modify: `docs/dev-ops/mysql/migrations/003_messaging_recovery.sql`
- Test: `bm-app/src/test/java/com/jay/test/v2/ConsumerInboxV2Test.java`

**Interfaces:**
- Consumes: 3.2broker；1.4Outbox；0.2事务Runner。
- Produces: ConsumerUnitOfWork.apply(String,EventEnvelope,Runnable):ConsumeOutcome；ConsumerInbox.tryInsert(String,EventEnvelope):boolean；RetryPolicy.next(int,Instant):Instant；EventRouting.route(EventEnvelope):RouteSlot；RetryJobRepository.schedule(EventEnvelope,int,Instant):void，claim/complete/retry复用3.1的OutboxLease契约。ConsumeOutcome=APPLIED/DUPLICATE/DEFERRED。

- [x] **Step 1 — 写失败测试。** 在 `ConsumerInboxV2Test` 中同eventId重复消息、不同eventId相同bizId、事务提交后Ack前宕机、无法持久化重试任务，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, assetEffectCountForBiz);
assertEquals(1L, inboxRowsForOriginalEvent);
assertEquals(1L, responseOutboxRowsForRecoveryEvent);
assertFalse(ackedWhenRetryPersistenceFailed);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ConsumerInboxV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** EventRouting按总览类型白名单核对目的槽，再经Runner进入目的业务事务；控制事件带userId仍去db00。消费Inbox、业务副作用、响应Outbox同事务，响应事件虽跨库，Outbox仍写本地。提交后手动Ack。相同eventId同摘要重复无需业务重做；不同摘要核查。新eventId重述已完成bizId必须写原业务回执响应，不再改变资产。未知schemaVersion/业务类型进入核查，不执行payload。短暂失败在本库新短事务持久化retry_job后Ack，RetryPublishJob使用3.1租约和3.2Confirm按next_attempt重投原eventId；重试累计次数不归零。无法持久化时保留未Ack并暂停该消费者/退避恢复，禁止立即无限requeue。最多12次、初始1秒、上限5分钟加抖动。ExchangeCompleted/SignInCompleted绑定独立通知消费者，不进入资产执行器。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测试数据库不可用、毒消息、顺序颠倒；错误阶段不能写成功Inbox或发成功回执，DEFERRED必须有持久化证据。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: consume events atomically with durable retries"`；不提交本地配置、凭据或原始用户数据。

### Task 3.4: 暂停核查、待处理事实与人工恢复

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/task/model/RecoveryStatus.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/model/RecoveryDecision.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/model/RecoveryAggregate.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/repository/RecoveryAggregatePort.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/service/RecoveryService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/repository/RecoveryRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlRecoveryRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/RecoveryCaseDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/recovery_case_mapper.xml`
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/RecoveryScanJob.java`
- Create: `docs/operations/v2-recovery-runbook.md`
- Modify: `docs/dev-ops/mysql/migrations/003_messaging_recovery.sql`
- Modify: `bm-domain/src/main/java/com/jay/domain/task/service/ConsumerUnitOfWork.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/RecoveryCaseV2Test.java`

**Interfaces:**
- Consumes: 3.1—3.3任务、Inbox、事件契约。
- Produces: RecoveryService.pause(RouteSlot,String,String,RecoveryStatus,String):void；resume(RouteSlot,String,String,long,String,String):void；RecoveryRepository.defer(EventEnvelope):void；RecoveryDecision(actions:List<EventEnvelope>)；RecoveryAggregatePort.lock(String id):RecoveryAggregate、updateStatus(String id,long expectedVersion,RecoveryStatus):boolean；Aggregate(id,state,recoveryStatus,version)。

- [x] **Step 1 — 写失败测试。** 在 `RecoveryCaseV2Test` 中使用测试专用recovery_test_aggregate表及RecoveryAggregatePort实现模拟RESULT_RECORDED（尚不依赖阶段4抽奖），业务暂停后送入新eventId及确认回执，修复后带正确/过期版本分别恢复，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals("RESULT_RECORDED", businessStateAfterPause);
assertEquals(RecoveryStatus.REVIEW, recoveryStatus);
assertEquals(0L, assetEffectsDuringPause);
assertEquals(1L, durableDeferredFacts);
assertThrows(OptimisticLockingFailureException.class, staleResumeCall);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RecoveryCaseV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 003新增recovery_case与deferred_fact/retry_job；同对象最多一个活动case，通过可空active_key唯一约束实现。RecoveryService按aggregateType注册RecoveryAggregatePort，生产领域适配器由4.4、5.4补齐，测试用专用表不虚构已存在抽奖实现。pause同库保存原阶段、recovery_status与证据；未知订单绑定原任务身份。ACTIVE之外扫描和消息不推进，晚到事实落deferred_fact后Ack。resume核对操作者、原因和版本，同事务恢复ACTIVE、关case、写恢复Outbox；deferred事实以新eventId、原bizId重放并记录已重放，避免旧Inbox阻断。扫描器使用RecoveryDecision，不生成新候选/金额/业务ID；runbook按0.3生命周期关闭。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测租约旧执行者、人工重复恢复、DEAD后新事件绕过、终态仅结案不回退业务阶段，以及关闭时无任务删除。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: audit and resume paused business workflows"`；不提交本地配置、凭据或原始用户数据。

## 阶段退出条件

真实MySQL与RabbitMQ下租约、Confirm/Return、事务后Ack及延迟重试通过；每个故障都有持久化恢复入口。资产协议无需直接发布消息；不得以消息到队列等同业务完成。

## 阶段0集成约束

已有V2WorkerConfig关闭流程停止worker与数据库许可；实现Relay后必须整合Confirm跟踪与关闭顺序，先停止新认领并等待Confirm及对应Outbox更新，再停止数据库许可并关闭连接池。不得直接把阶段0空闲worker关闭测试当作Confirm排空验收。
