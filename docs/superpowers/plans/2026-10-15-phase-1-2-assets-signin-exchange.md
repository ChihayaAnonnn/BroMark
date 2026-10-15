# BroMark 阶段1—2 资产签到兑换 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付有审计流水和稳定回执的资产内核与同库原子签到、兑换。

**Architecture:** 新增asset内核与exchange/sign-in服务，现有大仓储不整体重写。业务在同一用户事务组合资产和订单仓储，配置提前读取，v2写门隔离旧写。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MyBatis、MySQL 8.0 InnoDB、Redis、RabbitMQ、JUnit 5＋Vintage（兼容JUnit 4）、Maven。

**Spec:** [高并发积分支付与抽奖设计](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)。

## Global Constraints

- 阶段0完成后编译、测试、应用与容器统一Java 25，release=25；使用Boot 3.5.16及Jakarta体系，保留javax.sql等JDK API。
- 执行环境采用 MySQL 8.0 InnoDB，业务短事务使用 READ COMMITTED，关键读取显式加锁。
- 业务时区固定为 Asia/Shanghai。
- 首版积分使用 BigDecimal 与数据库定点小数，拒绝超过两位的小数或溢出，不使用 float、double，不在写入时静默舍入。
- 首版次数 SKU 不限库存，积分兑换在同一用户分库事务内完成扣积分与增加次数；签到奖励统一使用新资产内核。
- 首版奖品只支持积分与无权益结果；Redis 不保存唯一余额或库存事实，账户事务不调用 MQ、支付渠道或远程发货。
- 本文所有代码、SQL、测试和命令都是待执行任务；当前交付只包含实施文档，不代表实现完成。
- 路径相对 BroMark 仓库根目录；先读[实施总览](2026-10-15-credit-payment-raffle-implementation-index.md)的共享契约、测试环境和迁移规则。依赖：[阶段0全部任务](2026-10-15-phase-0-foundation.md)。

## Review Focus

- 金额1.001、负数、溢出或同键换商品：1.2、2.1拒绝。
- 首次并发建账建桶：1.1、1.3约束与锁收敛。
- 午夜重试与原周期退款：1.3、2.2固定日期。
- 扣款后增次或签到第二项失败：2.1、2.2整单回滚。
- 旧消费者和跨用户查单：2.3阻断绕写与越权。

## File Structure

domain/asset管理余额、周期与身份；exchange及签到只编排事务。v2 DAO和mapper独立，001/002兼容新增，006最终收紧约束。

## Tasks

### Task 1.1: 资产迁移、审计期初与执行器

**Files:**
- Create: `docs/dev-ops/mysql/migrations/001_asset_additive.sql`
- Create: `docs/dev-ops/mysql/migrations/manifest.json`
- Create: `docs/dev-ops/mysql/apply_migrations.py`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/AssetAccountDao.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/QuotaAccountDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/asset_account_mapper.xml`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/quota_account_mapper.xml`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/po/AssetAccountRow.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/po/QuotaAccountRow.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/AssetSchemaV2Test.java`

**Interfaces:**
- Consumes: 0.2隔离数据库与全部分片列表。
- Produces: 账户、周期桶、两类流水、用户请求回执；AssetAccountDao.lock(String):AssetAccountRow、QuotaAccountDao.lock(String,long):QuotaAccountRow。

- [ ] **Step 1 — 写失败测试。** 在 `AssetSchemaV2Test` 中全新库应用001，另准备重复账户及期初冲突的升级样本，以JdbcTemplate检查约束，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1, accountRowsForUser);
assertEquals(new BigDecimal("100.00"), openingBalance);
assertThrows(DataIntegrityViolationException.class, negativeBalanceWrite);
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AssetSchemaV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 对现有账户和周期表只新增字段，新建流水和回执，不自动SUM重复余额，不覆盖旧available_amount。保存opening_balance及总次数/周期额度/计数的审计来源。001同时包含db00号段表id_block。manifest区分全新建库与存量升级：全新表直接带目标唯一/非负约束，升级只兼容新增，旧数据约束最终收紧归006。测试全新库检查完整约束，升级重复样本只生成拒绝切换的precheck证据，不提前依赖006。执行器提供--env v2-it --through 001 --dry-run与执行模式，保存checksum及逐脚本进度；失败DDL不DROP回滚。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。执行器必须拒绝无测试环境白名单的实际执行；生产仅生成清单。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: add audited asset schema and migration runner"`；不提交本地配置、凭据或原始用户数据。

