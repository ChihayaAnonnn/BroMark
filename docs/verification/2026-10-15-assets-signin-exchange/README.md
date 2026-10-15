# 阶段1—2：资产、签到与兑换验收

2026-10-15。实现分支feat/java25-foundation，代码提交范围7d30974..6fdcf96。已完成本阶段隔离开发与验收；没有生产迁移、合并、推送或部署。

## 结果

| 验证范围 | 实际执行 | 失败 / 错误 / 跳过 |
| --- | ---: | --- |
| Router回归 | 28 | 0 / 0 / 0 |
| 应用：阶段0回归＋阶段1—2 | 39 | 0 / 0 / 0 |
| Python迁移执行器 | 4 | 0 / 0 / 0 |

应用39项包含17项已有回归和22项本阶段测试；RedisClusterAdmissionIT通过三主节点真实Redis Cluster，其他数据库并发使用MySQL 8.0 InnoDB，未以H2或mock锁替代。完整应用以命令行显式只启用v2-it，真实JWT HTTP完成兑换、签到与回执重放，余额100→90→92。完整Maven八项目构建、可执行JAR和Java25镜像构建通过。

逐类计数、测试名称、镜像ID和JAR摘要见[results.json](results.json)。两用户库的余额、总次数、日/月剩余、累计上限、有效抽奖计数核对均为0差异，见[核对结果](reconciliation.json)及[核对SQL](reconciliation.sql)。该核对只证明本次隔离样本账实一致，不代表真实历史期初已审计。

## 行为证据

- 100次同充值业务只产生一份回执，两笔并发80积分消费仅一笔成功；禁用账户、精度1.001、非正金额与溢出拒绝。
- 两发号器并发分配号段，耗尽、进程重建和“数据库提交后调用方收到未知结果”不发布可疑号段；订单槽与用户槽相等。
- 两个独立事务执行器并发首次建账建桶收敛；周期消费受总/月/日额度约束，增额累加上限，新周期初始化，退款100次只退原桶一次。
- 兑换同键100次返回同单；余额不足形成稳定REJECTED，充值后原键仍拒绝；扣款后故障整单回滚。
- 签到100个不同键同日只发一次积分和次数；跨午夜重试原日期回放原单，部分奖励故障全回滚，原键换日期冲突。
- 原回执命中不读配置；Outbox与资产共同提交或回滚，100次并发append收敛，改变payload冲突。
- JWT身份、伪造body.userId、跨用户查单、入口429/技术503不占业务身份、finally释放许可、独立查询额度均验证。
- 旧消费者直接重投抛迁移身份异常，V2启动时停用；直接旧积分DAO也不能绕过写门。
- 新库唯一及非负约束；升级重复账户和期初冲突不自动修正，旧available_amount保留；DDL提交/进度记录中断后重入及checksum变更拒绝均验证。

## 独立审查与修复

使用独立上下文只读审查，4个Important、0个Critical、0个Minor。四项全部经过失败→通过验证并在6fdcf96修复，没有重复审查轮次：

| 问题 | 失败证据 | 修复及通过证据 |
| --- | --- | --- |
| Redis Cluster路由键与Lua真实键不同槽 | 健康三节点集群仅接受2/20，错误落入fallback | Lua仅操作KEYS[1]，hash保存窗口；RedisClusterAdmissionIT接受20/20 |
| POST借用查询预算 | 占住QUERY后POST预查超时 | POST预查、日期检查、配置读取改为TRADE；TradeBudgetIsolationV2Test通过 |
| 同步Redis等待超出deadline | CLIENT PAUSE 1000ms，100ms调用等待约1秒 | 异步等待≤min(200ms,剩余deadline)，取消并释放；AdmissionDeadlineV2Test断言<500ms且能重新取得许可 |
| 首次DDL后无checksum记录 | 首DDL提交、首进度丢失，改脚本后被接受 | 业务DDL前先保存checksum/step=0；同窗口改脚本即拒绝 |

## 执行裁决与边界

1. v2 SQL仓储采用JdbcTemplate绑定当前Spring路由事务；账户MyBatis接口保留，没有照示例为每条SQL重复建立DAO/mapper。代价是维护两种SQL入口；真实MySQL原子性验证通过。
2. 积分与周期仓储共享接口修改采用可编译组合提交。代价是提交粒度与文档示例的单任务消息不同，范围仍局限本阶段。
3. MySQL检查约束错误实际为Spring UncategorizedSQLException/3819，测试核对数据库错误码；未为了示例异常类型改变数据库约束。
4. MySQL JSON会规范化数字小数位；金额回执持久化为十进制字符串，避免首次和重放对象因scale不同而不一致。
5. 新建v2_admission_config作为可信SQL发布面，签到校验各奖励活动有效期交集，另查用户黑名单；管理发布流程仍归阶段4。错误发布配置可能拒绝新交易，不能把本阶段当作完整配置管理产品。
6. 使用规格中credit/exchanges、rebates/sign-ins路径及不可变record DTO；QuotaReceipt额外冻结grant增量，重复业务同hash也不能换增量。
7. 完整启动测试曾因additionalProfiles继承默认dev而尝试连接开发Redis，已立即终止该轮；改命令行显式spring.profiles.active=v2-it后启动、HTTP与依赖隔离通过。
8. 审查未重新评估阶段0的全部生命周期与生产容量；已有回归通过，不推导吞吐、生产容量或未测试的长JDBC强制退出保证。
9. 阶段3发布确认/消费恢复、阶段4管理发布、阶段5支付接收预算与渠道协议、阶段6真实导入/期初审计/旧非空列兼容/最终约束及切换均保持后续范围。提前开放真实流量会超出本次验收，V2默认仍关闭。

无本阶段延期Minor。没有改写历史演示测试来制造“全库绿灯”。

## 完整旧测试套件

重新使用既有阶段0历史隔离fixture、v2-it profile和40秒Surefire fork上限运行无-Dtest过滤的全套测试。Router28通过，RaffleStrategyControllerTest通过，随后仍停在：

`com.jay.test.trigger.RaffleActivityControllerTest.test_draw_blacklist`，第61行`CountDownLatch.await()`，没有countdown。fork超时，完整套件不通过，后续旧测试未完成验证。该阻塞与阶段0记录一致；不因此宣称可合并或生产发布。

## 重现与原始记录

重现入口为[verify-assets.sh](../../operations/verify-assets.sh)，参数及隔离环境见[运行说明](../../operations/assets-v2.md)。脚本需要环境凭据，未写入仓库。

本次本地原始日志：`/tmp/phase12-final-acceptance.log`（Python4、Router28、app39、package及镜像）；`/tmp/phase12-full-http.log`（真实HTTP）；`/tmp/phase12-full-legacy.log`（完整旧套件超时）；`/tmp/phase12-review-fixes-{red,green}.log`及`/tmp/phase12-migration-review-{red,green}.log`（审查问题复现）。原始日志只作为本地辅助，仓库保留可复现脚本、按测试名称整理的结果与无用户原始数据的汇总。
