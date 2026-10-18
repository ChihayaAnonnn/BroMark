# BroMark 本地容量实验：发现 ID 号段边界缺陷

本轮完成固定资源下的对照、阶梯测试与三轮长时复验。发现并用 JFR 与独立探针验证了 ID 号段耗尽缺陷；业务代码未修改。结果可用于性能分析，不能写成已实现高并发容量或零错误。

## 环境与口径

- Apple M4、宿主 16 GiB；Docker 10 CPU、约 8 GiB，与其他本地服务共享宿主。
- 两应用各 1 CPU / 512 MiB / Xmx320m；HTTP 全部指向实例 1，两实例共同处理异步工作。不能称为双实例 HTTP 均衡吞吐。
- 基准提交 `1aadc89`，Java 25、v2-it 集成配置与测试专用启动器；实际业务类与已构建 JAR 逐项核对一致（560 类及 16 个 Router 类）。JAR SHA256 `afe6efdabb5ff056aaafeda189489dd9bf84db6bbc5c1350670dff2cc5aec46e`。
- 三个专属逻辑库共用一个MySQL实例，不能当作多台数据库扩容实测。单活动、单积分奖、100%命中积分奖，不能外推复杂策略或支付吞吐。
- 专属三库，200 个合成用户，期初每用户 100 万积分和额度，100 万库存。200 个身份不等于 200 个同时在途请求；100 个预分配 k6 VU 也不能直接称为 100 并发。
- mixed 配比 query/result/exchange/draw=7/1/1/1，因此 10 RPS mixed 约等于每秒 1 次抽奖、1 次兑换及 8 次查询。
- 目标：查询 P99≤200ms，受理 P99≤500ms，draw 同库交付 P99≤5秒，技术失败率<0.1%，持续负载无积压增长，停写对账无差异。阶梯与稳定候选额外采用遇到任意非 2xx 即停止升档的保守规则。

## 完整测量窗口

下面 P99 单位均为毫秒，HTTP 分位数包含原始失败响应；失败次数独立列示。交付只统计原始响应具有订单 ID 的 draw，并由数据库 ACCEPTED→DELIVERED 时间差计算，不把失败后重放算成成功请求。

| 窗口 | RPS×秒 | 原请求数 | 非2xx | query P99 | result P99 | draw受理 P99 | exchange P99 | draw交付 P99 |
|---|---:|---:|---|---:|---:|---:|---:|---:|
| baseline_full_10 | 10×120 | 1200 | 0 | 7.04 | 7.98 | 27.13 | 41.25 | 215.43 |
| light_10 | 10×120 | 1201 | 0 | 6.02 | 13.12 | 20.14 | 45.77 | 154.92 |
| query_20 | 20×180 | 3601 | 0 | 9.03 | — | — | — | — |
| query_50 | 50×180 | 9001 | 0 | 6.50 | — | — | — | — |
| query_100 | 100×180 | 18001 | {"429": 6} | 5.02 | — | — | — | — |
| draw_5 | 5×180 | 901 | {"503": 2} | — | — | 35.00 | — | 271.37 |
| mixed_20 | 20×180 | 3601 | {"503": 1} | 3.63 | 6.11 | 23.27 | 26.68 | 150.75 |
| stable_10_r1 | 10×600 | 6001 | {"503": 2} | 12.30 | 18.58 | 126.65 | 118.74 | 551.15 |
| stable_10_r2 | 10×600 | 6001 | {"503": 1} | 6.70 | 11.60 | 48.83 | 48.55 | 201.63 |
| stable_10_r3 | 10×600 | 6001 | {"503": 2} | 6.11 | 10.76 | 44.91 | 48.60 | 204.79 |

## 三轮长时复验

最高无错误短测 mixed 候选为 10 RPS，按该档执行 600 秒×3轮，共 18003 次原请求，其中技术失败 5 次，合并技术失败率 0.0278%。其中写请求 3600 次、技术失败 5 次，写请求单独失败率 0.1389%。两种分母分别报告，不用读请求稀释关键写入错误。该单独分母是补充分析，不事后改写预声明混合负载门禁。不将三轮合并百分位数与各轮 P99 混用。

- stable_10_r1：原请求 6001；技术失败 2（0.0333%）；原始受理 draw 598，已交付 598；实际 k6 iteration rate 10.0015/s。
- stable_10_r2：原请求 6001；技术失败 1（0.0167%）；原始受理 draw 600，已交付 600；实际 k6 iteration rate 10.0014/s。
- stable_10_r3：原请求 6001；技术失败 2（0.0333%）；原始受理 draw 599，已交付 599；实际 k6 iteration rate 10.0016/s。
- 阶梯测试后停写六域对账：differences=0，unfinishedOrders=0，pendingTasks=0，unresolvedReviews=0，passed=True。支付/签到没有本轮负载，不作为其容量或非零正确性证据。
- 三轮复验后停写六域对账：differences=0，unfinishedOrders=0，pendingTasks=0，unresolvedReviews=0，passed=True。支付/签到没有本轮负载，不作为其容量或非零正确性证据。

三轮均满足预声明的混合请求级延迟、总体技术失败率及采样积压目标；每轮仍有503，ID分配缺陷尚未修复。写请求单独失败率按独立分母呈现。

可靠性机制覆盖持久化受理后的恢复，不代表每个到达的HTTP请求都能受理。失败响应与最终业务一致性是两项独立结论。

