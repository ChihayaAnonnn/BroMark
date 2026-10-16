# Phase6 链路延迟定位

分析对象为已失败的 100 HTTP 请求/秒、600 秒实验，源代码 HEAD `2dd5bac`，运行镜像与前次验收相同。本次只增加分析文档，未修改业务实现或正式运行配置。容量结论仍为 NO-GO。

## 结论与证据边界

主要延迟发生在 **用户库 Outbox 的首条 PrepareDraw 创建到发布标记落库之间**，而非抽奖算法本身。需要优先解决 Relay 吞吐与恢复事件放大，再评估消费者并发。数据库许可确有等待，但原始数据不支持把它列为约 95 秒尾延迟的首要原因。

`published_at` 是 broker confirm 后更新 Outbox 的数据库时间，`created_at` 是业务事务内插入时间。因此该区间包含业务提交尾部、待发排队、领取、发送/confirm 和完成标记事务，不能进一步冒充精确的 broker RTT。Inbox 的 `processed_at` 也在处理事务内写入，不代表 Ack 时刻。下面的阶段耗时均来自同一个 MySQL 的时间，未混用客户端时钟。

原始导出保存在 `/tmp/phase6-latency-analysis/`；压测记录仍在 `/tmp/phase6-sustained-100/`。分析器只筛选本轮 HTTP 实际受理的 5,993 笔抽奖，排除了此前预置与暖机订单。分位数使用线性插值，不能把各阶段 P99 相加得到全链路 P99。

## 延迟拆分

| 区间 | P50（秒） | P95（秒） | P99（秒） |
|---|---:|---:|---:|
| ACCEPTED → CANDIDATE_FIXED | 4.92 | 86.51 | 92.37 |
| CANDIDATE_FIXED → RESULT_RECORDED | 0.87 | 2.72 | 4.73 |
| RESULT_RECORDED → COMMITTED | 0.83 | 21.43 | 27.44 |
| COMMITTED → DELIVERED | 0.50 | 20.89 | 26.81 |
| 首条 PrepareDraw 创建 → published_at | 4.87 | 86.50 | 92.31 |
| 首条 PrepareDraw published_at → CANDIDATE_FIXED | 0.04 | 0.19 | 0.61 |
| ACCEPTED → DELIVERED | 18.80 | 88.25 | 95.63 |

对最慢的 60 笔订单逐笔计算，首条事件创建到发布占总交付耗时的平均比例为 **95.92%**。这是逐订单比例的平均值，不是两个 P99 的比值。

最慢订单 `DR-01.002-app-72279` 的时间线（相对负载开始）：

| 时点 | 秒 |
|---|---:|
| ACCEPTED | 506.979 |
| 首条 PrepareDraw 创建 | 506.980 |
| 首条 PrepareDraw 发布标记 | 597.323 |
| CANDIDATE_FIXED | 597.436 |
| RESULT_RECORDED | 600.042 |
| COMMITTED | 602.535 |
| DELIVERED | 604.402 |

该订单约 97.42 秒完成，其中首条事件约 90.34 秒才被发布。恢复流程在约 512.90 和 521.00 秒又生成了 PrepareDraw；原始事件当时仍在等待发布。

## 瓶颈机制

### 1. Relay 每轮处理量小，按事件类型轮转，与到达率不匹配

链路为 `DrawController.create → DrawService.create → PrepareDraw → CandidateWorker → ReserveAward → ReservationHeld → ConfirmReservation → ReservationConfirmed → AwardDelivery`。正常一笔 CREDIT 抽奖需要六条持久事件；10 笔抽奖/秒本身就需要约 60 条事件/秒，另有约 10 条兑换通知/秒。

`OutboxRelayJob.tick` 依次扫描控制库、用户库 01、用户库 02，每库 `runOnce(slot, 8)`。`MessagingWorkers.relay` 在整轮 Outbox、Retry 完成后再 fixedDelay 250ms，250ms 不是整个循环周期。`MySqlOutboxRepository.claim` 逐类型、逐条 SELECT/UPDATE，并查询 lease_until；`OutboxRelay.runOnce` 对每条确认结果分别开事务落发布标记。轮转确保不同类型有机会，但并不保证高到达率类型能及时被清空。

用原始所有事件的创建/发布时间重建第 600 秒的未发布集合，1,234 条中 1,144 条为 PrepareDraw，占 92.71%。用户库 01 第 8～10 分钟累计新增 1,882 条 PrepareDraw，发布 1,387 条，即约 **10.46 条/秒进入、7.71 条/秒发布**，差额不断堆积。该数包含恢复生成事件，不能当成新的抽奖受理速率。

这解释了为什么其他类型还能较快前进，首条准备事件却越来越慢。单纯增加 Rabbit 消费线程不能直接消除发布前的积压。

