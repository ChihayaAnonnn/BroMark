# BroMark Outbox Relay 与恢复补发优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除发布前的持续积压与不必要补发，在保持账务、库存及恢复正确性的前提下，重新验证100请求/秒档的延迟与完成能力。

**Architecture:** 两条有界发布通道复用现有Relay执行器，按库/类型轮转、批量领取、分波发布和批量fencing完成。业务恢复通过所有者库的持久冷却记录收敛，已发布但未完成的Retry由目标库重投原运输身份；所有性能声明以隔离双实例实验和六域对账为依据。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MySQL 8.0 InnoDB/READ COMMITTED、JdbcTemplate、RabbitMQ Publisher Confirm、JUnit＋AssertJ、Docker固定镜像、Python unittest、k6。

**Spec:** [Outbox Relay与恢复补发优化设计](../specs/2026-10-16-outbox-relay-recovery-optimization-design.md)。同时阅读[原交易设计](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)和[延迟定位证据](../../verification/2026-10-16-latency-analysis/README.md)。

**Status:** 计划待执行；用户本轮要求撰写计划，不包含开始实现。代码基线`ad83092`，包含待评审设计和诊断证据；本计划不将Phase6的NO-GO改为通过。

## Global Constraints

- 路径均相对BroMark根目录；只修改本计划列出的BroMark组件，不修改7verse-ug、vivix-billing或其他服务。
- 保持Java25、Boot3.5.16、MySQL事实源、READ COMMITTED、UTC租约时间和Asia/Shanghai业务日期；不迁入Kafka/PostgreSQL，不改变事件schemaVersion=1。
- 不调整WORKER/TRADE/CALLBACK配额、Hikari池大小、JVM/GC或MQ拓扑；双实例三逻辑池各8及预留20连接为68，隔离环境按实际MySQL上限80验算。
- 初始批次32、每通道窗口8、通道数2、Publisher上限16；过期配额4、公平类型槽位8、空闲等待250ms。
- 领取预算1秒、每波发布3秒、每波完成1秒、单批20秒、租约30秒、余量5秒；校验`2×8≤16`及`1+ceil(32/8)×(3+1)+5=22<30`。
- 恢复停滞30秒、补发间隔30秒、同阶段/版本最多12次；每轮检查最多64候选、产生最多8条新恢复事件，2秒后不启动新候选。
- Rabbit等待、供应商调用、跨库读取不放进账户/领取/完成事务；逐行token fencing、消费failure_count及12次失败预算必须保留。
- REVIEW、WAITING_FIX、RETRY_EXHAUSTED阻止自动副作用；HELD超时不释放，原候选、金额、策略、业务身份及历史桶语义不变。
- 007迁移是新增结构；001—006内容和checksum不变；旧补发器必须全实例退出后才启用guarded，单实例旧tick与新循环互斥。
- 凭据/JWT/原始数据只来自环境和仓库外隔离目录；新输出目录不得覆盖旧诊断；生产部署、真实支付、合并和push不在本计划执行范围。

## Review Focus

1. 混合版本恢复器：旧补发器不遵守新冷却，部署切换必须先停止全部旧写者；Task4/10验证互斥与切换。
2. 人工resume改变version但旧任务有效：不能只按当前aggregate_version查任务；Task5/6验证历史任务和审计重放证明。
3. JDBC返回SUCCESS_NO_INFO或提交响应丢失：不能把整批记成功；Task2/3验证逐行fencing、回滚和不确定提交。
4. 持续新流量、迟提交的较小游标键及坏payload：不得永久漏扫或整批阻塞；Task2/6/7验证固定上界回绕与逐条隔离。
5. Retry已PUBLISHED但尚未完成：不得永久抑制或新建事件刷新generation；Task5/7验证原身份重投、回执与暂停竞态。

## 测试与执行约定

本机现有流程通过Docker使用JDK25/Maven，无需先安装宿主机JDK。执行前依据[隔离验收说明](../../performance/README.md)准备专属MySQL/Rabbit/Redis、网络及环境变量`V2_MYSQL_PASSWORD,V2_RABBIT_PASSWORD,MAVEN_CACHE_DIR,V2_TEST_DOCKER_NETWORK,V2_REDIS_CLUSTER_NODES`。缺依赖明确记录未执行，不能换mock后宣称真实SQL、并发或故障已通过。

各任务的聚焦命令统一为：

```sh
V2_TEST_SELECTOR='本任务测试类选择器' docs/operations/verify-java25.sh
```

使用现有固定digest镜像执行clean package；替换选择器为任务指定值，核对Surefire目标类确实执行、failures/errors/skipped均为0。新类尚不存在导致编译失败可以作为RED；凭据错误、依赖不可用和零测试不能作为RED。真实SQL测试复用`AssetTestSupport`及现有隔离库，不能指向dev/prod。每个任务保留RED/GREEN输出、提交hash和未完成项。

每个Task的Step5只提交其Files及必要的共享契约调整；禁止`git add .`。代码任务逐项完成后再进入后续任务，不提前勾选容量测试。

## 文件结构与依赖

| 任务 | 责任 | 依赖 |
|---|---|---|
| 1 | 配置、资源计算及真实DB deadline | 无 |
| 2 | 007结构、批量领取/完成仓储、查询索引 | 1 |
| 3 | 分波Publisher协调、结果与未发送释放 | 2 |
| 4 | 连续循环、执行隔离、唤醒及关闭接线 | 3 |
| 5 | 补发记录、任务证据和业务回执查询契约 | 2 |
| 6 | Draw/Stock/Payment唯一受保护恢复入口 | 4、5 |
| 7 | 目标库Retry原事件运输修复 | 4、5、6 |
| 8 | 分段观测、真实故障与回滚演练 | 6、7 |
| 9 | 用户分布、统计与实验工具修订 | 8 |
| 10 | 单变量诊断、三轮容量及Phase6退出裁定 | 9 |