### Task 1.2: 请求身份、金额与积分内核

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/RequestIdentity.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/BusinessId.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/AssetReceipt.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/RequestHasher.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/OrderIdGenerator.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/id/BlockOrderIdGenerator.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/IdBlockDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/id_block_mapper.xml`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/AssetKernel.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/repository/AssetRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlAssetRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/CreditLedgerDao.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/RequestReceiptDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/credit_ledger_mapper.xml`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/request_receipt_mapper.xml`
- Modify: `bm-domain/src/main/java/com/jay/domain/asset/service/RouteCodec.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/asset/model/AssetConflictException.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/CreditKernelV2Test.java`

**Interfaces:**
- Consumes: 1.1结构；0.2事务及数据库时钟。
- Produces: AssetKernel.lockAccounts(String,List<Long>):BusinessTime、credit(String,BusinessId,BigDecimal):AssetReceipt、debit(String,BusinessId,BigDecimal):Optional<AssetReceipt>；RequestHasher.hash(String,Map<String,String>):String；RouteCodec.newOrderId(String,String):String、newEventId():String；OrderIdGenerator.next(String,RouteSlot):String、prefetch():void。

- [ ] **Step 1 — 写失败测试。** 在 `CreditKernelV2Test` 中建立100.00余额，两笔不同业务并发消费80.00，同一充值业务并发100次，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, receipts.stream().filter(Optional::isPresent).count());
assertEquals(new BigDecimal("20.00"), balanceAfterTwoDebits);
assertEquals(1L, committedLedgerCountForBiz);
assertThrows(AssetConflictException.class, changedHashCall);
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CreditKernelV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 金额只接受正值、最多两位及DECIMAL(18,2)范围；不足返回Optional.empty，技术异常抛出。内核必须在现存用户事务中使用；唯一键建零余额锚点并锁积分账户，再排序锁活动账户，之后才锁请求/订单/桶。唯一流水保存delta与balance_after，重复返回原回执。摘要固定字段顺序、版本和小数格式。订单/事件ID按总览的持久号段方案：控制库锁id_block原子分配1000个序号，确认提交后本地发号，号段不复用，不在用户事务内补号段；生成与解码共享固定槽映射。未确认提交、耗尽或溢出先技术失败，不猜测号码是否可用。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。再测首次建账、账户禁用、1.001、负数、零与溢出；原回执不依赖当前配置。两发号器并发、宕机废弃余号段和分配提交未知均不得重复ID，编码槽必须等于用户路由槽。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: implement idempotent credit transactions"`；不提交本地配置、凭据或原始用户数据。

### Task 1.3: 周期额度增发、消费与原桶退还

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/QuotaDelta.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/QuotaReceipt.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/QuotaKernel.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/QuotaLedgerDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/quota_ledger_mapper.xml`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlAssetRepository.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/QuotaKernelV2Test.java`

**Interfaces:**
- Consumes: 1.2锁顺序与BusinessId；1.1周期桶。
- Produces: QuotaKernel.grant(String,long,BusinessId,QuotaDelta,BusinessTime):QuotaReceipt；consume(String,long,BusinessId,BusinessTime):Optional<QuotaReceipt>；refund(String,long,BusinessId,BusinessTime):QuotaReceipt。

- [ ] **Step 1 — 写失败测试。** 在 `QuotaKernelV2Test` 中建立总剩余10、日上限2已用1、月上限5已用2，再增额(5,1,2)，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(15L, afterGrant.getTotalRemaining());
assertEquals(2L, afterGrant.getDayRemaining());
assertEquals(5L, afterGrant.getMonthRemaining());
assertEquals(3L, nextDay.getDayRemaining());
assertEquals(7L, nextMonth.getMonthRemaining());
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=QuotaKernelV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 非负long增量检查累加溢出；按增额前上限建桶，再累加账户上限和当前桶剩余。consume总/月/日remaining>0条件扣减，检查影响行数，任一步失败全回滚；增加总与当日effective_draw_count。refund核验原消费身份，以唯一退款流水退总次数和原桶、减少有效计数，不改新桶或购买上限；原事实不匹配抛核查异常，由阶段3处理。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。覆盖首次增额建桶、两个实例建桶、午夜/跨月、同取消100次和总额足够但日月不足；用可控时钟与栅栏。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: enforce quota periods and idempotent refunds"`；不提交本地配置、凭据或原始用户数据。

