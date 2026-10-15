# BroMark 积分支付与抽奖实施总览 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将设计稿拆成可独立测试、逐阶段评审和受控发布的实施任务。

**Architecture:** 保留原六个业务Maven模块，新增bm-router技术模块与用户两库四表布局。用户资产操作使用同库短事务，支付与库存跨库通过 Outbox、Inbox、固定业务身份和状态机推进。按阶段0→1/2→3→4→5→6执行，不同时改造其他仓库。

**Tech Stack:** Java 25、Spring Boot 3.5.16、MySQL 8.0 InnoDB、MyBatis、Redis、RabbitMQ、JUnit 5＋Vintage（兼容JUnit 4）、Maven。

**Spec:** [设计文档](../specs/2026-10-15-concurrent-credit-payment-raffle-design.md)，2026-10-15 评审修订版；设计中的性能数字均为实验目标。

## Global Constraints

- 阶段0完成后编译、测试、应用与容器统一Java 25，release=25；使用Boot 3.5.16及Jakarta体系，保留javax.sql等JDK API。
- 执行环境采用 MySQL 8.0 InnoDB，业务短事务使用 READ COMMITTED，关键读取显式加锁。
- 业务时区固定为 Asia/Shanghai。
- 积分使用 DECIMAL(18,2) 与 BigDecimal；拒绝超精度或溢出，不静默舍入。
- 次数 SKU 不限库存；兑换扣款和增次同事务；签到全部奖励同事务。
- 首版奖品只支持积分权益与无权益兜底；策略版本与 stockPoolId 分离。
- 所有交易身份、拒绝回执、取消墓碑和已处理付款事实在其幂等保留期内保留；首版不实现归档、在线重分片或自动资产退款。
- 实施状态以任务复选框和验证报告为准；仅针对隔离环境执行测试，不变更生产数据库或自动部署。

## Review Focus

- 无身份、伪造 userId 或跨用户查单：0.4、2.3 必须拒绝。
- 午夜重试、新周期桶与取消退款：1.3、2.2、4.4 必须使用原日期和固定规则输入。
- 迟到消息、旧租约和新 eventId 重述：3.1—3.4、4.4、5.3 必须收敛原业务。
- 新旧概率版本并发与缓存丢失：4.1—4.4 必须保留同一库存池与原候选。
- 存量重复账户、旧签到及已发消息重投：6.1—6.3 必须阻止混写并保持账实可解释。

## 阅读与执行顺序

| 顺序 | 实施计划 | 核心交付 | 依赖 |
| --- | --- | --- | --- |
| 阶段0：技术基线 | [阅读计划](2026-10-15-phase-0-foundation.md) | Java 25升级、构建测试、路由、资源许可、身份入口 | 无 |
| 阶段1—2：资产、签到与兑换 | [阅读计划](2026-10-15-phase-1-2-assets-signin-exchange.md) | 资产内核、周期额度、原子业务接口 | 阶段0 |
| 阶段3：可靠消息 | [阅读计划](2026-10-15-phase-3-reliable-messaging.md) | 租约、发布确认、消费幂等、核查恢复 | 阶段1—2 |
| 阶段4：抽奖与库存 | [阅读计划](2026-10-15-phase-4-raffle-stock.md) | 策略发布、库存池、抽奖与取消协议 | 阶段3 |
| 阶段5：沙盒支付 | [阅读计划](2026-10-15-phase-5-sandbox-payment.md) | 渠道下单、通知事实、积分交付与查单 | 阶段3；按发布顺序在阶段4后 |
| 阶段6：验收与切换 | [阅读计划](2026-10-15-phase-6-validation-rollout.md) | 迁移演练、故障矩阵、安全容量报告 | 前述全部 |

阶段4与5共享消息和资产内核，允许独立开发分支，但本计划默认顺序执行；执行状态见阶段0及升级验收记录。

## 共享类型与模块边界

