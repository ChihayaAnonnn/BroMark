# 隔离切换演练与真实切换前提

本仓库脚本只允许 `--env v2-it`、`router_cutover_it_*` 和 `bromark-v2-mysql`；默认dry-run。真实数据库执行、外部部署需单独授权与环境清单。禁止把测试的审计签字替换为生产审计。

| 步骤 | 前提/命令 | 成功证据 | 停止条件/恢复点 |
|---|---|---|---|
| 1 冻结 | 全实例禁旧资产新增；按write-entry-inventory停旧HTTP、任务、消费者及管理脚本；先停受理后排空 | 每实例确认、在途0、旧消息/task清单与已发消息凭据 | 不确定实例或外部SQL写者时停在旧写冻结 |
| 2 审计 | 导出账户、原流水/原回执、总/日/月次数、有效计数、日期签到、已扣待兑换与已发奖消息；按交易证据规范化目标行 | run_id/auditor、停写/排空声明、每表数量/金额/checksum、零未解释差异 | 重复账户不能SUM；权益/周期不明则核查，禁止继续 |
| 3 预检 | `python3 docs/dev-ops/mysql/rehearse_cutover.py --env v2-it --phase precheck --input /tmp/audited.json --database router_cutover_it_RUN --out /tmp/precheck.json` | cutover_allowed=true；真实演练另建专属空库 | false不执行DDL/回填 |
| 4 准备 | 相同参数 `--phase prepare --execute`，凭据只来自V2_MYSQL_PASSWORD | 单事务导入原回执与精确身份；006约束，cutover_audit进度、每表checksum吻合 | DDL失败停止；保留库、输入hash和progress，无DROP。检查information_schema后原文件重入 |
| 5 核验 | 相同参数 `--phase verify --execute`；运行v2-reconciliation.sql与独立测试门禁 | 期初+流水=余额；完整周期审计；签到/发奖/付款/库存各已检查；原回执重投无资产变化 | 新核查单/不一致停受理，保留Worker |
| 6 业务开关 | 先共享资产统一V2，再活动抽奖；旧监听器保持关闭，新versioned队列独立启动 | 老消息只走审计适配或核查；原业务回执可重放 | 旧路径混写立即停止受理，不能交回旧代码 |
| 7 观察放量 | 仅在正确性、长期固定到达率和恢复证据均通过后，依次测试/小范围/完整活动 | release checklist签字、具体镜像digest/配置/迁移hash/指标 | 指标/账实阈值触发rollback runbook |

输入为**规范化目标行**：`run_id,auditor,stopped:true,drained:true,tables:{表名:[完整目标行]},identities:[{entry,legacy_id,biz_type,biz_id,request_hash,evidence}],unresolved:[]`。entry必须带完整业务命名空间（含用户/日期/奖励项需要的部分），不接受同ID不同语义。账务期初与已导入流水不能双计；旧已扣待兑换必须冻结原扣款身份与待发权益，不当作新同步兑换。未知消息不默认成功。

prepare要求目标表首次为空；恢复使用同run_id/同输入hash，禁止换文件覆盖已导入事实。重复账户和非空预检先于006，唯一主键由001建立；存量原表没有规范主键时须转入独立目标表逐行核验，不能对脏原表直接ALTER。脚本不自动从不完整历史推算权益。

旧SKU库存仅核对并退出。真实奖品按实际所有权建立一次库存池，新旧策略共用同池，不复制库存。真实服务切换必须补齐备份、最小权限、所有实例冻结证据；本地演练不等于已完成生产迁移。
