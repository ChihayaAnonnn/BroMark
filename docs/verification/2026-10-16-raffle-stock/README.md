# Phase4 抽奖与库存验收

在当前 BroMark 目录的 `feat/phase4-raffle-stock` 分支开发，基于 Phase3 `f32de58`；没有合并、推送或部署。最终源代码提交 `75a69a1`；应用98项（阶段4新增31项）、Router28项、迁移Python4项均通过，failures/errors/skipped均为0。完整计数与方法名见 [results.json](results.json)。独立只读审查提出4项Important、无Critical/Minor；四项均先复现失败再修复。

实现不可变策略及共享池引用、整数分随机积分、默认/权重/冻结黑名单/当日解锁、版本缓存和公开查询；用户短事务原子扣次、保存冻结规则输入；库存预留/确认/取消墓碑；用户取消互斥、原周期退款；积分与 NONE 幂等交付；独立恢复任务、核查恢复与显式对账。交易不读取展示缓存，不写 Redis 权威库存。

隔离环境使用 Java 25、Boot 3.5.16、MySQL 8.0 InnoDB/READ COMMITTED、RabbitMQ 和 Redis Cluster。执行：

```sh
V2_MYSQL_PASSWORD='<isolated password>' \
V2_RABBIT_PASSWORD='<isolated password>' \
MAVEN_CACHE_DIR=/tmp/bromark-router-audit-20261015/m2 \
docs/operations/verify-raffle.sh
```

真实测试包括：100个同键受理、独立实例数据库预算下争抢共享库存/候选、固定随机区间端点、旧版本实际淘汰重建、100个冷查询合并和失败回源节流、取消/结果竞态、跨月退原桶、暂停后晚到回执与审计恢复、确认回执丢失、关闭真实用户库连接池保留HELD、发奖技术回滚、真实RabbitMQ七段流程、库存不足NONE、HTTP鉴权/跨用户403/取消409，以及数字守恒但流水缺失的对账失败用例。

[reconciliation.json](reconciliation.json) 为两个用户库六类账实检查和控制库库存守恒、预留计数、日志计数的实际结果；[migration.json](migration.json) 记录004的9个实际迁移身份和校验和。payment/signin不属于本轮抽奖对账范围，不能用未检查项的0宣称通过。

[stage-timings.json](stage-timings.json) 保存真实HTTP样本各阶段首次提交的数据库UTC时间，覆盖 ACCEPTED、CANDIDATE_FIXED、HELD、RESULT_RECORDED、CONFIRMED、COMMITTED、DELIVERED。它不是容量、P99或生产性能报告。缓存内存记录为整应用heap使用量及版本条数，不能解释为缓存独占内存。

已观察的故障红灯与修复记录：

- 新接口缺失：strategy/stock/draw/progress/integration编译红灯，真实数据库行为绿灯。
- 连续失败查询100次回源：改为3秒失败缓存，目标变为一次失败回源。
- 守恒但删除扣次事实、库存改数不记日志：新增订单/流水关联及库存日志检查，定位并暂停相关对象。
- 未支持历史奖品停留ACTIVE且重试：改为WAITING_FIX，保留业务阶段；查询仍返回阶段和恢复状态。
- 累计次数错误满足当日解锁：冻结的当日次数不足仍返回兜底。
- 库存写入无事务、已有未审计池重复初始化：显式拒绝，不能产生分离提交的数字和日志。
- Redis Cluster测试容器重启后保留noaddr节点：只重新介绍本测试栈三个节点的当前IP，验证完整节点就绪，不重置槽位或数据。

独立审查补验：取消完成后首次晚到HELD仍保持CANCELLED/ACTIVE且只退一次；发布拒绝同ID不同权益的兜底；新建预算占满时取消仍可受理；NONE奖品即使错误流水与余额同步增加也必须被对账识别。

完整旧演示套件另行无筛选执行，使用40秒fork超时；`RaffleActivityControllerTest.test_draw_blacklist` 第61行的 CountDownLatch.await 无限等待仍导致超时。此已知基线限制未改写，后续旧测试不能声称全绿。

测试夹具会刻意建立仅库存域的订单、暂停单及错误消息，因此恢复日志中存在权威用户订单缺失、不可达和不可路由告警；这些用例保留事实，不据此释放库存。第一次完整应用测试曾叠加默认dev profile并出现远程Redis PING警告，立即停止拥有的测试容器，随后显式仅启用v2-it；未向远程环境提交业务事务。

公开活动 cachedAvailable=null 表示没有库存展示投影；最终奖品只在 COMMITTED/DELIVERED 展示。128版本为初始缓存上限；大库对账目前为显式维护操作，生产批次预算、真实缓存保留内存与容量实验由阶段6完成。生产数据迁移、历史切换和多节点RabbitMQ高可用不属于本次验收。

操作入口见 [runbook](../../operations/v2-raffle-runbook.md)，所有实施裁决及其代价见 [rulings.md](rulings.md)。
