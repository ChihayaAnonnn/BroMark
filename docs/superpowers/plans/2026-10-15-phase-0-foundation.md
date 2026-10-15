# BroMark 阶段0 技术基线 Implementation Plan

> **升级目标更新（2026-10-15）：** Java 25 LTS＋Boot 3.5.16；0.1 与 [升级计划](2026-10-15-java25-db-router-upgrade.md) Task 3 共用构建验收，不重复实施。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将编译与运行基线升级至 Java 25，并建立真实执行的测试、隔离环境、正确事务路由和受控身份入口。

**Architecture:** 先升级 Java 25、统一 Spring Boot 3.5.16 配套依赖，再把用户/控制库事务、数据库许可和认证作为所有v2入口共同边界。本阶段迁移 Jakarta EE API，保留旧接口行为仅用于迁移前诊断，新功能默认关闭。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MyBatis、MySQL 8.0 InnoDB、Redis、RabbitMQ、JUnit 5＋Vintage（兼容JUnit 4）、Maven。

**Spec:** [高并发积分支付与抽奖设计](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)。

## Global Constraints

- 阶段0完成后，编译、测试、应用运行与容器统一使用 JDK/JRE 25，字节码目标 release=25；框架为 Spring Boot 3.5.16 配套依赖。
- 执行环境采用 MySQL 8.0 InnoDB，业务短事务使用 READ COMMITTED，关键读取显式加锁。
- 业务时区固定为 Asia/Shanghai。
- 首版积分使用 BigDecimal 与数据库定点小数，拒绝超过两位的小数或溢出，不使用 float、double，不在写入时静默舍入。
- 首版次数 SKU 不限库存，积分兑换在同一用户分库事务内完成扣积分与增加次数；签到奖励统一使用新资产内核。
- 首版奖品只支持积分与无权益结果；Redis 不保存唯一余额或库存事实，账户事务不调用 MQ、支付渠道或远程发货。
- 实施状态以任务复选框和验证报告为准，未勾选内容不代表完成。
- 路径相对 BroMark 仓库根目录；先读[实施总览](2026-10-15-credit-payment-raffle-implementation-index.md)的共享契约、测试环境和迁移规则。依赖：无；开始前记录当前工作树状态。。

## Review Focus

- Java编译/运行/镜像不一致或测试未执行：0.1检查版本、字节码与Surefire执行数。
- 异常后线程复用造成串库：0.2检查路由清理。
- 普通任务占满连接挤压支付：0.3检查独立许可。
- 无会话、错误签名或伪造userId：0.4拒绝。
- 停机期间仍认领任务或许可泄漏：0.3覆盖关闭与异常释放。

## File Structure

现有入口为 `bm-app`，现有测试集中在该模块。`RoutedTransactionRunner`只负责许可、路由、事务和清理，不承载资产业务；`DatabaseBudget`只负责资源；`V2SecurityConfig`只负责身份。

## Tasks

### Task 0.1: 升级 Java 25 并恢复构建与测试基线

**Files:**

- Modify: `pom.xml`
- Modify: `bm-app/pom.xml`
- Modify: `bm-api/pom.xml`
- Modify: `bm-domain/pom.xml`
- Modify: `bm-types/pom.xml`
- Modify: `bm-infrastructure/pom.xml`
- Modify: `bm-trigger/pom.xml`
- Modify: `bm-app/Dockerfile`
- Modify: `bm-app/build.sh`
- Create: `docs/operations/java25-upgrade-checklist.md`
- Test: `bm-app/src/test/java/com/jay/test/v2/BuildBaselineV2Test.java`

**Interfaces:**
- Consumes: 现有Java 8、Boot 2.7.12父POM与openjdk:8-jre-slim运行镜像。
- Produces: Java 25 编译/测试/运行基线及major version=69的应用字节码；正常JUnit 4测试发现；由父POM统一管理Surefire与框架依赖。

