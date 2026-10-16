# 一次独立审查与修复

模型gpt-6-astra/high，独立上下文，只读 `bf49a24..9bc0cc8`。审查不运行测试、不修改分支、不派生代理。根执行者负责RED→GREEN与全量验证。

| 发现 | 等级及处置 | 验证 |
|---|---|---|
| PAYMENT缺失源对象不符合RecoveryService缺失契约 | Important，修复：恢复port直接锁元数据，统一EmptyResultDataAccessException | UNBOUND审计关闭先失败后通过；真实监听器先持久化retry再Ack |
| 本地提交后、渠道创建前中断无恢复 | Important，修复：明确NOT_FOUND后以原商户号/原价格幂等创建 | 渠道尚无订单的恢复先失败后通过 |
| 暂停期间已验证PAID被完成Inbox却未重放 | Important，修复：审计恢复后优先使用持久付款事实 | 查询UNKNOWN仍从CREATED推进唯一DELIVERED |
| 对账只数付款身份且缺少孤立事实/发放 | Important，修复：核对账户/环境/用户/金额币种及有效付款身份；全量反查事实和2×4用户CHARGE；坏快照单独核查 | 错币种付款事实、无源CHARGE均先失败后通过 |
| 坏通知阻塞整批，持续失败无限重试 | Important，修复：先按身份认领后逐条解析，永久损坏立即DEAD，业务12次指数退避后核查 | 坏通知不阻塞正常通知、持续失败封顶均先失败后通过 |
| 缺失验签原始字节摘要 | 原Minor提升Important，修复：首次原文SHA-256保留在payload.rawSha256，独立于业务摘要 | 精确原文摘要先失败后通过 |
| 已签名但JSON/字段非法返回503 | Minor，保留待后续映射400；当前不成功Ack、不发积分 | 已记录运维说明与最终限制 |

原始RED `/tmp/phase5-review-all-red.log`：8项全部失败；修复后对应8项通过。补充缺失源监听器移交测试，总9项回归；支付专项 `/tmp/phase5-review-payment-suite.log` 为32/32；最终 `/tmp/phase5-final-acceptance.log` 应用130/130、Router28/28、迁移4/4。未进行第二轮审查。

审查排除项及执行者裁定全部在rulings.md：真实渠道/退款、内部MQ被攻破后的认证、沙箱共享MySQL故障域、SQL商品发布、生产容量/分布式快照、跨库晚暂停、Phase3既有版本摘要策略、独立复跑证据。均有原因与错误代价。
