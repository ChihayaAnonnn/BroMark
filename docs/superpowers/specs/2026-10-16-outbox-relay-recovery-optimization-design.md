# Outbox Relay 与恢复补发优化设计

日期：2026-10-16

状态：待评审；尚未实施，容量验收仍为 NO-GO

适用范围：BroMark V2 的 Outbox、Retry 发布与自动业务恢复

实现基线：`2dd5bac`；运行证据以实际镜像和实验清单为准

## 1. 目的、范围与成功条件

本设计承接[并发交易设计](2026-10-15-concurrent-credit-payment-raffle-design.md)和[链路延迟定位报告](../../verification/2026-10-16-latency-analysis/README.md)，解决首条抽奖事件长时间等待发布、恢复任务重复补发，以及按事件类型领取时的无效扫描。用户要求先形成优化设计；本文不表示已批准实施或发布。

保留 MySQL 事实源、现有业务状态机、资产流水、Inbox 幂等、库存确认/取消协议、冻结候选和策略快照。优化不得以关闭恢复、扩大业务拒绝、放松持久化、改变奖品语义或隐藏排队数据换取延迟下降。

首个容量验收仍使用双实例、100 HTTP 请求/秒、持续600秒、三轮实际重复：查询 P99≤200ms，受理 P99≤500ms，抽奖确认及最终交付 P99≤5s，技术失败/入口过载拒绝和 dropped iterations 为0；六域对账差异为0，四域积压无持续增长。这里是目标，不是已有结果。首档通过后才恢复300/600/1000档与其余 Phase6 矩阵。

本期不修改 WORKER/TRADE/CALLBACK 许可、Hikari 池大小、Java/GC、MQ 拓扑或 Redis 权威边界；不迁入 PostgreSQL/Kafka，也不增加全局 topic leader。若发布阶段改善后出现新的主瓶颈，应单独取得证据并补充设计。

## 2. 已验证事实与证据限制

| 事实 | 实测或源码依据 | 对设计的约束 |
|---|---|---|
| 首条 PrepareDraw 创建→发布标记 P99为92.31s；发布标记→候选结果 P99为0.61s | 原600秒实验的同一MySQL时间 | 优先优化发布前路径，不能先把原因归于消费线程或算法 |
| 最慢60笔订单的首条发布区间平均占总交付耗时95.92% | 逐订单配对计算 | 分位数不可相加，也不能用两个P99之比替代贡献比例 |
| 第600秒未发布1,234条，其中PrepareDraw为1,144条 | 按创建/发布时间重建 | 必须按类型观察积压与年龄，避免整体吞吐掩盖类型饥饿 |
| 5,993笔抽奖产生45,055条六类事件，正常流程需35,958条 | 已受理订单关联的事件 | 恢复补发与重复响应增加25.30%工作量 |
| 每库每轮8条、三库串行、逐条完成事务、轮后fixedDelay | 当前实现 | 增加连续处理和批量事务，而非只缩短空轮询间隔 |
| Publisher最多16个在途发布；窗口不足直接返回UNKNOWN | `V2RabbitConfig`、`V2ConfirmedEventPublisher` | 领取批次必须与发布窗口分离 |
| 调度线程1→4的180秒短测：交付P99为23.37→21.60s | 单次诊断 | 调度隔离有价值，但不能宣称只加线程即可达标 |

`created_at→published_at` 包含业务提交尾部、排队、领取、发送、confirm及完成标记事务，不是精确 broker RTT。GC暂停、fsync、CPU throttling及HTTP内部各阶段的主因尚未完整测得。本设计不把这些未测因素写成已确认结论。

原负载脚本把业务选择与用户索引绑定，实际只有20个抽奖用户和另外20个兑换用户；后续必须同时保留这个可复现旧场景，以及明确修正后的均匀用户场景，分别报告。

## 3. 方案比较与参考来源

