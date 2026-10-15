# V2 可靠消息与核查恢复

## 开启与验收

先显式执行 `001`—`003` 迁移，包括追加的 `003_recovery_guard.sql`；应用启动不执行 DDL。仅隔离环境允许迁移脚本实际执行，真实环境采用阶段6维护窗口与审计流程。设置 `bm.v2.enabled=true`、`bm.v2.asset-write-mode=V2`、`bm.v2.messaging.enabled=true` 后开启新链路。消息开关默认关闭。

`bm.v2.messaging.prefix` 默认 `bromark.v2`，声明持久 direct exchange `<prefix>.events` 与独立持久队列 `<prefix>.workflow`、`<prefix>.notifications`。`queue-type` 默认 `quorum`，隔离实验可显式选择 `classic`；改变既有队列类型必须换队列名并走迁移。当前实验只有单节点，不能据此声称副本/磁盘故障容灾；生产需要至少三个独立节点、磁盘与副本故障验收。连接信息来自 `spring.rabbitmq`，新发布器内部独立连接，不替换 Boot 的旧 RabbitTemplate。

复现命令（密码由环境注入）：

```sh
export V2_MYSQL_PASSWORD=... V2_RABBIT_PASSWORD=...
MAVEN_CACHE_DIR=/tmp/bromark-router-audit-20261015/m2 docs/operations/verify-messaging.sh
```

该脚本明确应用003、执行迁移测试、Router及全部v2针对性测试，构建Java25应用镜像；不会部署应用。隔离 MySQL/RabbitMQ/Redis 的启动方式见 `assets-v2.md`。历史无限等待演示测试另用有限时间运行，不能将针对性验收说成整个旧套件通过。

## 消息边界

每库一张 Outbox/retry_job，源库枚举 `00/01/02`；用户订单四张分表不代表四张 Outbox。claim/完成/重试都是源库短事务；目的槽只用于消费路由。认领用数据库 UTC 时间、SKIP LOCKED 和代次 token，旧 token 无法完成或退回新代任务。过期和待发、业务类型轮转，有界批次8、最多128；已有 pending/expired 索引承担到时扫描。尚未做生产积压量性能验收，不能从 EXPLAIN 或功能并发测试推导吞吐。

mandatory + correlated confirms + persistent message；Return、broker Nack、超时/连接断开均不能记 PUBLISHED。连接断开产生的框架 negative confirm 归 UNKNOWN。Confirm仅证明发布，不证明业务完成。确认后状态更新失败会重投。发送器2线程、有界队列和16在途许可，专用连接限制channel数量，3秒确认超时。重试最多12次，约1秒起，指数退避，上限5分钟，向下20%抖动；消费者失败计数从数据库和消息头取最大值，旧投递不重置预算。

消费本地事务包括 Inbox、业务回执、资产/状态副作用和响应 Outbox。相同eventId摘要变化进入核查；同业务、不同eventId使用原回执生成新的响应运输事件，不再执行资产处理器。业务回执键包括consumerName/eventType/bizId.type/bizId.id；摘要冻结用户、目的槽、业务身份及payload。所有阶段判断与资产约束仍必须由领域处理器实施，公共消息层不替代状态机。

控制类型去db00，即使携带userId：ReserveAward、ConfirmReservation、CancelReservation、CreditGranted。用户类型核对userId与目的槽：PrepareDraw、ReservationHeld、ReservationConfirmed、ReservationCancelled、AwardDelivery、GrantCredit。ExchangeCompleted、SignInCompleted只进入notification-v2的本地投影，不发资产。

未知schema/type、伪造目的槽、非法头和超大消息先保存原始字节至db00.message_quarantine，并建立RAW_MESSAGE核查单，提交后Ack。不执行payload。原始证据仅限审计读取，普通日志不输出正文。短暂失败先在目的库独立短事务持久化retry_job，再Ack；数据库不可用或持久化失败则不Ack，暂停对应消费者。容器停止时broker可重新入队，5秒后退避重启；不会立即无限requeue。

## 暂停、恢复与扩展

recovery_guard 提供稳定的协调身份锁，pause、消费、resume 和剩余事实扫描共用。UNBOUND 核查版本跨 case 递增，首次创建 guard 从既有 case 最大版本接续。锁顺序为领域对象、协调身份、retry 行。

