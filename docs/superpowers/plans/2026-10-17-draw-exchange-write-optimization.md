# 抽奖、兑换写入优化实施计划

> 执行方式：当前会话直接实现、真实数据库回归、独立审查、相同资源对照实验。

目标：在单实例 2 核、4 GiB 下减少抽奖、兑换资产事务的 SQL 往返，保留幂等、账户锁、额度流水和 Outbox 原子提交。

设计：修改 `MySqlAssetRepository.lockAccounts`，已存在账户只执行 `SELECT FOR UPDATE`，不存在时才 UPSERT 并重新读取。保持积分账户先于活动账户、活动 ID 升序的锁顺序。修改 `writeQuota`，一次多值 INSERT 写入 TOTAL/MONTH/DAY 三条流水，并校验影响行数为 3。共用仓储的签到账户、后台发奖同样受益，接口和表结构不变。

- [x] 新增 `AssetWriteEfficiencyV2Test`，真实 MySQL 上记录 JDBC SQL，验证已有账户不初始化、额度流水单条 SQL、三桶写入以及回滚；旧实现应失败。
- [x] 在 `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/v2/MySqlAssetRepository.java` 实现优化。
- [x] 运行全部 `com.jay.test.v2.*Test`，包括抽奖/兑换并发幂等、禁用账户、资金和额度回滚。
- [x] 独立审查首次开户竞争与流水原子性。
- [x] 冻结基线和候选构建，分别使用全新隔离数据，单实例 2 CPU / 4 GiB、Xmx2g；许可、连接池及限流配置相同。预生成 200 个结果，1 VU 20 秒预热；5/100/300/600 VU 各 45 秒，query/result/exchange/draw=7/1/1/1，无固定 RPS，无 sleep。
- [x] 对照写入成功 QPS、成功 P99、429/503、积压、CPU/内存和最终对账；保留原始请求和资源采样。所有请求停止后对账，不把受理 QPS 当发奖完成 TPS。
- [x] 报告实验结果，将验证代码同步回主工作区；本次不自动提交。

边界：保留独立幂等预查，以保证配置不可用时可重放旧结果；保留事务内复查。账户首次开户会多一次缺失查询，需同时验证冷启动竞争。已有 ID 段边界故障单独标注，不在本次更改。原始证据保留在仓库外的新目录 `BroMark-write-ab-20261017`，不覆盖此前压测。

验收结论：实现、正确性回归、三轮 A/B/A 实验及证据归档已执行；性能收益未通过验收。同构建基线显著漂移，不能声称 QPS/P99 提升。代码保留为未提交候选。