阶段0的路由、事务、资源许可与时间契约已实现；其余为后续拟新增契约。DTO 放入 `bm-domain/src/main/java/com/jay/domain/<域>/model/`，每个类型独立文件、不可变字段、显式构造或 builder；统一使用Java 25 API；虚拟线程仅在许可边界后显式启用，也不为升级批量重写现有模型为record。HTTP DTO 放 `bm-api`，不能将登录 userId 从请求 DTO 传入内核。

| 契约 | 所属任务 | 字段或签名 |
| --- | --- | --- |
| RouteSlot / BusinessTime / Deadline / WorkClass | 0.2、0.3 | RouteSlot(dbKey,tableKey)；BusinessTime(Instant,LocalDate,YearMonth)；Deadline(Instant expiresAt)；WorkClass 枚举 TRADE、CALLBACK、QUERY、RELAY、WORKER、RECOVERY |
| UserTransactionRunner | 0.2、0.3 | `<T> T inUser(String userId, WorkClass work, Deadline deadline, Supplier<T> action)`；`<T> T inControl(WorkClass work, Deadline deadline, Supplier<T> action)`；`<T> T inSlot(RouteSlot slot, WorkClass work, Deadline deadline, Supplier<T> action)`仅供固定清单扫描与原库任务更新，不接收HTTP裸槽参数 |
| RequestIdentity / BusinessId / QuotaDelta | 1.2、1.3 | RequestIdentity(userId,operation,requestId,requestHash)；BusinessId(type,id,hash)；QuotaDelta(totalDelta,dayLimitDelta,monthLimitDelta)，三个增量均为非负 long |
| AssetReceipt / QuotaReceipt | 1.2、1.3 | AssetReceipt(bizId,delta,balanceAfter)；QuotaReceipt(bizId,activityId,totalRemaining,dayRemaining,monthRemaining,businessTime) |
| EventEnvelope | 1.4 | eventId、eventType、aggregateId、bizId、userId、routeSlot、requestHash、aggregateVersion、schemaVersion=1、payload；payload 为完整版本化 JSON，routeSlot表示目的业务库表，Outbox仍写源业务事务所在库 |
| AdmissionSnapshot | 2.1 | 活动ID、activityState、发布版本、起止时间、读取时间、当前调用截止时间、不可变SKU或签到权益、黑名单判定；不能跨入口调用复用 |
| RecoveryStatus | 3.4 | ACTIVE、REVIEW、WAITING_FIX、RETRY_EXHAUSTED；与业务 state 分列 |

接口方法抛出的技术异常使事务回滚；确定性拒绝使用明确 Result 对象并正常提交。所有交易 DTO 带请求摘要及原回执，后续查询另外读取当前订单状态，不能将初始 ACCEPTED 回执当作最终奖品结果。

消息路由按事件类型白名单判定：ReserveAward、ConfirmReservation、CancelReservation、CreditGranted去db00；PrepareDraw、ReservationHeld、ReservationConfirmed、ReservationCancelled、AwardDelivery、GrantCredit去用户分库。用户目的事件核对routeSlot与userId一致；控制目的事件允许携带userId但必须使用控制槽，不能因有userId而路由用户库。ExchangeCompleted、SignInCompleted仅供通知/投影，在003拓扑中绑定独立消费者，不触发再次发资产。

所有重放共用原bizId和内容摘要，运输事件使用新的eventId。1.2采用db00持久分配、不复用的号段实现全局序号；号段只在用户事务外预取，本地耗尽先返回技术失败并补充，不能在账户锁内申请号段。订单号格式为`类型-逻辑槽-生成节点-全局序号`，解码与生成由RouteCodec统一；eventId共用该序号空间并采用事件类型前缀。号段大小1000是首轮配置，崩溃未用部分废弃，提交结果未知的号段不发号，序号溢出停止分配；不依赖时钟或节点租约保证唯一。

## 测试环境和命令

阶段0已将skipTests默认改false，Surefire由Boot父POM管理，JUnit4通过Vintage执行。新增测试沿用 JUnit 4，并以 `V2Test` 结尾。单元测试不连接远程依赖；集成测试使用独立 `v2-it` profile 的 MySQL 8.0、Redis、RabbitMQ，连接信息来自环境变量。不得直接运行含既有用户数据的 dev/prod profile，也不把 H2 或 mock 锁当作 InnoDB 并发验收。

