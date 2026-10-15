# DB Router 升级验证材料

`results.json` 为本次实际 Surefire XML 提取的执行计数与用例结果，不包含机器环境属性、堆栈或 Maven 缓存。

- `original-behavior/`：原版 1.0.2 的行为探针，7 项通过代表问题行为被复现。
- `original-acceptance/`：原版 1.0.2 的期望行为测试，8 项中 7 项失败。
- 工作区平级目录 `db-router-spring-boot-starter/compatibility-prototype/`：最小适配原型，使用完全相同的 RouterAcceptanceTest，8 项通过。它不是生产版本。

在工作区根目录复现（需要 Docker 与 Maven Central 网络访问；只运行隔离探针，不启动 BroMark）：

```bash
docker run --rm -v "$PWD:/workspace" -w /workspace/BroMark/docs/verification/2026-10-15-db-router-upgrade/original-behavior maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c mvn -B -ntp test
docker run --rm -v "$PWD:/workspace" -w /workspace/BroMark/docs/verification/2026-10-15-db-router-upgrade/original-acceptance maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c mvn -B -ntp test
docker run --rm -v "$PWD:/workspace" -w /workspace/db-router-spring-boot-starter/compatibility-prototype maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c mvn -B -ntp test
```

第二条命令预期非零退出；其余两条预期成功。运行后可在各项目 target/surefire-reports 查看原始 XML。不要运行下载目录根部旧 POM 的发布流程，也不要将 target 或缓存加入版本控制。
