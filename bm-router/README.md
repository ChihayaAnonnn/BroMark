# BroMark Router

BroMark内部Maven子模块，统一继承根POM的Java25、Boot3.5及依赖管理。
保留com.jay.middleware:db-router-spring-boot-starter:2.0.0-SNAPSHOT坐标和cn.bugstack Java包名，现有调用方无需改动。

从BroMark根目录执行mvn -pl bm-app -am -DskipTests package会自动构建本模块，不再依赖平级Router仓库或预先mvn install。
验收使用docs/operations/verify-java25.sh统一执行28项Router及17项BroMark测试；MySQL/Redis/RabbitMQ连接信息由环境变量提供。

路由在事务前建立，活动事务不能修改库表；嵌套调用恢复外层路由，未知数据库拒绝回退。
SQL范围见[SQL说明](docs/sql-support.md)，版本固定为JSqlParser4.9；连接池关闭与初始化失败回收由本模块负责。