所有计划中的 Maven 命令在仓库根目录执行；`-Dsurefire.failIfNoSpecifiedTests=false` 仅允许上游模块没有目标测试，bm-app 目标类必须有非零执行数。每次运行保存该类 Surefire XML，任一目标集成测试被 skip 或缺依赖均视为尚未完成。测试失败步骤可以先编译失败，但最终绿灯必须包含真实行为断言。

测试类可直接使用 JUnit 4 的 assertEquals、assertTrue、assertFalse、assertThrows；并发辅助由0.2提供 `ConcurrentProbe.run(int, Callable<T>) -> List<T>`，用栅栏统一放行并收集所有异常，禁止 sleep 充当竞态同步。数据库故障点由测试专用 `FaultInjector.hit(String point)` 抛异常，生产实现为不可配置的空操作；业务时间测试替换 `BusinessClock`，生产从已锁账户的当前连接读取数据库时间。

计划中的测试代码展示关键断言，不是已经存在的测试文件；各任务的@Before负责构造所列命令、数据库样本和服务。若接口仍未实现可先获得编译红灯，实现类型后应确认行为红灯，再补齐逻辑；不能只将无法编译改成可编译就完成TDD。

## 数据库迁移约定

不引入 Flyway；使用版本化 SQL、scope manifest 和显式执行器，迁移不在应用启动时自动执行。`docs/dev-ops/mysql/migrations/manifest.json` 为每个脚本列明控制库/用户库、逻辑表/四张分表、执行顺序和校验和。001资产、002签到兑换、003消息恢复、004策略库存抽奖、005支付、006切换约束分别随对应任务编写；默认脚本只做兼容性新增，存量清理、期初回填和非空/唯一约束最终收紧须维护窗口内按6.1执行。

MySQL DDL不是整批原子回滚；执行器保存每库每脚本进度，checksum不一致立即停止，失败后核对 information_schema 和已完成步骤，再重入未完成步骤，不运行通用 DROP 回滚。全量历史导出 SQL 保持原样，不把迁移直接塞回旧初始化文件。测试库按全新建库路径应用同一套迁移，再测试存在存量冲突的升级路径。

## 设计覆盖与验收归属

| 设计内容 | 任务归属 |
| --- | --- |
| I1— I3、I11、I12：账户、请求、签到、兑换 | 1.1—1.4、2.1—2.3 |
| I4：付款身份及唯一履约 | 5.1—5.4 |
| I5— I8：扣次、结果、库存、发奖 | 4.1—4.5 |
| I9— I10：Outbox、租约和业务恢复 | 1.4、3.1—3.4 |
| 技术兼容、事务路由、连接预算、身份 | 0.1—0.4、6.3 |
| 监控与期初/流水对账 | 0.3、1.1、4.5、5.4、6.1—6.3 |
| 旧写退出、版本化队列、已发消息去重、回滚 | 2.3、6.1—6.3 |
| 性能目标与证据边界 | 6.2—6.3 |

## 执行检查点

- [x] 0.1构建测试基线已完成；阶段0针对性验收通过，完整旧演示测试超时情况见执行报告。
- [ ] 每个任务按失败测试→最小实现→通过验证→小提交执行；每份计划退出时核对设计覆盖表。
- [ ] 每阶段产出实际配置、测试计数、故障记录与账实报告，执行者再更新复选框。
- [ ] 阶段1—2先隔离验收；可靠消息、迁移演练完成前不对真实账户开放新旧混写。
- [ ] 阶段6形成具体发布包和停止/恢复步骤；任何外部部署另按用户授权执行。

## 机制参考

- [Boot 3.5依赖管理](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html)：用于统一框架依赖。
- [RabbitMQ确认机制](https://www.rabbitmq.com/docs/confirms)：发布确认不等于消费成功，不可路由消息需要单独处理。
- [k6固定到达率](https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/constant-arrival-rate/)：到达率场景需要足够VU，迭代与实际请求数分别记录。