在随后原配置复现实验的稳定采样窗（154.44 秒）中，Performance Schema 计数差值显示：用户库 01 两实例合计约 4.30 次 claim/秒、34.45 条领取/秒，接近每次 8 条的上限。按单实例平均约 465ms 一轮，明显长于配置的 250ms；包含三库领取、逐条完成事务以及 Retry 扫描的成本。该库在这个窗口执行约 147 次 COMMIT/秒，累计 COMMIT SQL 用时约 54.90 秒。SQL 用时是并发语句累计，不能当作一条请求的耗时，也未进一步分解为 fsync、锁或调度等待。

### 2. 恢复扫描给正常在途订单补发事件，形成负担放大

`DrawRecoveryJob` 默认每 10 秒调用 `DrawRecoveryService.tick`，遍历 2 库 × 4 表，每表选 8 笔 ACTIVE、未终态订单。`MySqlDrawRepository.scanActive` 按检查游标轮转，但没有“最后进展已停滞多久”的条件。`DrawRecoveryService.inspect` 对仍为 ACCEPTED 的订单直接生成新的 PrepareDraw，没有检查原 Outbox 是否仍为 PENDING 或持有有效租约。

库存恢复也会按双方状态补发结果。业务幂等避免重复扣款/发奖，但重复事件仍需要领取、发布、Inbox/业务回执验证和数据库提交。

本轮 5,993 笔抽奖的六类事件应为 35,958 条，实际为 45,055 条，放大 **25.30%**：

| 类型 | 实际条数 | 正常条数 |
|---|---:|---:|
| PrepareDraw | 9,505 | 5,993 |
| ReserveAward | 6,368 | 5,993 |
| ReservationHeld | 6,592 | 5,993 |
| ConfirmReservation | 7,168 | 5,993 |
| ReservationConfirmed | 7,372 | 5,993 |
| AwardDelivery | 8,050 | 5,993 |

这些事件的 Outbox attempt 都为 1：放大来自新的事件身份及后续重复响应，不能误记为同一事件的 broker 重试次数。恢复是在保护最终一致性；优化应减少不必要的补发，不能直接取消恢复。

### 3. 按类型领取与现有索引不匹配

领取语句含 `state + event_type + next_attempt_at`，但强制使用的 `pending` 索引只有 `(state,next_attempt_at,event_id)`。当旧 PrepareDraw 堆积时，寻找其他类型会遍历大量不匹配的待发记录。

在独立 `router_perf_it_diag_sqlprobe` 中复制用户库 01 的 Outbox，按第 600 秒重建未发布集合（956 条，包括 931 条 PrepareDraw；租约状态统一转为 PENDING，仅验证访问路径），增加探针索引 `(state,event_type,next_attempt_at,event_id)`，保持查询和数据不变，分别 FORCE 两种索引，各执行三次 EXPLAIN ANALYZE：

| 类型 | 旧索引扫描行数 | 探针索引扫描行数 | 旧/新查询执行时间，最后一次（毫秒） |
|---|---:|---:|---:|
| ExchangeCompleted | 928 | 1 | 0.629 / 0.038 |
| AwardDelivery | 946 | 1 | 0.594 / 0.026 |
| ConfirmReservation | 935 | 1 | 0.575 / 0.024 |
| ReserveAward | 921 | 1 | 0.528 / 0.027 |
| 不存在的 ReservationHeld | 956 | 0 | 0.548 / 0.018 |

PrepareDraw 本就在队首，两种索引都只扫描一行。因此索引问题是重复扫描和循环成本的放大因素，不能把单次亚毫秒的探针结果夸大为已解释全部 95 秒或已验证整体优化比例。探针没有改原数据库、迁移或业务代码。

### 4. 调度与消费许可：需要区分直接证据和影响程度

原始两份线程快照均只有 `scheduling-1`。Relay、Retry、Draw/Stock/Payment 恢复、Payment Inbox 等均通过 `@Scheduled` 运行；已声明的 `v2RelayExecutor`、`v2RecoveryExecutor` 没有接入这些执行路径。恢复扫描会占用该调度线程并执行多次数据库事务。

原快照有五条 workflow 线程等待同一个 WORKER semaphore，每实例的物理数据库 WORKER 许可仅 1。实际 Rabbit 回调运行于 `v2-workflow-*` 平台线程；虽然声明了虚拟线程 executor，实际消费路径没有提交任务给它。

这些限制真实存在，但消费者处理位于发布之后。原始首条事件发布到候选阶段的 P99 为 0.61 秒，因此不能根据线程快照把约 95 秒主要归因于 WORKER 许可，也不能宣称换虚拟线程就会解决。调度线程单变量实验见下一节。

## 对照实验

对原来的三个已排空实验库做一致性 dump，并恢复到新的隔离数据库。两个应用均保持原镜像、每实例 1 CPU/512MiB、Xmx320m、WORKER=1、Relay 每库 8 条、poll=250ms。主请求仍只发到实例 1，实例 2 参与后台处理。两次各 100 HTTP 请求/秒、180 秒，保留相同 200 用户和原业务分配。唯一业务运行配置差异为 `spring.task.scheduling.pool.size=1` 与 `4`。

