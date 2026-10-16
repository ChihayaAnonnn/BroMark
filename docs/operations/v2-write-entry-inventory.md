# V2 共享资产旧写退出清单

维护窗口全域停止共享积分写；不能只停一个活动。逐实例记录版本、配置、入口停止时间、在途数、队列 ready/unacked、task 未完成行和操作人；有一个实例未确认就停止切换。

| 入口 | 实际代码 | 切换动作 | 旧消息处理 |
|---|---|---|---|
| 旧积分兑换/充值 | CreditAdjustService、CreditRepository、IUserCreditAccountDao | HTTP 停止新增，DAO 资产门 V2 拒绝 | 原 request/bizId 与原回执导入，否则核查 |
| 旧签到返利 | BehaviorRebateService、RebateMessageCustomer | 停旧监听器，排空签到/奖励任务 | 保留日期、奖励项、原流水身份 |
| 旧积分奖品 | CreditAward、SendAwardCustomer | 全活动停旧发奖 | 已发消息也导入去重凭据 |
| 旧次数操作 | IRaffleActivityAccount[Day/Month]Dao | V2 DAO interceptor 拒绝旧写 | 已扣待兑换不能再次扣积分；以已审计权益迁移或核查 |
| 旧抽奖/库存 | RaffleActivityController、UpdateActivitySkuStockJob、ActivitySkuStockZeroCustomer | 停旧抽奖、SKU 同步、库存队列 | 次数 SKU 库存核对后退出；实物奖品暂不支持 |
| 管理/手工SQL/其他充值 | 仓库无统一受控管理员资产HTTP入口 | 撤销旧写身份DB权限、核对外部脚本/连接 | 代码拦截不能阻止外部SQL，必须人工签字 |
| v2 API/Worker | http/v2、listener/v2、job/v2 | 维护窗口先停新受理，仅迁移后启用 | versioned prefix，保留原业务身份/内容摘要 |

`LegacyAssetWriteInterceptor`覆盖四类旧账户MyBatis DAO；`AssetWriteGate`覆盖旧领域入口。它们不能替代停旧进程、关闭旧队列消费者和数据库权限收敛。迁移适配器仅查已审计映射，不凭消息ID猜业务ID；未知消息的核查写入失败必须拒绝Ack。