任务5可在代码依赖允许时独立开发，但本计划默认顺序执行；不因为表格中的独立性自动启动代理。

## Task 1: 固定资源契约并落实数据库deadline

**Files:** Create `bm-domain/src/main/java/com/jay/domain/task/model/RelayPolicy.java`、`bm-app/src/main/java/com/jay/config/V2RelayProperties.java`、`bm-app/src/main/java/com/jay/config/V2RecoveryRedriveProperties.java`；Modify `bm-infrastructure/src/main/java/com/jay/infrastructure/transaction/RoutedTransactionRunner.java`、`bm-app/src/main/java/com/jay/config/V2TransactionConfig.java`、`bm-app/src/test/resources/application-v2-it.yml`；Test `bm-app/src/test/java/com/jay/test/v2/RelayBudgetV2Test.java`。

**Interfaces:** `RelayPolicy(int claimLimit,int publishWindow,int expiredReserve,int fairnessSlots,Duration idlePoll,Duration lease,Duration claimTimeout,Duration publishTimeout,Duration settleTimeout,Duration batchTimeout,Duration safetyMargin)`，`validate(int lanes,int publisherLimit):void`。两ConfigurationProperties分别绑定`bm.v2.messaging.relay`和`bm.v2.recovery.redrive`，mode默认legacy，全部默认值复制Global Constraints。现有`UserTransactionRunner`三种in方法签名不变。

- [ ] **Step1 — 写RED。** `RelayBudgetV2Test`：默认校验通过；窗口9×2、批次128/窗口8、lease=22秒被拒绝；过期许可等待不进入事务；1秒预算下真实`SELECT SLEEP(3)`不能提交；并发不同deadline不互相修改超时；异常后路由、事务和连接可再次使用。
关键测试方法：`rejectsOversizedWindowAndUnsafeLease`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThatThrownBy(() -> oversizedWindow.validate(2, 16)).isInstanceOf(IllegalArgumentException.class);
assertThatThrownBy(() -> lease22Seconds.validate(2, 16)).isInstanceOf(IllegalArgumentException.class);
```

- [ ] **Step2 — 验证RED。** 选择器`RelayBudgetV2Test,AdmissionDeadlineV2Test`；保存行为失败或新增类型编译失败的输出。
- [ ] **Step3 — 实现。** 配置校验同时考虑单批20秒和完整租约公式，非法模式/零或负参数拒绝启动。RoutedTransactionRunner每次调用使用独立TransactionTemplate，保留嵌套同route/work约束；按剩余时间设置事务/Statement超时，执行前及提交前重验deadline。隔离JDBC URL显式设置connectTimeout/socketTimeout=1000ms，bounded模式启动时验证有效网络读超时，不能只在Java future上计时；不全局修改共享JdbcTemplate超时。记录超时与清理耗时，未确认提交按不确定结果处理。
- [ ] **Step4 — 验证GREEN。** 同选择器通过，补跑`RoutingTransactionV2Test,DatabaseBudgetV2Test,TradeBudgetIsolationV2Test`；核对RC、permit先于连接、超时回滚及无ThreadLocal残留。超时取消/清理开销也纳入租约实测，不能用理论公式替代。
- [ ] **Step5 — 提交。** `git diff --check`，仅暂存本任务Files，`git commit -m "feat: validate relay budgets and database deadlines"`。

## Task 2: 007结构与批量租约仓储

**Files:** Create `bm-domain/src/main/java/com/jay/domain/task/model/ClaimedPublish.java`、`bm-domain/src/main/java/com/jay/domain/task/model/PublishResolution.java`、`bm-domain/src/main/java/com/jay/domain/task/model/BatchSettleReport.java`，`bm-domain/src/main/java/com/jay/domain/task/repository/BatchOutboxRepository.java`；Create `docs/dev-ops/mysql/migrations/007_recovery_dispatch.sql`、`docs/dev-ops/mysql/migrations/007_outbox_indexes.sql`；Modify `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlOutboxRepository.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlRetryJobRepository.java`（均位于`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/`）、`docs/dev-ops/mysql/migrations/manifest.json`、`docs/dev-ops/mysql/tests/test_migrations.py`；Test `bm-app/src/test/java/com/jay/test/v2/BatchOutboxV2Test.java`、`bm-app/src/test/java/com/jay/test/v2/BatchIndexV2Test.java`；Create test helper `bm-app/src/test/java/com/jay/test/v2/RelayOptimizationTestSupport.java`。

**Interfaces:** `ClaimedPublish(RouteSlot ownerSlot,String eventId,String aggregateId,long aggregateVersion,String eventType,String rawEnvelope,String payloadHash,String leaseToken,Instant leaseUntil,int attempt,int failureCount,String consumerName,Instant claimedAt)`；Outbox的failureCount=0、consumerName为空串。`PublishResolution(ClaimedPublish task,PublishOutcome outcome,String reason)`。`BatchSettleReport(Set<String> completed,Set<String> retried,Set<String> dead,Set<String> lost,Instant databaseNow)`。`BatchOutboxRepository extends OutboxRepository`新增：

```java
List<ClaimedPublish> claimBatch(RouteSlot slot, RelayPolicy policy, Deadline deadline);
BatchSettleReport settleBatch(RouteSlot slot, List<PublishResolution> results, Deadline deadline);
Set<String> releaseUnsent(RouteSlot slot, List<ClaimedPublish> unsent, Deadline deadline);
boolean quarantineMalformed(ClaimedPublish task, String reason, Deadline deadline);
```

旧OutboxLease、claim/complete/retry接口保留legacy兼容；新返回对象先携带rawEnvelope，不能因一个JSON坏行无法解析而整批回滚。

- [ ] **Step1 — 写RED。** 两实例并发各取32，eventId交集为空；混入4个过期任务、≥8种稀疏类型和大量PrepareDraw，核对不重复选行、每库独立类型游标、最老填充及过期剩余额度；全是过期任务仍可取满32。旧token完成/释放均更新0行。人为制造SUCCESS_NO_INFO、部分失租、坏JSON，断言只更新实际拥有行、坏行独立核查、其余合法行推进。Explain断言pending_type/pending/expired及published_recovery各走设计路径，001—006hash不变。
关键测试方法：`claimsFairBatchAndFencesExpiredOwner`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThat(claimed.stream().map(ClaimedPublish::eventId).distinct()).hasSize(32);
assertThat(report.lost()).contains(oldLease.eventId());
assertThat(report.completed()).doesNotContain(oldLease.eventId());
```

