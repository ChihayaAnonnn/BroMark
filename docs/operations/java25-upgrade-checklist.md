# Java 25 升级记录

执行分支 feat/java25-foundation；目标 release=25、class major=69、Boot3.5.16、MyBatis3.0.5。
构建镜像 maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976，Maven3.9.16 / Temurin25.0.4.1+1。
运行镜像 eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64，Temurin25.0.4.1+1。
旧构建在Java25因未显式启用Lombok处理器而产生缺失构造器/方法编译错误，已记录red日志。网络下载问题不计作红灯。
保留javax.sql；未发现现有Spring Security配置或javax.validation调用。
现有自定义Executor调用点：ThreadPoolConfig、SendMessageTaskJob、AwardRepository；虚拟线程开关不会替换它们。
默认不启用全局虚拟线程，阶段0预算完成后显式有界worker启用。
Router仅本地安装，源仓库独立维护；未验证上游再分发许可，不发布。

## Jakarta迁移完整清单

- `bm-app/src/test/java/com/jay/test/domain/activity/RaffleActivityAccountQuotaServiceTest.java`
- `bm-app/src/test/java/com/jay/test/domain/activity/RaffleActivityPartakeServiceTest.java`
- `bm-app/src/test/java/com/jay/test/domain/award/AwardServiceTest.java`
- `bm-app/src/test/java/com/jay/test/domain/credit/CreditAdjustServiceTest.java`
- `bm-app/src/test/java/com/jay/test/domain/rebate/BehaviorRebateServiceTest.java`
- `bm-app/src/test/java/com/jay/test/domain/strategy/LogicChainTest.java`
- `bm-app/src/test/java/com/jay/test/domain/strategy/LogicTreeTest.java`
- `bm-app/src/test/java/com/jay/test/domain/strategy/RaffleStrategyTest.java`
- `bm-app/src/test/java/com/jay/test/domain/strategy/StrategyArmoryDispatchTest.java`
- `bm-app/src/test/java/com/jay/test/infrastructure/AwardDaoTest.java`
- `bm-app/src/test/java/com/jay/test/infrastructure/RaffleActivityAccountMonthDaoTest.java`
- `bm-app/src/test/java/com/jay/test/infrastructure/RaffleActivityCountDaoTest.java`
- `bm-app/src/test/java/com/jay/test/infrastructure/RaffleActivityDaoTest.java`
- `bm-app/src/test/java/com/jay/test/infrastructure/RaffleActivityOrderDaoTest.java`
- `bm-app/src/test/java/com/jay/test/infrastructure/StrategyRepositoryTest.java`
- `bm-app/src/test/java/com/jay/test/redis/RedisTest.java`
- `bm-app/src/test/java/com/jay/test/trigger/RaffleActivityControllerTest.java`
- `bm-app/src/test/java/com/jay/test/trigger/RaffleStrategyControllerTest.java`
- `bm-domain/src/main/java/com/jay/domain/activity/service/armory/ActivityArmory.java`
- `bm-domain/src/main/java/com/jay/domain/activity/service/product/RaffleActivitySkuProductService.java`
- `bm-domain/src/main/java/com/jay/domain/activity/service/quota/rule/impl/ActivitySkuStockActionChain.java`
- `bm-domain/src/main/java/com/jay/domain/award/service/distribute/impl/UserCreditRandomAward.java`
- `bm-domain/src/main/java/com/jay/domain/credit/service/CreditAdjustService.java`
- `bm-domain/src/main/java/com/jay/domain/rebate/service/BehaviorRebateService.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/armory/StrategyArmoryDispatch.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/armory/algorithm/AbstractAlgorithm.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/armory/algorithm/impl/OLogNAlgorithm.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/rule/chain/impl/BlackListLogicChain.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/rule/chain/impl/DefaultLogicChain.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/rule/chain/impl/RuleWeightLogicChain.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/rule/tree/impl/RuleLockLogicTreeNode.java`
- `bm-domain/src/main/java/com/jay/domain/strategy/service/rule/tree/impl/RuleStockLogicTreeNode.java`
- `bm-domain/src/main/java/com/jay/domain/task/service/TaskService.java`
- `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/ActivityRepository.java`
- `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/AwardRepository.java`
- `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/BehaviorRebateRepository.java`
- `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/CreditRepository.java`
- `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/StrategyRepository.java`
- `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/TaskRepository.java`
- `bm-infrastructure/src/main/java/com/jay/infrastructure/redis/RedissonService.java`
- `bm-trigger/src/main/java/com/jay/trigger/http/RaffleActivityController.java`
- `bm-trigger/src/main/java/com/jay/trigger/http/RaffleStrategyController.java`
- `bm-trigger/src/main/java/com/jay/trigger/job/SendMessageTaskJob.java`
- `bm-trigger/src/main/java/com/jay/trigger/job/UpdateActivitySkuStockJob.java`
- `bm-trigger/src/main/java/com/jay/trigger/job/UpdateAwardStockJob.java`
- `bm-trigger/src/main/java/com/jay/trigger/listener/ActivitySkuStockZeroCustomer.java`
- `bm-trigger/src/main/java/com/jay/trigger/listener/CreditAdjustSuccessCustomer.java`
- `bm-trigger/src/main/java/com/jay/trigger/listener/RebateMessageCustomer.java`
- `bm-trigger/src/main/java/com/jay/trigger/listener/SendAwardCustomer.java`