- [ ] **Step 1 — 写失败测试。** 在 `BuildBaselineV2Test` 中检查实际测试运行时与Application字节码版本，记录当前测试发现情况。测试私有辅助方法 `classFileMajorVersion(Class<?>):int` 用DataInputStream读取对应class资源的magic、minor和major，不依赖外部javap进程。断言如下：

```java
assertEquals("25", System.getProperty("java.specification.version"));
assertEquals(69, classFileMajorVersion(com.jay.Application.class));
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BuildBaselineV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 统一依赖与测试发现。** 移除Surefire 2.6覆盖，使用Boot父POM管理版本；skipTests默认改false。保留JUnit 4并加入BOM管理的junit-vintage-engine测试依赖，防止JUnit Platform遗漏旧测试。如果Step 2未发现测试，先仅修复发现配置，重跑确认上述版本断言确实失败。AMQP、Redis、Actuator与Micrometer去掉跨版本覆盖，启用所需Actuator/Prometheus依赖；validation使用Boot 3配套Jakarta体系，保持现有接口契约。
- [ ] **Step 3a — 统一 Java 25 编译与运行。** 根POM设置java.version=17和maven.compiler.release=25，移除旧source=8/target=8及子模块重复source/target/compilerVersion覆盖，让六模块继承同一release配置。Maven本身和Surefire fork也使用JDK 25，核对java -version、javac -version与mvn -version；具体25补丁版本和供应商在实施时记录并锁定。Dockerfile改用经amd64/arm64验证的Java 25 JRE镜像并记录digest；启动命令用exec，JAVA_OPTS放在-jar之前，避免把JVM选项当成jar文件名。build.sh记录镜像标签/digest和目标架构，不能继续把Java 25产物装入Java 8镜像。
- [ ] **Step 3b — 核对 Java 25 兼容性。** 验证Lombok注解处理、MyBatis、DB Router动态代理/ThreadLocal路由、Redisson及fastjson/fastjson2序列化。检查JDK移除模块和强封装导致的编译或反射异常，优先升级兼容组件或显式补入所需API；禁止用宽泛--add-opens掩盖问题，确需单项开放必须记录组件、原因和去除条件。Java升级不顺带重写业务为record，数据库预算完成后显式启用受控虚拟线程。保存effective-pom、依赖树及兼容检查表。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。另运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AliasMethodAlgorithmTest,BuildBaselineV2Test test`，要求旧Alias算法回归实际执行。
- [ ] **Step 4a — 验证产物与镜像。** 执行 `mvn -DskipTests package` 验证打包，再执行 `docker build -t bromark-v2:jdk25-it bm-app` 和 `docker run --rm --entrypoint java bromark-v2:jdk25-it -version`，确认镜像内为25。此任务先完成不依赖业务环境的版本和打包检查；同一镜像的数据库、路由与序列化启动冒烟在0.2建立隔离环境后完成，不倒置任务依赖。保存镜像digest、JVM参数与测试报告；打包或容器能显示版本不替代行为测试。回退使用匹配的旧产物/旧运行时组合，Java 25新包不能降回Java 8运行；新业务状态的回滚仍遵守阶段6协议。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "build: upgrade java baseline to 17 and align dependencies"`；不提交本地配置、凭据或原始用户数据。

### Task 0.2: 隔离集成环境、事务路由与测试工具

**Files:**
- Create: `bm-app/src/main/java/com/jay/config/V2TransactionConfig.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/repository/UserTransactionRunner.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/RouteSlot.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/BusinessTime.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/Deadline.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/WorkClass.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/BusinessClock.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/FaultInjector.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/RouteCodec.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/transaction/RoutedTransactionRunner.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/transaction/DatabaseBusinessClock.java`
- Create: `bm-app/src/test/java/com/jay/test/v2/support/ConcurrentProbe.java`
- Create: `bm-app/src/test/java/com/jay/test/v2/support/V2IntegrationTestConfig.java`
- Create: `bm-app/src/test/java/com/jay/test/v2/support/FaultInjector.java`
- Create: `bm-app/src/test/resources/application-v2-it.yml`
- Create: `docs/dev-ops/docker-compose-v2-test.yml`
- Test: `bm-app/src/test/java/com/jay/test/v2/RoutingTransactionV2Test.java`

**Interfaces:**
- Consumes: 0.1构建基线。
- Produces: 总览中的UserTransactionRunner（含可信扫描inSlot）、RouteCodec.routeUser(String):RouteSlot、RouteCodec.routeOrder(String):RouteSlot、BusinessClock.now():BusinessTime；ConcurrentProbe与FaultInjector测试支持。

- [ ] **Step 1 — 写失败测试。** 在 `RoutingTransactionV2Test` 中使用独立三库、两业务库四表的测试数据源，分别验证同线程复用、事务回滚与异步扫描，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(expectedSlot, codec.routeUser(userId));
assertThrows(IllegalStateException.class, () -> runner.inUser(userId, work, deadline, () -> { throw new IllegalStateException(); }));
assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RoutingTransactionV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 在事务获取连接前doRouter，finally清路由；inControl显式选db00。禁止嵌套不同路由，拒绝事务中切库。隔离级别READ_COMMITTED；数据库时间通过当前事务连接读取并转换Asia/Shanghai。ConcurrentProbe统一放行并传播所有失败；FaultInjector接口放domain/asset/service，生产空实现，测试支持类提供可控实现。Compose固定MySQL 8.0镜像的实测digest及持久化配置，Redis/RabbitMQ也记录解析digest；只暴露隔离端口、环境变量凭据。集成测试使用V2IntegrationTestConfig显式导入所测组件，不扫描Application及旧@Scheduled/监听器；需要broker测试时只加载指定v2监听器。生产不加载测试配置。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。在不同用户槽与全部8个订单分表运行回滚探针；扫描路由不复用用户ThreadLocal。用0.1生成的Java 25镜像在该隔离环境启动，验证主库连接、MyBatis/DB Router事务及序列化冒烟，记录实际运行时与镜像digest。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "test: add isolated routed transaction harness"`；不提交本地配置、凭据或原始用户数据。

