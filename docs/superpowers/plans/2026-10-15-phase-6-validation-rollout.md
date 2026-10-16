# BroMark 阶段6 故障容量与切换验收 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给出可复现的正确性、恢复和安全容量证据，以及可停止、可恢复的切换操作。

**Architecture:** 先在隔离库演练存量迁移与旧写退出，再串起故障与对账门禁，最后做固定到达率实验。发布以具体版本、配置、迁移清单和证据包为单位，不自动对真实服务执行。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MyBatis、MySQL 8.0 InnoDB、Redis、RabbitMQ、JUnit 5＋Vintage（兼容JUnit 4）、Maven。

**Spec:** [高并发积分支付与抽奖设计](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)。

## Global Constraints

- 阶段0完成后编译、测试、应用与容器统一Java 25，release=25；使用Boot 3.5.16及Jakarta体系，保留javax.sql等JDK API。
- 执行环境采用 MySQL 8.0 InnoDB，业务短事务使用 READ COMMITTED，关键读取显式加锁。
- 业务时区固定为 Asia/Shanghai。
- 首版积分使用 BigDecimal 与数据库定点小数，拒绝超过两位的小数或溢出，不使用 float、double，不在写入时静默舍入。
- 首版次数 SKU 不限库存，积分兑换在同一用户分库事务内完成扣积分与增加次数；签到奖励统一使用新资产内核。
- 首版奖品只支持积分与无权益结果；Redis 不保存唯一余额或库存事实，账户事务不调用 MQ、支付渠道或远程发货。
- 2026-10-16：运维工具、正确性回归和首档容量诊断已执行，详见[验收报告](../../verification/2026-10-16-validation-rollout/README.md)。容量退出条件未通过，发布保持NO-GO。
- 路径相对 BroMark 仓库根目录；先读[实施总览](2026-10-15-credit-payment-raffle-implementation-index.md)的共享契约、测试环境和迁移规则。依赖：[阶段0](2026-10-15-phase-0-foundation.md)至[阶段5](2026-10-15-phase-5-sandbox-payment.md)全部退出条件通过。

## Review Focus

- DDL半途失败或重复账户：6.1停止切换并核查，不DROP回滚。
- 旧消息已发后重投：6.1导入业务身份和去重凭据。
- 测试为零或被skip：6.2验收失败。
- 202受理和请求发送被计作完成：6.3独立跟踪终态。
- 恢复速率不高于新增、仅靠拒绝保延迟：6.3不能认定安全容量。

## File Structure

operations维护切换/回滚/核查；verification维护故障矩阵；performance维护负载、统计与报告。仅006改数据库约束；本阶段不新增交易语义，也不接真实支付。

## Tasks

### Task 6.1: 存量切换、回放凭据与账实演练

**Files:**
- Create: `docs/dev-ops/mysql/migrations/006_cutover_constraints.sql`
- Create: `docs/operations/v2-cutover-runbook.md`
- Create: `docs/operations/v2-rollback-runbook.md`
- Create: `docs/operations/v2-write-entry-inventory.md`
- Create: `docs/operations/v2-reconciliation.sql`
- Create: `docs/dev-ops/mysql/rehearse_cutover.py`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/LegacyIdentityAdapter.java`
- Modify: `docs/dev-ops/mysql/migrations/manifest.json`
- Test: `bm-app/src/test/java/com/jay/test/v2/MigrationRehearsalV2Test.java`

**Interfaces:**
- Consumes: 阶段1—5 migration及业务身份；2.3旧写门；3.4核查。
- Produces: rehearse_cutover.py --env v2-it --phase precheck/prepare/verify生成JSON证据；LegacyIdentityAdapter.resolve(String entry,String legacyId):Optional<BusinessId>。

- [x] **Step 1 — 写失败测试。** 在 `MigrationRehearsalV2Test` 中准备重复账户、旧签到/待兑换/已发奖及已发消息样本，停写排空后回放，并测试半途DDL失败，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(0L, duplicateAssetEffectsAfterLegacyReplay);
assertEquals(0L, unexplainedBalanceDifferences);
assertFalse(cutoverAllowedWithDuplicateAccounts);
assertFalse(oldDirectWriterAllowedAfterSwitch);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MigrationRehearsalV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 清点所有共享积分写入口，先全域停旧资产写并排空，再活动级迁移抽奖，不能只按新活动隔离。保存审计期初、周期上限/桶/有效计数、签到日期身份、旧流水与消息bizId映射；不能SUM重复账户或将未知旧消息默认成功。006先precheck再回填和收紧唯一/非空约束，每库每表校验行数、金额与checksum，失败停止并保留进度。旧SKU库存只核对后退出同步，实际奖品按归属建池不复制库存。导入已发消息去重凭据及版本化队列；LegacyIdentityAdapter不能证明身份则写核查。回滚只停止新受理并保留新Worker排空，不把新状态交回旧代码。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。runbook必须列出每一步前提、命令、成功证据、停止条件和恢复点；脚本默认dry-run，实际写入必须显式选择隔离环境。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "docs: add audited cutover and rollback rehearsal"`；不提交本地配置、凭据或原始用户数据。

