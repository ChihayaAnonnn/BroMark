# PostgreSQL 迁移与容量复测 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 实际运行 PostgreSQL V2 链路并完成 8 核 4 GiB 同负载复测。
**Architecture:** SQL兼容集中在路由连接边界，事务/业务逻辑沿用。独立PostgreSQL DDL与种子、对账工具，MySQL保留。
**Tech Stack:** Java25、Spring JDBC/Hikari、PostgreSQL、Docker、k6。
**Spec:** docs/superpowers/specs/2026-10-18-postgresql-capacity-design.md

## Global Constraints
- 应用 8 核、4 GiB、3 池各64；请求分布及阶段严格同前轮。
- 不覆盖旧证据、不提交密钥、不关闭事务持久性、不把202当交付。
- 当前开发不提交或合并代码；所有新资源保存身份并异常安全清理。

## Review Focus
- PostgreSQL duplicate-key 后事务失效：验证幂等重复仍可继续查询和提交。
- UTC和lease截止：clock_timestamp、微秒间隔与Java读取一致。
- JSON/bytea/Decimal：回读数据和hash不改变。
- 动态路由连接与Hikari生命周期：原池预算、监控和关闭可用。
- 暖机/正式/排空的统计隔离：错误和积压必须同成功率一起报告。

### Task 1: SQL与JDBC兼容
Files: bm-router/.../dynamic/PostgresSql.java, PostgresConnection.java；bm-infrastructure/.../v2下确需兼容的幂等写入。
Interface: public static java.sql.Connection PostgresConnection.wrap(Connection connection)；所有SQL转换集中在PostgresSql.translate(String)。root在DynamicDataSource只对PG启用。
- [x] 写已知函数/UPSERT/唯一冲突/bytea/JSON转换失败测试，先红后绿。
- [x] 实现V2已知SQL适配及防止事务被幂等冲突破坏的最小修改。
- [x] 路由单元测试和真实PG事务/重复请求验证。

### Task 2: PostgreSQL表结构
Files: docs/dev-ops/postgresql/v2-schema.sql；相关初始化检查测试。
Interface: 空数据库执行同一幂等schema脚本，每个逻辑库保留所需全部V2表和4分表；不含CREATE DATABASE/连接命令，由root建库。
- [x] 对照MySQL manifest审计全部表、主键、唯一键、索引和约束。
- [x] 编写DDL并在真实PG执行两遍，验证表/索引/唯一约束/JSON/日期/bytea。

### Task 3: 配置、集成与实验
Files: bm-app pom/test profile；bm-router DynamicDataSource/DataSourceAutoConfig；docs/performance PG runner/报告。
- [x] 加PG驱动/profile、仅PG路由连接包装；MySQL行为不变。
- [x] 新容器/卷、三库种子，真实集成测试和HTTP预置200交付。
- [x] 相同阶段压测及资源/等待采集、停写对账，与MySQL直连结果比较。
- [x] 独立审查；修复发现问题；新鲜核验资源/证据/差异，冻结并汇报。

完成记录：2026-10-18，迁移与真实 HTTP/8C4GiB 复测已完成；308 项验证通过。高负载资产差异 0，但未完成订单 31359、待处理任务 52948，容量验收 false。详见 docs/performance/2026-10-18-postgresql-8c4g-report.md。