| 方案 | 优点 | 限制 | 决策 |
|---|---|---|---|
| 有界连续处理、批量领取/完成、补发限流、匹配索引 | 直接覆盖已有证据，保持现有一致性协议 | 需要处理批量租约和恢复并发 | 采用 |
| 只增加调度线程、批次或连接数 | 改动少 | 不消除重复补发；大批次还可能打满16个发布槽位 | 不单独采用 |
| 每类型全局leader、游标扫描、事务内顺序发布 | 可以支持特定顺序要求 | 引入连接占用、领导者和回扫协议；不符合当前事务外发布边界 | 本期不采用 |

参考当前工作区源码，不能据此推定线上已启用或具有某个吞吐量：

- `7verse-ug`（HEAD `e57445edb`）：[活动Worker连续处理](../../../../7verse-ug/app/sillytavern/internal/activitydelivery/worker.go)、[批量领取及租约校验](../../../../7verse-ug/app/sillytavern/internal/dao/event_outbox_dao.go)。借鉴有任务即继续、空闲再等待、短事务领取和租约fencing。
- `vivix-billing`（HEAD `c9f87ed8`）：[独立topic Worker及扫描/发布预算](../../../../vivix-billing/app/payment/internal/relay/outbox_relay.go)、[恢复原履约命令](../../../../vivix-billing/app/payment/internal/job/delivery_recovery_cron.go)。借鉴执行隔离、工作量和截止时间、冻结命令复用。
- UG 的 PostgreSQL CTE/RETURNING、部分索引需改写为 MySQL 实现。billing 的部分发布在持有事务时执行，本设计不迁入；其 `paid_at` 年龄条件也不等于完整的消息在途检查。跨批次持续处理直接参考 UG 活动Worker，不能仅凭 billing 注释认定其外层ticker已经做到这一点。

## 4. 必须保持的不变量

1. 领取、完成、重试和恢复决策均进入所属路由的短事务；取得数据库许可后才获取连接。所有租约和到期判断使用该MySQL的UTC时间。
2. Rabbit发送、confirm等待、供应商查单和跨库事实读取，不放进账户或任务领取事务。
3. 发布确认只证明运输完成，不证明业务完成。只有有效confirm且未发生mandatory return，才能把对应任务标为PUBLISHED。
4. 完成/重试更新逐条匹配`event_id + state=PROCESSING + lease_token`；零行代表失去所有权。不能把一个批次的成功推广到所有行。
5. 发布已成功而完成事务未确认时，仍允许原运输事件重投；Inbox与业务回执吸收重复，不补扣款、不重选奖。
6. 运输重投保持eventId；业务恢复生成新eventId，但保持业务ID、requestHash、冻结快照与原业务语义。不能复用旧eventId后期待已处理Inbox重新生成响应。
7. REVIEW、WAITING_FIX、RETRY_EXHAUSTED及人工暂停继续阻止自动副作用；HELD年龄只能触发核对，不能成为释放库存的依据。
8. Retry的消费失败generation与发布attempt是不同计数。批量化不得丢失failure_count、跳过mayApply检查或刷新已有12次自动重试预算。

## 5. 运行结构与连续处理

### 5.1 两条发布执行通道

每实例使用现有`v2RelayExecutor`的两个线程：一个持续处理Outbox，一个持续处理Retry。每通道轮转`db00→db01→db02`，每次只持有一个所属库的领取批次；同一批完成或恢复为可重试状态后，再进入下一个库。所有实例仍依靠数据库租约共同工作，不选全局leader。

`MessagingWorkers.relay`的旧Scheduled执行仅属于legacy模式；bounded模式启动上述两个循环，不能同时启动旧tick。ID号段预取仍在事务外，失败保留任务而不生成新业务身份。

每个完整三库轮次若领取到工作，就立即开始下一轮；若三库均无到期工作，等待最多250ms，或被本进程提交后唤醒。唤醒采用合并信号，不为每个事件排一个内存任务；信号丢失、跨实例提交和进程重启都由周期性数据库检查覆盖。

每通道轮转库起点，避免总先服务db00。一个库有大量积压时也不得连续独占下一库的轮次。到期Retry有自己的执行通道，不会因为新Outbox持续进入而永远得不到机会；二者仍共享原物理DB的RELAY许可，串行的是短数据库操作，不是broker等待。

### 5.2 有界发布窗口