### Task 6.2: 故障矩阵、对账与回归门禁

**Files:**
- Create: `bm-app/src/test/java/com/jay/test/v2/ReconciliationV2Test.java`
- Create: `docs/verification/v2-failure-matrix.md`
- Create: `docs/verification/v2-evidence-template.md`
- Create: `docs/dev-ops/v2-verify.sh`
- Test: `bm-app/src/test/java/com/jay/test/v2/FailureMatrixV2Test.java`

**Interfaces:**
- Consumes: 阶段0—5聚焦测试和FaultInjector；6.1演练数据。
- Produces: v2-verify.sh --env v2-it运行目标测试并核验Surefire计数；汇总4.5定义的ReconciliationReport，必须含CREDIT、QUOTA、STOCK、DELIVERY、PAYMENT、SIGNIN六个checkedScopes，未检查项不能作为零差异。

- [x] **Step 1 — 写失败测试。** 在 `FailureMatrixV2Test` 中按设计第十五节故障表逐项映射真实测试名称，依次覆盖多实例、服务重启、broker中断和数据库连接中断，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(0L, report.getCreditMismatch());
assertEquals(0L, report.getQuotaMismatch());
assertEquals(0L, report.getStockMismatch());
assertEquals(0L, report.getDuplicateDelivery());
assertEquals(0L, report.getPaymentMismatch());
assertEquals(0L, report.getSigninMismatch());
assertEquals(6, report.getCheckedScopes().size());
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=FailureMatrixV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [x] **Step 3 — 实现最小行为。** 故障矩阵每行包含设计条目、测试类/方法、注入点、前后SQL、原bizId、最终状态及证据路径；复用已有测试，不为每条重新造协议。对原回执重放、租约过期、所有取消竞态、跨日规则、REVIEW晚到及旧消息重投建立覆盖索引；真实崩溃与连接丢失不能只靠抛异常替代。验证脚本执行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*V2Test,AliasMethodAlgorithmTest' test`，解析XML发现零测试/skip立即失败。依赖不可用写未执行记录，不生成通过报告；证据脱敏，原始运行输出保存在git忽略目录。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。恢复结束后核对每个已受理订单都为终态或具备明确持久化核查单；积压清零不等于账实通过。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "test: gate v2 workflows on faults and reconciliation"`；不提交本地配置、凭据或原始用户数据。

### Task 6.3: 固定到达率实验、资源预算与发布证据

**Files:**
- Create: `docs/performance/v2-load.js`
- Create: `docs/performance/v2-load-config.json`
- Create: `docs/performance/collect_v2_results.py`
- Create: `docs/performance/track_v2_orders.py`
- Create: `docs/performance/seed_v2_testdata.py`
- Create: `docs/performance/test_collect_v2_results.py`
- Create: `docs/performance/v2-capacity-report-template.md`
- Create: `docs/operations/v2-release-checklist.md`
- Modify: `bm-app/src/main/java/com/jay/config/V2BudgetProperties.java`
- Modify: `bm-app/src/main/java/com/jay/config/V2WorkerConfig.java`
- Test: `bm-app/src/test/java/com/jay/test/v2/CapacityEvidenceV2Test.java`

**Interfaces:**
- Consumes: 阶段0预算与指标、阶段4/5完成状态、6.2正确性门禁。
- Produces: k6脚本按scenario运行固定到达率；seed_v2_testdata.py --env v2-it --users N --run-id ID生成隔离用户、资产期初和奖池数据，仅允许服务停写的隔离库；track_v2_orders.py --accepted PATH --base-url URL --out PATH增量跟踪受理订单，身份token只从环境读取；collect_v2_results.py --run-dir PATH输出sent/received/rejected/accepted/completed、P50/P95/P99、oldestAge、drainSeconds与差异数；Python unittest验证统计边界。

