# BroMark 阶段5 沙盒支付闭环 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付可查单、可重复通知、跨库幂等到账的沙盒支付流程。

**Architecture:** 支付事实留db00，用户资产留用户分片；下单/查单均在事务外，通过稳定商户号恢复未知结果。用户分库写积分与CreditGranted回执Outbox，支付域确认原回执后才DELIVERED。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MyBatis、MySQL 8.0 InnoDB、Redis、RabbitMQ、JUnit 5＋Vintage（兼容JUnit 4）、Maven。

**Spec:** [高并发积分支付与抽奖设计](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)。

## Global Constraints

- 阶段0完成后编译、测试、应用与容器统一Java 25，release=25；使用Boot 3.5.16及Jakarta体系，保留javax.sql等JDK API。
- 执行环境采用 MySQL 8.0 InnoDB，业务短事务使用 READ COMMITTED，关键读取显式加锁。
- 业务时区固定为 Asia/Shanghai。
- 首版积分使用 BigDecimal 与数据库定点小数，拒绝超过两位的小数或溢出，不使用 float、double，不在写入时静默舍入。
- 首版次数 SKU 不限库存，积分兑换在同一用户分库事务内完成扣积分与增加次数；签到奖励统一使用新资产内核。
- 首版奖品只支持积分与无权益结果；Redis 不保存唯一余额或库存事实，账户事务不调用 MQ、支付渠道或远程发货。
- 本计划已完成开发与隔离验收；应用130项（支付新增32项）、Router28项、迁移4项通过。范围及遗留小项见[验收报告](../../verification/2026-10-16-sandbox-payment/README.md)。
- 路径相对 BroMark 仓库根目录；先读[实施总览](2026-10-15-credit-payment-raffle-implementation-index.md)的共享契约、测试环境和迁移规则。依赖：[阶段3](2026-10-15-phase-3-reliable-messaging.md)与[资产内核](2026-10-15-phase-1-2-assets-signin-exchange.md)；默认在阶段4后执行。

## Review Focus

- 下单超时但渠道已建单：5.1按原商户号查询。
- 不同事件描述同一付款：5.2付款身份去重。
- 撤销先到或通知币种/金额不符：5.2不发积分。
- 到账后回执丢失：5.3新eventId重述返回原回执。
- 晚撤销与测试适配器误入生产：5.4保留账务事实并拒绝配置。

## File Structure

domain/payment定义渠道契约和支付状态，infrastructure/gateway/payment只实现测试适配器；db00 mapper与用户asset mapper保持独立连接。005所有支付表落控制库，GrantCredit路由必须验证用户槽。

## Tasks

### Task 5.1: 沙盒渠道契约与幂等支付下单

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/payment/model/PaymentCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/model/PaymentView.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/model/ProviderOrder.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/model/VerifiedPaymentEvent.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/model/ProviderPayment.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/model/NotificationAck.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/adapter/PaymentProvider.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/repository/PaymentRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/gateway/payment/SandboxPaymentProvider.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlPaymentRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/PaymentOrderDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/payment_order_mapper.xml`
- Create: `docs/dev-ops/mysql/migrations/005_payment.sql`
- Create: `bm-app/src/main/java/com/jay/config/V2PaymentConfig.java`
- Modify: `docs/dev-ops/mysql/migrations/manifest.json`
- Test: `bm-app/src/test/java/com/jay/test/v2/PaymentCreationV2Test.java`

**Interfaces:**
- Consumes: 3.1—3.4公共消息；0.2控制库事务；0.4身份。
- Produces: PaymentService.create(PaymentCommand):PaymentView；Command(userId,requestId,productId,provider,deadline)；PaymentProvider.create(String orderId,BigDecimal amount,String currency):ProviderOrder、query(String merchantOrderId):ProviderPayment、verify(byte[],Map<String,String>):VerifiedPaymentEvent。PaymentView含orderId、state、providerCreateStatus、recoveryStatus、渠道参数及到账回执；ProviderOrder含merchantOrderId/创建状态/参数；ProviderPayment含已验证付款事实或UNKNOWN；NotificationAck含渠道HTTP状态和响应原文。

- [x] **Step 1 — 写失败测试。** 在 `PaymentCreationV2Test` 中同键重复下单、渠道超时但已创建、回调先于下单回执，且同键更换商品，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(firstOrderId, retried.getOrderId());
assertEquals(1L, sandboxOrdersForMerchantId);
assertEquals("UNKNOWN", unknownView.getProviderCreateStatus());
assertEquals("PAID", stateAfterLateCreateReceipt);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PaymentCreationV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 005在db00新建支付订单、provider_inbox、provider_payment_fact；支付订单冻结商品金额币种、积分权益、用户与渠道环境；商品为已发布服务端配置。本地CREATED及请求摘要先提交，再事务外调用provider.create，商户号始终paymentOrderId。provider_create_status PENDING/READY/UNKNOWN/CLOSED单独于CREATED/PAID/DELIVERED；超时按原号查询，不换单。订单号固定控制域槽，PAID不能被迟到下单回执覆盖。沙盒只允许test/local-sandbox profile，prod启用即启动失败；凭据环境变量，不实现真实渠道SDK。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。沙盒契约实现四种控制：返回超时但已建单、先回调后响应、重复通知、查单未知；已有原回执不依赖当前商品价格。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: create idempotent sandbox payment orders"`；不提交本地配置、凭据或原始用户数据。

