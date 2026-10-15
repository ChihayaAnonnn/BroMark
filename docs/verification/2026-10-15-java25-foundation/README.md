# Java 25 与阶段0执行验收

2026-10-15；BroMark分支feat/java25-foundation，独立Router分支feat/java25-router。已本地提交，未部署、合并、推送或发布制品。v2默认关闭。

## 实施与验证

| 项目 | 实测结果 |
|---|---|
| 构建/运行 | Temurin25.0.4.1+1、Maven3.9.16、Boot3.5.16、MyBatis3.0.5 |
| 六模块字节码 | 348个class均major69（各模块计数见results.json） |
| Router | 28项，failures/errors/skipped均0；真实MySQL8.0.46，2库4表、1,000虚拟线程 |
| BroMark | 17项，failures/errors/skipped均0；包含原JUnit4 Alias3项、真实DAO、路由/预算/线程/监控/JWT/MVC/Redis/RabbitMQ |
| 镜像 | arm64应用真实启动，health UP、无JWT401、正确签名sub返回200、管理端点403、实际DAO/策略/Redis armory成功 |
| 架构验证 | arm64应用；同一固定Temurin JRE digest在amd64显示25.0.4.1，不将其称为amd64全业务验收 |
| 关闭 | 停止受理、worker与许可排空；三个物理Hikari池各完成关闭 |
| 全量旧测试 | **未通过**：45秒fork超时，线程栈确认RaffleActivityControllerTest.test_draw_blacklist在第61行无限CountDownLatch.await；其余旧测试不得宣称已通过 |

检查结果来自Surefire XML，清单及镜像ID见[results.json](results.json)。[依赖树](dependency-tree.txt)、[完整effective POM压缩包](effective-pom.xml.gz)、[产物校验](artifact-checksums.json)均已保存；POM为七项目展开结果，压缩前约6万行，可用gzip -dc解压。
中间件版本Redis7.4.11、RabbitMQ3.13.7；Compose固定digest与隔离端口，连接凭据来自环境变量。未访问dev/prod数据库。原始日志和线程栈留在本机/tmp/bromark-*.log与/tmp/bromark-legacy-thread-dump.txt，不提交原始运行环境或凭据。

## 审查与红绿证据

执行了一次独立只读审查，两个Important已一次修复并验证；没有Critical。

- 原始Router正式验收13项中11项失败；自动配置、原子库表切换、嵌套恢复及代理事务顺序改造后通过。
- 原SQL/池生命周期5项全部失败，改造后通过；13条完整分表SQL及真实MySQL验收通过。
- Java8字节码基线明确失败：expected69 actual52；Java25六模块通过。Lombok显式处理器与无用JDK内部导入清理完成。
- 空请求字段原来返回200，加入Validation实现后400；硬编码big_market的控制DAO原来访问错误库，去除schema限定后读取配置控制库。
- 审查补充的8个SQL边界用例全部先失败：IS NULL、ORDER BY/GROUP BY、HAVING、duplicate-update中子查询及限定列/通配符。改用完整语义AST字段遍历后，Router28项全部通过。
- 参数缺失/类型错误原来503，修复后400；保留技术失败503及404客户端错误。BroMark17项全部通过。

保留一项Minor：V2IntegrationTestConfig尚不是独立可启动测试配置。已测且受支持的隔离入口是Java25RouterIntegrationTest.databaseContext()及V2FoundationWiringTest显式配置；不要将未测辅助类当成已可用入口。

## 复现

1. 使用docs/dev-ops/docker-compose-v2-test.yml启动隔离服务，提供V2_MYSQL_PASSWORD和V2_RABBIT_PASSWORD。
2. 保持独立Router仓库在BroMark平级目录，执行docs/operations/verify-java25.sh。脚本使用固定Maven镜像，先本地安装Router再运行针对性测试、打包和构建镜像；本次已完整执行通过。
3. ROUTER_MYSQL_HOST/V2_MYSQL_HOST、V2_REDIS_HOST/PORT、V2_RABBIT_HOST/PORT可调整；默认Docker访问host.docker.internal隔离端口。MAVEN_CACHE_DIR可设置本地缓存目录。
4. 全量旧测试使用显式spring.config.location指向application-v2-it.yml、v2-it profile和-Dsurefire.timeout=45，不能回落dev。历史样本仅导入独立router_legacy_*库。