两组均增加相同频率的数据库采样、线程快照和独立查单。它们是共享宿主机上的一次短时诊断，不满足 600 秒 × 3 轮的容量认证，不能代替原验收矩阵。实验原始材料在 `/tmp/phase6-latency-analysis/{baseline,scheduler4}/`。

| 指标 | 调度线程 1 | 调度线程 4 |
|---|---:|---:|
| HTTP 实际受理 / 最终交付抽奖 | 1,792 / 1,792 | 1,796 / 1,796 |
| 首条 PrepareDraw 创建→发布 P99 | 21.26 秒 | 10.20 秒 |
| ACCEPTED→DELIVERED P50 / P99 | 15.52 / 23.37 秒 | 8.95 / 21.60 秒 |
| RESULT_RECORDED→COMMITTED P99 | 8.50 秒 | 9.02 秒 |
| 第 180 秒未发布事件 / 其中 PrepareDraw | 162 / 143 | 67 / 19 |
| 抽奖事件放大倍数 | 1.274 | 1.277 |
| HTTP 429 / 503 | 12 / 3 | 6 / 5 |

线程快照确认两组确为 1 与 4 个 scheduling 线程。第一组两实例合计 36 个调度线程抽样中，19 个在等待下一任务、14 个在 Relay、3 个在 DrawRecovery；第二组允许恢复与 Relay 并行。周期性抽样存在偏差，不能把这些比例视为精确利用率。

本次对照支持“调度共享会影响发布延迟”，但 **扩到 4 线程仍然未达到 5 秒完成目标，也未消除恢复放大与积压**；确认阶段 P99 也没有同步改善。两组的 CPU/内存/数据库许可与事件批量均未改变。结果不能归纳为“只加调度线程即可修复”，也不声明可重复的优化百分比。

两组都只有活动读路径暖机，存在写入冷启动和技术拒绝；非 2xx 响应未算入受理样本，也未做本次逐请求 unknown-resolution。运行顺序、共享宿主机与缓存变化仍是限制。最终终态统计、停写对账见随附 JSON；这些诊断仍不构成发布批准。

## 受理延迟与尚未证明的原因

原抽奖 HTTP 受理 P99 约 946.55ms，兑换约 796.15ms；两者的 P50 都约 25ms。受理包含 Redis 准入、控制库配置读取、TRADE 许可、账户锁、账务/订单/Outbox 提交，当前原始记录缺少这些内部边界的独立 span，无法严谨地把 HTTP 尾延迟细分到某一个函数。

查询较快不代表数据库写入足够：活动查询、查单与写入走不同的事务路径与配额。原始 MySQL 全局连接峰值 21/80，快照中没有 Hikari getConnection 阻塞，证据不支持把连接数耗尽列为主因。原数据库锁计数是跨实验累计值，没有起止差值，不能冒充本轮行锁耗时。

应用 CPU 多数采样未满额，MySQL 后半程约 85%～97% CPU（Docker 的百分比按一个核计，不能读成整台 VM 的使用率），Rabbit 多数约 10%。容器内存接近限制，但没有 GC 暂停或 memory pressure 的有效数据；GC、IO/fsync、CPU throttling 都不能凭资源快照定案。

压测脚本还有分配偏差：用户索引与业务选择都直接取同一 iteration 的模。200 用户里只有 20 个抽奖用户、20 个兑换用户，二者不重叠；活动查询为 140 个用户，预置查单为另外 20 个。用户库 01 承担 3,297 笔抽奖，库 02 为 2,696 笔，不能据此宣称 200 用户均匀写入或均匀分片。

## 建议修复顺序与验收

1. 先优化 Relay 服务能力：独立有界执行资源、受时间/条数约束的连续 drain、可配置批次与按积压年龄/类型需求分配；减少逐事件完成事务的往返，保留每条 lease token fencing、confirm/UNKNOWN 语义。不要只把 poll 调得更频繁而制造更多空查询。
2. 恢复扫描引入停滞阈值与补发判定，检查同阶段原事件的待发/有效租约或去重意图，并限制每个聚合版本的补发速率。保留故障接管与人工核查机制，不能用关闭恢复换取压测成绩。
3. 为实际领取谓词验证类型前导的 pending/expired 索引并同步调整 FORCE INDEX；保留通用到期扫描所需访问路径。先做 Explain/锁语义验证，再做同负载单变量实验。
4. 发布前积压受控后，再调整 WORKER 许可及 Hikari 池；保留 CALLBACK、QUERY、RECOVERY 的独立配额，并按实际 MySQL 80 连接上限验算所有实例。不要直接提高所有线程/连接数。
5. 为 HTTP 准入、许可等待、事务、账户锁 SQL、claim、confirm、发布完成、消费开始/结束补充可关联 eventId/orderId 的耗时证据。修正业务与用户分配的耦合，分别保留均匀用户与明确热点场景。

每次只改变一个变量，重新执行相同负载，检查各阶段延迟、按类型 Outbox 年龄/增长、四域积压和六域对账。单项诊断改善后仍需补齐 600 秒三轮、完整时钟/资源证据与后续压力档。此报告没有将容量 NO-GO 改成通过。
