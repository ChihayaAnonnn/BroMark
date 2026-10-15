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