初始诊断配置：每库最多领取32条，每通道最多同时启动8个发布future，两通道合计最多16，与现有Publisher窗口一致。每波等待所有已启动future得出确认、失败或UNKNOWN，再落库并处理下一波。不能把32条一次性全部交给`publishAttempt`。

一波共享3秒发布截止时间，不能对每个future重新等待3或6秒。领取事务预算1秒，每波完成事务预算1秒，单批最多20秒；这些预算包括许可等待，并须落实到事务、JDBC语句和网络超时，单独构造Deadline并不能中止正在执行的SQL。

初始租约仍为30秒。启动参数必须满足：

```text
通道数 × 每通道发布窗口 ≤ Publisher在途上限
领取预算 + ceil(领取批次/发布窗口) × (发布预算+完成预算) + 5秒余量 < 租约
```

默认值为`1 + 4×(3+1) + 5 = 22 < 30秒`。批次不能任意调大，必须重新校验该关系。每次启动发布波次前使用DB时间校验本批新租约的剩余预算；当前执行者不得在剩余时间不足以覆盖一波及余量时启动新发布。

正常批次应远低于上述最坏预算；该预算不是正常延迟承诺。持有的任务超过本批截止时间后停止启动新发布。确定尚未调用Publisher的行，可在fenced释放事务中恢复为PENDING和领取前attempt；必须同时匹配本次token及attempt。已经调用Publisher的行，包括UNKNOWN，绝不能回退attempt。进程崩溃时不猜测哪些已发送，由租约到期按原不确定结果语义接管。

### 5.3 恢复与发布隔离

Draw/Stock/Payment恢复、通用核查事实扫描与Provider Inbox的调度只提交合并后的工作，不在Spring共享调度线程执行长扫描。使用现有`v2RecoveryExecutor`，每种任务至多一个queued/running标记；重复tick合并，队列满时保留DB事实并在后续tick重试，不能丢弃业务任务或调用者线程代跑。

恢复每轮最多检查64个候选、最多产生8条新恢复事件，达到2秒工作预算后不再启动新候选；已启动动作继续受自己的有限DB/远程deadline约束。候选游标覆盖所有用户分库和订单分表。不能将2秒预算误称为已启动RPC必在2秒内返回。

Provider Inbox保持CALLBACK/WORKER配额与既有故障移交语义；本期只移走共享调度线程中的实际执行，不改变支付事实处理协议。关闭发布执行资源之前，恢复入口也必须停止接收新任务。

## 6. 批量领取与批量完成

### 6.1 领取事务

每次领取使用一次DB UTC时间，产生每行独立的新token和本批公共leaseUntil。批量读取payload、attempt；Retry同时读取consumer_name和failure_count，作为本次发送header的快照，避免发布前再为每条读取一次generation。

32条配额分为：最多4条到期PROCESSING、最多8个已注册事件类型各1条到期PENDING、剩余额度按全局`next_attempt_at,event_id`领取最老PENDING。每通道、每库分别保存类型游标并跨轮次循环；类型列表来自现有事件路由注册，不在每个批次执行无限制DISTINCT类型发现。已选行在同事务中按ID排除，不能因同一事务重复加锁而重复入选。

过期PROCESSING按`lease_until,event_id`批量SKIP LOCKED，空余额交给PENDING。若PENDING不足，最后再以剩余额度领取到期PROCESSING，避免过期任务单独存在时只能每次处理4条。只要有足够PENDING，初始32条至少保留20条按最老时间领取；未知类型也通过这条通道推进到既有RETURNED/核查流程，不静默丢弃。

全部SELECT均为有界`LIMIT ... FOR UPDATE SKIP LOCKED`。选中后用JDBC batch或等价批量UPDATE统一写PROCESSING、token、leaseUntil和attempt+1；核对更新数后提交。payload解析失败不得整批回滚造成队首永久阻塞，应保留合法任务身份，进入现有隔离/核查路径，不将坏数据发布成成功。

MySQL实现不依赖PostgreSQL RETURNING。可复用本次SELECT的payload、DB时间和内存中生成的token组装返回对象，不再逐条SELECT lease_until。领取事务失败时不发布任何未确认提交的领取结果。

### 6.2 完成事务

