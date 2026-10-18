# 单应用2核4GiB资源证据验证

## 结论

已找到CPU持续接近两核配额、频繁节流的证据，出现在300和600 VU过载档；4 GiB内存上限未触发。总响应吞吐升高主要伴随大量429，成功响应约223～227 QPS，数据库许可排队也仍然存在。HTTP成功包含异步受理，不能解释为稳定履约容量。

## 环境与证据口径

单应用CPU配额2核、内存限制4 GiB、JVM最大堆2 GiB；新鲜resproof1数据集、200用户。TRADE=2、WORKER=1、各逻辑连接池8、入口限额保持前轮配置。混合query/result/exchange/draw=7/1/1/1，constant-vus无sleep，每档45秒、20秒预热排除。各档连续、积压继承。

MySQL、Redis、Rabbit和k6在应用配额外，宿主共享其他项目。诊断增加了JFR profile、jcmd子进程及每秒docker exec开销；cgroup计量包括应用容器中的诊断命令。新旧差异不能当作优化收益。CPU百分比以2核配额为100%。配额由Docker inspect和cpu.max同时确认。内存峰值由内核memory.peak记录，阶段采样最大值单独列出。

本轮CPU最高阶段均值为两核配额的93.0%，已出现持续接近配额并伴频繁节流的阶段。全采集期内核内存峰值1022.7 MiB，占4 GiB的25.0%；最终内存事件计数为 `{'low': 0, 'high': 0, 'max': 0, 'oom': 0, 'oom_kill': 0, 'oom_group_kill': 0, 'sock_throttled': 0}`。

业务提交 `1aadc89004de11c0f133f16950e9cf7f55531234`；JAR SHA256 `afe6efdabb5ff056aaafeda189489dd9bf84db6bbc5c1350670dff2cc5aec46e`。前轮冻结原始文件SHA已重新核对，共532个文件，未修改前轮证据。

## 新轮QPS与CPU

| VU | 请求数 | 总QPS | 成功QPS | 成功P99(ms) | CPU均值(配额%) | 至少10秒窗口CPU范围(配额%) | 达到90%总秒数 / 最长连续秒数 | 节流周期/总周期 | 节流累计计数(ms) |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 5 | 17369 | 385.6 | 216.8 | 112.1 | 53.3 | 33.5～62.9 | 0.0 / 0.0 | 16/471 | 836.3 |
| 100 | 14856 | 322.8 | 191.2 | 4153.4 | 56.4 | 44.3～66.9 | 1.0 / 1.0 | 26/461 | 1299.2 |
| 300 | 49386 | 1061.7 | 222.7 | 4589.4 | 93.0 | 89.9～99.3 | 40.1 / 28.0 | 404/491 | 35133.7 |
| 600 | 50579 | 1089.6 | 227.3 | 4905.9 | 92.3 | 90.4～98.9 | 36.1 / 17.0 | 321/481 | 22485.3 |

CPU用usage_usec增量/单调时间/2核计算；节流周期来自nr_throttled，时长来自throttled_usec增量。节流累计计数不等于HTTP附加延迟，不以它计算CPU损失百分比；多线程计量也不保证能直接和墙钟相除。个别约1秒CPU增量样本超过100%，原值保留在resource-analysis.json；确切原因未进一步归因，不能据此断言CPU配额失效。为降低短区间计量/采样对齐影响，表格另给出非重叠、至少10秒窗口的CPU均值范围；未满10秒尾段不进入该范围。持续饱和的操作性判断采用采样区间配额使用至少90%，结合节流趋势；一分钟内的均值或约1秒区间都不能排除更短峰值。

## 新轮内存与GC

| VU | 阶段采样最大内存(MiB) | 截至阶段内核生命周期峰值(MiB) | 采样堆使用最大(MiB) | GC后堆最大(MiB) | GC最大暂停(ms) | GC暂停占阶段墙钟% | Full GC次数 |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 5 | 626.8 | 656.9 | 108.9 | 59 | 10.40 | 0.97 | 0 |
| 100 | 730.9 | 754.4 | 139.7 | 74 | 24.38 | 1.26 | 0 |
| 300 | 847.5 | 862.0 | 212.6 | 108 | 70.98 | 1.68 | 0 |
| 600 | 896.4 | 899.5 | 255.1 | 118 | 48.40 | 1.23 | 0 |

内存限制是4096 MiB，堆上限2048 MiB。memory.peak是进程启动以来的内核高水位，包含启动/预置订单/诊断，不应解释为本档单独产生的峰值。memory.events完整计数保存在resource-analysis.json。停压后离线JFR分析工具初次使用512 MiB分析容器时堆不足，失败日志保留为jfr-analysis-tool-initial-failure.txt，重试使用独立2 GiB分析容器；这发生在应用停止之后，不是被测应用OOM。CPU与内存pressure文件在本机容器中返回空内容，不能把它当成PSI=0。没有长时内存稳定性或泄漏结论。

## 许可与连接池

