# Task 8 精简验收报告（2026-10-17）

范围：Relay 有限观测、真实故障正确性、Rabbit 中断后的持续到达恢复。固定两应用各 CPU 1、512 MiB、Xmx320m，Relay batch20/lease30/settle1；不把本轮当容量或生产发布测试。Task 1–7 的实现和实证未重做。

## 最终回归与版本

- Java：固定 `maven@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976`（Java25），挂载 `/tmp/bromark-java25-m2:/m2`，运行 `mvn -q -Dmaven.repo.local=/m2/repository -pl bm-app -am -Dtest='Router*Test,*V2Test,Java25RouterIntegrationTest,V2FoundationWiringTest,AliasMethodAlgorithmTest' -Dsurefire.failIfNoSpecifiedTests=false clean package`。日志 `/tmp/task8-standard-full5.log`、退出码 `/tmp/task8-standard-full5.exit`、75份 XML `/tmp/task8-standard-full5-evidence`：Router28+app241=269，failure/error/skip 全0。原 full2 的 RelayLifecycle ready 竞态、full3 的 RaffleIntegration 25秒 COMMITTED 错误和中断的 full4 各保留原日志，未合并为成功。Raffle 测试现在显式 legacy Relay+guarded recovery、007 schema 和 guarded-only job，原 HTTP 断言及25秒期限未放宽。
- Python：`python3 -m unittest discover -s docs/performance -p 'test_*.py'`，`/tmp/task8-python-final.log`，55/55 OK。为故障工具的固定 `/workspace` classpath 约束，又以同 JAR/源码运行 `RelayOptimizationFaultV2Test` 聚焦测试，`/tmp/task8-final-probe.log` exit0，175 classpath entry 经工具逐项校验。首次 Rabbit 入口因完整回归挂载 `/src` 触发 classpath 拒绝，发生在创建实验目录和操作资源之前；`/tmp/task8-rabbit-ap-console.log` 保留，不计故障结果。
- Rabbit 实际运行 preflight JAR SHA256 `afe6efdabb5ff056aaafeda189489dd9bf84db6bbc5c1350670dff2cc5aec46e`；`RelayFaultProbe.class` SHA256 `fdc44c3319fdcb22579ee909024a9703343188973cb9ff0f4139d7588819f81c`；`BoundedOutboxRelay.java` SHA256 `c09c0f6a06a1b0cf8ffc1d686d68d79b44f808914cdeea50bb33450e911b54ee`。`artifact-postflight.json` 指纹 unchanged=true。早期 g/l 仅留 hash/日志，没有原二进制字节副本，不能用此版回填。

## 故障与恢复证据

已审通过的 confirm 后真实 SIGKILL、MySQL 提交前连接 KILL、旧 token 原对象晚结算、暂停后晚 receipt、legacy Relay+guarded 回退见 [证据索引](task8-evidence.md)。各案例原 identity、前后 SQL、原始目录和补救边界独立保存；ag 回退 manifest `c3a80bd049c479bb22d5d42dcd11120f7c08168ab089d4d22e918b6bc097ce14`，ak 正常采样 manifest `77f9e86a85aa103831be757f391407ada851767bd356a0e1e4ffd5a651004cfd`。失败的 y/ad/ai 与独立补救 z/ae 均未冒充原案通过。

fresh Rabbit：`source /tmp/bromark-relay-20261016.env` 后运行 `python3 docs/performance/fault_relay_optimization.py --env v2-it --seed /tmp/bromark-task8-seed-t8rabbit2/seed.json --out /tmp/bromark-task8-rabbit-recovery-20261017-ap --rabbit-recovery`。实际 console `/tmp/task8-rabbit-ap2-console.log`、结果 `rabbit-result.json`、逐请求 intent/response/最终 receipt、逐秒四域 SQL/队列/Prometheus/cgroup/GC 原文均在 raw 目录。fresh 自有 Rabbit 确实 stop/start 同 ID；65 原 HTTP 全202，65 原单 DELIVERED，六类正常事件各65。恢复窗 44.397秒内44次尝试，实际0.991/s、最大请求启动间隔1.005秒；新增受理1.014/s、完成1.284/s，未完成13→1。最终 ORDER/OUTBOX/RETRY/BROKER 均 valid 且 count0；Broker 无精确 oldest age，记 null。2083个 raw 文件 SHA256 全复算匹配，`SHA256SUMS.json` SHA256 `71a702e12961fc4da554012106e1c83e98dc59fbc77827dbaa6054aabdadda75`；自有3容器删除回执/absent，专属 Rabbit 卷保留。

此前 t8rabbit1 的 `/tmp/bromark-task8-rabbit-recovery-20261017-am` 虽原脚本 exit0，主机实际 sleep586秒，HTTP 最大间隔584.761秒，裁定 INVALID_CONTINUITY。其原 PASS JSON 未改，独立派生分析 `/tmp/bromark-task8-rabbit-invalid-analysis-20261017-ao.json` 与停写 an 分开保存；本轮 ap 才是连续到达证据。临时 `caffeinate -is -t 1800` 仅覆盖剩余实验，结束后停止，不更改永久电源设置。

停写后另运行 `python3 docs/performance/reconcile_v2_run.py --seed /tmp/bromark-task8-seed-t8rabbit2/seed.json --out /tmp/bromark-task8-rabbit-post-20261017-aq.json --stopped`。输出 SHA256 `6f8e3140408d2107705bc738bb05e64f503aeda52ce2211eda1380edec714696`：CREDIT/QUOTA/STOCK/DELIVERY/PAYMENT/SIGNIN 六域差值总0，unfinishedOrders/pendingTasks/unresolvedReviews 全0。该停写对账独立于故障窗口速率判断。

## 口径与限制

保留现有有限标签 claim/confirm/settle、完成/失租/释放、redrive 与四域积压观测；完整设计10.1采集不作为本次精简门槛。confirm 是应用 callback 时长，batch finished 是 loop+release 正常返回，均不能解释为纯网络 RTT 或每条消息运输成功。MySQL global/digest 包含采样及其他负载；Broker exact oldest age 不可得，Com_* 缺失，HTTP P99 无直方图，不填0或推断数值。短时 Rabbit 样本仅证明本配置下恢复窗持续到达和最终正确性，不推出容量上限。已知容器 writer inventory 无法排除任意未知外部进程。
