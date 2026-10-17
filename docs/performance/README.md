# 隔离性能诊断工具

当前简化实验只验证本次 `bromark-relay-20261016-*` 隔离资源上的 mixed 分布与一次 10 rps、120 秒 smoke。`fullCapacityPassed` 和 `releaseAuthorized` 恒为 `false`；600 秒、三轮、完整场景矩阵与发布门禁仍按原容量计划另行执行。smoke 完成或单轮 SLO 良好不能替代容量结论。

先从当前 `/tmp/bromark-relay-20261016.env` 注入环境变量。`V2_MYSQL_CONTAINER`、`V2_TARGET_RUN_ID`、`V2_APP_CONTAINER_1`、`V2_ACTIVE_RABBIT_CONTAINER`、`V2_TEST_DOCKER_NETWORK` 和 `V2_TOKEN_FILE` 必须指向本次拥有的容器、三库、fresh Rabbit 与外部 token 文件；runner 会核容器 ID、网络、127.0.0.1:19871 端口、应用绑定的三库与 broker 及真实库名。所有密钥只留仓库外，不打印完整容器 Env。不要运行旧 `sample_v2_runtime.py` 或 `fault_v2_runtime.py`：它们仍含 phase6 目标。

seed 只对全新 `router_perf_it_<run>_{control,one,two}` 应用 manifest 中的 001–007；`scope=all` 覆盖三库。原历史 fixture、预置查单订单、60 秒读写暖机需要各自独立证据，不能将新 seed 称为旧历史基线。mixed 的 result 查询要求 `seed.json.resultOrders` 为每个用户提供已完成预置订单。在已启动并核验拥有的双应用与 fresh Rabbit 后，先独立预置并保存请求/查单原始回执：

```sh
python3 docs/performance/prepare_v2_results.py --seed /tmp/SEED/seed.json --out /tmp/RUN-precreate
```

此 helper 为每个 token 用户复用确定的 `pre:<seedRun>:<index>` 请求 ID；503 带/不带订单 ID、网络未知都会留原始 journal，同一原请求 ID 重试，不能换新请求偷偷补齐。只有每单查询到 DELIVERED 才原子写入 seed 的 `resultOrders`。简化 Task10 的 20 用户 seed 需预置 20 单；200 用户均匀性已由独立合成 k6 探针验证。预置目录、暖机和正式负载使用不同目录与请求 ID；仅正式目录的请求进入正式分母。例如：

```sh
python3 docs/performance/run_v2_experiment.py --seed /tmp/SEED/seed.json --out /tmp/RUN-warm \
  --rate 10 --duration 60 --scenario mixed --run-id RUN-warm --phase warmup --user-distribution uniform
python3 docs/performance/run_v2_experiment.py --seed /tmp/SEED/seed.json --out /tmp/RUN-formal \
  --rate 10 --duration 120 --scenario mixed --run-id RUN-formal --phase formal --user-distribution uniform
```

`--user-distribution` 默认 `legacy` 以保留旧对照。`uniform` 使用 k6 全局 iteration，把 mixed 每十次固定为 query/result/exchange/draw = 7/1/1/1；query 连续编号与其余三类独立编号分别加 offset 后映射用户。`test_relay_optimization.py` 用真实多 VU Docker k6 和本地合成 HTTP 服务检查 200 用户覆盖；该探针不访问业务容器，不代表容量。

runner 只发送 k6 请求并启动独立查单 tracker，不启动旧采样器。`execution.json.collectionComplete=false`、`task8SamplerRequired=true` 明示四域证据缺失；Task 10 另用 Task 8 `sample_relay_optimization.py` 的 owned 容器 sampler 保存负载期 ORDER/OUTBOX/RETRY/BROKER，排空期独立标记。Rabbit 管理计数没有精确最老消息时间，Broker age 为 `null` 并附原因。停写后的排空不能证明持续流量下恢复速度。

正式追踪后，在相同三库读取受理订单的 `ACCEPTED`、`confirmed_at`、`delivered_at`：

```sh
python3 docs/performance/export_v2_stages.py --seed /tmp/SEED/seed.json \
  --requests /tmp/RUN-formal/requests.jsonl --completions /tmp/RUN-formal/tracked.jsonl \
  --out /tmp/RUN-formal/completions.jsonl
```

导出器只查正式请求中具有原 orderId 的 draw/payment，保留 503 带 orderId 的原回执查询；503 无 orderId 与网络未知单列，不能假定拒绝。新受理的 draw 订单用同库服务端 `ACCEPTED→confirmed_at` 和 `ACCEPTED→delivered_at` 计算确认、交付延迟；HTTP 延迟来自客户端 k6，轮询观测延迟另列。`clock-before.json` 在负载开始前记录 client/server/client 与采样不确定度。确认是订单达到 COMMITTED 的持久事实；Relay confirm 指标是每 attempt 的应用观察时长，不能互换，也不能把各 P99 相加。

`collect_v2_results.py` 的 `sent/received/accepted/completed/pending/rejected/unconfirmed/unknown/http503WithOrderId/http503WithoutOrderId` 均为**请求或去重订单数**；`queryLatencyMs`、`acceptanceLatencyMs`、`serverDrawConfirmationLatencyMs`、`serverDrawDeliveryLatencyMs`、`completionLatencyMs` 为毫秒 P50/P95/P99；`pollObservationDelayMs` 为毫秒；`oldestAge` 与 `drainSeconds` 为秒。`userCountsByOperation` 是每操作唯一用户数；`requestsByDatabase` 是按用户 Java 路由的请求数，查询接口实际还会读控制库。`transportRetries` 是相同事件身份的运输重试次数；`redriveEmitted` 是新增恢复事件数，`redriveSuppressed` 是抑制次数，缺原始证据时均为 `null`，不写成零。`smokeDataComplete` 只说明这一份请求与订单追踪数据完整；`smokeTechnicalPassed` 还要求实际120秒到达、k6/tracker退出码、运行前后资源身份、零 dropped、资源预算、时钟、负载期四域非增长、停写对账和本轮 SLO，缺一项即 false 并列入 `smokeFailures`。运行目录 `metadata.json.task8SampleFiles` 可列出 Task8 每个真实 `sample.json` 路径，且 `reconciliation` 和 `resourceBudgetOk` 必须来自本轮独立证据；缺失不补零。旧 `safeCapacityCandidate` 仍按完整 600 秒、三轮与六域/四域/时钟门禁计算。`fullCapacityPassed=false` 和 `releaseAuthorized=false` 始终保留。

`python3 -m unittest discover -s docs/performance -p 'test_*.py'` 运行工具回归。测试里的合成 JWT 字符串和 HTTP server 只检查分布及解析行为；真实业务结果必须另存原始请求、阶段、四域采样、停写六域对账和文件 SHA256。热点 8/32 参数扩展、006 历史基线恢复、完整 Java 容量类和正式压力矩阵在本次精简范围外。