### Task 1.4: 事务内Outbox写入

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/task/model/EventEnvelope.java`
- Create: `bm-domain/src/main/java/com/jay/domain/task/repository/OutboxWriter.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlOutboxWriter.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/OutboxDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/outbox_mapper.xml`
- Create: `docs/dev-ops/mysql/migrations/001_outbox_base.sql`
- Modify: `docs/dev-ops/mysql/migrations/manifest.json`
- Test: `bm-app/src/test/java/com/jay/test/v2/OutboxAtomicV2Test.java`

**Interfaces:**
- Consumes: 1.1迁移器；1.2资产。
- Produces: 总览EventEnvelope；OutboxWriter.append(EventEnvelope):void，仅允许已开启的业务事务，写当前连接所在库Outbox，不能根据目的routeSlot切库。

- [ ] **Step 1 — 写失败测试。** 在 `OutboxAtomicV2Test` 中同时写资产与Outbox，在提交前注入异常；另验证成功提交但尚未发布，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(0L, rolledBackLedgerRows);
assertEquals(0L, rolledBackOutboxRows);
assertEquals(1L, committedPendingRows);
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxAtomicV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 001_outbox_base随001执行，每库一张；event_id主键，待发送与租约过期索引。持久化完整权益快照、稳定bizId与schemaVersion=1，重复eventId不同payload冲突。同事务写事件，禁止业务直接RabbitTemplate发送；日志只记摘要。此任务不实现发布器。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: persist business events atomically"`；不提交本地配置、凭据或原始用户数据。

### Task 2.1: 准入配置与原子兑换

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/AdmissionSnapshot.java`
- Create: `bm-domain/src/main/java/com/jay/domain/exchange/model/ExchangeCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/exchange/model/ExchangeResult.java`
- Create: `bm-domain/src/main/java/com/jay/domain/exchange/service/ExchangeService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/exchange/repository/ExchangeRepository.java`
- Create: `bm-domain/src/main/java/com/jay/domain/activity/service/AdmissionService.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlExchangeRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlAdmissionRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/ExchangeOrderDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/exchange_order_mapper.xml`
- Create: `docs/dev-ops/mysql/migrations/002_signin_exchange.sql`
- Modify: `docs/dev-ops/mysql/migrations/manifest.json`
- Test: `bm-app/src/test/java/com/jay/test/v2/ExchangeAtomicV2Test.java`

**Interfaces:**
- Consumes: 1.2—1.4资产与Outbox。
- Produces: AdmissionService.forExchange(String,long,String,Deadline):AdmissionSnapshot；ExchangeService.exchange(ExchangeCommand):ExchangeResult；Command(userId,requestId,sku,configVersion,deadline)，Result(orderId,state,AssetReceipt,QuotaReceipt)。

- [ ] **Step 1 — 写失败测试。** 在 `ExchangeAtomicV2Test` 中同键并发100次，在扣积分后注入失败，拒绝后充值再用原键，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, exchangeOrderCount);
assertEquals("DELIVERED", result.getState());
assertEquals(initialBalance, balanceAfterInjectedRollback);
assertEquals("REJECTED", sameRequestAfterRecharge.getState());
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ExchangeAtomicV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 先查用户回执，未命中才读db00主库准入；用户事务再次查身份并核验快照与business_at。余额不足正常提交REJECTED；成功同时扣积分、增次、写两类流水/DELIVERED订单/回执/ExchangeCompleted。v2不调用SKU库存链、不发GrantQuota；所有仓储不另开事务。准入快照仅当前调用短deadline有效。
- [ ] **Step 3a — 固定关闭与拒绝边界。** AdmissionSnapshot包含activityState；活动关闭等业务拒绝作为快照判定传入用户事务持久化REJECTED，不能在读配置后直接抛技术异常丢失拒绝回执。配置数据库不可用是技术失败，不占请求身份。普通关闭允许此前在途快照在deadline内完成；严格关闭由6.1运维流程暂停全体入口新受理、确认所有实例在途事务排空后再记录关闭完成，未确认实例不能视为成功。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。重复命中时db00与Redis不可用仍回放；验证旧库存dispatch和RabbitTemplate均未调用。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: exchange credits and quota in one transaction"`；不提交本地配置、凭据或原始用户数据。

### Task 2.2: 每日唯一签到与整单奖励

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/rebate/model/SignInCommand.java`
- Create: `bm-domain/src/main/java/com/jay/domain/rebate/model/SignInResult.java`
- Create: `bm-domain/src/main/java/com/jay/domain/rebate/service/SignInService.java`
- Create: `bm-domain/src/main/java/com/jay/domain/rebate/repository/SignInRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlSignInRepository.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/dao/v2/SignInDao.java`
- Create: `bm-app/src/main/resources/mybatis/mapper/v2/sign_in_mapper.xml`
- Modify: `docs/dev-ops/mysql/migrations/002_signin_exchange.sql`
- Modify: `bm-domain/src/main/java/com/jay/domain/activity/service/AdmissionService.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/SignInAtomicV2Test.java`

**Interfaces:**
- Consumes: 2.1准入与1.2、1.3资产。
- Produces: SignInService.signIn(SignInCommand):SignInResult；Command(userId,requestId,targetDate,deadline)，Result(orderId,businessDate,state,奖励项原回执列表)；AdmissionService.forSignIn(String,LocalDate,Deadline):AdmissionSnapshot。

- [ ] **Step 1 — 写失败测试。** 在 `SignInAtomicV2Test` 中100个不同请求同日领取积分及次数，第二项失败及次日原日期重试，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(1L, successfulSignInOrderCount);
assertEquals(1L, creditRewardLedgerCount);
assertEquals(1L, quotaRewardLedgerCount);
assertEquals(originalOrderId, nextDayRetry.getOrderId());
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SignInAtomicV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 成功签到唯一(userId,businessDate)，奖励唯一signOrderId+rewardItemId。先查原请求/日期记录再读取新配置；已有签到只关联新请求，不重新计算。新targetDate等于锁账户后数据库业务日；拒绝只写请求回执，不占签到日。按activityId一次锁全部相关账户，同事务完成所有奖励、明细、流水、回执与SignInCompleted。无奖励配置或任一奖励活动过期整单拒绝，技术失败全回滚。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。测未来/历史日期、原键换日期、同日换键、部分发放故障；原命中不依赖当前配置。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: grant daily sign-in rewards atomically"`；不提交本地配置、凭据或原始用户数据。

