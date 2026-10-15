# Java 25 与 DB Router 改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将下载的 DB Router 原始源码改造成经过 MySQL 验收的 Boot 3 组件，并建立 BroMark 的 Java 25 构建与运行基线。

**Architecture:** 独立维护 Router，不覆盖上游 Maven 坐标；保留现有分片映射。先完成组件的事务路由、SQL 支持边界和生命周期验收，再升级 BroMark；虚拟线程按入口逐步启用，使用既有数据库许可约束并发。

**Tech Stack:** Java 25 LTS、Spring Boot 3.5.16、MyBatis Starter 3.0.5、JUnit 5（旧测试保留 Vintage）、MySQL 8.0 InnoDB、Docker、Maven。

**Spec:** [升级评估与改造约束](../specs/2026-10-15-java25-db-router-upgrade-assessment.md)。

## Global Constraints

- 路径默认相对工作区；BroMark 与 db-router-spring-boot-starter 是两个独立项目。
- 原型只是参考实现。以下任务均未完成，不因原型 8 项通过而勾选。
- Router 正式坐标：`com.jay.middleware:db-router-spring-boot-starter:2.0.0-SNAPSHOT`；保留原 Java 包名，不覆盖上游 1.0.2。
- 两库四表映射不变，db00 为控制库；活动事务不能修改数据库或表槽。
- 编译、测试和运行使用 Java 25，release=25，应用 class major=69。正式镜像选择执行时最新受支持 Java 25 补丁并锁定 digest，不能沿用探针镜像就宣称完成补丁升级。
- 不修改生产数据、生产配置或自动发布依赖；测试使用独立 MySQL 容器、端口和数据库。

## Review Focus

- 表键变更失败导致库键先被修改：Task 1 验证原子预检与异常恢复。
- 默认库与非法库被混淆：Task 1 验证显式 db00、未知键拒绝、无路由控制库访问。
- SQL 正则误改字符串、join 或子查询：Task 2 明确支持范围并拒绝不支持输入。
- 关闭上下文或初始化部分失败泄漏连接池：Task 2 验证每个池仅关闭一次。
- Java 25 构建成功但旧测试被遗漏、自定义线程池未切换：Task 3 验证 Vintage 执行计数及实际入口线程类型。

## Task 1: 正式化自动配置与事务路由

