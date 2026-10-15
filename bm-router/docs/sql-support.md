# SQL 支持边界

使用 JSqlParser 4.9 AST 改写，仅对 @DBRouterStrategy(splitTable=true) 的 Mapper 生效。
支持单表 SELECT、INSERT VALUES、UPDATE、DELETE，含别名、反引号、参数占位符、普通函数与字面量。
拒绝 JOIN、CTE、子查询、UNION、多语句、schema 限定表、缺失/非法表槽；不支持语句在 JDBC 执行前失败。
SQL 会由 AST 重新格式化；参数映射顺序保持原顺序。
完整 BroMark 分表 SQL 清单位于 src/test/resources/bromark-sharded-sql.tsv，由5个分表 Mapper 的全部 XML 语句提取。
默认无路由访问 db00；未知路由不得回退；分表 Mapper 必须有表槽。

审查修复：TablesNamesFinder 4.9 漏遍历 IS NULL、ORDER BY/GROUP BY 及 duplicate-update，不能作为完整验证器。正式实现遍历固定版本语义AST的全部实例字段（包含继承字段、集合），跳过parser token/parent元数据；不能访问字段时拒绝，不静默放行。子查询即使位于这些从句内也拒绝；表限定列、排序和通配符一并改写。JSqlParser升级时必须重跑这些边界和MySQL验收。