### Task 5.2: 通知验签、付款身份与付款确认

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentNotificationService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentFactService.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/ProviderInboxDao.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/PaymentFactDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/provider_inbox_mapper.xml`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/payment_fact_mapper.xml`
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/PaymentNotificationController.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/PaymentInboxJob.java`
- Modify: `docs/dev-ops/mysql/migrations/005_payment.sql`
- Test: `bm-app/src/test/java/com/jay/test/v2/PaymentFactV2Test.java`

**Interfaces:**
- Consumes: 5.1渠道验证；3.1租约；1.4Outbox。
- Produces: PaymentNotificationService.receive(String provider,byte[] raw,Map<String,String> headers):NotificationAck；PaymentFactService.apply(VerifiedPaymentEvent):void。VerifiedEvent(provider,providerAccount,environment,eventId,transactionId,merchantOrderId,type,amount,currency,userBinding,canonicalHash)。

- [x] **Step 1 — 写失败测试。** 在 `PaymentFactV2Test` 中同付款不同eventId、相同eventId重复但投递时间变化、金额币种冲突、撤销早于付款和持久化失败，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, paymentFactRowsForTransaction);
assertEquals(1L, grantCreditCommandsForOrder);
assertEquals(0L, grantCommandsAfterEarlyReversal);
assertFalse(acknowledgedBeforeInboxCommit);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PaymentFactV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 入口保留原始字节验签，校验providerAccount/environment与事件类型，限制64KiB；规范化业务摘要不含签名时间等投递元数据。Inbox以provider/account/environment/eventId唯一，verified payload与摘要提交后才按渠道契约Ack，重复内容冲突建核查单。Worker先租约认领，查单在事务外；事务先唯一建付款身份占位并锁，再锁订单，核验商户号、用户、金额币种与订单快照。付款事实、CREATED→PAID、paid_at和GrantCredit Outbox同事务。付款身份以provider/account/environment/transactionId唯一；撤销先到也有可锁的持久身份及reversed事实，禁止之后无条件生成GrantCredit。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。同一transactionId试图绑定另一订单、错误签名/环境、超大载荷、错币种都不得发积分；渠道Ack格式由沙盒契约测试固定。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: persist verified notifications and payment facts"`；不提交本地配置、凭据或原始用户数据。