**Files:**
- Modify: `db-router-spring-boot-starter/pom.xml`
- Modify: `db-router-spring-boot-starter/src/main/java/cn/bugstack/middleware/db/router/config/DataSourceAutoConfig.java`
- Modify: `db-router-spring-boot-starter/src/main/java/cn/bugstack/middleware/db/router/DBContextHolder.java`
- Modify: `db-router-spring-boot-starter/src/main/java/cn/bugstack/middleware/db/router/DBRouterJoinPoint.java`
- Modify: `db-router-spring-boot-starter/src/main/java/cn/bugstack/middleware/db/router/strategy/impl/DBRouterStrategyHashCode.java`
- Create: `db-router-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `db-router-spring-boot-starter/src/test/java/audit/RouterAcceptanceTest.java`
- Create: `db-router-spring-boot-starter/src/test/java/audit/RouterTransactionContractTest.java`

**Interfaces:**
- Consumes: 原有 `IDBRouterStrategy.doRouter(String)`、`setDBKey(int)`、`setTBKey(int)`、`clear()`。
- Produces: 保持旧接口；增加 `DBContextHolder.setRoute(String db, String tb):void`，整体预检后修改两键；自动配置提供共享 `PlatformTransactionManager` 与 `TransactionTemplate`。

- [ ] **Step 1 — 建立失败验收。** 将验证材料中的 RouterAcceptanceTest 复制到正式模块，先用 Boot 3 测试 POM 和原始代码复现红灯。增加 RouterTransactionContractTest：活动事务从 db01/000 切到 db01/001、db02/000 均拒绝，两键保持原值；doRouter 在失败时也不留下半更新上下文；异常嵌套恢复外层路由；无外层时退出两键为空；显式 db00 和无路由均访问控制库，db99 拒绝；真实代理 `@DBRouter`＋`@Transactional` 方法首次取连接时已使用用户库。固定用户输入集合比较原算法全部槽，拒绝零/负数及非 2 的幂的总槽数。
- [ ] **Step 2 — 验证红灯。** 在模块运行 `mvn -Dtest=RouterAcceptanceTest,RouterTransactionContractTest test`，确认测试被发现并有行为断言失败；依赖下载错误不能算红灯。
- [ ] **Step 3 — 实现兼容契约。** 参考 compatibility-prototype，但使用 setRoute 整体预检，不能直接复制两个独立 setter 的半更新风险。单键旧接口保留同样事务检查，策略统一通过 setRoute 写入。切面恢复调用前上下文，显式设置早于事务切面的顺序；拒绝 null/空路由。自动配置先于 JDBC 默认配置，使用 imports、共享事务管理器和严格数据源映射。重写 POM，移除旧默认 release、签名/发布凭据、跳过测试配置及无用依赖；核实上游许可和来源后记录再分发条件。
- [ ] **Step 4 — 验证绿灯。** 重跑 Step 2，要求 failures/errors/skipped 均为 0；保存用例计数与 effective-pom。尚未完成许可核实不得发布制品。
- [ ] **Step 5 — 提交。** 建立独立 Git 历史，保存上游校验与来源记录；检查 diff，仅提交组件源码、测试和构建配置，不提交 target/缓存。

## Task 2: SQL 边界、连接池生命周期与 MySQL 验收

**Files:**
- Modify: `db-router-spring-boot-starter/src/main/java/cn/bugstack/middleware/db/router/dynamic/DynamicMybatisPlugin.java`
- Modify: `db-router-spring-boot-starter/src/main/java/cn/bugstack/middleware/db/router/dynamic/DynamicDataSource.java`
- Modify: `db-router-spring-boot-starter/src/main/java/cn/bugstack/middleware/db/router/config/DataSourceAutoConfig.java`
- Create: `db-router-spring-boot-starter/src/test/java/audit/RouterSqlBoundaryTest.java`
- Create: `db-router-spring-boot-starter/src/test/java/audit/RouterLifecycleTest.java`
- Create: `db-router-spring-boot-starter/src/test/java/audit/RouterMySqlIntegrationTest.java`
- Create: `db-router-spring-boot-starter/docs/sql-support.md`
- Create: `db-router-spring-boot-starter/docs/docker-compose-it.yml`

**Interfaces:**
- Consumes: Task 1 路由与共享事务管理器。
- Produces: `DynamicDataSource.close():void` 幂等关闭所有唯一物理池；分表 SQL 支持范围与 MySQL 真实事务验收。

- [ ] **Step 1 — 写失败测试。** SQL 首版仅支持单物理表 SELECT/INSERT/UPDATE/DELETE，解析后修改表标识符；覆盖别名、反引号、大小写、参数字符串内含 from/update 仍不被修改。join、多语句、子查询、CTE 和缺失表槽明确抛 IllegalArgumentException。生命周期测试用可计数 DataSource，断言默认池与 db00 复用只关一次、重复 close 不重复关闭、第二个池初始化失败时第一个被关闭。
- [ ] **Step 2 — 验证红灯。** 运行 `mvn -Dtest=RouterSqlBoundaryTest,RouterLifecycleTest test`，确认原正则和无生命周期实现不满足断言。
- [ ] **Step 3 — 实现受限 SQL 改写和关闭。** 使用解析器而非正则操作表名；固定 `com.github.jsqlparser:jsqlparser:4.9`，只接受测试定义的 AST 范围，不支持的语句在执行前拒绝。DynamicDataSource 实现 AutoCloseable，按对象身份去重关闭物理池；初始化异常回收已创建池。连接池配置绑定需进行类型转换，未知属性拒绝，不静默忽略。
- [ ] **Step 4 — 验证 MySQL。** Compose 启动隔离 MySQL 8.0 InnoDB，记录实际补丁/digest。RouterMySqlIntegrationTest 使用三个独立库、两用户库四表，复制原型 1,000 虚拟线程 Mapper 断言；追加跨全部槽的提交/回滚、READ COMMITTED、连接数上限、异常后上下文清理、事务中切路由拒绝。用完整业务 SQL 清单执行 SQL 支持范围检查，超出首版范围的调用必须显式调整或扩展解析测试后实现，不能跳过。运行 `mvn -Dtest=RouterSqlBoundaryTest,RouterLifecycleTest,RouterMySqlIntegrationTest test`，全部实际执行并通过，保存报告。
- [ ] **Step 5 — 提交。** 核对许可和依赖清单，保存 SQL 边界与 MySQL 验收记录；不执行远程 deploy。

## Task 3: 接入 BroMark 的 Java 25 基线

**Files:**
- Modify: `BroMark/pom.xml` 及六个子模块 `pom.xml`
- Modify: `BroMark/bm-app/Dockerfile`、`BroMark/bm-app/build.sh`
- Modify: `BroMark/bm-app/src/main/java/com/jay/config/ThreadPoolConfig.java`
- Modify: BroMark 中实际使用 javax.annotation、javax.validation、旧安全 API 的源文件（先用 rg 生成精确清单，保留 javax.sql）
- Create: `BroMark/bm-app/src/test/java/com/jay/test/v2/BuildBaselineV2Test.java`
- Create: `BroMark/bm-app/src/test/java/com/jay/test/v2/Java25RouterIntegrationTest.java`
- Create: `BroMark/docs/operations/java25-upgrade-checklist.md`
- Modify: `BroMark/docs/superpowers/specs/2026-10-15-concurrent-credit-payment-raffle-design.md`
- Modify: `BroMark/docs/superpowers/plans/2026-10-15-phase-0-foundation.md` 与实施总览、后续阶段计划中的技术基线引用

**Interfaces:**
- Consumes: Task 2 验收通过的内部 Router 制品。
- Produces: Java 25 / Boot 3.5.16 的构建、测试、应用镜像与受控虚拟线程入口；保持既有分片、资产接口及数据库预算契约。

- [ ] **Step 1 — 写失败测试并审计调用点。** BuildBaselineV2Test 断言 `java.specification.version="25"`、Application class major=69；Java25RouterIntegrationTest 覆盖实际 DAO、默认库和两库四表事务回滚。枚举全部路由 DAO SQL、自定义 Executor、Java EE 导入和安全配置文件，写入 checklist，不能用抽样替代完整清单。
- [ ] **Step 2 — 验证红灯。** 先修复测试发现，再以旧依赖运行目标测试，记录版本及路由断言失败；保留旧 JUnit 4 测试并用 Vintage 验证其执行数。
- [ ] **Step 3 — 迁移依赖和运行时。** 安装内部 Router 至本地 Maven 仓库供隔离验证；Boot 统一至 3.5.16、MyBatis Starter 至 3.0.5，移除跨 BOM 覆盖。release=25，确认 Lombok 在 Java 25 的显式 annotationProcessorPaths 配置；迁移 Jakarta EE API 和 Spring Security 6，回归原认证/授权语义。Docker 和 Maven 运行时统一 25；JAVA_OPTS 放在 -jar 之前。数据库许可边界完成前不开启虚拟线程入口；启用时显式配置自定义 Executor 生命周期和并发准入，不假定 Boot 开关能替换它。同步设计与所有计划中的 Java 17、major=61、javax/Security 5 基线。
- [ ] **Step 4 — 验证整体。** 在 BroMark 运行 `mvn -pl bm-app -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BuildBaselineV2Test,Java25RouterIntegrationTest,AliasMethodAlgorithmTest test`，核对新旧测试实际计数；执行完整测试并区分环境失败。构建镜像、验证镜像 Java 25，再在隔离环境执行真实 DAO、MQ/Redis、序列化及认证冒烟。虚拟线程入口断言实际 `Thread.currentThread().isVirtual()`，验证许可上限和关闭释放；不宣称未测吞吐提升。保存依赖树、effective-pom、镜像 digest、测试计数和未解决项。
- [ ] **Step 5 — 提交与衔接。** 检查 diff 和文档基线一致性，提交可回退的依赖/运行时迁移；不变更生产数据库和部署。通过后继续阶段0的事务执行器、数据库预算及身份入口任务。
