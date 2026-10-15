# Router内嵌模块验收

2026-10-15：按用户要求，将此前已验证的Router正式代码、测试与资源整合到BroMark/bm-router。保留原内部Maven坐标及Java包名；父POM统一管理Java25、Boot与MyBatis版本。原平级项目保留为历史快照，当前构建无需访问该目录或先执行install。

## 实际验证

- 整合前mvn -pl bm-router -am validate失败：Reactor未注册该模块。
- 整合后verify-java25.sh从BroMark根目录以单次clean package构建八个Reactor项目（父项目加七模块）。Docker仅挂载BroMark目录，不挂载平级源码。
- Router28项、应用17项均实际执行，failures/errors/skipped均0。已修复旧脚本漏选V2FoundationWiringTest的筛选问题；该类两项验收本次一并执行。
- 21个迁入源码/测试/资源文件逐字节一致；应用包内Router JAR的SHA256与本次bm-router产物一致。
- 七模块共364个class全部major69；应用镜像Java25.0.4.1，真实启动health UP，实际DAO/策略/Redis armory调用成功。
- 当前测试及产物摘要见[results.json](results.json)。全量旧手工演示测试的无限等待限制仍适用，不能将针对性45项通过说成全量旧业务回归通过。

## 使用

在BroMark根目录执行mvn -pl bm-app -am -DskipTests package即可自动构建Router与应用。
完整针对性验收使用docs/operations/verify-java25.sh；先按docs/dev-ops/docker-compose-v2-test.yml启动隔离服务并提供V2_MYSQL_PASSWORD、V2_RABBIT_PASSWORD。
本次未改变路由算法、事务、SQL支持或安全行为；没有修改平级历史仓库、生产数据库或部署。
