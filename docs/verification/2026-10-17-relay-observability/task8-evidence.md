# Task 8 观测与故障证据

本页索引隔离环境的实际结果；原始目录位于本次执行宿主 `/tmp`，不是仓库内可移植附件。资源固定为两应用各 CPU 1 / 内存 512 MiB / Xmx320m、batch20、lease30、settle1；不构成 Task9 容量结论或生产发布批准。详细失败记录与裁定保留在本次工作目录 `.superpowers/sdd/2026-10-16-outbox-relay-recovery-optimization/task8-report.md`。

## 已完成独立切片

| 切片 | 原始目录 | 实际结论与边界 |
|---|---|---|
| confirm 后 SIGKILL | `/tmp/bromark-task8-raw-20261016-g` | 真 exit137，原 event attempt1→2/token变化、账务一次；原构建仅 hash/日志，没有原二进制字节副本 |
| 六域非零人口 | `/tmp/bromark-task8-correctness-20261016-l` | HTTP draw/signin 与沙盒 paid callback；六域非零后停写全0；同样没有原二进制字节副本 |
| MySQL 连接 KILL | `/tmp/bromark-task8-mysql-kill-20261017-u` | 实际 UPDATE 后 COMMIT 前连接 KILL，原1秒内约89ms，同 event 接管；不是 COMMIT 已发送后的网络不确定性 |
| 旧 token 晚结算 | `/tmp/bromark-task8-oldtoken-20261017-w` | 新 owner 仍 PROCESSING 时真实原 ClaimedPublish 经独立1秒生产 repo 事务丢失租约、当前行不变；不声称原 runOnce 越20秒截止或实际 UPDATE affected0 |
| 暂停后晚回执 | `/tmp/bromark-task8-paused-20261017-ab` | 原消息发送前冻结→生产 pause→实际消费 durable defer/HELD资产不变→审计 resume→账务一次；不自动声称 UNKNOWN 后物理晚发送 |
| bounded 回退 legacy Relay | `/tmp/bromark-task8-rollback-20261017-ag` | 真实 guarded dispatch，存活旧 writer 拒绝；两143正常退出、目标连接0、同 broker/volume/queue，guarded保留、原身份/attempt/dispatch保留、账务一次 |
| 正常采样 | `/tmp/bromark-task8-sampler-smoke-20261017-ak` | 4 HTTP202/原receipt/DELIVERED；六类事件各4、放大率1.00、dispatch0，末四域有效0；采样实际0.81–1.00秒有干扰 |
| Rabbit 中断与持续到达恢复 | `/tmp/bromark-task8-rabbit-recovery-20261017-ap` | fresh broker 真 stop/start；65 原 HTTP202/65 DELIVERED，六类事件各65。恢复窗44次尝试/44.397秒、最大请求启动间隔1.005秒，新增受理1.014/s、完成1.284/s、未完成13→1；停写后四域有效0，六域对账0。原始2083文件hash全匹配，manifest SHA256 `71a702e12961fc4da554012106e1c83e98dc59fbc77827dbaa6054aabdadda75` |

u及之后冻结实际 JAR、probe 所有 inner classes、source（含未跟踪）、runner 外部副本与 postflight hash。g/l 的缺失不可用新构建回填。资源 inventory 只证明已知本次容器写者，无法排除任意外部进程。

## 失败与补救不能拼成通过

- y 因 mysql CLI JSON 转义失败，z 仅原单审计 resume 补救；完整暂停证明来自 fresh ab。
- ad 因 docker stop 弃用 warning 破坏回执校验失败；ae 仅原单正常恢复，完整回退来自 fresh ag。
- ai 首样本 CLI 表头解析失败，尚无 HTTP intent；fresh ak 正常采样通过。
- am 原脚本 exit0，但独立审计发现584.761秒 HTTP gap、585.418秒采样 gap，宿主 pmset 证实休眠。裁定 INVALID_CONTINUITY；其66单最终正确性及 an 六域0不能补成持续到达门槛。原 PASS JSON 不改，另存裁定与派生分析 ao。后续门禁增加全程/恢复 maxgap≤3秒、attempt rate≥0.8/s、wall/monotonic差检查。
- 早期 d/r 失败及 h/s 原单补救单独保留；没有重造未终态或重置账务事实。

