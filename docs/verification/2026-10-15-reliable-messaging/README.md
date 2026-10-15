# 阶段3：可靠消息验收

实现分支 `feat/phase3-reliable-messaging`，基线 `9f7a639`。本阶段提供公共消息/恢复机制，不实现阶段4抽奖库存、阶段5支付领域或阶段6真实存量切换。

## 功能与故障覆盖

- 源库短事务SKIP LOCKED认领、数据库时间、到期接管、新token围栏；目的槽与源Outbox分开。实际持锁时另一个待发任务仍可认领，未来执行类型不阻塞已到期业务类型。
- 持久拓扑/消息、mandatory、correlated Confirm/Return；真实broker无绑定Return，发送后未写状态重投。真实AMQP TCP代理截断broker Basic.Ack并关闭连接，broker队列已有消息而发布结果UNKNOWN。
- Inbox、业务回执、资产/状态与响应Outbox同事务。相同运输事件不重做；两实例不同eventId同bizId串行收敛一次副作用，恢复事件重发原响应。
- 本地持久延迟重试后Ack，累计次数不归零；12次耗尽DEAD+核查事实；新eventId无法绕过消费/重试发布耗尽的暂停。真实关闭数据库连接池时不Ack、不执行处理器；重试持久化失败时真实broker投递保持unacked，关闭物理channel后才能重新取到。
- 未知schema/type/路由先存原始证据及核查单，拒绝执行payload；缺处理器WAITING_FIX。错误业务阶段回滚Inbox、资产和响应。
- pause保留原阶段；晚到事实去重落库；resume验证版本、操作者和原因，保留bizId/权益内容、换eventId。测试专用InnoDB领域表验证RESULT_RECORDED不回退，终态只结案；130条积压先128后扫描剩余2条。
- JVM Asia/Shanghai下重试/租约仍使用UTC DATETIME，业务时区继续Asia/Shanghai。
- 完整应用启动独立V2消费者，通知投影不发资产；实际应用close在Confirm后/状态写入前等待，期间数据库许可仍可用，最终Outbox PUBLISHED后才完成关闭。

## 复现与边界

运行 `docs/operations/verify-messaging.sh`，连接隔离MySQL8.0.46、Redis7.4.11、RabbitMQ3.13.7；Java25.0.4.1+1、Boot3.5.16、Spring AMQP3.2.12。密码来自环境；脚本应用003并记录checksum/步骤、执行迁移/Router/v2测试、构建镜像，不部署。

测试计数、测试方法和构建物指纹见 `results.json`。当前实验单节点broker，不能据此声称副本容灾或生产容量。发布确认不代表业务完成。没有真实DRAW/RESERVATION/PAYMENT适配器，缺失处理器不会静默执行。

无-Dtest过滤的完整旧套件在隔离历史fixture、v2-it profile、40秒fork上限下仍超时：`com.jay.test.trigger.RaffleActivityControllerTest.test_draw_blacklist` 第61行无限CountDownLatch.await（无countdown）。Router28及RaffleStrategyControllerTest通过，后续旧测试未完成；不能宣称完整套件绿灯。源码既有阻塞未为本阶段改写。

## 执行取舍与代价

1. 沿用阶段1/2 JDBC仓储，共用当前路由事务连接，未重复生成无调用的MyBatis DAO/XML；代价是文件拆分与原计划不同。
2. 未知schema原字节先核查，通用resume不自动执行；代价是修复后需要受审计工具验证并重新交付。
3. 下游适配器键统一DRAW/RESERVATION/PAYMENT及CREDIT_DELIVERY/NOTIFICATION；无合法对象保存UNBOUND任务身份；代价是后续适配器必须正确注册、返回/锁定真实对象。
4. EventEnvelope不含源业务类型，源发布耗尽绑定OUTBOX_TASK+eventId，不猜目的对象为源订单；代价是阶段4/5领域恢复需要纳入这类任务核查。完整固定事件仍可人工恢复重述。
5. resume一次128事实，其余由已结案且ACTIVE的扫描器继续；代价是较大积压跨多个轮次完成。
6. V2模式关闭旧消息/库存调度器，旧模式行为保留；代价是旧待发任务需阶段6迁移，不能直接在新链路消费。
7. 3.3消费与3.4恢复核心共同提交，随后单独提交应用接线/关闭与runbook；代价是提交边界与任务编号不同。

独立审查范围 `9f7a639..bc14918`，4项Important、0项Critical、0项Minor，全部已复现并修复：重复消息消耗预算、延期冲突证据丢失、UNBOUND版本ABA，以及无领域对象时暂停与消费/扫描的竞态。6项新增审查回归先失败再通过；原耗尽测试已改为12轮真实认领的重试代次。从既有case版本7接续为8也已验证。

最终代码 `1e33b55`，整体验收应用67项、Router28项、Python迁移4项，失败/错误/跳过均为0；Java25镜像构建通过。新增 `003_recovery_guard.sql` 保留已执行003脚本校验和，代价是额外公共协调表及迁移步骤。详细方法与构建物指纹见 results.json。

审查保留的阶段4/5领域适配器、源任务归属、JDBC方式、原始未知消息人工修复、阶段6旧任务迁移，以及多节点容量验收，仍按前述取舍和边界执行。完整旧套件无限等待保持披露，不自动合并。

## 本地证据

原始日志不含业务原文交付物：`/tmp/phase3-pre-review.log`（阶段3功能），`/tmp/phase3-reviewed-acceptance.log`（整体验收），`/tmp/phase3-full-legacy.log`（完整旧套件超时）。故障红灯：`phase3-confirm-loss-red.log`、`phase3-exhaust-red.log`、`phase3-unbound-red.log`、`phase3-retry-dead-red.log`、`phase3-timezone-red.log`、`phase3-fairness-red.log`。仓库保存可复现脚本及按测试名汇总结果，原始临时日志只作本机辅助。

Final build: `/tmp/phase3-final-build.log`; review RED: `/tmp/phase3-review-red.log`; asset reconciliation: `reconciliation.json` (all six mismatch counts zero in each user database). Full decisions: `rulings.md`.
