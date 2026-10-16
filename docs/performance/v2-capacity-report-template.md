# V2 容量实测报告

结论先写：候选安全速率/未达到目标/尚未执行。禁止只凭HTTP 202或拒绝后的低延迟宣称容量。

| 项目 | 实测 |
|---|---|
| 代码SHA、镜像digest、JDK、k6版本、迁移hash | |
| 机器CPU/内存、压测端/服务端是否独立、其他负载 | |
| 实例数、每物理DB总连接、各类许可、预留 | |
| Worker/prefetch、MySQL fsync/binlog、副本、Rabbit队列持久化/副本 | |
| 数据规模、审计期初、SKU不限库存语义、有限库存名额/兜底比例 | |
| rate/duration/warmup/repeats；实际请求分布 | |
| sent/received/accepted/rejected/completed/pending/unknown | |
| 业务拒绝/过载拒绝、dropped_iterations、压测端CPU瓶颈 | |
| 查询P50/P95/P99、受理P50/P95/P99、服务端完成P50/P95/P99 | |
| 时钟偏差与不确定度、轮询请求数及观测延迟 | |
| oldestAge/backlog趋势/drainSeconds；恢复处理速率与持续新增 | |
| 六域checkedScopes/差异、未终态与持久核查单 | |
| 三次重复的波动、资源图、瓶颈及修订依据 | |
| 原始日志/JSONL/指标路径及sha256 | |

只有全部条件满足才能给候选安全容量。缺少长期/重复/独立压测机/终态时间/六域核验时写未通过。旧语义基线不能量化优化收益。库存耗尽后单独切段，不能把无限兜底当有限库存确认能力。
