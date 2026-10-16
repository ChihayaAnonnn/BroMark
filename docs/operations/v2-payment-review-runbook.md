# V2 沙箱支付核查与恢复

仅测试适配器，无真实支付、退款或自动扣回。默认 `bm.v2.payment.enabled=false`；启用需同时开启v2与messaging，并使用 `test` 或 `local-sandbox` profile。任何包含prod的活动profile都拒绝启动。隔离测试用 `test,v2-it`，v2-it放最后覆盖本地连接配置；禁止dev/prod混入测试连接。

环境变量 `V2_PAYMENT_ACCOUNT` 与 `V2_PAYMENT_SIGNING_SECRET` 必填，HMAC-SHA256密钥至少32字符；不提交凭据。服务端 `payment_product` 发布amount/currency/credit；客户端只传requestId/productId/provider。商品变动不影响已冻结订单。同一user/requestId更换商品或渠道409。商户号始终PAY-00.000-*，超时UNKNOWN/202，不能重建商户单号。

HTTP：POST `/api/v2/payment/orders`、GET `/api/v2/payment/orders/{id}` 要求JWT并校验归属；POST `/api/v2/payment/notifications/sandbox` 不要求用户JWT，必须验证原始body的 `x-sandbox-signature`。64KiB上限，签名覆盖所有原始字节；业务摘要排除投递元数据。成功持久化才200原文 `SUCCESS`；数据库/预算不可用非200，渠道重投。相同事件冲突持久化证据并核查，仍返回SUCCESS避免无限毒消息重投。

`PaymentInboxJob` 每轮最多8条，以token与30秒租约认领；过期worker不能落地事实。付款身份provider/account/environment/transactionId先锁，再锁订单；核对商户、用户、环境、金额币种。付款事实+PAID+GrantCredit同db00事务。消息跨库；用户积分流水、Inbox及CreditGranted同事务；新eventId恢复重述返回原回执。仅已验证CreditGranted可把PAID改DELIVERED。业务状态、渠道创建状态、恢复状态分别显示。

## 核查证据与条件

- 错额/错币种：比对payment_order.snapshot的原价格、provider_inbox规范化事件、provider_payment_fact；不修改冻结订单去匹配错误事件。查渠道原商户号及付款身份，只有确认来源数据修正后才能恢复。
- 错归属/重复绑定：provider/account/environment/transactionId只对应一个商户、用户和金额币种。两个订单都留核查证据，不把既有身份迁到新订单。核对内部调用、渠道账户与通知日志。
- 早撤销：reversed事实在付款前也持久存在；后PAID不得发积分。保持REVIEW，必须有渠道最终事实及人工处置依据，禁止只清recovery_status绕过撤销事实。
- 晚撤销：GrantCredit可能已经发布或执行；保留PAID/DELIVERED与原流水/回执，进入REVIEW，不承诺撤回已发命令，不自动扣回或退款。
- PAID但无回执：查正确用户库CHARGE/charge:{orderId}的原权益摘要、delta、receipt、消费Inbox/consumer_receipt和Outbox。无流水则重述原GrantCredit；有流水则原回执重述。不能凭MQ Ack/通知Ack/Outbox SENT手改DELIVERED。
- 源订单暂停：到账回执在db00 deferred_fact提交后才Ack。读取当前对象version与active recovery_case，使用现有 `RecoveryService.resume(control,PAYMENT,id,version,operator,reason)` 审计恢复；不能直接SQL把状态设ACTIVE。版本不符拒绝；未完成订单重放deferred回执，后续独立PaymentRecoveryJob继续查单/原命令恢复。DELIVERED核查关闭不再发积分。
- 用户库不可用：保留PAID与消息重试，不跨库事务、不修改本地失败状态；恢复连接后原命令幂等执行。

对账调用 `PaymentReconciliationService.check()`，仅声明PAYMENT检查范围；REVIEW/DEAD等暂停订单单独查payment_review/recovery_case。`checkOrder(id)` 对照用户唯一charge流水、冻结权益及原DELIVERED回执，再锁源订单复核版本；版本变化下轮重查。发现差异仅核查，不改余额。当前全量手动检查不宣称生产规模或分布式快照；后续Phase6需分批与容量验证。配合本目录v2-payment-reconciliation.sql收集证据。

隔离验收：`V2_MYSQL_PASSWORD=... V2_RABBIT_PASSWORD=... MAVEN_CACHE_DIR=... docs/operations/verify-payment.sh`，正式005仅控制库，001–004校验和不变。该脚本不启动真实支付渠道，不发布部署。