每波结果分为CONFIRMED、需要退避、失去租约和需要暂停业务四组。CONFIRMED在同一路由的一个短事务内批量CAS完成；所有条件仍逐条包含token。只把实际匹配的行统计为本次完成。

采用JDBC batch时必须能识别每条的影响行数；若驱动返回SUCCESS_NO_INFO，不得据此推断每条fencing成功，应改用一次批量锁定、筛出仍归本批所有的行，再进行带token条件的批量更新并核对影响行数。禁止每条额外开启一个完成事务。

普通Outbox退避可同库批量落库，保持现有指数退避、抖动和12次预算。Retry失败移交即使未达到DEAD，只要涉及beforeRetry中的聚合暂停检查或deferred fact保存，也按单聚合事务处理，不能把不同用户账户放进一个批量事务。需要DEAD、暂停聚合及保存deferred fact的情况走原业务锁顺序下的专门短事务；不能先锁整个Retry批次，再反向获取账户/订单锁。不能跨路由合并完成，也不能在持有完成事务时等待Rabbit结果。

完成事务提交结果不确定时，不把该波加入“已确认落库”的指标；继续依赖任务状态和租约收敛。不能因为完成失败而生成新的运输eventId。

## 7. 恢复补发条件与多实例去重

### 7.1 唯一恢复决策入口

Draw扫描与Stock扫描发现的同一抽奖，最终都进入用户分库的`DrawRecoveryService`决策入口。Stock扫描提供发现线索；不能再在db00独立生成另一组抽奖恢复事件。决定仍先读权威库存事实，再在用户库短事务内重读订单、核对版本及状态后提交。

支付PAID→GrantCredit恢复以db00的支付订单为所有者，采用相同补发保护。CREATED供应商查单/重建沿用既有商户身份与事务外调用，不并入本次事件补发去重。

通用`RecoveryService.scan`继续处理deferred事实，不再额外对专用恢复器负责的DRAW/RESERVATION/PAYMENT执行正常ACTIVE订单的无动作扫描。它不得形成另一个未受保护的补发入口。

跨库事实读取失败、身份/摘要不匹配、对端明确暂停时不补发；保留资产/库存占用并告警。无法找到权威用户订单的库存记录也不能自动释放，按原核查协议处理。

### 7.2 持久化补发记录

新增每库一张`v2_recovery_dispatch`，不修改运输事件或业务回执结构：

| 字段 | 含义 |
|---|---|
| aggregate_type, aggregate_id | 联合主键；DRAW位于用户库，PAYMENT位于db00 |
| observed_state, observed_version | 最近决策依据，不冒充订单当前状态 |
| last_event_type, last_event_id | 最近实际补发的事件，可定位对应Outbox |
| last_redrive_at | 最近补发的DB UTC时间，可空 |
| next_redrive_at | 当前聚合允许下次补发的最早时间 |
| redrive_count | 当前阶段/版本累计补发数；检查次数另计 |

同一聚合只有一个跨实例保护行；不在Redis或进程内防重。主业务锁顺序为：账户/活动账户（涉及用户域）→业务订单→补发记录→有关任务行。只有持有这些本地锁后才能最终决定新增补发。

扫描用现有`last_progress_at`和检查游标筛选；`checked_at`只表示尝试检查，不能刷新业务进展时间或last_redrive_at。补发记录只在实际Outbox写入同一事务中递增，事务回滚时二者一并回滚。状态检查、游标touch与真实补发必须分别计数。

### 7.3 自动补发判定顺序

默认停滞阈值30秒、补发间隔30秒。候选必须是ACTIVE且未终态，`last_progress_at ≤ DB_NOW-30s`，并在最终锁内重验。状态/版本发生变化时重新判定，并不能跳过30秒停滞条件。

依次执行：