上游许可仍未确认，仅本地安装；不能deploy或公开再分发。正常运行中的用户容器未更改。阶段0的Deadline只约束受理/许可等待，尚不取消执行中的JDBC；同槽嵌套按内联callback执行，捕获其技术异常不会自动增加rollback-only语义。Confirm关闭跟踪、所有权及公开读取回源预算随后续业务阶段集成。未声称吞吐提升、生产容量或新旧混用安全。

## 执行决策与代价

- Ruling: use Java25 major69 and Jakarta throughout phase0 — user selected Java25; cost if wrong: baseline migration revert.
- Ruling: enable controlled virtual-thread worker only after phase0 budget exists — upgrade task3 requires budget but phase0 depends on upgrade; cost if wrong: virtual-thread feature remains off until safety gate.
- Ruling: upstream license unverified despite provenance inspection — preserve copyright and prohibit external publication; local install only; cost if wrong: cannot distribute until permission is established.
- Ruling: SQL parse failures preserve parser cause; test checks IllegalArgumentException anywhere in exception chain rather than requiring deepest cause — wrapper classification vs diagnostic cause; cost if wrong: test could miss differently wrapped failures, constrained by explicit type check.
- Ruling: phase0 0.1 shares upgrade task3 build evidence; no duplicate migration — overlapping tasks. Cost if wrong: repeat baseline checks only.
- Ruling: replace unused Redisson Spring Data starter bridge with Redisson core3.26.0 — all application access uses explicitly configured RedissonClient; avoid SpringData32 vs Boot3.5 mismatch. Cost if wrong: consumers relying on auto-created RedisTemplate would need explicit starter, none found by complete source scan.
- Ruling: user approved in-place branch; Java25 upgrade overrides old Java17 baseline.
- Ruling: reserve7 total physical permits vs each logical Hikari pool8 to preserve callback/recovery capacity even if traffic concentrates on one logical DB; production must validate this inequality. Cost if wrong: conservative capacity, later tune with workload evidence.
- Ruling: shutdown confirmation drain belongs to future phase3 Relay; phase0 stops workers and drains database permits only. Cost if wrong: must add broker confirm tracker when Relay exists.
- Ruling: control SQL must use configured routed control database rather than hardcoded schema — isolation and deploy database-name compatibility; cost if wrong: callers inside user transactions must move control reads outside/into inControl, audited current calls occur before user routing.
- Ruling: do not rewrite manual legacy demos or business recovery flows as part of foundation — tests hang by construction; cost if wrong: those demos need separate conversion into deterministic tests before full-suite acceptance.
- Final: Ruling: future asset/Outbox/Inbox/payment ownership and public-query budgets remain subsequent phases — only foundation authorized; cost if wrong: those business flows cannot be opened yet.
- Final: Ruling: legacy full-suite timeout remains a failed broad gate — no green full-suite claim; cost if wrong: legacy behavior remains unverified outside targeted cases.
- Final: Ruling: no external Router publication without license confirmation — local-only adaptation; cost if wrong: deployment supply chain needs separate permission/artifact distribution.
- Final: Ruling: no production throughput or cluster capacity claims — functional probes only; cost if wrong: capacity must be measured before rollout.
- Final: Ruling: Deadline bounds admission/permit waiting, not executing JDBC cancellation — phase0 minimum; cost if wrong: integrate query/socket/call deadlines before enabling business traffic.
- Final: Ruling: same-slot nested calls are inline callbacks, not savepoints or an extra REQUIRED interceptor — no specified nested contract; cost if wrong: catching inner technical exceptions requires explicit rollback or later semantic change.
- Final: Ruling: only tested shutdown/permit drain is claimed, not all forced-timeout failures — recovery validation belongs to later phases; cost if wrong: long JDBC interruption/drain requires fault tests before rollout.
- Final: Ruling: dependency versions/images rely on recorded live pulls — patch/architecture evidence retained; cost if wrong: refresh vendor patch selection before deployment.
- Final: Ruling: inherited legacy asset/recovery behavior is outside foundation scope — no rewrite; cost if wrong: later business phases must close known reliability gaps.

## 后续模块整合

Router现已嵌入BroMark/bm-router，不再需要平级仓库或预先install。当前统一构建及复现入口见[模块整合验收](../2026-10-15-embedded-router/README.md)。本报告保留此前独立模块阶段的历史证据。
