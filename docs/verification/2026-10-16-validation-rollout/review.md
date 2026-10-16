# 一次独立审查与修复

审查范围592658a..a3c14ec，gpt-6-astra/high，独立只读；未复审。C1 Critical，I1—I6 Important全部接受，在一次修复中完成反例验证。

| 项 | 问题 | 修复/证据 |
|---|---|---|
| C1 | 身份映射无原流水回执会重复入账 | 用户/正确路由/业务hash/原ledger+receipt必须吻合；006 proof持久化；Java直接导入SQL后重放，原余额与原回执不变 |
| I1 | 映射只核对行数 | 每字段checksum；实际MySQL修改biz_id反例RED→GREEN |
| I2 | JSON float丢分币 | Decimal及按列类型规范化；实际MySQL大金额.01/.02反例RED→GREEN |
| I3 | 202无orderId被忽略 | 成功交易合同缺ID或状态计unconfirmed并阻断 |
| I4 | 六域漏取消退款 | 逐订单核对CONSUME/REFUND、原日/月桶与delta，取消无退款反例 |
| I5 | 排空掩盖负载期增长；repeats手填 | 只分析负载时间窗资源序列；三个不同run的原始记录与配置重新验证，不读取手填通过值 |
| I6 | tracker断连退出、runner忽略子进程失败 | 实际HTTP断连继续并计错；记录tracker/sampler退出码和collectionComplete |

Python反例及统计27/27、cutover9/9；完整应用138/Router28，详见tests.json与README最终证据。数据库连接实际KILL测试属于应用138项。

延期Minor M1：runner没有传入HOT_USER_CONCURRENCY8/32，默认只跑2；可以直接k6传环境变量，当前不能声称通过runner测过8/32。

审查未判断：完整长期容量矩阵/重复/同语义基线；未独立复跑父执行器运行事实；生产切换/权限/真实渠道/副本整体丢失；既有交易协议全面重设计；poll-ms性能收益。所有这些不从功能绿灯推断，裁定见rulings.md。

自查补强同一修复过程：完整积压域、组合正确性门禁、原签到/已扣兑换回放、DDL原checksum审计绑定及Java UTF16路由互通；对应RED/GREEN日志见raw-evidence.json。未追加独立复审。
