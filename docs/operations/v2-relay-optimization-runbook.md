# V2 Relay 优化隔离切换与回滚手册

本手册仅适用于 `bromark-relay-20261016-*` 隔离资源和 `router_perf_it_<run>_{control,one,two}` 全新目标库。正式发布需要另行授权；本地通过不等于生产容量或发布批准。所有操作保存命令、UTC 时间、配置、源码/JAR/镜像 SHA256、容器 ID、队列前缀、迁移校验和及原始 SQL/日志路径。

## 进入前检查

1. `source /tmp/bromark-relay-20261016.env`，不得打印环境、密码、JWT 或付款签名密钥。确认两应用实例、本次 MySQL/Rabbit、本次 Redis 以及三个 `bromark-assets-redis-{1,2,3}` 的容器 ID、镜像摘要、网络、运行状态；本次资产节点网络为 `bromark-assets-cluster-it`。任何名称/ID/网络/目标库/队列归属不符立即停止。只允许操作本次脚本启动并拥有的应用和专属依赖。
2. 核对本次三个目标库全部 001—007 manifest checksum、007 `v2_recovery_dispatch` 必需列与索引；确认 `bm.v2.budget.replica-count=2`、物理连接预算、双实例模式、Rabbit prefix、Hikari 网络超时及 30 秒共同关闭 deadline。两实例和库时钟要校准，时间误差写入证据。
3. **guarded 前置条件**：列出该三库的所有应用/定时器/人工任务写者，停止所有实例的旧 Draw/Stock/Payment/通用自动补发及扫描；等待在途决策和事务排空，并从进程/配置/数据库许可共同证明。任何旧写者仍在运行或证据不可得，拒绝 guarded。禁止滚动窗口中旧恢复器与 guarded 同写。保存停写前后 Outbox、Retry、dispatch、attempt、业务余额/库存的快照。
4. 固定源码/JAR/test-only 探针 class/镜像 hash 并保存运行原字节（包括未跟踪源码）后，先在新库/新队列做正常样本与无故障六事件核查；再以不同目录做独立故障。每个请求保留 requestId、HTTP 原始响应、orderId；503/UNKNOWN 携 orderId 必须按原身份查回执，不能从分母删除。无 orderId 的 503 也保留拒绝记录。

## 隔离验证与停止条件

- 先跑 `RelayObservationV2Test,RelayOptimizationFaultV2Test` 和 Python 资源所有权测试。`docs/performance/fault_relay_optimization.py --env v2-it --seed /tmp/<new-run>/seed.json --out /tmp/<new-raw-dir>` 的每个 case 新目录必须含 manifest、原事件/订单身份、前后 SQL、真实信号或连接 KILL 回执、原始时间序列、SHA256。脚本必须在任何 kill/stop 前验证容器 ID/网络/目标库、只停止自身启动的进程/依赖。异常、测试名、模拟断连不代替真实故障。
- 故障次序：双实例同身份竞争；真实 broker confirm 后屏障→SIGKILL 目标 PID→原 eventId 接管；完成 UPDATE 后、COMMIT 调用前核同事务连接 ID→MySQL 真 KILL→从数据库事实核实回滚与重放（不能称已发 COMMIT 的不确定提交）；Rabbit stop/recover 期间持续新请求，采 ORDER/OUTBOX/RETRY/BROKER 数量与年龄、到达/完成/排空率；旧 token 晚写；暂停后晚 receipt。MySQL KILL 前后同一 admin 调用取服务器 UTC 微秒上下界，完成上界加 999ns 后仍须小于 app 原 Deadline；过期拒 PASS。单案出现未解释账务/库存差异、永久增长积压、丢失原身份、未知资源归属或脚本连接失败，停止后续案并保存失败原始记录。
- 结束新流量后保留 Worker 排空；所有本次目标库写者停下后执行 `reconcile_v2_run.py --stopped`，同时核实六域差异、未终态订单、未结核查和待发任务。专门正确性样本要有真实付款到账及签到非零人口；正常固定 CREDIT 六事件放大样本单独计算。零差异但 PAYMENT/SIGNIN 人口为零不能报告完整六域覆盖。

## 回退

性能回退先停新受理，按共同 30 秒 deadline quiesce/排空，保持 guarded 恢复，单独将 Relay 从 bounded 切回 legacy；验证同一实例旧 tick 与新 loop 互斥，原 Outbox/Retry eventId、token、generation、attempt、dispatch 与账务事实保留，重新受理后能收敛。任何未完成/不确定任务由原租约或运输重试接管，不能伪记 PUBLISHED。