- [ ] **Step2 — 验证RED。** 选择器`BatchOutboxV2Test,BatchIndexV2Test,OutboxLeaseV2Test`；migration测试新增007未存在时失败，不能跳过。
- [ ] **Step3 — 实现。** 007均scope=all、shards=1：dispatch表按设计7.2字段、联合主键及UTC datetime(6)；observed_state/version初始化为空串/0、last_event_type/id及last_redrive_at可空、next_redrive_at初始DB_NOW、redrive_count初始0。为Outbox/Retry新增pending_type、aggregate_event，为Retry新增published_recovery，保留pending/expired，不建expired_type。SQL按4/8/剩余配额有界SKIP LOCKED；一次DB时间、每行token、一次批量更新；排除本事务已选ID。完成采用批量锁定筛出本token行再批量条件更新，核对精确影响数，避免依赖SUCCESS_NO_INFO。普通Outbox失败批量，涉及业务检查的Retry失败按单聚合、账户→订单→任务顺序移交；DEAD/核查/deferred语义保持。releaseUnsent仅对调用者确认未提交Publisher的集合恢复attempt-1，匹配token及原attempt。新增脚本checksum登记；测试helper显式加载007，生产启动不建表。
- [ ] **Step4 — 验证GREEN。** 同选择器通过；运行`V2_MIGRATION_THROUGH=007 docs/operations/verify-assets.sh`实际apply至007、重复apply并演练中途断开后恢复，记录每库索引、schema_migration和旧hash。SQL计数证明领取/完成没有每条lease_until查询及每条完成事务；事务断开不返回假成功报告。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "feat: batch outbox leases with fenced settlement"`。

## Task 3: 分波发布与截止时间收敛

**Files:** Create `bm-domain/src/main/java/com/jay/domain/task/service/BoundedOutboxRelay.java`；Modify `bm-domain/src/main/java/com/jay/domain/task/service/OutboxRelay.java`仅保留/适配legacy，不混改其策略；Test `bm-app/src/test/java/com/jay/test/v2/BoundedRelayV2Test.java`。

**Interfaces:** `BoundedOutboxRelay(BatchOutboxRepository repo,ConfirmedEventPublisher publisher,RelayPolicy policy,FaultInjector faults)`；`runOnce(RouteSlot slot):int`返回本轮实际领取数；`stopAndDrain(Duration timeout):void`停止新领取并等待活动批次。复用`ConfirmedEventPublisher.publishAttempt(EventEnvelope,String,int,Deadline)`，header代次来自ClaimedPublish快照，不再逐条failureCount查库。

- [ ] **Step1 — 写RED。** 可控Publisher捕获32条分4波启动，每通道≤8；两个Relay共用真实16槽位Publisher时≤16且无窗口超额UNKNOWN；一波中confirm/return/timeout混合只完成成功行，所有future共享3秒截止；第1波超时不对后续future累计每条6秒。批次20秒到期/租约余量不足时不启动新波；从未发送的集合可释放attempt，已调用即使UNKNOWN也不回退。坏payload不调用Publisher并持久核查。
关键测试方法：`publishesThirtyTwoInFourBoundedWaves`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThat(maxInFlight.get()).isLessThanOrEqualTo(8);
assertThat(waveCount.get()).isEqualTo(4);
assertThat(releasedIds).doesNotContainAnyElementsOf(attemptedIds);
```