各窗口负载期四域样本的峰值、首末三分之一均值、缺测数量在 `derived-results.json`。约10秒粒度的采样只支持该分辨率下的趋势判断，不证明毫秒级瞬态不存在；停写排空与负载期趋势分别记录。

## 请求身份复核

全部正式窗口共 55509 个原请求，requestId 全局唯一。停写后单独核查 5701 个正式写请求：5693 个成功响应都有匹配数据库回执，成功回执缺失与身份不符均为0；8个失败写请求在停写时均无持久化请求回执。未重放任何正式失败请求。详见 [receipt-audit.json](/Users/vivix/Desktop/workspace/BroMark-practice-capacity-20261017/receipt-audit.json)。

## 确认的缺陷与未确认的归因

- mixed_20 原请求 `pcmixed20:load:3238` 为 exchange，503、无 orderId、5.83ms；响应 epoch 1792219434.655。JFR 在同一时刻捕获 `BlockOrderIdGenerator.next → RouteCodec.newEventId → ExchangeService.exchange` 的 `ID block empty; prefetch outside transaction`。
- `prefetch()` 仅在整个共享号段已空时申请下一段；当只余一个 ID 时直接返回，随后业务连续取订单 ID 与事件 ID，第二次可能失败。不同线程之间也没有为一次操作预留完整预算。`IdBoundaryProbe.java` 使用实际已编译生成器与内存号段存储，确定性复现“预取返回→最后一个订单 ID→事件 ID 耗尽”。该探针不冒充真实 DB 负载测试。
- draw_5 两个 503 均无订单 ID；相邻数据库订单号分别跨越 3998→5001、5997→6001，与号段边界吻合。但没有同请求异常栈，因此仅认定相关线索，不把两笔强行归为同一已确认异常。
- 查询 100 RPS 出现 429，而配置 query-rps=100；保护边界与现象一致，但没有单变量调高限额对照，不把它认定为数据库物理容量上限或唯一触发原因。
- 历史 10 RPS 的秒级 JDBC 提交长尾在本轮两种观测方式的短测中均未复现。本轮为200用户、历史为20用户，且运行时间和宿主负载不同，不能声称完成了历史问题的完全同条件复现，更不能将历史 JDBC 超时归因于本次 ID 异常。
- 未修改代码来“凑绿”。下一轮修复应在业务事务外原子预留本次操作所需 ID，并补充号段边界与并发回归；不能简单在业务事务内临时申请号段。

## 工具与证据限制

- k6 summary 未输出 dropped_iterations，全部保留 null/未知；实际 iteration 数、唯一请求数和 k6 实测速率单独核对，不补零，不声称已直接观测到 dropped=0。
- 第二次 warmup 复用了首次 requestId，写操作实际为幂等重放；请求行 phase 也被外部 helper 固定为 formal。两次预热按独立目录和请求前缀排除，正式三轮 ID 互不重复；首轮长测包含新实例冷写入影响。原始字节不改。
- draw_5/mixed_20 失败诊断期间额外启用了异常 JFR，时间与原始录制文件均保留；三轮正式长测未启用该异常录制。
- 原方式额外发起128次HTTP查单（查单错误0），这些不进入主请求分母。原方式包含每约3秒全量采样及在线 HTTP 查单；轻量方式每约10秒一次 SQL union 与 Rabbit CLI，结束后导出 DB 阶段。两项观测方式一起变化且顺序执行，不能把215ms→155ms包装成单变量优化效果。

## 结论与简历边界

查询 50 RPS 的3分钟短测无错误；查询100 RPS有预算拒绝。写入链路存在已确认的ID号段缺陷，原版本的长时数据必须连同失败率一起呈现。本轮未测得系统物理极限，也未验证高并发抽奖容量；不写“支持千级QPS”“零丢单零错误”或“优化提升N倍”。可以写已经实现的可靠性机制，以及“使用k6、JFR与数据库对账定位号段切换异常”的真实工作。

## 可复核证据

- [阶梯与诊断原始目录](/Users/vivix/Desktop/workspace/BroMark-practice-capacity-20261017)：脚本、请求、k6、采样、JFR、确定性探针、构建字节及初次对账。
- [稳定性复验原始目录](/Users/vivix/Desktop/workspace/BroMark-practice-stability-20261017)：三轮请求、DB阶段、采样、资源记录、最终对账与清理。
- [派生结果](/Users/vivix/Desktop/workspace/BroMark-practice-capacity-20261017/derived-results.json)；[已确认失败](/Users/vivix/Desktop/workspace/BroMark-practice-capacity-20261017/confirmed-failure.json)；[Java边界探针输出](/Users/vivix/Desktop/workspace/BroMark-practice-capacity-20261017/id-boundary-probe.log)。
- private/ 目录权限0700，RSA私钥及JWT文件权限0600；私钥、JWT与seed受该目录保护，不提交、不纳入公开哈希清单。拥有的临时应用/Rabbit已清理；Rabbit卷与专属三库保留。共享服务不改动。

校验清单：[阶梯与诊断 SHA256SUMS.json](/Users/vivix/Desktop/workspace/BroMark-practice-capacity-20261017/SHA256SUMS.json)；[稳定性复验 SHA256SUMS.json](/Users/vivix/Desktop/workspace/BroMark-practice-stability-20261017/SHA256SUMS.json)。各清单排除密钥目录，记录公开文件及本报告的 SHA256。
