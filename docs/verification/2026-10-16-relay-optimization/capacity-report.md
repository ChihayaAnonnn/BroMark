# Relay 优化容量裁定：10 rps smoke 未通过

## 版本、环境与证据

源码 `caf2484`；本轮未修改 Java 或重构建。实际 JAR SHA256 `afe6efdabb5ff056aaafeda189489dd9bf84db6bbc5c1350670dff2cc5aec46e`，与 Task 8 最终回归 JAR 相同。固定 Maven/Java 25 镜像 `maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976`。两应用各 1 CPU、512 MiB、`-Xmx320m`，bounded Relay＋guarded recovery、settle 1 秒；原连接池/许可/Rabbit 配置由原 `start_app` 使用，实际 inspect 在 `resource-budget.json`。fresh Rabbit ID、双应用 ID、三库、网络和 writer inventory 在 `preflight.json`、`active-writer-inventory.json`、`formal/execution.json`；库为 `router_perf_it_t10smoke1_{control,one,two}`，只查本轮实际 ID。007 结构/迁移 hash 由 seed 和 harness preflight 检查。共享宿主同时承载数据库、Rabbit、两应用、k6、tracker 与 SQL/CLI sampler，结果只适用于该隔离环境，不能推断生产容量。

原始目录：首次编排失败 `/tmp/bromark-task10-smoke-20261017`（在预热前，55 个文件逐项复算匹配，`SHA256SUMS.json` SHA256 `bd7d1d1508f226196775a0bae0fa8679e28aa4baaf4f4b0041c5f9ac673de5d2`）；实际运行 `/tmp/bromark-task10-smoke-20261017-b`（1715 个文件逐项复算匹配，manifest SHA256 `dcddde40a3afc16a400c3c6ec663917f9f0de4d6285bf1f42059f2775eefbd61`）。仓库外编排脚本保存在后者 `orchestration.py`，SHA256 `e36197c7047d12a67e02a87a6da67191433ff4d1de45266162b019b68c6c36c4`；JAR/测试探针/工具/Java 源码字节冻结在 `artifact-bytes/`，postflight 1332 文件 fingerprint unchanged=true。原始请求、tracker、k6、SQL、Rabbit、Prometheus、cgroup CPU、GC 和停写对账均留 raw。初版编排在 k6 summary 缺少 `dropped_iterations` metric 时误把它当 0；`*.initial.json` 保留初版元数据/collector，最终 `metadata.json`、`results.json` 已派生修正为 `null`/缺证，失败项增加 dropped 未验证，具体修正见 `derivation-note.json`，原 k6 文件未改。最终 `formal/results.json` SHA256 `a5464f6c8cdbf67313d023e3231b1233353b892958a492ffd0e0357d6665d4aa`，`reconciliation.json` SHA256 `1d1b79adf5a03c4a1a3e3bdf8e3fff094fd8f3f4dfa26f4acb0ae6d3c1744817`。seed/token/private key 在权限受限的仓库外 `/tmp/bromark-task10-seed-20261017`，未写入 Git。

## 执行与正式分母

20 个用户以确定原 `pre:t10smoke1:<index>` requestId 预置，20/20 DELIVERED。首次预置原始请求 journal 在失败目录 `precreate/`；实际运行目录再次核对映射与交付。预置不入预热/正式分母。独立 `warmup` 10 rps×60 秒：k6 601 iterations、k6/tracker exit 0；其请求目录独立。预热后独立四域样本 ORDER/OUTBOX/RETRY/BROKER 均 valid、count 0。正式 `formal` 为 uniform mixed 7 query、1 result、1 exchange、1 draw，每 10 次一组，10 rps×120 秒；k6 1201 iterations、0 interrupted，但 summary 未输出 `dropped_iterations`，按缺证/null 处理，不认定 dropped=0；k6/tracker exit 0、owned app/broker 身份未变。20 个身份在四类操作均出现，请求分库 db01 600/db02 601。Task 8 Sampler 有 34 份正式样本，跨度 118.17 秒，位于 runner 121.45 秒负载窗口，四域有效。

| 正式结果 | 实测 | 本档要求/裁定 |
|---|---:|---|
| 原网络请求 | 1201 | 约 1200；到达实现 |
| 原 2xx 非查询业务受理/终态 | 237/237 | 受理都有明确终态 |
| 原异常响应 | 503 无订单 ID 2；status 0 无响应 1 | 技术失败 0；**失败** |
| 业务/过载拒绝 | 0 | 0；通过 |
| query/result P99 | 1527.47 ms | ≤200 ms；**失败** |
| exchange/draw 入口 P99 | 4896.39 ms | ≤500 ms；**失败** |
| 同库 draw 确认/交付 P99 | 11665.01 / 13911.75 ms | ≤5000 ms；**失败** |
| 跨时钟 completion P99 | 未验证 | 开跑前客户端/DB 偏差 21.42 ms、采样不确定度 24.31 ms，超过预声明 10 ms；不填 0 |
| 负载期四域趋势 | ORDER 首/末三分之一均值 0/2；OUTBOX 0/1.91；RETRY 0/0；BROKER 0/2.18 | BROKER 超出 collector 的首均值＋2 容差；**失败**。末点全 0 与趋势独立 |
| 停写六域 | CREDIT/QUOTA/STOCK/DELIVERY/PAYMENT/SIGNIN 差异 0；未完订单/待处理任务/待审 0 | 通过；含预置、预热、正式和原请求补救，不能当正式容量分母 |