- [ ] **Step2 — 验证RED。** 选择器`BoundedRelayV2Test,PublisherConfirmV2Test`；第一次使用可控future验证调度，再以真实Rabbit校验return/confirm。
- [ ] **Step3 — 实现。** 一批只在领取提交后解析/发布；16槽位限制尚未结束的逻辑发布尝试，UNKNOWN仍可能迟到发送/确认，必须维持原at-least-once处理。按8条窗口、共享绝对截止等待，批量settle后进入下一波。使用领取/settle返回的DB时间与单调时钟保守推算剩余租约，纳入采样不确定度；无法证明剩余预算时不再启动。局部记录仅包含确定未调用Publisher的行，关闭或超时后fenced释放；崩溃后不恢复本地记录，交给租约。批量提交不确定不统计完成、不换eventId；保留`relay.after-confirm`故障点。
- [ ] **Step4 — 验证GREEN。** 同选择器及`MessageListenerV2Test,ConsumerInboxV2Test`通过；记录实际并发、波次deadline及事务开启时Publisher拒绝调用的断言。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "feat: publish leased events in bounded waves"`。

## Task 4: 连续循环、调度隔离与生命周期接线

**Files:** Create `bm-trigger/src/main/java/com/jay/trigger/job/v2/RelayDrainWorker.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/CoalescingJobDispatcher.java`；Modify `bm-trigger/src/main/java/com/jay/trigger/job/v2/OutboxRelayJob.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/RetryPublishJob.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/DrawRecoveryJob.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/StockRecoveryJob.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/PaymentRecoveryJob.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/RecoveryScanJob.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/PaymentInboxJob.java`（同目录），`bm-app/src/main/java/com/jay/config/V2RabbitConfig.java`、`bm-app/src/main/java/com/jay/config/V2WorkerConfig.java`；Modify `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlOutboxWriter.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlRetryJobRepository.java`用于提交后唤醒；Test `bm-app/src/test/java/com/jay/test/v2/RelayLifecycleV2Test.java`。 Modify: `bm-app/src/main/java/com/jay/config/V2AssetConfig.java`, `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java`, `bm-app/src/main/java/com/jay/config/V2PaymentConfig.java`.

**Interfaces:** `RelayDrainWorker(BoundedOutboxRelay relay,RelayPolicy policy)`，`run():void,wake():void,quiesce():void`。`CoalescingJobDispatcher(ExecutorService executor)`，`submit(String key,Runnable work):boolean,quiesce():void`；false表示已合并、停止或拒绝，不代表业务完成。唤醒使用`TransactionSynchronization.afterCommit`；没有活动事务不伪造提交信号。MySqlOutboxWriter保留单DataSource构造器作NOOP兼容，新增`MySqlOutboxWriter(DataSource ds,Runnable afterCommitWake)`；实际业务配置统一注入有唤醒的writer，不能继续各自new无唤醒实例。

- [ ] **Step1 — 写RED。** 有工作时连续两个轮次不等待250ms；全空轮才等待且wake可提前结束，信号丢失仍轮询；库起点轮转，Outbox持续增长时Retry仍执行。legacy/bounded每次仅一组发布器；同key100次tick至多1个queued/running，拒绝时不能caller-runs且标记可重试。回滚不唤醒；停止后无新领取，当前批次先于预算/数据源关闭完成。
关键测试方法：`coalescesTicksAndKeepsLegacyMutuallyExclusive`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThat(maxQueuedOrRunningForDraw.get()).isEqualTo(1);
assertThat(legacyStarts.get() + boundedStarts.get()).isEqualTo(1);
```

- [ ] **Step2 — 验证RED。** 选择器`RelayLifecycleV2Test,MessagingShutdownV2Test,MessagingApplicationV2Test`。
- [ ] **Step3 — 实现。** v2RelayExecutor两个线程各运行Outbox/Retry循环，按三库完整轮次判断忙闲，不为每条事件排任务。恢复及Provider Inbox的Scheduled方法只提交同key合并任务到现有v2RecoveryExecutor；任务异常和拒绝均清理合并标记。Provider Inbox保持CALLBACK/WORKER语义。guarded开关在Task6接线后方可启用；本步不得启动占位的新恢复器。执行设计关闭顺序，所有join使用同一30秒deadline，关闭失败保留任务供接管。
- [ ] **Step4 — 验证GREEN。** 同选择器通过，启动两实例检查线程与实例模式，实际阻塞一个恢复动作时Relay仍发布；真实SIGTERM超时路径不把未确认任务标PUBLISHED，旧配置legacy仍启动。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "feat: isolate continuous relay and recovery scheduling"`。

## Task 5: 持久补发保护与在途证据契约

**Files:** Create `bm-domain/src/main/java/com/jay/domain/task/model/RedriveDecision.java`、`bm-domain/src/main/java/com/jay/domain/task/model/RedriveState.java`、`bm-domain/src/main/java/com/jay/domain/task/model/DeliveryEvidence.java`，`bm-domain/src/main/java/com/jay/domain/task/repository/RecoveryDispatchRepository.java`、`bm-domain/src/main/java/com/jay/domain/task/repository/DeliveryEvidenceRepository.java`，`bm-domain/src/main/java/com/jay/domain/task/service/RecoveryRedriveGuard.java`；Create `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlRecoveryDispatchRepository.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlDeliveryEvidenceRepository.java`；Modify `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlConsumerInbox.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlRecoveryRepository.java`（同目录）；Test `bm-app/src/test/java/com/jay/test/v2/RecoveryRedriveGuardV2Test.java`。

**Interfaces:** `RedriveDecision`枚举EMIT、FRESH、LOCAL_IN_FLIGHT、REMOTE_RETRY、OBSERVATION_WINDOW、COOLDOWN、PAUSED、UNKNOWN、EXHAUSTED、CONFLICT。`RedriveState(String type,String id,String observedState,long observedVersion,String lastEventType,String lastEventId,Instant lastRedriveAt,Instant nextRedriveAt,int count)`。`DeliveryEvidence(boolean localInFlight,Instant latestPublishedAt,boolean remoteRetryUnfinished,boolean operationCompleted,boolean blocked,boolean uncertain,boolean conflicting)`。端口方法均要求调用者已建立本库事务：`RecoveryDispatchRepository.lock(String type,String id):RedriveState`、`record(String type,String id,String state,long version,EventEnvelope event,Instant now,Duration interval):void`；`DeliveryEvidenceRepository.local(String aggregateType,String aggregateId,String eventType,String requestHash):DeliveryEvidence`、`target(EventEnvelope command):DeliveryEvidence`、`operationCompleted(String consumer,EventEnvelope event):boolean`。构造器`RecoveryRedriveGuard(Duration staleAfter,Duration interval,int maxPerStage)`初始为30秒/30秒/12；`RecoveryRedriveGuard.decide(RedriveState guard,String state,long version,Instant lastProgressAt,DeliveryEvidence evidence,Instant now):RedriveDecision`为纯判定，不能自行跨库或提交。

- [ ] **Step1 — 写RED。** DB时间29.999秒拒绝、达到30秒允许；未来PENDING、有效/过期PROCESSING均阻止补发；last published仅过29秒不补；当前DEAD阻止，历史DEAD只有“已审计关闭＋新重放映射”才排除。version变但原任务有效仍阻止。两个独立连接锁同guard，成功Outbox＋record只一次；故意回滚后count和事件都不增加；纯touch不刷新last_progress_at或冷却。Retry PUBLISHED无consumer_receipt判为REMOTE_RETRY，有匹配已提交回执允许后续响应回放，摘要冲突拒绝。
关键测试方法：`rollsBackOutboxAndCooldownTogether`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThat(eventsAfterRollback).isEqualTo(eventsBefore);
assertThat(stateAfterRollback.count()).isEqualTo(stateBefore.count());
assertThat(successfulRedrivesAcrossTwoConnections).isEqualTo(1);
```

