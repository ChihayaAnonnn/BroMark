# Java 25 与 DB Router 升级评估

评估日期：2026-10-15。用户已选择 Java 25 LTS 为升级目标。

## 当前模块归属

用户确认Router为其此前开发的模块，并要求整合。当前实现位于BroMark/bm-router，与应用统一构建；下文独立下载、许可排查及本地安装描述是前期评估历史，不再作为当前构建流程。当前无需平级源码仓库或预先安装JAR。

## 结论与技术选型

可以推进升级，但 `cn.bugstack.middleware:db-router-spring-boot-starter:1.0.2` 不能作为 Boot 3 的直接替换依赖。建议采用 Java 25、Spring Boot 3.5.16、MyBatis Starter 3.0.5，并维护一个具有独立坐标的 DB Router 适配版本。初始评估完成源码下载、原版行为验证和隔离原型。随后已完成正式本地适配与BroMark Java25/阶段0实施：Router28项、BroMark17项针对性测试通过，镜像已启动验证；[执行报告](../../verification/2026-10-15-java25-foundation/README.md)记录全量旧测试超时及遗留项。未发布适配制品或部署。

[Spring Boot 3.5 官方要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)覆盖 Java 17–25；[MyBatis 官方兼容表](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)将 Starter 3.0 对应至 Boot 3.2–3.5。Boot 4 不属于本次验证范围，升级到 Java 25 无需同时迁移 Boot 4。