1. 根据冻结订单和权威事实选定允许的恢复动作，验证本地版本未变；不重选奖、不重新读取商品价格替换冻结值。
2. 检查同聚合、同动作类型的本库任务：任何PENDING（包括尚未到期的退避）或PROCESSING都抑制新补发。过期PROCESSING交给Relay接管，不另外新建命令。
3. 当前恢复周期的DEAD、明确核查或暂停阻止自动重述，转现有核查处理。历史DEAD只有在关联核查已审计关闭、且有人工恢复生成的新重放身份记录时，才从当前周期的阻断集合排除；不能仅因业务状态重新为ACTIVE就忽略它。不能用新eventId刷新一个已耗尽的失败流程。
4. 若本库原任务已PUBLISHED，观察窗口为`max(last_progress_at,相关任务最新published_at)+30秒`；窗口未结束不补发。它只缓冲正常传递，不表示证明远端没有在执行。
5. 在事务外的权威事实读取同时检查目标域对应的Retry、业务操作回执及核查状态。PENDING或PROCESSING的Retry（包括未来退避）、当前周期DEAD、暂停或查询不确定均抑制新业务事件。Retry为PUBLISHED且没有已完成业务回执时，也不新建事件，按7.5节修复原运输重试；若原业务操作已完成，则允许在其余条件满足后以新eventId请求原响应回放。只读证据与业务事实同样不提供跨库原子性；新任务与补发竞争仍由消费者幂等及暂停检查保护。
6. 在最终所属库事务内，锁定并检查补发记录，DB_NOW未到next_redrive_at则跳过；否则写新Outbox与last_redrive_at/next_redrive_at，保持原业务ID与摘要，生成新运输eventId。

任务匹配不能仅用`aggregate_version=当前订单version`：人工恢复可能递增业务版本，但旧阶段的有效任务仍应抑制补发。以聚合身份、动作类型、冻结业务摘要和任务状态核对；发现同身份不同摘要转核查。

单阶段/版本最多自动补发12次，达到上限转RETRY_EXHAUSTED并写证据，不能重新开始一个无限补发周期。业务进展可以建立下一阶段计数，单纯touch、重启或另一实例接管不能重置。显式人工恢复记录操作者与原因，保持现有审计协议。

这里保证的是“同一聚合在本地决策事务中，30秒窗口至多新增一次自动补发”。跨库Retry状态可能在观察后改变，因此不承诺跨库零重复或exactly-once。

### 7.4 状态与恢复动作

| 用户/支付状态 | 权威事实 | 动作 |
|---|---|---|
| ACCEPTED | 无需库存事实改变资格 | PrepareDraw |
| CANDIDATE_FIXED | 已HELD | 重述原ReservationHeld回执 |
| CANDIDATE_FIXED | 没有预留 | 原候选的ReserveAward |
| RESULT_RECORDED | 已CONFIRMED | 重述原ReservationConfirmed回执 |
| RESULT_RECORDED | 仍HELD | ConfirmReservation |
| CANCELLING | 已CANCELLED | 重述ReservationCancelled回执 |
| CANCELLING | HELD或无预留 | CancelReservation，保留墓碑语义 |
| COMMITTED | 尚未DELIVERED | AwardDelivery |
| PAID | 尚未履约且未暂停 | 冻结GrantCredit |

矛盾事实仍按原设计转核查，例如已确认库存不能因为用户取消或超时被释放。晚到回执、Inbox与业务响应回放保持原协议。通用`RecoveryService`的deferred fact重放与人工resume不能被一个普通冷却计时器永久阻断，但它们生成的实际在途任务仍受上述检查保护。

### 7.5 已发布Retry仍未完成的运输修复

当前实现没有用Retry DONE表示消费完成；`retry_job=PUBLISHED`不能替代`consumer_receipt`中的已提交业务操作回执。若简单忽略PUBLISHED就可能用新eventId刷新generation，若永久将它视为在途又可能失去恢复入口。

由目标库的运输修复任务处理这类记录，仍使用现有恢复执行器和每轮预算，不从用户业务事务跨库写目标库：

1. 有界检查已PUBLISHED超过30秒的Retry，按原consumer_name与业务操作身份核对已提交的consumer_receipt及摘要；有匹配回执则该运输重试已完成业务，不重投。
2. 没有回执、聚合仍允许自动处理且没有未关闭核查时，在目标库短事务中CAS `PUBLISHED→PENDING`，保留原eventId、payload、failure_count和发布attempt，清空旧租约并设置本次到期时间。两个实例只有一个能完成该CAS。
3. Relay重新领取并发布原eventId，header仍使用原failure_count；发布attempt继续累计。发布后新的published_at重新启动30秒观察窗口，不因再次扫描马上重投。
4. 已达发布attempt上限时，按原锁顺序转RETRY_EXHAUSTED并保存证据，不能回退attempt。历史已审计恢复的DEAD遵守7.3节的恢复周期规则。