- [ ] **Step2 — 验证RED。** 选择器`RecoveryRedriveGuardV2Test,RecoveryCaseV2Test`。
- [ ] **Step3 — 实现。** 创建/锁定007保护行，按设计7.3顺序判定，count只随新恢复事件同事务增加；阶段/版本变化仅在有真实业务进展或审计resume证明时建立新计数，重启不重置，12次耗尽交现有核查流程。本地证据不限制当前aggregate_version；目标证据查询在目标库单独短事务中读取Retry/回执/核查，异常返回UNKNOWN而非“无任务”。复用MySqlConsumerInbox原operation/businessHash算法，提取包内可复用方法，不能改已有键或把Inbox存在等同业务完成。历史DEAD的排除需读取recovery_case关闭和deferred_fact.replayed_event_id的实际关联。
- [ ] **Step4 — 验证GREEN。** 同选择器及`ConsumerInboxV2Test,MessagingReviewRegressionV2Test`通过；核对两连接竞争、提交回滚及旧版本任务证据，不能仅以进程锁fake通过。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "feat: guard redrive with durable in-flight evidence"`。

## Task 6: 统一抽奖/库存/支付恢复与固定上界扫描

**Files:** Modify `bm-domain/src/main/java/com/jay/domain/raffle/service/DrawRecoveryService.java`、`bm-domain/src/main/java/com/jay/domain/payment/service/PaymentRecoveryService.java`、`bm-domain/src/main/java/com/jay/domain/task/service/RecoveryService.java`；Modify `bm-trigger/src/main/java/com/jay/trigger/job/v2/StockRecoveryJob.java`；Modify `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlDrawRepository.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlStockRepository.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlPaymentRepository.java`（infrastructure v2目录），`bm-app/src/main/java/com/jay/config/V2RaffleConfig.java`、`bm-app/src/main/java/com/jay/config/V2PaymentConfig.java`、`bm-app/src/main/java/com/jay/config/V2RabbitConfig.java`；Create `bm-domain/src/main/java/com/jay/domain/task/model/RecoveryScanPage.java`、`bm-domain/src/main/java/com/jay/domain/task/model/RecoveryScanPosition.java`；Test `bm-app/src/test/java/com/jay/test/v2/GuardedWorkflowRecoveryV2Test.java`。

**Interfaces:** 保留`DrawRecoveryService.inspect(String order):RecoveryDecision`、`PaymentRecoveryService.inspect(String id):RecoveryDecision`，新增注入Task5仓储/guard。`RecoveryScanPosition(Instant after,String afterId,Instant upper,String upperId)`；`RecoveryScanPage(List<String> ids,RecoveryScanPosition next,boolean exhausted)`；Draw/Stock/Payment仓储新增`scanStale(String state,RecoveryScanPosition cursor,Instant cutoff,int limit):RecoveryScanPage`，在已路由事务内执行。扫描游标按库/表/状态隔离，不作为业务完成事实。首轮after=null、afterId为空；upper为空时捕获该分区当前最大合格键，此轮固定该上界，exhausted后下一轮重新捕获并从头回绕。Retry扫描复用相同契约，只把时间键换为published_at。

- [ ] **Step1 — 写RED。** 两实例Draw与Stock同时检查同一ACCEPTED，30秒窗口一条新事件；CANDIDATE_FIXED/HELD、RESULT_RECORDED/CONFIRMED、CANCELLING/无预留、COMMITTED、PAID分别按设计表发允许动作。远端超时/暂停不补，不释放HELD；取消与晚到回执不改变原候选。持续插入新行且晚提交较小键，固定上界扫完回绕能再次找到；八张用户分表都被覆盖。恢复每轮64检查/8新增，2秒后不启动新候选。legacy/guarded只能启用一个分支，通用scan仍重放deferred但不再重复扫描专用正常ACTIVE聚合。
关键测试方法：`unifiesDrawAndStockRecoveryWithoutChangingCandidate`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThat(newPrepareDrawEventsWithin30Seconds).isEqualTo(1);
assertThat(after.candidate()).isEqualTo(before.candidate());
assertThat(heldStockAfterAuthorityTimeout).isEqualTo(heldStockBefore);
```