完整回退旧二进制须再次停止所有写者和恢复器、排空在途，明确旧恢复器会重新带回原补发行为；不能自动无缝回退。绝不删除 Outbox、Retry、dispatch、审计映射，不重置 attempt，不直接修改余额、次数或库存。回退前后都做相同六域/任务核对并保留 hash，失败时保持停止状态以供审计。

## 独立切片与观测口径

同一 CLI 的 `--old-token`、`--mysql-kill`、`--paused-receipt`、`--rollback`、`--sampler-smoke`、`--rabbit-recovery` 分别使用 fresh run/目录；不能把补救拼接成原案成功。`--recover-order/--recover-user` 仅正常收敛原失败单；已有真实补发事件时同时给冻结 raw 中的 `--recover-event/--recover-hash`，不能任取第一个 PrepareDraw。暂停原单的补救用 `--resume-paused-order/--recover-user`，必须保留 gated-before、真实 version、operator/reason 和审计 replay 映射。失败目录不可覆写。

旧 token 切片捕获原真实 ClaimedPublish，在新 owner 仍 PROCESSING 时，以独立正常 1 秒 deadline 调生产仓库投递迟到结算。原 runOnce 的 20 秒期限不变；这证明仓库 fencing，不证明原调用越过截止，更不能把过滤旧身份而未发 UPDATE 写成执行了受影响零行的 UPDATE。暂停切片冻结真实发送前字节，再由生产 pause/实际消费者 defer/审计 resume 验证；是否存在 UNKNOWN 须另核实际时间线。

回滚先用实际 listener 停止和正常 guarded 决策产生非零 dispatch，实际拒绝存活旧 writer，再并行停止两个已知应用。共同 30 秒内只接受退出 0/143、无 OOM/error、inspect 精确 ID 与停止回执；目标库连接归零、完整已知应用配置清单确认后才启动 legacy Relay + guarded。Rabbit ID/持久卷/队列不变。完整清单仅覆盖已知容器，不能证明任意外部 host 进程不存在。

持续到达实验独立保存 HTTP 原 intent/响应、SQL 原 ACCEPTED/DELIVERED 集合和采样起止。完成率分母使用实际采样窗口秒数；不得将停写 drain 归入恢复窗口。所有 503/网络 UNKNOWN 必须按原 requestId 核回执；缺少证据时失败。最终必须有非零 DELIVERED 人口、三库任务排空及两条实际 Broker 队列 ready/unacked 均零。

采样器只读本案 fresh broker/app ID 和 seed 三库。Outbox/Retry 按类型与状态给 count 和最老 created_at 年龄；ORDER 以原 ACCEPTED 阶段时间计龄。Rabbit 停止时 count/age 为 null；其 CLI 不提供精确最老消息时间，因此 broker age 始终 null 并注明不可得。不可填 0 或用 published 时间冒充入队时间。MySQL 物理实例全局计数含采样开销与其他活动；只给窗口差值，不能归因单次请求。cgroup 累计值按相同容器窗口差分，GC 暂停以原 GC 日志为准。

生产管理端点安全规则保持不变；测试启动器仅在 `arm-sampler` 文件存在时把真实 registry scrape 原子写本地 evidence，采样检查新鲜度。`batch=finished` 仅表示循环及未发送释放正常返回，运输结果分别看 wave/confirm 和已结算 completed；UNKNOWN/NACK/RETURN 不可记完成。HTTP config 包含控制库许可等待，asset_tx 从取得资产库许可后计时；分段均值或 P99 不可相加冒充端到端 P99。

发送器按 1 rps 计划执行，必须同时通过恢复窗口和全程窗口的实际连续性检查：相邻请求启动间隔不超过 3 秒、实际 attempt rate 至少 0.8/s；采样 wall 与 monotonic 时长相差超过 2 秒也拒绝。宿主休眠、发生器停顿与严重采样延迟都不能算持续到达。必要时记录有界的本次 keep-awake 进程及清理回执，不修改永久电源配置或重启共享 Docker；已失败的原结果与独立审计裁定并存，另 fresh run 重测。

恢复计数的 checked/suppressed 定义为进入 `WorkflowRedrive.commit` 的实际保护判断，不是扫描行数；在更早阶段返回的扫描不自动计入。SQL 集成 fixture 导出的原因/阶段计数须关联同次成功 Surefire 方法，显式说明其中的 stale/time/failure 设置；它不能替代真实故障窗口。batch `finished` 仅表示循环与未发送清理正常结束，运输成功仍查各 wave/confirm 的有限结果（包括 nacked/returned/unknown）。