已有消费事务可能在检查后提交，因此仍可能产生运输重复；同eventId的Inbox去重会吸收它。读取核查后又被暂停的竞争，也由消费事务再次检查recovery_status保护。该任务仅修复运输，不新建业务命令，不重置失败generation，不查询Redis猜测完成。

初版在retry_job建立`(state,published_at,event_id)`索引，以有界时间游标分页，并核对已提交业务回执。历史已完成记录可被再次扫到，但每次工作量受分页预算限制。每个完整轮次固定扫描上界，使用published_at+event_id推进，扫至该上界后回绕，以捕获晚提交；不能因新行持续进入而永不回扫，也不能用全表扫描上线。Explain及增长数据集实验是该运输修复能力启用的门槛。

## 8. 领取与恢复查询索引

初版通过新增迁移建立以下索引，并以实验验证收益；不修改001—006已审计脚本及checksum：

| 表 | 新索引 | 用途 |
|---|---|---|
| outbox、retry_job | pending_type(state,event_type,next_attempt_at,event_id) | 类型公平领取 |
| outbox、retry_job | aggregate_event(aggregate_id,event_type,state,created_at,event_id) | 本库与目标域的在途、暂停/重试检查 |
| retry_job | published_recovery(state,published_at,event_id) | 已发布但业务未完成的运输修复分页 |

继续保留现有pending/expired索引：全局最老领取和全局过期接管仍需要不以类型为前缀的路径。初版领取算法的过期配额走现有expired，不建立或引用expired_type。未来只有实际需要按类型过期查询、且Explain证明收益时，才另行评估`(state,event_type,lease_until,event_id)`，避免本期增加无收益的写放大。

类型领取切换到pending_type，不能仍FORCE旧pending。最老填充使用pending，过期填充使用expired。恢复查询使用aggregate_event限定小范围，再验证摘要和状态。不采用LIMIT1但过滤数百条其他类型的访问路径。

原有订单恢复索引保留；候选先按状态和last_progress_at分段、有界分页，再读取检查/补发记录，避免全量JOIN后COALESCE排序。多个业务状态分别查找并合并少量候选；每个分库、分表、状态的扫描轮次固定上界，游标使用`last_progress_at + 主键`，扫至该上界回绕，防止持续新流量导致永不回扫、较小ID的晚提交订单被永久跳过。游标不是业务完成事实。

最终索引取舍以新旧访问路径的Explain、实际rows examined、锁等待、写入成本和端到端实验决定。本文不承诺某索引带来固定倍数提升。

## 9. 配置、资源与关闭

新增配置集中于`bm.v2.messaging.relay`和`bm.v2.recovery.redrive`，不散落魔法常量：

| 配置 | 初始值 | 约束 |
|---|---:|---|
| relay.mode | legacy | legacy/bounded，互斥运行 |
| relay.claim-limit | 32 | 1—128，并通过租约预算校验 |
| relay.publish-window | 8 | 两通道合计≤16 |
| relay.expired-reserve | 4 | ≤claim-limit，空余额可转移 |
| relay.fairness-slots | 8 | 给全局最老填充保留容量 |
| relay.idle-poll-ms | 250 | 仅空闲轮等待 |
| relay.lease-seconds | 30 | 大于完整批次最坏预算及余量 |
| relay.batch-deadline-seconds | 20 | 不启动超出预算的新发布 |
| recovery.redrive.mode | legacy | legacy/guarded；不能与旧补发器并行 |
| recovery.redrive.stale-seconds | 30 | 正常未终态不立即补发 |
| recovery.redrive.interval-seconds | 30 | 同聚合自动补发冷却 |
| recovery.redrive.max-per-stage | 12 | 耗尽转核查，不能自动重置 |

