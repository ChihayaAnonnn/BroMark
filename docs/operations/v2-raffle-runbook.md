# v2 抽奖库存操作记录

004 通过 `apply_migrations.py --env v2-it --through 004` 显式执行，禁止应用启动自动 DDL。`verify-raffle.sh` 只使用隔离测试容器，沿用环境变量凭据。生产迁移与切换仍属于阶段6。

新抽奖同时要求 `bm.v2.enabled=true`、`bm.v2.messaging.enabled=true`、`bm.v2.raffle.enabled=true` 和 `asset-write-mode=V2`。默认不开启 raffle。现有旧写围栏仍生效，不能混用旧抽奖或 Redis 库存同步。

策略通过 StrategyPublicationService 发布；库存预算通过控制库短事务中的 StockReservationService.createPool 创建，addStock 以业务ID、增量和原因补货/核减。池、初始化日志一起提交；概率换版只换策略引用，不复制或重置池。不能用现有未审计池冒充新预算。

正常流为 ACCEPTED → CANDIDATE_FIXED → RESULT_RECORDED → COMMITTED → DELIVERED。RESULT_RECORDED 为取消分界，之后取消返回409。此前取消进入 CANCELLING，库存 CANCELLED 回执提交后才退还原总/月/日额度。库存 HELD 超时和用户库不可达均不自动释放。

抽奖与库存恢复器使用独立 RECOVERY 许可、每物理库/表最多8项；先在事务外观察另一域，再以本域版本、ACTIVE 状态核对。扫描游标与业务 last_progress_at 分离，不因重扫抹掉最老 HELD 年龄。事实缺失或依赖不可达告警并保守占用，不能重新随机。

对账为显式维护操作：RaffleReconciliationService.check(RouteSlot) 在所选物理库锁账户或池以获得一致结果，不能无预算地定时执行。报告 checkedScopes 说明实际检查范围；payment/signin 未检查，其0不能解释成已核对。差异仅暂停有关账户/池，保留原数字与核查证据。大库对账需要在阶段6补批次/维护预算。

暂停恢复沿用 [可靠消息恢复流程](v2-recovery-runbook.md)：记录操作者、原因、证据和期望对象版本，调用 RecoveryService.resume。DRAW、RESERVATION、ASSET_RECONCILIATION、STOCK_RECONCILIATION 接入实际对象版本；恢复不得改写已固定候选、权益或库存数字。未知历史奖品进入 WAITING_FIX，查询仍显示业务阶段及恢复状态，隐藏不可交付奖品。

v2_raffle_stage 保存各阶段首次提交的数据库UTC时间；订单 confirmed_at/delivered_at 用于确认/交付延迟。公开活动接口只返回版本与展示字段；cachedAvailable=null 表示尚无库存展示投影，不当作0库存。交易读取主库准入和实际库存，禁止从展示缓存裁决。