### Task 2.3: v2接口、订单归属与旧写门

**Files:**
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/CreditExchangeController.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/SignInController.java`
- Create: `bm-api/src/main/java/com/jay/api/v2/ExchangeRequest.java`
- Create: `bm-api/src/main/java/com/jay/api/v2/SignInRequest.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/AssetWriteGate.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/RequestAdmission.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/admission/RedisRequestAdmission.java`
- Create: `bm-app/src/main/resources/redis/v2_rate_admission.lua`
- Create: `bm-app/src/main/java/com/jay/config/V2FeatureProperties.java`
- Modify: `bm-trigger/src/main/java/com/jay/trigger/http/RaffleActivityController.java`
- Modify: `bm-trigger/src/main/java/com/jay/trigger/listener/RebateMessageCustomer.java`
- Modify: `bm-trigger/src/main/java/com/jay/trigger/listener/CreditAdjustSuccessCustomer.java`
- Modify: `bm-trigger/src/main/java/com/jay/trigger/listener/SendAwardCustomer.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/CreditRepository.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/AwardRepository.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/exchange/service/ExchangeService.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/rebate/service/SignInService.java`
- Modify: `bm-domain/src/main/java/com/jay/domain/activity/service/AdmissionService.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/AssetApiV2Test.java`

**Interfaces:**
- Consumes: 0.4身份与2.1、2.2业务。
- Produces: 设计的exchange/sign-ins POST/GET；AssetWriteGate.check(String entry):void；RequestAdmission.enter(String userId,Deadline):Permit、checkActivity(String activityKey,Deadline):void；ExchangeService.get(String userId,String orderId):ExchangeResult、SignInService.get(String userId,LocalDate date):SignInResult；查询同时校验归属并读取原回执及当前状态。

- [ ] **Step 1 — 写失败测试。** 在 `AssetApiV2Test` 中用MockMvc调用认证接口、跨用户查询与body伪造身份；切换写门后重投旧消息，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(403, crossUserQueryResponse.getStatus());
assertEquals(200, repeatedExchangeResponse.getStatus());
assertEquals(0L, legacyDirectBalanceWrites);
assertEquals(429, rateLimitedResponse.getStatus());
assertEquals(0L, receiptsCreatedByEntryRejection);
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AssetApiV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 只用CurrentUser.sub，DTO不接受userId。成功200，持久化拒绝明确业务码，冲突409、未认证401、过载429/503；GET核对归属。v2-enabled及asset-write-mode=LEGACY/V2控制入口，V2禁止旧DAO直接写余额；无稳定身份的v1交易停止受理。旧消费者切换后停用或迁移适配，不因活动隔离允许旧积分奖品直接写共享账户。缺少迁移身份不得推断成功或Ack丢弃。
- [ ] **Step 3a — 实现入口准入。** RequestAdmission.enter在配置读取前限制用户频率及实例最大在途，返回Permit并在HTTP调用finally释放；AdmissionService读取权威配置后、进入用户事务前按活动限速。Redis Lua原子维护分布式额度，不能在账户锁内访问Redis；user-rps、activity-rps、max-in-flight与local-fallback-rps必须在部署配置显式给出并验证。Redis不可用时仅采用经验证的保守本地预算，否则拒绝新交易；结果查询独立配额，支付通知使用独立接收预算。入口拒绝不占业务身份；同键重试仍能查原回执。测试Redis中断、请求突发、超时释放及普通流量不能借用查询/回调预留额度。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: expose v2 asset APIs and fence legacy writes"`；不提交本地配置、凭据或原始用户数据。

## 阶段退出条件

全部任务绿灯，期初加流水等于余额；签到100个不同键不多领，兑换无DEBITED。仅隔离验收，可靠消息和迁移完成前不开放真实混写。
