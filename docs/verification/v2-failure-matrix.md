# V2 故障覆盖与证据索引

每行列真实测试方法；执行结果以本次tests.json中的方法记录为准。异常注入、关闭连接池、TCP断开和进程崩溃分别标注，不能互代。注入前后SQL由测试中的业务断言执行；原始运行日志保存在/tmp验收目录。

| ID | 设计故障 | 测试/注入与SQL证据 | 原bizId/终态 | 边界 |
|---|---|---|---|---|
| F01 | 同一兑换请求并发提交100次 | `ExchangeAtomicV2Test.retriesRejectsAndRollbackAreAtomic`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F02 | 扣积分后、次数增额前注入异常 | `ExchangeAtomicV2Test.retriesRejectsAndRollbackAreAtomic`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F03 | 同日签到使用100个不同请求键并发提交 | `SignInAtomicV2Test.differentKeysCannotMultiplyRewardsAndFailureRollsBackAll`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F04 | 签到多个奖励中的一个写入失败 | `SignInAtomicV2Test.differentKeysCannotMultiplyRewardsAndFailureRollsBackAll`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F05 | 签到响应丢失后跨午夜重试原日期 | `SignInAtomicV2Test.differentKeysCannotMultiplyRewardsAndFailureRollsBackAll`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F06 | 兑换首次创建周期桶，同时增加日月上限 | `QuotaKernelV2Test.grantsPeriodsAndRefundsOriginalBucketsExactlyOnce`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F07 | 拒绝请求在充值后使用原键重试 | `ExchangeAtomicV2Test.retriesRejectsAndRollbackAreAtomic`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F08 | 100积分并发两笔80积分兑换 | `CreditKernelV2Test.serializesConcurrentDebitsAndReplaysOriginalCredits`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F09 | 事务回滚或数据库连接中断 | `MessageListenerV2Test.actualClosedDatabasePoolCannotAckOrCreateSuccessfulInbox`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 实际关闭Hikari池，尚非MySQL服务网络中断 |
| F10 | 账户扣减提交后丢失响应 | `ExchangeAtomicV2Test.retriesRejectsAndRollbackAreAtomic`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F11 | 发布成功但任务状态未更新 | `PublisherConfirmV2Test.lostConfirmConnectionIsUnknownEvenWhenBrokerAcceptedMessage`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F12 | 消费完成但响应事件丢失后进行业务重放 | `ConsumerInboxV2Test.atomicallyDeduplicatesTransportAndBusinessAndReplaysOriginalResponse`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F13 | 任务租约过期后旧Worker恢复 | `OutboxLeaseV2Test.claimsEachSourceOnceAndFencesExpiredOwner`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F14 | 固定候选后宕机 | `DrawAcceptanceV2Test.sameRequestConsumesOnceAndCandidateSurvivesDateAndRuleChanges`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 实际进程SIGKILL/Rabbit STOP证据见Phase6 faults.json；固定候选点的异常注入与真实崩溃点分开报告 |
| F15 | 受理后排队跨日、黑名单变化或其他抽奖完成 | `RaffleIntegrationV2Test.freezesBlacklistAndBusinessDayAndRollsBackTechnicalAcceptance`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F16 | 库存HELD后用户数据库不可用 | `RaffleFaultV2Test.unavailableUserDatabaseNeverReleasesHeldStock`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 实际关闭Hikari池，尚非MySQL服务网络中断 |
| F17 | 取消先提交、预留请求晚到 | `StockProtocolV2Test.cancellationTombstoneAndZeroQuantityAreFinal`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F18 | 取消与记录结果同时执行 | `DrawCancellationV2Test.cancelAndHeldHaveExactlyOneWinnerAndRefundOriginalPeriod`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F19 | 库存已确认而确认回执丢失 | `RaffleFaultV2Test.recoveryRestatesOriginalCandidateAfterConfirmedReceiptLoss`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F20 | 日期跨天或跨月后取消 | `DrawCancellationV2Test.acceptedCancellationRefundsOldBucketsAndCannotFixLateCandidate`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F21 | 策略更新及旧版本缓存淘汰 | `FrozenStrategyV2Test.freezesPublishedInputsAndRestoresOldVersionAfterEviction`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F22 | 同一库存池已有分配后发布新概率版本 | `StockProtocolV2Test.sharedVersionsConserveStockAndDuplicatesDoNotAllocateTwice`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F23 | 活动关闭与创建事务并发 | `DrawAcceptanceV2Test.sameRequestConsumesOnceAndCandidateSurvivesDateAndRuleChanges`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 严格全实例关闭还需维护窗口冻结证据 |
| F24 | 数据库路由线程复用与异步扫描 | `RoutingTransactionV2Test.reusesThreadRollsBackAndScansEverySlot`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F25 | 业务已进入 REVIEW 后晚到新事件 | `RaffleFaultV2Test.pausedDrawDefersLateReceiptThenAuditedResumeContinuesSameAward`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F26 | 普通交易或库存 Worker 占满自身配额 | `TradeBudgetIsolationV2Test.postPreflightAndConfigCannotBorrowReservedQueryCapacity`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F27 | MQ不可用、服务滚动重启 | `MessagingApplicationV2Test.applicationCloseKeepsDatabaseOpenUntilConfirmedOutboxUpdateDrains`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 实际进程SIGKILL/Rabbit STOP证据见Phase6 faults.json；固定候选点的异常注入与真实崩溃点分开报告 |
| F28 | 支付撤销早于付款通知 | `PaymentFactV2Test.earlyReversalAndWrongCurrencyNeverGrant`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F29 | 渠道下单超时或回调早于下单响应 | `PaymentCreationV2Test.callbackBeforeCreateResponseCannotOverwritePaidState`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |
| F30 | 迁移期间旧签到或发奖消息重投 | `MigrationRehearsalV2Test.issuedLegacyMessageReplaysImportedOriginalReceiptAndUnknownIdentityRequiresReview`；对应测试源码的SQL/assertions | 测试随机身份，原回执/状态断言；见XML和日志 | 针对性集成；完整证据见本次Surefire |

## 尚需运行的整体验收

- 独立服务进程SIGKILL、broker停机/恢复、MySQL连接KILL与至少两实例接管，使用隔离环境操作脚本记录前后SQL。
- 六域全量对账遇到故意保留的坏样本必须报告差异和核查单，不能从全库报告中隐藏。
- 每个已受理订单终态或持久核查单；积压0不是通过依据。