## 设计10.1证据映射

| 要求 | 实际证据 / 口径 |
|---|---|
| claim 时间/行数/SQL条数 | `bm.v2.relay.duration{stage=claim}`、count claimed；ak 真实24行。精确 SQL 条数复用 Task2 实测32行 claim15 SELECT+1 UPDATE、settle2 SELECT+1 UPDATE（`task2-sql-final.log` 的 BATCH_SQL_COUNTS）；另采目标 schema digest 窗口差，不为埋点每行再查DB |
| 每波启动/结果、confirm/settle | count started 与 duration wave/confirm/settle 的有限 outcome；ak 两实例累计各24。nacked/returned/UNKNOWN不算完成；batch finished只指循环与cleanup正常 |
| 失租/未发送释放/超时 | 对应 lost_lease/released_unsent/timeout；实际旧 token slice 与确定性失败/截止测试分别核证，未发生的在线标签不冒充非零采样 |
| 每库每类型速率 | 真实 Prometheus 原值按同 app 窗口做 completed/claimed差分，标签限定库与事件；独立业务完成率用 SQL DELIVERED ID 集合，不能混称消息速率 |
| Outbox/Retry类型、状态、age | 每秒采样 SQL 原文+返回，created_at oldest age；ORDER 原ACCEPTED时间独立计龄，不与任务混加 |
| BROKER域 | fresh broker两目标队列 ready/unacked实际值；停止期间 unavailable/null。CLI无精确 oldest timestamp，age 始终null并注明原因，不能填0或用发布时刻代替 |
| 检查/补发/抑制/耗尽 | `WorkflowRedrive.commit` 实际判断与 afterCommit counter；Guarded SQL fixture 每方法导出实际 finite counters，必须join同次成功XML。fixture含显式时间/失败设置，非负载证明；早期扫描返回不计checked |
| 许可/事务/Hikari | 实际registry按工作类别permit/transaction、连接池active/pending；HTTP asset_tx在取得permit后开始。ak实际抓到两实例原文 |
| 行锁/语句差 | performance_schema全局 locks/Questions与目标schema digest原值；全局包含采样及其他负载。服务器未返回Com_*项明确missingCounters，不填0、不归因单请求 |
| GC/CPU | 原GC日志、同容器cgroup cpu.stat差；完整进程GC含startup，不冒充精确HTTP窗口。采样自身开销保留 |
| HTTP阶段与P99 | http_admission/activity_admission/config/permit/asset_tx原timer；ak前三类与asset_tx各4。无直方图不能由timer count/sum编造P99，短故障样本不证明≤500ms SLO |

最终回归：固定 Java25 Maven 镜像、隔离仓库缓存，标准 `Router*Test,*V2Test,Java25RouterIntegrationTest,V2FoundationWiringTest,AliasMethodAlgorithmTest` 选择器 `/tmp/task8-standard-full5.log` 实跑 Router28+app241=269，失败/错误/跳过均0；XML备份 `/tmp/task8-standard-full5-evidence`。Python performance `unittest discover` `/tmp/task8-python-final.log` 为55/55 OK。对应 JAR SHA256 `afe6efdabb5ff056aaafeda189489dd9bf84db6bbc5c1350670dff2cc5aec46e`、探针类 `fdc44c3319fdcb22579ee909024a9703343188973cb9ff0f4139d7588819f81c`。Rabbit独立停写 `/tmp/bromark-task8-rabbit-post-20261017-aq.json` SHA256 `6f8e3140408d2107705bc738bb05e64f503aeda52ce2211eda1380edec714696`，CREDIT/QUOTA/STOCK/DELIVERY/PAYMENT/SIGNIN差值、未完订单、pending任务和待审均0。完整命令及版本限制见 `task8-simplified-report.md`。


回归迭代限制：full3 269项/1error，HTTP原单25秒停COMMITTED，保留失败。只读诊断支持共享历史fixture在legacy恢复下产生大量旧任务的解释，尚非完全因果证明。RaffleIntegration现显式覆盖guarded恢复+legacy Relay，安装007并断言实际模式/guarded任务；原HTTP七步、身份与25秒断言不变。它不再用默认legacy恢复配置，也不代表历史schema真正隔离；原模式单跑成功不能替代完整门禁。