正式 2xx 受理 237 中，draw/exchange 原网络成功与 3 个异常请求分开统计。`original-request-recovery.jsonl` 记录原 requestId 原用户在正式负载**结束后**重放，三笔均获 200/DELIVERED 和原 orderId；原状态仍是 503/0，绝不改写为正式 2xx。`original-request-db-facts.json` 中同一订单的 ACCEPTED 或 exchange business_at 服务端时间分别为 1792205481.371251、1792205488.890825、1792205493.272478，均早于补救观察时间约 1792205607.97；可证本次补救前数据库已有对应原业务记录，不能由重放后的成功倒推入口成功。原 requestId 分别为 `t10formal1:load:789`、`:808`、`:908`；其原业务最终交付且六域账务一致。正式 `smokeDataComplete=false` 保留原无响应及 503 的缺口。

负载时四域单点峰值 ORDER 12、OUTBOX 9、RETRY 0、BROKER 12，最终停写前四域都有效且 0。Broker 无精确 oldest message timestamp，原采样记 null/原因；停写归零不替代负载期无增长。SQL sampler 本身每轮约 0.8–1 秒，且 Prometheus、Rabbit CLI、共享主机其他进程有干扰；现有证据只能定位现象，不能从这些观测直接归因长尾到某个线程、SQL 或 Broker。`clock-after.json` 偏差 15.87 ms，仍超 10 ms；不事后改阈值。`transportRetries`、`redriveEmitted`、`redriveSuppressed` 未经独立正式窗口归因，报告为未验证，不以 0 代替。

## 正确性、清理与裁定

Task 8 最终 Java 269/269（failure/error/skip 0）和 Python 55/55 见 [Task 8 报告](../2026-10-17-relay-observability/task8-simplified-report.md)；Task 9 工具 Python 66/66 和真实多 VU 合成 probe 见本工作区 SDD `task9-simplified-report.md`，它不是业务吞吐。Task 10 未改 Java/配置，复用相同 JAR。停写后应用停止、目标三库 processlist 为 0，六域对账 `passed=true`、四项差异/未终态均 0。支付、签到的非零正确性来自 Task 8 独立人口；本次 mixed 没有支付或签到请求，因此不作为本次容量证据。

`cleanup.json` 记录本次自有双应用及 fresh Rabbit 删除且 absent；Rabbit 数据卷保留，隔离三库和账务不清除。旧 phase6、ChatSpace 和共享 daemon 设置未触碰；临时 caffeinate 已停止。本次原始失败与修复分开留档。

**最终：** `developmentValidationPassed=true`，`smokePassed=false`，`fullCapacityPassed=false`，`releaseAuthorized=false`。下一步先排查原 503/无响应与超时、对负载期 BROKER 增长做可重复定位、改进有不确定度界限的时钟测量；需新隔离 run 重测 10 rps smoke。原同资源 A/B、五项消融、100/s×600秒×3轮、后续矩阵、独立压测机与发布审阅清单保持未执行/NO-GO，不对本轮失败升档。

永久保留索引为 [raw-index.json](/Users/vivix/Desktop/workspace/BroMark-relay-evidence-20261017/raw-index.json)；对应原始目录在 `/Users/vivix/Desktop/workspace/BroMark-relay-evidence-20261017/raw/` 下，保留同名失败与正式运行目录。SDD 工作记录和 [完整裁定记录](/Users/vivix/Desktop/workspace/BroMark-relay-evidence-20261017/decisions.md) 同目录保存。独立 [Task 10 审查](/Users/vivix/Desktop/workspace/BroMark-relay-evidence-20261017/sdd/task10-simplified-review.md) 与 [全分支审查](/Users/vivix/Desktop/workspace/BroMark-relay-evidence-20261017/sdd/final-branch-review.md) 对当前精简开发交付均 PASS、无阻断；后者记录两项未在本轮修复的 minor：Publisher cancel/强制 close 缺直接测试，以及持久 Retry 恢复缺完整目的 route 重算。后续分别补聚焦回归与坏路由隔离验证；这两项审查意见不抹去 smoke 失败，也不授权发布。

永久归档 `SHA256SUMS.json` 覆盖 7683 个文件，其 SHA256 为 `064cf93d34e1cca78d30f537ec63ed71eec1bbe1305224f1341e18730fe74bb2`。该归档索引与前述两轮实验各自的 raw manifest 分别保留，不互相替代。
