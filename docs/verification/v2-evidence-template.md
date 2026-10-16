# 单次验收证据

- run_id / UTC开始结束 / 代码SHA / 工作树状态 / 镜像digest / JDK
- 命令（脱敏）/ 退出码 / 原始输出路径及sha256 / Surefire各类tests,failures,errors,skipped
- 容器/机器CPU内存 / MySQL持久化 / Rabbit队列类型与副本 / Redis布局
- 故障条目 / 注入类型（异常、连接断开、进程SIGKILL不得混称）/ 注入前后SQL
- 原bizId/事件ID（测试身份）/ 接收时间 / 最终业务与恢复状态 / 原回执
- CREDIT,QUOTA,STOCK,DELIVERY,PAYMENT,SIGNIN 各已检查范围、数量与差异
- 已受理未终态清单 / 持久核查case / 未执行或未通过项
- 压测：sent/received/accepted/rejected/completed、网络未知、超载/业务拒绝、三个延迟分布、积压与排空

缺依赖、零测试、skip、缺scope、无原始输出均标记未通过。git只存合成摘要和可复现脚本，原始日志/JWT/用户数据留在忽略目录或/tmp。
