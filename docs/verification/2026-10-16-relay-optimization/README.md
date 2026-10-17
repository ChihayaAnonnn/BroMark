# Relay 优化小规模运行验收

2026-10-17 对 `caf2484` 执行最终 bounded Relay＋guarded recovery 的一次真实业务 smoke。裁定：`developmentValidationPassed=true`、`smokePassed=false`、`fullCapacityPassed=false`、`releaseAuthorized=false`。本轮没有升压、改变资源配额或放宽 SLO；Phase6 继续 NO-GO。数值、原请求补救和 raw 索引见 [容量报告](capacity-report.md)。

Task 8 同一 Java 源码/JAR 的 Router/V2 回归 269/269、故障正确性与非零支付/签到已单独验证；Task 9 的 Python 66/66 与真实多 VU k6 probe 只证明工具。Task 10 首次编排在 20 用户预置完成后因仓库外临时脚本未建 sampler 父目录失败，尚未预热；失败 raw `/tmp/bromark-task10-smoke-20261017` 保留。修复临时编排后使用同一隔离三库运行一次 60 秒预热和一次 120 秒正式负载，raw `/tmp/bromark-task10-smoke-20261017-b`。两次自有容器均已删除，Rabbit 卷保留，三库账务事实未删除。

正式 10 请求/秒 uniform mixed 的入口出现 2 个 503 和 1 个无响应，查询 P99 1527.47 ms、交易入口 P99 4896.39 ms，均超过本档阈值。服务端抽奖确认 P99 11665.01 ms、交付 P99 13911.75 ms。四域采样有效，但 BROKER 负载末三分之一均值 2.18、首三分之一为 0，按现有 collector 增长门槛失败。时钟校准偏差 21.42 ms，超过预声明 10 ms；跨时钟 completion P99 因此未验证。`smokeTechnicalPassed=false` 是真实失败，不用停写后的归零替代负载期判断。

下一验证动作是先定位入口 503/无响应、长尾及负载期 Broker 积压，并在独立压测机和可信时钟校准条件下重新做新 run；之后按原 Phase6 计划完成同资源基线/消融、100/s×600 秒×3 轮及后续矩阵。任何新实验都须新库、新队列、原始请求账与停写对账；本次报告不批准发布。