### Task 0.3: 在连接获取前建立资源许可与基础指标

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/asset/service/DatabaseBudget.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/BudgetExceededException.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/Permit.java`
- Create: `bm-infrastructure/src/main/java/com/jay/infrastructure/transaction/BoundedDatabaseBudget.java`
- Create: `bm-app/src/main/java/com/jay/config/V2BudgetProperties.java`
- Create: `bm-app/src/main/java/com/jay/config/V2WorkerConfig.java`
- Create: `bm-app/src/main/java/com/jay/config/V2MetricsConfig.java`
- Modify: `bm-app/src/main/java/com/jay/config/ThreadPoolConfig.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/transaction/RoutedTransactionRunner.java`
- Modify: `bm-infrastructure/pom.xml`
- Test: `bm-app/src/test/java/com/jay/test/v2/DatabaseBudgetV2Test.java`

**Interfaces:**
- Consumes: 0.2的WorkClass、Deadline与事务执行器。
- Produces: DatabaseBudget.acquire(String physicalDb,WorkClass,Deadline):Permit、available(String physicalDb,WorkClass):int；Permit实现AutoCloseable；BudgetExceededException表示许可超时；RoutedTransactionRunner所有入口统一持有许可。

- [ ] **Step 1 — 写失败测试。** 在 `DatabaseBudgetV2Test` 中将TRADE许可耗尽，并让CALLBACK、QUERY、RECOVERY各自仍有许可；模拟事务异常，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertThrows(BudgetExceededException.class, () -> budget.acquire("mysql-it", WorkClass.TRADE, deadline));
assertNotNull(budget.acquire("mysql-it", WorkClass.CALLBACK, deadline));
assertEquals(initialFree, budget.available("mysql-it", WorkClass.RECOVERY));
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DatabaseBudgetV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 定义BudgetExceededException与可用许可查询；按物理实例聚合逻辑库，禁止分类借用。许可申请在连接前、事务结束释放，过期deadline拒绝；独立有界API/Relay/Worker/恢复线程池，拒绝策略不静默丢任务。记录许可等待、连接等待、事务与锁等待、拒绝率；Micrometer标签仅用业务分类与结果。关闭生命周期停止受理和认领后等待事务及Confirm，最后关池。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: bound database work and expose baseline metrics"`；不提交本地配置、凭据或原始用户数据。