Java 25 可使用虚拟线程。现有 ThreadLocal 可在虚拟线程上隔离路由，不必改成 InheritableThreadLocal；子任务必须在执行线程内部建立路由，再开启事务。虚拟线程不会增加数据库连接容量，仍需数据库许可与连接池上限。Boot 的 `spring.threads.virtual.enabled=true` 不会自动替换所有自定义线程池；BroMark 的 ThreadPoolConfig 需要单独评估。[Boot 线程执行文档](https://docs.spring.io/spring-boot/3.5/reference/features/task-execution-and-scheduling.html)

## 源码来源

下载目录：工作区 `db-router-spring-boot-starter/`，与 BroMark 平级。

- 原始快照 `src/`（独立Router仓库根提交d3aad34；当前分支已改造）：从 Maven Central 的 [1.0.2 sources.jar](https://repo.maven.apache.org/maven2/cn/bugstack/middleware/db-router-spring-boot-starter/1.0.2/db-router-spring-boot-starter-1.0.2-sources.jar)提取的 13 个 Java 文件和 spring.factories，保留作者头部。
- 原始快照 `pom.xml`：同版本二进制jar内嵌POM，包含旧构建和发布配置；当前正式POM已移除这些配置，未运行release/deploy/sign。
- `upstream-checksums.json`：发布 jar 和源码 jar 的 SHA-256。
- `compatibility-prototype/`：独立适配原型，具有自己的 POM、源码和验收测试，不覆盖原始源码。

截至本次检索，Maven Central 未发现更新的 Boot 3 兼容版本。原源码注释指向 GitCode，但未取得完整 Git 历史；同名 GitHub 仓库只有 README 等文件，不能当作原始发布源码历史。正式维护前需核实上游许可与再分发条件、建立独立版本历史，不能从同名仓库推断许可。

## 原版问题与改造边界

| 部位 | 现有行为 / 风险 | 适配要求与状态 |
|---|---|---|
| 自动配置 | 只有 spring.factories 注册，Boot 3 不发现 | 原型增加 AutoConfiguration.imports，并在 JDBC 默认配置前加载；已验证 |
| 嵌套路由 | 注解 DAO 的 finally 无条件 clear，清掉调用方路由 | 原型保存并恢复调用前上下文；已验证 |
| 路由参数 | DTO 的 null 字段被转成字符串 `"null"` | 原型拒绝空路由；已验证 |
| 错误库号 | AbstractRoutingDataSource 默认可退回默认库 | 原型禁用未知键 fallback，将 db00 显式加入映射；未知库拒绝已验证，显式 db00 仍需专项测试 |
| 事务路由 | Spring 已绑定连接后，修改 ThreadLocal 不会换实际连接 | 原型拒绝活动事务中的库/表键变更；库键已验证，表键及异常原子性待补 |
| 事务管理器 | TransactionTemplate 内部新建私有管理器 | 原型改用共享 PlatformTransactionManager bean；已验证 |
| 驱动配置 | 缺省 driver-class-name 填为 HikariDataSource 类名 | 原型缺省时交给 Boot 推导；原版测试也通过，不能列为已复现故障 |
| SQL 改写 | 正则替换 from/into/update 后单词，不能可靠解析复杂 SQL | 原型保留；仅验证简单单表查询。正式版限定语法并拒绝不支持语句，不能声称支持 join/子查询/反引号等 |
| 资源关闭 | 动态数据源包装多个连接池，关闭与失败回收缺少显式保证 | 原型未修复；正式版必须覆盖容器关闭及部分初始化失败 |
| 分片算法 | 两库四表使用原 hash 扰动和位掩码 | 保持完全一致，不迁移数据；正式版验证正数且总槽数为 2 的幂 |

Boot 3 不再读取 spring.factories 中的 EnableAutoConfiguration 条目，需新的 imports 文件。[官方迁移说明](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)

原型在切面上增加了早于事务切面的顺序，但尚未用实际 Spring 代理上的 `@Transactional` 验证该顺序。事务内切换表键与库键必须整体预检，禁止失败后留下半更新的上下文；当前两个 setter 独立检查的原型不能代替正式的原子性实现。

## 已执行验证

运行环境：Docker arm64，Maven 3.9.11，Temurin **25.0.1+8**，Boot 3.5.16，验收测试使用 MyBatis Starter 3.0.5、H2 2.3.232（MySQL 模式）。这是确切实测版本，不代表最新 Java 25 安全补丁；正式环境需另行选择当前受支持补丁并锁定镜像 digest。

镜像：`maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c`。

| 测试组 | 执行数 | 失败 | 错误 | 跳过 | 含义 |
|---|---:|---:|---:|---:|---|
| 原版行为探针 | 7 | 0 | 0 | 0 | 断言原版问题确实存在，以及 ThreadLocal 隔离等行为 |
| 原版验收测试 | 8 | 7 | 0 | 0 | 同一套期望行为暴露不兼容和路由问题 |
| 适配原型验收测试 | 8 | 0 | 0 | 0 | 有限适配可以消除这些已复现问题 |

原型验收包括自动配置发现、嵌套恢复、null 拒绝、未知库拒绝、事务内切库拒绝、共享事务管理器、驱动推导，以及 **1,000 个虚拟线程任务**。后者在每个任务内部计算路由、开启事务、调用带注解的 MyBatis Mapper 读取物理表，覆盖两库四表全部 8 个槽，断言结果与原 hash 一致并清理上下文。

测试汇总和原版测试源码见 [验证材料](../../verification/2026-10-15-db-router-upgrade/README.md)。这些是 H2 功能验证，不是 MySQL InnoDB 锁语义、吞吐量、连接预算或 BroMark 全链路验收。未验证 Java 21 或 Boot 4；未执行 BroMark 全量测试。

## 正式改造约束

1. 保留 cn.bugstack 原 Java 包名以降低调用方改动，但使用内部独立 Maven 坐标，禁止覆盖上游 1.0.2。
2. 保留现有 userId → db01/db02、表后缀 000–003 的映射；默认控制库显式 db00。
3. 路由先于事务取连接；活动事务不能切换路由。异常、嵌套调用和任务退出均恢复或清理上下文。
4. 不靠无限创建虚拟线程绕过连接预算；业务事务内不启动并行子任务，也不传播 JDBC Connection/事务 ThreadLocal。
5. 先补完路由、SQL 边界和资源生命周期验收，再接入 BroMark。原型不能直接用于生产。
6. BroMark 迁移还包括 javax.annotation/validation 等 EE API 到 jakarta、Spring Security 6、Lombok 注解处理及 Redisson/序列化兼容；`javax.sql.DataSource` 属于 JDK API，保留原包名。

## 与既有计划的衔接

Java25已实施；阶段0、设计稿与后续计划的技术基线已同步为Boot3.5/Jakarta，0.1复用[升级计划](../plans/2026-10-15-java25-db-router-upgrade.md)的构建验收。阶段0新增事务、预算与身份接口已通过本地针对性验收；后续资产业务按各自阶段实施，不将基础设施通过等同于业务已完成。

## 正式实现验收更新

执行分支：BroMark feat/java25-foundation、Router feat/java25-router。编译/运行为Temurin25.0.4.1+1，Maven3.9.16，六模块所有class major=69。Router完整语义AST字段遍历已补充IS NULL、ORDER BY/GROUP BY、duplicate-update子查询和限定通配符的审查回归。28项包含真实MySQL两库四表、1,000虚拟线程；BroMark17项覆盖真实DAO、Spring配置、预算、JWT和中间件。原型章节为历史证据，正式状态以执行报告为准。上游许可仍未确认，仅本地安装，禁止对外发布。
