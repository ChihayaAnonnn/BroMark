# Phase5 沙箱支付验收

已完成计划5.1–5.4的开发、隔离测试和独立整分支审查修复。分支 `feat/phase5-sandbox-payment`，起点 `bf49a24`，验收代码 `ffc5967d2ebd72d89e956a8342d9b48372ef17bc`。后续报告提交只包含文档。未合并、推送或部署。

## 交付

- 稳定控制域商户号、请求内容幂等、服务端发布商品与冻结权益、渠道创建状态独立于付款/到账状态。超时、抢先回调、创建前中断均可按原商户号恢复。
- 原始字节验签、64KiB限制、渠道账户/环境校验、已验证通知Inbox及原文SHA-256，事件与付款身份双层去重。先撤销阻止发放，晚撤销保留流水与核查证据。
- db00付款事实/PAID/GrantCredit同事务；用户积分流水/消费Inbox/CreditGranted同事务；新eventId重述返回原回执，支付域核验后才DELIVERED。
- JWT订单创建/归属查询，暂停回执持久化、审计恢复、查单与已验证付款恢复、完整付款身份与用户2库×4表CHARGE对账。坏通知逐条隔离，持续失败12次封顶，DEAD不自动认领。
- 沙箱默认关闭，仅test/local-sandbox，prod与test同时启用仍拒绝启动。无真实渠道SDK、退款或自动追回。

## 实测

`docs/operations/verify-payment.sh` 在隔离MySQL8、RabbitMQ、Redis Cluster及Java25 Docker环境运行，最终日志 `/tmp/phase5-final-acceptance.log`。

| 项目 | 结果 |
|---|---:|
| 应用验收 | 130通过，0失败/错误/跳过 |
| 其中新增支付测试 | 32通过 |
| Router | 28通过 |
| Python迁移测试 | 4通过 |
| 编译主类 | 531，全部major69 |
| 正式005迁移 | 2个控制库记录，用户库无005 |
| 两个用户库资产对账 | 各6项差异均0 |

真实HTTP→验签通知→MySQL Inbox→RabbitMQ跨库积分→原回执→DELIVERED样本：`PAY-00.000-app-775007`，冻结100.00积分、1个有效付款身份、1条CHARGE流水，支付域回执与用户原流水回执完全相同。本轮样本paid→delivered为425.943毫秒；只是单次隔离功能观测，不是容量或P99结论。完整时间戳见[阶段数据](stage-timings.json)。

[结构化结果及逐项测试](results.json)、[对账证据](reconciliation.json)、[迁移记录](migration.json)、[运维与恢复说明](../../operations/v2-payment-review-runbook.md)。原001–004及已应用005_payment.sql校验和均未改动，新增005_payment_inbox_failure.sql独立记录。

## 审查与边界

一次独立只读审查范围 `bf49a24..9bc0cc8`。5项Important全部修复，另将原文摘要缺失提升为Important并修复。原8项故障测试在修复前全部失败，修复后通过；增加缺失源回执的真实监听器持久移交测试后，9项审查回归全部通过，支付专项32项和最终全量130项通过。未派第二次审查。

完整[决策及代价](rulings.md)、[执行记录](execution.md)、[审查与修复](review.md)。保留1项Minor：已签名但JSON/字段错误的外部通知当前返回503，应后续映射400；不会确认成功或发积分。

不筛选的旧测试套件最终运行45.629秒后fork超时，dump定位既有 `RaffleActivityControllerTest.test_draw_blacklist:61` 的CountDownLatch.await；详情见results.json中的legacy记录；不能把V2验收通过说成全部历史测试通过。测试库刻意保留错币种、撤销、损坏和孤立记录等故障证据，因此不声称整套支付故障夹具全局差异为0；新的正常闭环样本已独立核对。全量对账是显式维护检查，生产批量、容量和切换属于Phase6。