### Task 5.3: 跨库积分履约与原回执反馈

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentCreditService.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/GrantCreditListener.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/listener/v2/CreditGrantedListener.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentFactService.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlPaymentRepository.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/PaymentDeliveryV2Test.java`

**Interfaces:**
- Consumes: 5.2 PAID+GrantCredit；1.2信用内核；3.3消费事务。
- Produces: PaymentCreditService.grant(EventEnvelope):AssetReceipt；PaymentFactService.onCreditGranted(EventEnvelope):void；GrantCredit/CreditGranted均携带paymentOrderId、userId、routeSlot、benefitSnapshot、requestHash、原积分回执。

- [x] **Step 1 — 写失败测试。** 在 `PaymentDeliveryV2Test` 中同GrantCredit事件重投、新eventId恢复重述、到账提交后响应未发布、回执重复，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, creditLedgerRowsForCharge);
assertEquals(expectedCredit, committedCreditDelta);
assertEquals("DELIVERED", paymentView.getState());
assertEquals(originalReceipt, regeneratedResponseReceipt);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PaymentDeliveryV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 用户分库按charge:{paymentOrderId}及冻结权益摘要幂等credit，积分流水+消费Inbox+CreditGranted Outbox同事务，提交后Ack。完成业务收到新eventId仍写原回执响应。db00消费回执核对订单归属、权益摘要、版本与金额，仅PAID→DELIVERED并保存delivered_at；只凭通知收取、Outbox发送或MQ Ack不能delivered。在delivery事务外查询权威事实仅供恢复，不依赖缓存余额。后到撤销若GrantCredit已提交或执行，只进入复核，不声称能追回已发命令或自动扣回积分。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测用户库中断、源订单暂停时回执持久化、routeSlot伪造、新eventId但金额变化核查；不能重复入账。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: fulfill paid credits with durable receipts"`；不提交本地配置、凭据或原始用户数据。

### Task 5.4: 查单恢复、用户查询与支付对账

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentRecoveryService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentReconciliationService.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/job/v2/PaymentRecoveryJob.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/PaymentOrderController.java`
- Create: `bm-api/src/main/java/com/jay/api/v2/PaymentOrderRequest.java`
- Create: `docs/operations/v2-payment-reconciliation.sql`
- Create: `docs/operations/v2-payment-review-runbook.md`
- Modify: `bm-domain/src/main/java/com/jay/domain/payment/service/PaymentService.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/payment/repository/PaymentRepository.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlPaymentRepository.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/PaymentRecoveryV2Test.java`

**Interfaces:**
- Consumes: 5.1—5.3服务；3.4RecoveryService；0.4身份。
- Produces: PaymentService.get(String userId,String orderId):PaymentView；PaymentRecoveryService.inspect(String orderId):RecoveryDecision；PaymentReconciliationService.check():ReconciliationReport，使用4.5定义的类型并标记PAYMENT检查范围；PaymentRepository实现3.4 RecoveryAggregatePort注册PAYMENT；设计的payment/orders POST/GET。

- [x] **Step 1 — 写失败测试。** 在 `PaymentRecoveryV2Test` 中CREATED长期未知、PAID未履约、DELIVERED后撤销、第三人查询，以及test adapter进入prod，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(originalPaymentId, recovered.getOrderId());
assertEquals("DELIVERED", afterReplay.getState());
assertEquals(beforeReversalBalance, afterLateReversalBalance);
assertEquals(403, crossUserQueryStatus);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PaymentRecoveryV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 按state/recovery_status/last_progress_at游标扫描，CREATED查原商户号并进入统一付款事实内核，PAID重放原GrantCredit新eventId；DEAD或REVIEW不自动扫描，禁止本地超时改失败。查询分别给付款状态、渠道创建状态、到账与核查状态，不泄露其他用户。对账按已付款身份与唯一charge流水/回执核验，撤销单独复核；无回执不直接覆写delivered。runbook列出错额、错归属、早撤销与晚撤销证据及恢复条件，无自动退款。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。完整下单→通知→发积分→回执→delivered运行在隔离真实MySQL/MQ；记录paid到delivered各阶段耗时。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: recover and reconcile sandbox payments"`；不提交本地配置、凭据或原始用户数据。

## 阶段退出条件

沙盒端到端、重复乱序及未知结果测试通过；每次到账唯一且能解释为原PAID订单，退款撤销留下复核依据。此阶段不授权真实支付渠道上线。

## 执行状态

已在 `feat/phase5-sandbox-payment` 完成5.1–5.4，代码验收提交 `ffc5967`。独立审查5项Important及提升等级的原文摘要问题已修复，保留1项Minor输入错误码映射；历史测试已知等待问题单独记录。见[验收报告](../../verification/2026-10-16-sandbox-payment/README.md)及[决策记录](../../verification/2026-10-16-sandbox-payment/rulings.md)。未授权或执行真实支付上线。