- [ ] **Step2 — 验证RED。** 选择器`GuardedWorkflowRecoveryV2Test,RaffleFaultV2Test,PaymentRecoveryV2Test`。
- [ ] **Step3 — 实现。** Stock只发现订单并调用统一Draw入口，不在控制库独立append抽奖恢复事件。跨库事实与目标证据先在事务外取得；用户最终事务按账户/活动账户→订单→guard→任务行加锁，重验版本、停滞、状态及Task5证据后原快照生成新eventId，append与record同提交。Payment仅为PAID履约接入guard，CREATED供应商流程保持不变。按状态分段、有界、固定上界分页替换全量JOIN/COALESCE排序；失败touch不修改业务进度。12次耗尽通过RecoveryService持久暂停＋核查，不新建额度或自动取消。
- [ ] **Step4 — 验证GREEN。** 同选择器及`DrawCancellationV2Test,RaffleReviewRegressionV2Test,PaymentReviewRegressionV2Test`通过；两进程实测竞争、跨日原桶及固定候选故障仍通过，guarded缺007时启动失败而非降级绕过保护。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "feat: unify guarded workflow redrive"`。

## Task 7: 已发布Retry的原身份运输修复

**Files:** Create `bm-domain/src/main/java/com/jay/domain/task/model/PublishedRetrySnapshot.java`, `bm-domain/src/main/java/com/jay/domain/task/service/RetryTransportRecovery.java`、`bm-trigger/src/main/java/com/jay/trigger/job/v2/RetryTransportRecoveryJob.java`；Modify `bm-domain/src/main/java/com/jay/domain/task/repository/RetryJobRepository.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlRetryJobRepository.java`、`bm-app/src/main/java/com/jay/config/V2RabbitConfig.java`；Test `bm-app/src/test/java/com/jay/test/v2/RetryTransportRecoveryV2Test.java`。

**Interfaces:** `RetryJobRepository.scanPublished(RecoveryScanPosition cursor,Instant cutoff,int limit):RecoveryScanPage`；`PublishedRetrySnapshot(EventEnvelope event,String consumerName,int attempt,int failureCount,Instant publishedAt)`, `findPublished(String eventId):Optional<PublishedRetrySnapshot>`与`rearmPublished(String eventId,int expectedAttempt,int expectedFailureCount,Instant expectedPublishedAt):boolean`均在目标库当前事务内执行。`RetryTransportRecovery.inspect(RouteSlot target,String eventId):boolean`、`tick():void`；true仅表示CAS成功恢复原任务，不表示已消费。复用Task5 `operationCompleted`和暂停证据。

- [ ] **Step1 — 写RED。** PUBLISHED未满30秒不修复；满30秒且无业务回执，两个连接最多一个PUBLISHED→PENDING，eventId/payload/failure_count/attempt不变；下一次真实发送attempt才增加。回执已完成或暂停不重投；检查后消费提交只产生运输重复、不产生第二份权益；expectedPublishedAt/generation变化CAS失败。12次发布上限转持久核查，不能尝试第13次或回退attempt。晚提交低键在固定上界回绕后找到。
关键测试方法：`rearmsOriginalRetryWithItsFailureGeneration`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThat(after.event().eventId()).isEqualTo(before.event().eventId());
assertThat(after.failureCount()).isEqualTo(before.failureCount());
assertThat(after.attempt()).isEqualTo(before.attempt());
```

- [ ] **Step2 — 验证RED。** 选择器`RetryTransportRecoveryV2Test,ConsumerInboxV2Test,MessageListenerV2Test`。
- [ ] **Step3 — 实现。** 仅在guarded模式调度，复用恢复执行器/64检查、8修复、2秒启动预算；published_recovery索引分页。先按原业务锁顺序核对聚合允许自动执行，再以原consumer/operation键校验回执和摘要，CAS原行恢复为PENDING、next_attempt_at=DB_NOW、清空租约；发布后published_at重新启动观察窗口。达到attempt上限走单聚合DEAD/核查事务。绝不从用户事务跨库写Retry，绝不生成新事件或failure_count=0。
- [ ] **Step4 — 验证GREEN。** 同选择器通过；真实Rabbit发送header仍是原failure_count，重复消息被Inbox吸收；旧批次token/旧generation回写不能覆盖修复后的任务。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "feat: recover published retries without resetting identity"`。

## Task 8: 分段观测、故障证据与切换回滚

**Files:** Create `bm-domain/src/main/java/com/jay/domain/task/service/RelayObservation.java`、`bm-infrastructure/src/main/java/com/jay/infrastructure/event/MicrometerRelayObservation.java`；Modify `bm-domain/src/main/java/com/jay/domain/task/service/BoundedOutboxRelay.java`、Task5/6/7恢复服务及`bm-app/src/main/java/com/jay/config/V2MetricsConfig.java`；Modify `bm-trigger/src/main/java/com/jay/trigger/http/v2/DrawController.java`、`bm-trigger/src/main/java/com/jay/trigger/http/v2/CreditExchangeController.java`、`bm-domain/src/main/java/com/jay/domain/raffle/service/DrawService.java`、`bm-domain/src/main/java/com/jay/domain/exchange/service/ExchangeService.java`用于HTTP阶段观察；Create `docs/performance/fault_relay_optimization.py`、`docs/operations/v2-relay-optimization-runbook.md`；Test `bm-app/src/test/java/com/jay/test/v2/RelayOptimizationFaultV2Test.java`、`bm-app/src/test/java/com/jay/test/v2/RelayObservationV2Test.java`。 Modify: `bm-app/src/main/java/com/jay/config/V2RabbitConfig.java`, `bm-app/src/main/java/com/jay/config/V2RaffleConfig.java`, `bm-app/src/main/java/com/jay/config/V2PaymentConfig.java`.

**Interfaces:** `RelayObservation.duration(String stage,RouteSlot slot,String eventType,String outcome,long nanos):void`、`count(String action,RouteSlot slot,String eventType,String reason,long n):void`，提供NOOP；stage/action/reason必须枚举有限白名单。BoundedOutboxRelay新增带RelayObservation的构造器，保留Task3四参数构造器委托NOOP，旧调用不能因观测接入改变行为。新故障脚本CLI：`--env v2-it --seed PATH --out PATH`，先校验本次专属容器/目标库，只停止自己启动的应用或依赖。

- [ ] **Step1 — 写RED。** 相同order/event不产生指标标签；claim/confirm/settle/permit/HTTP准入及事务可区分，UNKNOWN不记完成。真实confirm后SIGKILL、MySQL完成事务连接KILL、Rabbit中断、旧token晚写、暂停后晚回执均有前后SQL和原身份。未全停旧恢复器时runbook precheck拒绝guarded；Relay退回legacy而guarded保留时仍能收敛。
关键测试方法：`replaysOriginalIdentityAfterConfirmCrash`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```java
assertThat(replayedEventId).isEqualTo(originalEventId);
assertThat(awardLedgerRowsForOriginalBusinessId).isEqualTo(1);
assertThat(sixScopeDifferences).isZero();
```

- [ ] **Step2 — 验证RED。** 选择器`RelayObservationV2Test,RelayOptimizationFaultV2Test`；故障脚本不存在/边界错误应失败，不能只用抛异常代替SIGKILL。
- [ ] **Step3 — 实现。** 覆盖设计10.1全部有限标签指标，orderId/eventId仅进结构化抽样日志，不改变事件hash/schema。HTTP准入、配置读取、DB许可及资产事务分别计时。脚本保留原bizId、阶段时间、窗口/模式/镜像hash，连接KILL与进程崩溃分别记录。runbook列环境、schema、全实例旧写者、互斥、配置、共同关闭deadline、停止条件和回退点；目标明确为隔离环境，真实发布需另行授权。
- [ ] **Step4 — 验证GREEN。** 聚焦测试及真实双实例故障脚本通过；故障结束停写后以现有reconcile_v2_run检查六域、未终态/核查/任务，输出原始路径与hash。所有模式切换/回滚过程保留dispatch、attempt与账务事实；连接失败不能生成伪成功报告。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "test: prove relay faults and guarded rollback"`。