| VU | TRADE许可平均等待(ms) | TRADE拒绝计数增量 | 连接获取均值(ms) | 连接等待者采样最大数(全池合计) | 连接获取超时增量 |
|---:|---:|---:|---:|---:|---:|
| 5 | 9.18 | 0 | 0.0020 | 0 | 0 |
| 100 | 439.84 | 195 | 0.0041 | 0 | 0 |
| 300 | 792.40 | 469 | 0.0185 | 0 | 0 |
| 600 | 800.16 | 455 | 0.0133 | 0 | 0 |

许可等待发生于事务/连接获取之前，是每次TRADE事务尝试的指标，不是每个HTTP请求的独占耗时。Hikari取数为各池sum/count增量汇总，采样pending=0不代表所有瞬间都无等待。429由API合并不同预算拒绝，不能全部归为Redis限流。

JFR的container-cpu-throttling视图在完整记录范围（包含启动、预置、预热与排空，与单档窗口不同）中记录799个节流周期、累计约61秒节流计数，交叉支持存在CPU配额节流。GC表以文本GC日志和阶段窗口为准，JFR GC事件汇总口径另存。JFR hot-methods覆盖整个记录，不能当作600 VU档独有CPU热点。

## 线程快照交叉验证

线程快照是单个时间点证据，不是时间占比。

| 快照 | 含许可获取栈线程 | 其中parking线程 | HTTP许可获取栈线程 | 消费许可获取栈线程 |
|---|---:|---:|---:|---:|
| phase-diagnostics/vus_100-Thread.print.txt | 112 | 112 | 96 | 15 |
| phase-diagnostics/vus_300-Thread.print.txt | 206 | 177 | 192 | 14 |
| phase-diagnostics/vus_5-Thread.print.txt | 16 | 16 | 2 | 14 |
| phase-diagnostics/vus_600-Thread.print.txt | 208 | 207 | 193 | 13 |

表中含许可获取栈不等于全部处于等待，parking列单独统计TIMED_WAITING (parking)。栈定位到 `Semaphore.tryAcquire → BoundedDatabaseBudget.acquire:58 → RoutedTransactionRunner`，写请求继续关联ExchangeService/DrawService，消费请求关联ConsumerUnitOfWork。许可排队与CPU饱和可同时存在，不能由CPU饱和推断所有延迟来自CPU。

## 前轮原始证据复核

| VU | CPU均值(配额%) | 节流周期/总周期 | 节流累计计数(ms) | GC最大暂停(ms) | GC暂停占阶段墙钟% |
|---:|---:|---:|---:|---:|---:|
| 1 | 30.6 | 0/581 | 0.0 | 11.29 | 0.87 |
| 5 | 40.7 | 6/635 | 72.2 | 14.03 | 1.22 |
| 20 | 39.1 | 5/637 | 50.4 | 15.11 | 1.30 |
| 50 | 37.8 | 3/639 | 399.9 | 15.82 | 1.13 |
| 100 | 40.1 | 5/597 | 75.3 | 21.66 | 1.11 |

前轮各档memory.events的max/oom/oom_kill累计均为0。此前仅说“没有证据用满”不够完整：已有短暂配额节流证据，但未见持续饱和证据。本报告保留这一区别。

## 失败、积压和清理

- 5 VU：HTTP状态分布 `{'200': 8031, '202': 1735, '429': 7599, '503': 4}`。
- 100 VU：HTTP状态分布 `{'200': 7436, '202': 1364, '429': 6055, '503': 1}`。
- 300 VU：HTTP状态分布 `{'0': 1, '200': 9876, '202': 483, '429': 39025, '503': 1}`。
- 600 VU：HTTP状态分布 `{'0': 34, '200': 10052, '202': 498, '429': 39995}`。

停压仅观察30秒后停止应用，停写核查完整结果保存在reconciliation.json；未排空时不认定容量验证通过。

```json
{
  "differences": {
    "CREDIT": 0,
    "QUOTA": 0,
    "STOCK": 0,
    "DELIVERY": 0,
    "PAYMENT": 0,
    "SIGNIN": 0
  },
  "unresolvedReviews": 0,
  "unfinishedOrders": 4067,
  "pendingTasks": 13,
  "passed": false
}
```

## 复核入口

- 原始目录：`/Users/vivix/Desktop/workspace/BroMark-resource-validation-20261017`。
- `resource-budget.json`与`resources/*.json`：配额、每秒cpu.stat/memory.peak/events及应用ID。
- `resources/*.prom`与`gc-1.log`：JVM、许可、连接池指标及GC逐事件记录。
- `private/resource.jfr`（受保护原始记录，排除公开清单）与`jfr-summary.txt`：JFR记录和可读性校验；`jvm/`、`phase-diagnostics/`：各阶段线程/堆快照及压力源CPU记录。
- `vus_*/requests.jsonl`、`k6-summary.json`与`results.json`：原始请求和QPS统计。
- `analyze_resources.py`可独立复算`resource-analysis.json`；`closure-verified.json`核对资源清理。
- `SHA256SUMS.json`覆盖新证据与本次计划/报告，排除private及清单自身。旧证据目录只读。