这些是首轮实验起点。保留WORKER=1及其他工作类别许可；应用三个逻辑池各8、两副本和预留20连接的预算仍为`3×8×2+20=68`，验算上限使用实际MySQL 80而非原配置声明100。增加循环速度不能绕过数据库预算。

关闭顺序：停止HTTP新受理和恢复调度→停止发布循环新领取→在共享30秒关闭预算内完成已启动publish及结果落库→关闭消费者、发布器和执行器→数据库许可排空→关闭数据源。超时强制停止时，未确认任务保留租约/不确定状态供接管；不得因关闭将任务伪记成功。现有Lifecycle phase必须以这一顺序验证，不能仅分别声明destroyMethod。

## 10. 观测与验收

### 10.1 必须增加的证据

- Relay：claim耗时/领取行数/SQL条数、每波启动与结果、confirm耗时、完成事务耗时/条数、失租行数、未发送释放、批次超时、每库每类型处理速率。
- 队列：按类型的Outbox与Retry待发/PROCESSING数量和最老年龄；ORDER/OUTBOX/RETRY/BROKER四域保持独立采样。
- 恢复：检查次数、实际补发数、按原因抑制数（未停滞、有效任务、远端重试、冷却、暂停、事实不确定）、各阶段补发次数和耗尽核查数。
- 数据库：工作类别许可等待、事务耗时、连接池active/pending、按窗口的行锁/语句计数差值。增加GC暂停与CPU throttling证据后才能讨论它们的影响。
- HTTP：准入、控制库配置读取、许可等待、资产事务分别计时，继续定位受理P99；不能用发布优化自动推定受理≤500ms。

指标标签只使用有限的库、类型、工作类别和结果，不把userId/orderId/eventId变成高基数指标标签；这些身份用于结构化日志和抽样关联。保持事件schemaVersion=1，不为埋点改变业务请求摘要。

### 10.2 正确性与故障验收

| 场景 | 必须满足的结果 |
|---|---|
| 双实例同时领取、租约过期后旧实例写回 | 同一有效租约唯一；旧token不能更新新领取状态 |
| 32条领取、两个通道同时运行 | 最多16个发布在途；无因本地超额提交产生的UNKNOWN |
| 一波部分confirm、return、超时 | 只完成明确成功行，其余退避；无整批假成功 |
| confirm后SIGKILL、完成事务提交不确定 | 重投原eventId，原业务效果一次、最终可收敛 |
| 大量PrepareDraw与少量其他类型/未知类型并存 | 稀疏类型持续推进，未知类型可进入现有隔离/核查路径 |
| 未来PENDING、有效/过期PROCESSING、未来Retry | 均不生成额外恢复命令；过期租约由Relay接管 |
| Retry已发布但未完成、或消费恰在检查后提交 | 原eventId和generation运输重试；已提交业务不重复变化 |
| 两实例Draw扫描与Stock扫描竞争同一阶段 | 30秒内最多一条新恢复事件，记录与Outbox同事务 |
| 回滚、重启、版本变化、人工resume | 冷却不因重启重置；新状态重新判定，人工恢复不丢deferred事实 |
| 对端重试耗尽、暂停、未知或不可达 | 不刷新失败预算，不释放HELD，不绕过暂停 |
| 固定候选阶段宕机、跨日、取消与晚到回执 | 原候选/策略不变，历史桶退款和库存墓碑语义保持 |
| 正常负载无故障且订单均在30秒内推进 | 自动恢复新增事件数为0；不能通过关闭恢复实现 |
| Rabbit中断再恢复且持续有新请求 | 处理速率实测高于到达率、积压下降，完成后六域零差异 |

保留已有Java与Router回归、Python工具门禁、两进程故障实验和六域对账。新增场景必须真实执行，缺依赖不能用skip当通过。

### 10.3 性能实验