### Task 0.4: 接入可信身份及统一HTTP错误契约

**Files:**
- Create: `bm-trigger/src/main/java/com/jay/trigger/security/V2SecurityConfig.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/security/CurrentUser.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/V2ExceptionHandler.java`
- Create: `bm-domain/src/main/java/com/jay/domain/asset/model/AssetConflictException.java`
- Create: `bm-trigger/src/main/java/com/jay/trigger/http/v2/V2IdentityProbeController.java`
- Modify: `bm-trigger/pom.xml`
- Test: `bm-app/src/test/java/com/jay/test/v2/V2IdentityV2Test.java`

**Interfaces:**
- Consumes: 0.1的Boot兼容依赖；0.3预算异常。
- Produces: CurrentUser.requireUserId():String；AssetConflictException extends RuntimeException作为共享摘要冲突类型并映射409；v2用户接口使用已验证JWT的sub作为用户身份，401/403/400/409/429/503统一响应。

- [ ] **Step 1 — 写失败测试。** 在 `V2IdentityV2Test` 中使用测试生成的RSA签名token、错误签名/issuer/audience、过期token以及伪造userId参数，断言如下；示例是测试方法中的关键断言，命令与服务变量由该测试的 `@Before` 创建。

```java
assertEquals(401, unauthenticatedResponse.getStatus());
assertEquals(401, invalidSignatureResponse.getStatus());
assertEquals("u1", authenticatedResponse.getContentAsString());
```

- [ ] **Step 2 — 验证失败。** 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=V2IdentityV2Test test`。预期相关行为断言失败；新接口尚不存在时编译失败也属于本步红灯，但不能以依赖不可用或测试被跳过代替行为失败。
- [ ] **Step 3 — 实现最小行为。** 本计划选择JWT作为设计中已验证会话的实现适配：在bm-trigger引入BOM管理的OAuth2 resource-server starter，固定RS256验证、公钥与issuer/audience由环境配置，校验exp/nbf和非空sub；不实现注册或token签发。v2-enabled时缺认证配置启动失败，测试密钥只在test resources。安全链只匹配/api/v2，保护用户交易与订单查询；支付通知只豁免会话认证，仍由渠道验签任务校验；探针仅test profile。Deadline/预算错误映射429或503，参数400、幂等409，技术错误不伪装REJECTED。
- [ ] **Step 3a — 划定公开读取。** 仅GET /api/v2/activities/{id}及/{id}/awards可匿名读取（4.1实现），其他用户域v2请求必须认证；公开读取仍经过QUERY与回源预算，不能公开资产、规则快照、订单或Actuator管理端点。测试匿名只可访问公开展示接口，伪造身份仍不能交易。
- [ ] **Step 4 — 验证通过。** 重跑同一命令，要求目标测试实际执行、failures=0、errors=0、skipped=0，并核对 `bm-app/target/surefire-reports` 中该类计数；集成测试依赖不可用时记录未完成，不能改成 mock 后声称并发或恢复已验收。同时断言不存在token时x-user-id及body.userId均不能获得权限；未来订单查询在2.3继续测所有权。
- [ ] **Step 5 — 提交本任务。** 检查 `git diff --check`，只暂存本任务 Files 中的变更，执行 `git commit -m "feat: authenticate v2 requests and normalize errors"`；不提交本地配置、凭据或原始用户数据。

## 阶段退出条件

0.1—0.4真实测试通过；六模块release=25、Application字节码major=69、Maven与镜像运行时为25；保存依赖树、JDK供应商与补丁、中间件版本、镜像digest、兼容检查表、连接预算和隔离环境说明。后续计划不能绕过事务执行器、许可和认证。