## 全部分表SQL

对应独立Router仓库 src/test/resources/bromark-sharded-sql.tsv：5个Mapper共13条语句，全部AST范围检查通过。

## 资源与身份

v2 默认关闭；开启 bm.v2.enabled=true 时必须提供 bm.v2.auth.public-key（Base64 X509 RSA公钥）、issuer、audience，固定RS256并校验exp/nbf/sub/aud/iss，不签发token。
匿名读取仅GET /api/v2/activities/{id}及/{id}/awards；POST /api/v2/payments/notify/{channel}只豁免JWT，渠道验签由阶段5实现。其他v2请求必须认证，身份只读JWT sub。
无JWT时 x-user-id 和请求userId不能获得身份；/actuator管理端点默认拒绝，health例外。identity探针仅v2-it profile注册。
六类许可按物理实例独立，不借用；默认总7，各逻辑池至少8，启动校验总许可严格小于任一映射逻辑池上限。旧入口尚未迁入v2预算，禁止新旧写入混用作为容量证明。
Virtual worker容量16，独立API/Relay/Recovery有界平台池；只有worker显式使用虚拟线程。v2入口在工作线程内调用UserTransactionRunner。
停机停止受理、停止worker并等待事务释放，之后关闭物理池；阶段3尚未实现的Confirm跟踪不能由本阶段宣称已验收。
许可等待/拒绝/事务指标为bm.v2.database.*；连接等待使用Hikari指标，池标签为固定小集合；lock.wait为performance_schema全局累计InnoDB行锁等待秒数，每物理实例采样一次，查询需QUERY许可和监控读权限，失败返回NaN而非伪零。
RouteCodec订单槽采用 TYPE-02.001-node1-123 格式；v2生成器需沿用这一槽编码。旧订单格式不通过此解码入口。

## 隔离复现

使用 docs/dev-ops/docker-compose-v2-test.yml；设置V2_MYSQL_PASSWORD和V2_RABBIT_PASSWORD，三个新库由v2-init.sql建立。独立测试显式导入组件，不扫描旧调度器/监听器。
测试主机默认host.docker.internal（Maven在Docker中运行），本机执行可设置V2_MYSQL_HOST；中间件探针默认本地隔离端口16386和15686。
全量旧测试必须用spring.config.location指向application-v2-it.yml，不允许回落dev；旧样本使用router_legacy_*独立库，不覆盖新验收库。
回退到升级前提交86c457a及匹配Java8产物/镜像；Java25字节码不能在Java8运行。未执行生产迁移或部署。
