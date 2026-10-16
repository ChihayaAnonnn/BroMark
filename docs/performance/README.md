# 隔离容量实验

只操作 BroMark 专用容器、全新 `router_perf_it_*` 库。原始输出/JWT/密钥放 `/tmp`；不得提交真实用户数据。生产部署未授权。

1. 启动专用依赖，执行 `docs/dev-ops/v2-verify.sh --env v2-it`。设置隔离 `V2_MYSQL_PASSWORD,V2_RABBIT_PASSWORD,MAVEN_CACHE_DIR`。`tests.json.passed=true` 只表示功能回归，不是容量通过。
2. 停止本次目标库所有写者后准备：
   ```sh
   python3 docs/performance/seed_v2_testdata.py --env v2-it --users 1000 --run-id RUN --out /tmp/SEED --stock 1000000 --stopped
   ```
   首次签到必须有足够的独立用户，至少 rate×duration；重复签到另跑。seed导入审计期初，固定100万积分/次数供合成实验，不能套用真实账户。
3. 只在test/v2-it配置启动两个应用，独立限制CPU/内存、同一新队列prefix与目标库。声明`bm.v2.budget.replica-count=2`，三个逻辑连接池按一个物理MySQL聚合；沙盒凭据仅从环境传入。应用参数必须能覆盖配置。启动前后记录镜像/JAR/迁移hash。
4. 混合负载的10%结果查询需要给每个测试用户预建一个抽奖订单，并将 `seed.json.resultOrders` 写为 `{user:orderId}`；等待其完成，开跑前对账。预建请求不计入本次主负载。
5. `export V2_TOKEN_FILE=/tmp/SEED/tokens.json`，独立tracker从此文件读取JWT。每次一个新输出目录：
   ```sh
   python3 docs/performance/run_v2_experiment.py --seed /tmp/SEED/seed.json --out /tmp/RUN --rate 100 --duration 600 --scenario mixed --run-id RUN
   ```
   初始预热60秒另目录、不计入正式档。逐档100/300/600/1000，每档≥600秒，关键配置重复3次。发现持续积压、未解释差异或资源越界立即停止提升压力，保留恢复预算，记录未通过档及原因。不要在失败档之上继续灌入。
6. 独立tracker每秒最多50次查单，轮询计入`tracked.polling.json`，不混入主负载。开跑前/后记录MySQL和应用时钟、k6版本；完成耗时取同DB服务端ACCEPTED→DELIVERED或PAID→DELIVERED，HTTP观测延迟另记。`export_v2_stages.py`导出服务器时间；校准不确定度要一同报告。
7. 等待终态，停止新增但保留Worker排空，最终停止本次两个应用后：
   ```sh
   python3 docs/performance/reconcile_v2_run.py --seed /tmp/SEED/seed.json --out /tmp/RUN/reconciliation.json --stopped
   python3 docs/performance/export_v2_stages.py --seed /tmp/SEED/seed.json --requests /tmp/RUN/requests.jsonl --completions /tmp/RUN/tracked.jsonl --out /tmp/RUN/completions.jsonl
   python3 docs/performance/collect_v2_results.py --run-dir /tmp/RUN
   ```
   metadata.json按报告模板填**实际证据**，不可猜填true。聚合复测报告时附三个run的原始路径/hash和波动，不能将一次运行的repeats手填成3。`results.json.safeCapacityCandidate=false`是合法的未达标报告，发布仍NO-GO。

场景：mixed为70查询/10结果/10兑换/10抽奖；hot-activity为同活动抽奖；hot-user在指定2/8/32 VU同用户；signin-first与signin-repeat区分身份；payment-create只创建付款订单，必须另由测试沙盒驱动有效付款再跟踪到账，未付款不能算完成；payment-replay输入环境`V2_NOTIFICATION_FILE`指向预先签名的原付款事实，重复通知不新增权益。版本切换在独立受控发布器切换同池概率版本，不能重置库存；recovery-with-arrivals在持续主流量下运行故障脚本/采样，处理速率必须高于新增。

真实故障：`fault_v2_runtime.py --env v2-it --seed /tmp/SEED/seed.json --out /tmp/faults.json`仅识别拥有当前目标库的两个应用，停止专用Rabbit、SIGKILL应用1并由应用2接管，再恢复依赖。数据库连接故障由DatabaseDisconnectV2Test实际KILL MySQL连接验证。整机/副本整体丢失不在此故障模型。

报告必须披露本机同时运行的其他项目、压测端与服务端共享硬件、单节点MySQL/Rabbit；这样的本地诊断不能当作独立压测机或生产安全容量。有限库存耗尽后的兜底区间单列，查询/受理/完成分别统计。
