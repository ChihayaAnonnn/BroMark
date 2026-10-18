# PostgreSQL V2 迁移与 8 核 4 GiB 复测（2026-10-18）

已实现并实际运行当前 V2 抽奖、兑换、活动查询和结果查询的 PostgreSQL 后端。通过真实 HTTP/k6 复测，100 并发成功 QPS 达 2425，600 并发为 1881；300 并发回落到 1316，因此这轮结果不能表述为全面提升，也不能作为稳定端到端容量。

## 范围与实现

- 保留 MySQL 配置，新增 `application-v2-pg.yml` 和真实集成测试 profile `v2-pg-it`。旧 V1/MyBatis 没有迁移。切换方式是选择 PostgreSQL profile 并提供三库连接环境变量，不是把原 MySQL 数据卷转成 PG。
- PostgreSQL 17.11、PGJDBC 42.7.11；新独立容器、卷、三逻辑库，各库 74 张 V2 表及四分表结构。DDL：`docs/dev-ops/postgresql/v2-schema.sql`，检查脚本同目录 `v2-schema-check.sql`，真实执行两遍验证幂等。
- 在 `PostgresSql` / `PostgresConnection` 集中适配已知 V2 SQL：UPSERT、重复请求、JSONB、bytea、时间函数及 JDBC 参数/结果。只在 PG 路由包装连接，原 Hikari 池继续负责预算、指标和生命周期；SQL 翻译缓存严格限制 1024 条。
- 重复 inbox/库存插入通过 `ON CONFLICT DO NOTHING` 与既有业务身份校验继续事务，避免 PG 唯一键异常使整个事务失效。UTC 同时覆盖会话、JDBC Calendar 和 Timestamp 参数读写；使用非 UTC JVM + 服务端时间/typed LocalDateTime 验证，避免读写偏移互相抵消导致假通过。
- 原网络超时保护只支持 MySQL unwrap，首次 HTTP 启动因此失败；已添加 PG 原生连接超时校验并通过真实接受/拒绝用例。失败日志和修复前产物保留在证据目录。PG `connectTimeout` 以秒配置，转换成毫秒后与任务租约预算比较，见 [PGJDBC 文档](https://jdbc.postgresql.org/documentation/use/)。语句内统一时间采用 `statement_timestamp`，见 [PostgreSQL 时间函数](https://www.postgresql.org/docs/17/functions-datetime.html)。
- `docs/performance/postgres_v2_backend.py` 提供 PG 种子/原生查询支持，要求 `V2_PG_EVIDENCE_DIR` 位于 Git 外部，校验自建容器身份和本次库名前缀。

## 条件与统计口径

应用单实例 Docker 配额 **8 CPU、4 GiB**，JVM `-Xmx2g`，三连接池各 64。Docker VM 总共 10 核、约 8 GB，应用、PG、Redis/Rabbit 和 k6 共享 VM；数据库不计入应用配额，PG 容器未单独限 CPU/内存。128 MB shared_buffers、max_connections=300，fsync / synchronous_commit / full_page_writes 全部开启。

200 用户；查询/结果/兑换/抽奖为 7:1:1:1；预置 200 个订单全部交付后，1 VU 暖机 20 秒，5/100/300/600 VU 各 45 秒。闭环无 sleep、无固定 RPS，`load.js` 与上次 MySQL Docker 直连实验哈希相同。每阶段 QPS 使用 k6 实际统计窗口，包含末尾等待，不机械除以 45。

入口 RPS/in-flight 与数据库分类许可人为限制沿用关闭设置，但连接池、请求/SQL deadline、后台调度容量、认证和业务不变量保留。旧 test-only 描述 JSON 残留“2 CPU”文字，不代表实际运行配额；本次 Docker inspect、cgroup 和只读产物挂载证明 8 CPU/4 GiB。

结果查询始终读取预置且已交付的 `seed.resultOrders[user]`，不轮询本轮新建订单；其成功率/P99 不能代表本轮新订单完成路径。

成功 QPS = HTTP 2xx 且业务非 REJECTED；写成功 QPS = draw + exchange 的同口径总和。**202 表示接收，不能视为交付**。HTTP 503/超时可能发生在事务提交结果不明确时，不能全部算作业务拒绝。

## 相同负载参考对比

基线：`BroMark-8c4g-bottleneck-ab-20261018/candidate-analysis.json`（MySQL Docker 直连）。P99 为成功请求端到端毫秒。

| VU | MySQL 成功 QPS | PG 成功 QPS | 变化 | MySQL P99 ms | PG P99 ms | PG 写成功 QPS | PG 写成功 P99 ms |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 5 | 777.01 | 1618.56 | +108.3% | 40.76 | 16.98 | 323.48 | 26.84 |
| 100 | 1639.71 | 2425.09 | +47.9% | 431.96 | 259.47 | 484.65 | 324.97 |
| 300 | 1675.81 | 1315.89 | -21.5% | 1121.47 | 1621.72 | 260.14 | 2781.97 |
| 600 | 1725.66 | 1881.02 | +9.0% | 1416.81 | 1299.25 | 375.73 | 1738.45 |

| VU | PG HTTP503 | PG HTTP429 | PG 无响应/超时 status0 | 技术失败占比 | 所有请求 P99 ms |
|---:|---:|---:|---:|---:|---:|
| 5 | 13 | 0 | 0 | 0.018% | 16.98 |
| 100 | 19 | 0 | 0 | 0.017% | 259.46 |
| 300 | 200 | 4 | 38 | 0.385% | 1884.09 |
| 600 | 28 | 0 | 88 | 0.135% | 1350.57 |

429 是现有 BudgetExceededException 的 HTTP 映射，可包含 deadline，不能直接认作固定 RPS 限流。300 VU 的 4 次 429 根因未逐条证明；本轮没有业务 REJECTED。正式四阶段共 331707 次请求。

## CPU、内存与积压

CPU 为采样窗口 cgroup usage 增量折算核数；内存为 cgroup memory.current 采样峰值，非 RSS。PG/k6 与应用不是完全相同采样窗口。资源窗口包含部分阶段尾部，具体时间在原始样本。

| VU | 应用平均核数 /8 | 应用内存 MiB | PG 平均核数 | k6 平均核数 | Hikari 获取均值 ms | 池超时增量 | 阶段末采样订单积压 |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 5 | 1.53 | 663.5 | 1.16 | 0.55 | 0.001 | 0 | 6110 |
| 100 | 3.08 | 861.7 | 2.24 | 1.35 | 0.172 | 0 | 17172 |
| 300 | 3.11 | 919.4 | 3.06 | 1.56 | 11.733 | 100 | 22648 |
| 600 | 3.02 | 1081.2 | 2.39 | 1.48 | 8.394 | 0 | 30850 |

所有阶段无 OOM、无 Full GC、无 cgroup quota throttling。平均约 3 核不等于瞬时从未用满；短窗口 CPU 高峰和 GC 暂停保留在 analysis.json。600 VU 最长 GC 暂停约 273.1 ms，不能仅凭低平均 CPU 排除调度/GC 对尾延迟的影响。

负载结束最多排空 30 秒，随后停止应用再对账。统计含预置和暖机：

| 指标 | MySQL 直连参考 | PostgreSQL |
|---|---:|---:|
| 总抽奖订单 | 26019 | 34390 |
| DELIVERED | 1605 | 3031 |
| 未完成订单 | 24414 | 31359 |
| 资产差异 | 0 | 0 |
| 未解决人工复核 | 0 | 0 |

PostgreSQL 最终状态：`{"RESULT_RECORDED": 2023, "DELIVERED": 3031, "COMMITTED": 1086, "CANDIDATE_FIXED": 6739, "ACCEPTED": 21511}`；待处理任务 52948。对账覆盖 CREDIT/QUOTA/STOCK/DELIVERY/PAYMENT/SIGNIN，全部资产差异为 0，但 `passed=false`，原因是仍有未完成订单/任务。小规模 HTTP 冒烟 982 个订单全部 DELIVERED，证明完整业务链可走通；高负载交付能力仍不足。

## 当前瓶颈证据与限制

600 VU：`user_credit_account ... FOR UPDATE` 两库服务端平均约 17.17/17.04 ms，单一 `award_stock ... FOR UPDATE` 约 41.85 ms；采样出现 WALWrite、WalSync 和 transactionid/tuple 等等待。WAL 刷盘计数增量 4416、总刷盘时间 46443.07 ms，约 10.52 ms/次；这是数据库全局计数，不能当接口耗时或端到端等待占比。

300 VU 附近发生一次 requested checkpoint、刷盘统计增长，并出现连接池 100 次获取超时；时间相关性不足以证明 checkpoint 是该阶段吞吐回落的唯一原因。SQL 统计采样前后 pg_stat_statements 无淘汰/重置，原始数据及 SQL 差分见 `sql-analysis.json`。SQL 执行均值不是请求 P99；等待会话采样数不是累计等待时长。

已观察到数据库持久化与热点行竞争，后台推进仍积压。下一步应单独测 stock 热点串行化、Relay/worker 各阶段服务率与 WAL 存储延迟，固定同一阶段独立重复测试后再优化，不能把“换 PG”当作已消除瓶颈。

这是前后两次参考对比，**不是只替换引擎的严格 A/B**：PG 新卷/新缓存，MySQL 历史数据卷；驱动、SQL 适配、JSON 存储、事务默认隔离和持久化实现不同；三库仍在单一物理数据库实例，依赖共享 10 核 VM。阶段连续执行、积压和数据库缓存随时间变化，300→600 非单调。不可用本轮宣称 PG 普遍优于 MySQL 或稳定支持 2425 QPS。

## 验证与复现

- 251 项 MySQL V2 回归 + 28 项原 router 审计 + 16 项 PG 适配单测 + 13 项真实 PG 集成，共 **308 项**，最终记录均通过。MySQL 首次全套出现一次既有 Outbox 并发死锁；相同 repository 字节码的定向与全量重跑通过，首轮失败日志保留。
- PG 实测覆盖三库分表、额度并发扣减、幂等/回滚、inbox 重复后事务继续、JSONB Outbox claim/complete/hash、bytea、UTC及网络 deadline。不是对所有旧 V1/所有故障模型的迁移认证。
- 证据目录：`/Users/vivix/Desktop/workspace/BroMark-postgresql-8c4g-20261018`，含 run_postgres.py、pg_tools.py、原始 requests.jsonl、k6 汇总、cgroup/Prometheus、JVM/GC、SQL 前后快照、停写对账、运行时目标证明、失败尝试、源码/产物 SHA。密钥和令牌只在 Git 外 private 目录，公开哈希清单不包含它们。
- 复现需独立 PG、匹配的 Rabbit/Redis 依赖、Docker Java25 Maven 依赖缓存及私有测试配置；run_postgres.py 为本机绑定的实验脚本，库名、容器身份和证据目录必须新建，不能直接复用本轮数据库。应用使用 `v2-it,v2-pg-it` profile；生产默认配置没有切换。
- 本次新增 PG 已停止、数据卷保留；负载/应用临时容器清理，原有依赖恢复初始停止状态。`verify_evidence.py` 核验配额/哈希/308 项结果、原始负载一致、对账与旧证据完整性；`SHA256SUMS.json` 冻结结果。