重试按运输代次幂等记录失败。旧代副本由已有 retry_job 接管，不创建成功 Inbox、不执行处理器、不递增计数或推迟 next_attempt；真正到期认领后的新代消息才能执行，12 轮失败才耗尽。已延期事件出现相同身份的不同内容副本时，保留首次事实，冲突原文另存 raw quarantine 并建立核查记录，成功持久化后才 Ack。

`recovery_status` 与业务 `state` 分离。REVIEW/WAITING_FIX/RETRY_EXHAUSTED均停止自动业务推进；晚到事实写deferred_fact，无成功Inbox、资产变化或成功响应。每对象最多一个active_key非空核查单，关闭时置空，保留审计历史。DEAD是任务状态，不代表业务回滚。

阶段4/5通过 `RecoveryAggregatePort` 注册真实领域适配器，Spring bean名使用 `DRAW`、`RESERVATION`、`PAYMENT`；用户积分交付可注册 `CREDIT_DELIVERY`。适配器必须在当前库锁对象、以version更新恢复状态、判断终态及从原固定事实生成RecoveryDecision；不存在对象返回null。`scanActive(limit)`只返回当前库ACTIVE对象，恢复决定不得生成新业务ID、奖品、金额或策略快照。事件处理器以事件类型为Spring bean名实现EventHandler，返回响应事件列表；Inbox/回执缓存和Outbox由公共UnitOfWork提交。当前没有虚构抽奖或支付领域适配器，缺失处理器建立WAITING_FIX。

源Outbox的owner业务类型不在EventEnvelope内。发布耗尽时，以`OUTBOX_TASK`和原eventId绑定核查单、保存完整固定事件，不猜测目的对象是源订单；领域恢复器需要把这些任务纳入其事实核查。retry_job属于已知目的对象，耗尽时同事务暂停对应领域对象/核查身份，并保存待处理事实。

人工恢复入口是受控运维调用 `RecoveryService`，本阶段没有暴露裸路由HTTP管理接口：

1. 从所属数据库定位case、原阶段、错误证据、当前对象版本、Inbox/资产流水及跨库事实。跨库暂停不是原子冻结，重新读取双方。
2. 修复代码/配置或核对已提交权益；不得手工改积分、库存、固定奖品、付款阶段或bizId。未知业务对象保留UNBOUND任务身份，不造订单。
3. 在业务事务外调用 `BlockOrderIdGenerator.prefetch()`。通过受控的操作身份调用 `resume(slot,aggregateType,id,expectedVersion,operator,reason)`；operator/reason必填，过期版本或重复恢复失败。记录caseId及调用结果，不开放任意外部routeSlot输入。
4. 同事务恢复ACTIVE、关闭case、写恢复Outbox。每个待处理事实保留bizId/payload/hash，换eventId并记录replayed_event_id。一次最多128条；余下事实由RecoveryScanJob仅在case已关闭且对象ACTIVE时分批重放。再次暂停会阻止扫描。终态只结案并标记TERMINAL_NOOP，不回退业务阶段或发新命令。
5. 对OUTBOX_TASK使用原eventId作为恢复对象ID、case.object_version作为版本；原DEAD任务保留，新运输事件重述原事实。RAW_MESSAGE需先通过升级后的解码/白名单/领域适配器验证原始证据，再由受审计工具重新交付；不能让通用resume自动执行未知payload。查单确认业务终态，不能凭MQ Ack结案。

## 停止与重启

正常关闭由V2消息生命周期先停止新认领，排空正在发送的Confirm及源Outbox更新，停止恢复扫描和消费者，再由阶段0生命周期停止数据库许可、排空事务并关闭连接池。不能直接杀数据库后声称消息已排空。关闭失败时保留PROCESSING及租约，重启后过期接管；不删除Outbox、Inbox、retry_job、case或deferred_fact。

依赖故障时确认数据库主库与broker健康，查最近case和DEAD任务，先修复依赖再受审计恢复；不通过清空Inbox或重置attempt“解锁”。旧调度器在V2写入模式不启动，旧队列/已发消息的迁移与去重仍归阶段6验收。
