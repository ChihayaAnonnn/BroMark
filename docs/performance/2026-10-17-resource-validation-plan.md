# 单应用2核4GiB资源使用验证

目的：验证混合闭环负载下是否存在持续CPU配额饱和、内存上限触发或GC压力，补齐前轮资源证据，不改变业务代码或限流/数据库许可配置。

## 范围与判据

- 同一业务提交与JAR，单应用2 CPU / 4 GiB、Xmx2g；MySQL/Redis/Rabbit/k6在配额外，共享宿主。新鲜resproof1数据集，200合成用户。
- 复核前轮冻结原始证据的SHA；逐档复算CPU使用增量、节流次数/时长、memory.events、GC、数据库许可和连接池等待。
- 新轮constant-vus、无sleep：5/100/300/600 VU，各45秒；20秒预热排除。请求混合query/result/exchange/draw=7/1/1/1，不以固定RPS控制压力。
- CPU饱和操作性判据：窗口使用接近2核，报告采样区间达到配额90%的总时长、最长连续时长，并结合节流计数。短暂节流只说明局部时间触及配额，不自动认定它是主要吞吐瓶颈。throttled_usec不能直接当成HTTP额外延迟或CPU损失百分比。
- 内存以cgroup memory.max/current/peak/events为准；报告内核生命周期峰值与阶段采样峰值，检查max/oom/oom_kill。Xmx2g是堆上限，不是4GiB容器总内存。
- 每秒采集cgroup CPU/压力/内存与已有本地JVM指标；GC日志逐事件解析；额外JFR profile、jcmd heap/thread/flags用于保留诊断证据。诊断存在开销，不能将新旧吞吐差值当业务优化收益。
- 同步报告总QPS、成功QPS、429/503/无响应、成功P99、TRADE许可等待和Hikari获取等待；HTTP 202只是受理，不等于履约完成。

## 边界与收尾

各档连续、积压继承；不构成单变量容量对照，也不能排除更长运行或其他负载的内存增长。压力源在应用配额外，宿主共享其他服务；记录这些限制，不用采样均值证明所有瞬间都未饱和。

应用退出/OOM时停止发压；保留失败和原始记录。停压仅观察30秒，停止应用后做核查，未完成订单如实记录，不声称稳定容量通过。保留新数据库和Rabbit卷用于恢复；不启动或删除前轮积压；恢复六个依赖的初始运行状态。新证据目录：`/Users/vivix/Desktop/workspace/BroMark-resource-validation-20261017`。不提交密钥/JWT，不自动提交或推送。
