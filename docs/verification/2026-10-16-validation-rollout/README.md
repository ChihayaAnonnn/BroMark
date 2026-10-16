# Phase6 开发、正确性与容量验收

**正确性与运维工具已交付；阶段退出和生产发布为NO-GO。** 首个100请求/秒持续档未达到延迟与积压目标，停止升压并保留全部未通过项。没有生产部署、合并或push。

- 分支：feat/phase6-validation-rollout；功能验证以tests.json/validation.json的实际计数为准。
- 应用138项、Router28项；Python统计/门禁27项、切换9项；原迁移4项，均实际执行，无skip。
- 切换：精确原身份+原ledger/receipt+路由证明，导入后回放、旧签到日期与已扣款兑换、重复账户拦截、实际DDL中断/恢复、映射篡改及大金额分币校验。
- 实故障：MySQL连接KILL无未提交账务/Outbox；Rabbit STOP与应用SIGKILL由另一实例接管；CANDIDATE_FIXED阶段SIGKILL保留原候选和一次发奖；原回执跨实例重放。
- 独立审查1 Critical/6 Important已修复，另有自查补强；延期Minor M1见review.md。完整旧演示测试未筛选运行仍出现fork timeout，最新日志/tmp/phase6-legacy.log；不声称旧全套绿灯。
- [容量实测与停止依据](capacity-report.md)、[六域对账](reconciliation.json)、[审查](review.md)、[全部裁定](rulings.md)、[原始证据路径与hash](raw-evidence.json)。

复现完整正确性门禁（先停专属实验库写者，凭据只来自环境）：

```sh
V2_RECONCILIATION_SEED=/tmp/SEED/seed.json V2_RECONCILIATION_STOPPED=1 V2_EVIDENCE_DIR=/tmp/VERIFY docs/dev-ops/v2-verify.sh --env v2-it
```

还需提供V2_MYSQL_PASSWORD、V2_RABBIT_PASSWORD和MAVEN_CACHE_DIR；缺依赖或缺六域证据返回未通过。seed/负载/独立tracker/六域检查见../../performance/README.md；切换、停止与恢复见../../operations/v2-cutover-runbook.md和v2-rollback-runbook.md。

性能运行使用f29d926运行包镜像e211f170…；最终额外变更为测试和运维门禁，打包项目class比对见class-match.json。最终JAR/镜像hash见artifacts.json。原始JSONL、日志、测试JWT/密钥保留在/tmp隔离目录，git只存合成摘要与脚本。

发布前仍必须完成容量失败定位、完整矩阵/重复/可比基线、真实权限与全实例旧写冻结审计；真实支付和副本整体丢失不在本次授权。功能绿灯和六域零差异不代表发布批准，validation.json.releaseAuthorized始终false。

后续定位见[链路延迟分析与单变量诊断](../2026-10-16-latency-analysis/README.md)：发布前积压、恢复事件放大、领取索引与调度对照。容量 NO-GO 不变。
