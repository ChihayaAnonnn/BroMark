# 资产、签到与兑换：隔离验收与运行配置

阶段1—2只在隔离数据库验收。阶段3可靠消息及阶段6迁移完成前，不向真实共享账户开放，也不允许新旧资产写入混用。

## 配置

`bm.v2.enabled`默认false，`bm.v2.asset-write-mode`默认LEGACY。开启新交易须同时显式设置enabled=true、asset-write-mode=V2，并配置已验证的JWT公钥、issuer及audience。

以下入口预算没有生产默认值，V2启动必须明确提供并验证正值：

```yaml
bm:
  v2:
    enabled: true
    asset-write-mode: V2
    user-rps: 20
    activity-rps: 20
    max-in-flight: 20
    query-rps: 20
    query-max-in-flight: 4
    local-fallback-rps: 1
```

以上数字仅为隔离验收配置，不是容量结论。Redis故障使用每实例、每类别共享的保守预算，local-fallback-rps不能大于用户、活动或查询额度。交易与查询使用不同在途许可；数据库CALLBACK预算沿用阶段0，支付通知入口预算由阶段5接入，不能借用交易额度。Lua仅操作传入键（hash保存窗口与计数），兼容Redis Cluster。Redis异步等待最多200ms且不超过请求剩余时间，超时取消等待并释放在途许可；固定秒窗口允许跨窗口边界突发，max-in-flight与数据库预算继续限制实际并发。

V2模式停止三类旧资产监听器启动，同时在旧HTTP交易、积分仓储、发奖仓储和旧账户DAO写操作设置写门。直接重投旧消息会抛出迁移身份缺失异常，不作为成功Ack；历史订单/消息排空与身份导入属于阶段6。不要在仍运行的实例上只修改配置文件来宣称切换完成，必须维护窗口内统一停写、排空并重启验证。

## 数据库与权威配置

迁移入口为`docs/dev-ops/mysql/apply_migrations.py`，不在应用启动时执行。需要mysql CLI，密码只通过MYSQL_PWD注入。

```sh
python3 docs/dev-ops/mysql/apply_migrations.py --env v2-it --through 002 --dry-run
# 仅隔离库允许实际执行，必须明确白名单：
V2_MIGRATION_ALLOW=isolated-router-test-only python3 docs/dev-ops/mysql/apply_migrations.py --env v2-it --through 002 --mode fresh
# 存量库只新增可空字段，输出重复/未审计/期初冲突，不能据此开启新写：
V2_MIGRATION_ALLOW=isolated-router-test-only python3 docs/dev-ops/mysql/apply_migrations.py --env v2-it --through 002 --mode upgrade
```

脚本默认只列router_control/router_one/router_two；生产env只能dry-run。001旧账户升级不SUM、不覆盖available_amount、不自动回填期初或提前增加唯一约束，最终审计及约束归006。已提交DDL不DROP回滚；schema_migration保存脚本及升级字段摘要、逐语句进度；任何业务DDL前先持久化step=0校验和记录，校验和变化即拒绝重入。中断时先检查information_schema再执行同一版本，已创建对象采用IF NOT EXISTS。

db00的v2_admission_config是本阶段的SQL发布面：EXCHANGE键为sku，SIGNIN键为daily，ACTIVITY键为次数奖励activityId。snapshot遵循AdmissionSnapshot的JSON结构，价格使用十进制字符串，必须由可信运维发布完整不可变权益。签到读取全部次数奖励活动的权威状态和有效期，取有效期交集；v2_asset_blacklist按用户拒绝新交易。策略配置发布流程在阶段4完成。

新库账户有唯一及非负约束，旧表兼容新增保持原列。真正迁移时006还须处理旧非空列插入兼容性、旧周期身份、初始有效计数和重复账户，不能直接把upgrade执行成功当成完成切换。

## API

- POST `/api/v2/credit/exchanges`：requestId、sku、configVersion；GET `/api/v2/credit/exchanges/{orderId}`。
- POST `/api/v2/rebates/sign-ins`：requestId、targetDate；GET `/api/v2/rebates/sign-ins/{date}`。

仅使用已验证JWT.sub，DTO不声明userId。成功及稳定业务拒绝均200，拒绝携带state=REJECTED与reason；冲突409，未认证401，跨用户403，未找到404，入口限流429，技术失败503。签到拒绝不建立成功签到日身份。入口或格式拒绝不认领业务请求。

重试先查用户回执，命中不读取配置/Redis活动准入或申请号段；同日不同键关联原签到，跨午夜保留原日期。结果查询有独立入口和数据库QUERY预算。已提交金额以十进制字符串持久化，避免MySQL JSON改变小数位表示。

## 重现

启动已有隔离Compose（凭据从环境提供），执行：

```sh
# 需要Docker与python3；不需要宿主Java/Maven。
docker compose -f docs/dev-ops/docker-compose-v2-test.yml up -d
MAVEN_CACHE_DIR=/tmp/bromark-router-audit-20261015/m2 docs/operations/verify-assets.sh
# 验证脚本另启动三节点隔离Redis Cluster，无外部端口；停止：
docker compose -f docs/dev-ops/docker-compose-v2-redis-cluster.yml stop
```

迁移测试仅清理专用router_migration_it/router_migration_checksum_it测试库的表，不清理其他服务数据库。Java测试使用MySQL InnoDB、实际路由事务及Redis Lua；完整应用启动测试通过命令行显式只启用v2-it，避免默认dev profile叠加。历史演示测试含无限等待，完整旧套件另作限时检查并报告，不能宣称全库测试通过。