- [x] **Step 1 — 写失败测试。** 在 `CapacityEvidenceV2Test` 中构造发送1000、受理600、拒绝400、最终完成550的样本，并注入未确认结果和恢复期间持续新增，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(600L, acceptedCount);
assertEquals(550L, completedCount);
assertEquals(400L, rejectedCount);
assertFalse(safeCapacityWithGrowingBacklog);
```

- [x] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CapacityEvidenceV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 使用k6 constant-arrival-rate；同硬件同业务规则基线，明确SKU不限量的语义变化。混合负载70%查询/10%结果查询/10%兑换/10%抽奖；同活动100/300/600/1000每秒逐档，热点用户并发2/8/32；另签到、支付重投、版本切换与持续新增下排空。每档预热后至少10分钟、关键配置重复3次。抽奖/支付每个受理订单以稳定ID记录到隔离JSONL并独立跟踪终态，截止时仍处理中单列，不能以202计完成。k6不足的迭代和压测机瓶颈单列，查询/交易/完成三种延迟独立统计。连接按全部实例、物理DB聚合；起点8个Worker、prefetch16只是配置。查询P99≤200ms、受理≤500ms、支付到账及抽奖确认≤5s且积压不增长、对账零差异才候选安全容量；排空估算仅处理速率>新增时适用。
- [x] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。运行 `python3 -m unittest discover -s docs/performance -p test_collect_v2_results.py`；Java计数测试通过不代替实际压测。报告必须附版本、机器、中间件持久化/副本、digest、连接数、原始结果、重复波动及未达标瓶颈；发布清单先形成可审阅包，真实部署仍按用户授权。
- [x] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "perf: measure v2 completed capacity and recovery"`；不提交本地配置、凭据或原始用户数据。

本任务补充约束：k6每个到达率迭代只发送一个计入主负载的请求，轮询由独立tracker执行并使用查询配额，轮询流量额外报告，不能隐藏在70/10/10/10分布内。JSONL仅含测试身份编号与订单/请求ID，不记录token；服务端confirmed_at、paid_at、delivered_at为完成时间依据，轮询观测延迟另记。运行前记录k6版本和数据库/应用时钟偏差，不能把压测端时钟与服务端时间直接相减而不校准。

先用seed脚本准备足够测试用户、积分、总/日/月次数和可售名额，保存开跑前审计期初并对账；签发测试JWT使用隔离环境测试密钥，token映射路径由环境变量提供，不进git。签到测试为每次首次领取提供不同用户，重复领取负载单独统计。有限库存热点测量必须记录名额分配量和兜底比例，库存耗尽后的区间单独报告，不能将大量不限库存兜底当作有限库存确认容量。缺余额/次数的业务拒绝与入口过载拒绝分别计数。

## 阶段退出条件

设计故障矩阵全部有执行证据或明确未通过项，账实零差异，安全容量有原始数据与配置支撑；发布包包含切换与停止条件。未达到性能目标时报告实测瓶颈和修订依据，不改写为已完成能力。


## 2026-10-16 执行结论

- [x] 6.1 运维实现及隔离演练：审计原回执/业务身份/路由、旧签到日期与已扣款兑换回放、重复账户停止、实际DDL中断/恢复和source checksum绑定。
- [x] 6.2 回归与六域门禁：应用138、Router28、Python统计/门禁27、切换9、原迁移4实际通过；完整脚本只有同时取得停写六域账实证据才通过。真实MySQL KILL、Rabbit STOP、双实例SIGKILL接管及固定候选崩溃有独立证据。
- [x] 6.3 工具及首个失败档报告：100请求/秒600秒，59,983请求，11,982受理全部终态；查询P99=58.75ms、受理846.18ms、服务端确认94.99s，Outbox积压峰值1,301。停止升压，未将202或排空后的零当作容量通过。
- [ ] 6.3 Step3完整矩阵和阶段退出：首档违反延迟与积压条件，按停止门禁未升压300/600/1000，也未认证三个重复、热点8/32、签到/支付/版本切换持续容量或持续新增下的恢复优势。安全容量未确定，生产NO-GO。

原始证据与边界见[验收报告](../../verification/2026-10-16-validation-rollout/README.md)、[容量失败报告](../../verification/2026-10-16-validation-rollout/capacity-report.md)、[裁定](../../verification/2026-10-16-validation-rollout/rulings.md)。真实迁移/发布/合并/push均未执行。旧未筛选演示套件fork timeout单列，不宣称旧全套绿灯。