## Task 9: 修正用户分布并补齐统计边界

**Files:** Modify `docs/performance/v2-load.js`、`docs/performance/v2-load-config.json`、`docs/performance/run_v2_experiment.py`、`docs/performance/collect_v2_results.py`、`docs/performance/sample_v2_runtime.py`、`docs/performance/export_v2_stages.py`、`docs/performance/seed_v2_testdata.py`、`docs/performance/README.md`（同目录）；Create `docs/performance/test_relay_optimization.py`；Modify `docs/dev-ops/v2-verify.sh`、`docs/dev-ops/verify_v2_reports.py`；Test新增`bm-app/src/test/java/com/jay/test/v2/RelayCapacityEvidenceV2Test.java`。

**Interfaces:** runner新增`--user-distribution legacy|uniform`并转发`USER_DISTRIBUTION`，默认legacy维持旧对照；hot-user转发HOT_USER_CONCURRENCY=2/8/32。结果新增`firstPrepareCreatedToPublishedMs,relayByTypeMs,serverDrawConfirmationLatencyMs,serverDrawDeliveryLatencyMs,eventAmplification,redriveEmitted,redriveSuppressed,userCountsByOperation,requestsByDatabase`；旧计数/四域/时钟/三轮门禁保留。所有JSON字段的单位写入README。seed新增`--migration-through 006|007`，默认007，仅对新的隔离库生效，用于原版本及DDL写放大对照。

- [ ] **Step1 — 写RED。** 200个身份下legacy只有20个抽奖用户，uniform每类连续200次请求覆盖200个用户且每10次业务仍7/1/1/1；k6多VU不依赖本地可变计数。runner实际传8/32并保留默认2。构造202无终态、503有/无原回执、最后排空但负载期增长、单轮伪填repeats=3、PAYMENT/SIGNIN零人口，均不能认定完整容量通过；重复运输与新增恢复事件分开计数。
关键测试方法：`test_uniform_distribution_and_incomplete_evidence_fail_closed`；核心断言如下，变量由该方法的上述fixture或实际探针创建，不是生产接口：

```python
self.assertEqual(200, measured_uniform_draw_users)
self.assertEqual(20, measured_legacy_draw_users)
self.assertFalse(incomplete_report["safeCapacityCandidate"])
```

- [ ] **Step2 — 验证RED。** `python3 -m unittest discover -s docs/performance -p test_relay_optimization.py`；Java选择器`RelayCapacityEvidenceV2Test,CapacityEvidenceV2Test`。新增Python测试必须实际调用k6探针核验分配公式，不能仅在Python复制公式后自证通过。
- [ ] **Step3 — 实现。** mixed使用全局iteration计算cycle=iteration/10、slot=iteration%10；query序号=cycle×7+slot，result/exchange/draw序号=cycle，分别用固定offset 0/53/101/149再模用户数，不用各VU的局部counter。其他场景保留其用户语义。原始JSONL仍无token。warmup读写60秒与正式目录分离；记录操作用户数和分库，预置订单不入正式分母。基于同DB时间分别导出确认（用户订单COMMITTED/confirmed_at）/交付（DELIVERED）/Relay区间；firstPrepare仅取本轮实际受理抽奖的首条原事件，不把额外补发、预置订单混入其分位数，全部类型另报relayByTypeMs。客户端观察需要跑前校准；正常六事件放大率单独计算。v2-verify迁移上限改007并实际检查schema，校验新测试计数；seed/migration scope=all支持所有隔离库，不能hardcode三个旧库后声称新实验已迁移。
- [ ] **Step4 — 验证GREEN。** 全部`python3 -m unittest discover -s docs/performance -p 'test_*.py'`及聚焦Java通过；执行两个短小真实k6探针核对用户分布和hot-user参数，这些探针只验证工具，不认定容量。六域+四域+时钟+三轮缺一仍NO-GO。
- [ ] **Step5 — 提交。** 仅暂存本任务Files，`git commit -m "perf: measure relay stages and independent user distribution"`。