1. 使用相同硬件限额、数据规模/历史行数、持久化配置和实际镜像，重新跑未优化对照；旧180秒短测仅为诊断。
2. 分别隔离验证连续处理、批量领取/完成、恢复保护、索引；索引访问路径验证可先在隔离副本做，端到端仍按单变量归因。记录每档的完整有效配置。
3. 同时维护旧用户分配场景与均匀用户场景。均匀场景业务每10次保持7/1/1/1比例，但用户选择独立轮转；连续200个每类业务请求覆盖200个不同用户，不能仍共享iteration取模导致20人写入。
4. HTTP、交易受理、后台完成和独立查单分别计数；明确记录每类业务的用户数、分库分布和拒绝原因。503等不确定响应逐请求核实回执，不能从统计中删除。
5. 60秒读写暖机并确认初始积压为0后，100档持续600秒，执行三轮；后半程每60秒统计各类型年龄与数量，不允许连续三个窗口上升且停压时仍高于暖机基线。全程四域必须采全；停压后的0不替代负载期不增长证据。
6. 正常无故障抽奖事件放大率定义为六类实际新事件数/(已受理抽奖数×6)，最终应为1.00；运输重投另统计。该场景奖品为固定CREDIT且不耗尽，不把取消、NONE兜底或故障补发混入分母。
7. 达到第1节门槛后才继续高档位、热点2/8/32、签到、支付重放、策略切换和持续到达下的恢复实验。运行前校准所有时钟；同DB阶段耗时与客户端观察延迟分别报告。

## 11. 实施边界与发布回滚

### 11.1 涉及组件

| 组件 | 设计变化 |
|---|---|
| `OutboxRepository`、`OutboxLease` | 批量结果与逐条fencing；领取携带Retry generation及DB时间 |
| `MySqlOutboxRepository`、`MySqlRetryJobRepository` | 批量领取/完成、索引选择、generation与耗尽语义 |
| `OutboxRelay`、`OutboxRelayJob`、`RetryPublishJob` | 两条有界连续循环、分波发布、库/类型轮转 |
| `V2RabbitConfig`、`V2WorkerConfig`、配置属性 | 互斥模式、共享16槽位预算、真实执行隔离与关闭顺序 |
| `DrawRecoveryService`、`StockRecoveryJob`、`PaymentRecoveryService` | 停滞/在途检查、唯一补发入口、持久冷却 |
| 新增目标库Retry运输修复任务 | 已发布未完成的原事件CAS重投，保留failure_count |
| 新增补发记录仓储、迁移 | `v2_recovery_dispatch`与有证据支持的新索引 |
| 性能脚本与验收门禁 | 用户分配修正、分段耗时、恢复放大与四域证据 |

详细接口签名、测试命令和任务checkbox应在书面设计评审后进入独立实施计划，本文不代替实施文档。

### 11.2 上线与回滚条件

采用新增的007系列迁移，登记scope、分表次数及checksum；现有已审计脚本保持不变。`v2_recovery_dispatch`和任务索引是新增结构，旧行无需改业务事实。具体DDL执行按现有迁移运行手册，经目标环境审核后进行，不在服务启动时建表。

先停止所有实例的旧自动补发/扫描写者并排空在途决策，再启用guarded模式；不能在滚动窗口让旧恢复器与新恢复器同时工作，否则新保护无法约束旧代码。随后在隔离环境逐项启用bounded发布、批量事务与索引访问路径。正式环境启用前须完成本设计的故障、回归和容量门禁。

Relay新旧实例理论上可依靠相同token协议共同接管，但不能以混跑数据作为容量验收；模式变更必须保证同一实例内旧tick与新循环互斥。新索引和补发表存在时legacy程序应仍可启动。

性能回退优先把Relay恢复为legacy并保留guarded恢复，所有补发记录保留。完整回滚到旧二进制需要协调停写/恢复器切换，明确会重新引入旧补发行为，不能自动无缝回滚。任何回滚不删除Outbox、Retry、补发证据，不重置attempt，不直接调整账务或库存。

## 12. 设计评审检查结果

本文已检查：发布窗口与批量预算相容；发送不持有DB事务；逐条token和Retry generation保留；跨库观察不冒充原子性；Draw/Stock不会各自独立补发；旧任务与人工resume版本变化不会漏掉在途检查；核查/取消/冻结候选语义保持；新旧模式与迁移回滚有边界；容量结论与未来目标分开。

本次交付为待评审设计和已保存的诊断证据。尚未实施上述优化，也没有新的容量通过结论。