## Task 10: 单变量实验、完整门禁与退出裁定

**Files:** Create `docs/verification/2026-10-16-relay-optimization/README.md`、`docs/verification/2026-10-16-relay-optimization/ablation-matrix.md`、`docs/verification/2026-10-16-relay-optimization/capacity-report.md`（JSON证据由实际执行生成）；Modify `docs/operations/v2-release-checklist.md`、`docs/superpowers/plans/2026-10-15-phase-6-validation-rollout.md`、`docs/superpowers/plans/2026-10-15-credit-payment-raffle-implementation-index.md`（后两者同plans目录）。

**Interfaces:** 复用`v2-verify.sh --env v2-it`、`run_v2_experiment.py --seed PATH --out PATH --rate N --duration 600 --scenario mixed --run-id ID --user-distribution legacy|uniform`、故障脚本、独立tracker和六域对账。结果中releaseAuthorized保持false；容量通过与发布授权分别裁定。

- [ ] **Step1 — 建立失败对照。** 原始基线使用ad83092对应运行包与006结构，最终实现和消融使用相同007结构。新隔离数据复制原合成数据规模和历史行数；固定双实例各1CPU/512MiB、Xmx320m、持久化/副本、许可、用户分配和镜像。复测未优化配置，若不能重现则记录差异并重新定位，不引用旧95秒作为本轮A/B起点。启动前读写暖机60秒且待发排空，保存开跑前六域与时钟证据。
- [ ] **Step2 — 执行可归因实验。** 最终完整配置作为共同对照，逐次只关闭一项优化：①诊断补丁在非空轮后也等待250ms；②临时BatchOutboxRepository适配旧claim接口，保持相同批次/窗口及其他配置；③临时settle适配器逐条调用旧complete/retry；④guarded→legacy（只在全新隔离库切换且停止全部旧决策）；⑤类型查询只把FORCE pending_type改回pending。适配器必须保留原任务身份、generation和fencing，先通过聚焦正确性测试再压测。只在仓库外一次性诊断checkout制作这些补丁，保存完整patch/源hash/镜像hash，不把实验开关或适配器合入产品。每对只有一个关闭项，不将所有关闭项相互比较后伪称单变量；另用原ad83092运行包及006数据集复测原始未优化基线，007单独对照记录DDL写放大。索引Explain可先做副本探针，端到端必须同数据/资源。不得为消融而在真实环境混跑旧/新恢复器；只用新的隔离队列/库。运行失败立即停止升压并保留拒绝、UNKNOWN、积压和恢复证据。
- [ ] **Step3 — 执行正确性完整门禁。** 停本次目标库写者，设置`V2_RECONCILIATION_SEED`、`V2_RECONCILIATION_STOPPED=1`、`V2_EVIDENCE_DIR`后运行`docs/dev-ops/v2-verify.sh --env v2-it`。要求目标测试实际执行、零skip、六域检查真实完成、未解释差异0；旧演示fork timeout仍单列，不能改写成全套绿灯。证据包含迁移007及二进制hash。
- [ ] **Step4 — 执行容量与恢复验收。** 在legacy用户分配和uniform场景分别按100/s、600秒、三轮运行；不得复制一次结果冒充三轮。查询P99≤200ms、受理≤500ms、确认及交付≤5s，技术失败/入口过载拒绝/dropped iterations=0，四域负载期无持续增长，正常固定CREDIT无耗尽场景事件放大率1.00且自动新增恢复事件0，六域差异0。持续到达下实际故障恢复处理速率须高于新增，不能用停写排空代替。首档不通过即明确NO-GO；首档通过才按Phase6继续300/600/1000、热点2/8/32、签到/支付/版本切换与各自三轮要求。独立压测机、权限审计或其他原退出条件未具备时，仍分别记录未完成，不能宣布生产安全容量。
- [ ] **Step5 — 保存裁定并提交。** 报告列每项通过/失败/未执行、源版本/镜像/配置、SQL/故障原身份、raw hash、时钟、拒绝逐请求解析及共享硬件限制；更新Phase6只能勾选真实已满足项。关闭本次拥有的隔离服务，保留账务及恢复证据。`git diff --check`，只提交本任务证据摘要和清单：`git commit -m "perf: record relay optimization validation and capacity verdict"`。不得因预算或时间不足把目标标完成。

## 设计覆盖与最终交付

| 设计要求 | 计划归属 |
|---|---|
| 资源预算、SQL/网络超时、不可变配置 | 1、3、4 |
| 批量领取、公平/过期配额、坏行、fencing及Retry单聚合失败 | 2、3 |
| 两通道连续处理、提交后合并唤醒、Provider Inbox隔离、关闭 | 4 |
| 007结构、索引、旧迁移/legacy兼容 | 2、5、7、10 |
| 停滞/冷却/在途/历史DEAD、原业务响应、人工恢复 | 5、6 |
| 唯一Draw/Stock入口、PAID恢复、全库表固定上界游标 | 6 |
| Retry PUBLISHED运输修复、原eventId/generation、耗尽 | 7 |
| HTTP/Relay/DB分段观察、真实崩溃、切换/回滚 | 8 |
| 旧/均匀用户分布、完整统计及参数、真实矩阵 | 9、10 |

代码与工具交付完成不等于容量通过；Task10保留未达标即NO-GO的合法终态，完整Phase6退出条件仍按实际证据判断。每项未完成必须留下可恢复的配置、检查点和下一验证动作。本文件完成时所有执行checkbox均保持未勾选。
