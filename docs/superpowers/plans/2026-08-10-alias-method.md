# Alias Method Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the strategy probability lookup table with Alias Method tables so strategy assembly is O(n) and each random selection is O(1) without probability-precision-sized storage.

**Architecture:** `StrategyArmoryDispatch` remains the production entry point. It builds an immutable `AliasTable` for the default prize pool and each `rule_weight` sub-pool, then persists the table through `IStrategyRepository`. Runtime dispatch reads the table and chooses a column plus a coin flip; existing responsibility-chain, rule-tree, and stock logic remain unchanged.

**Tech Stack:** Java 8, Spring Boot, JUnit 4, Redisson/Redis, Maven.

## Global Constraints

- Preserve all existing public activity and strategy APIs.
- Use O(n) table construction and O(1) selection.
- Normalize configured `BigDecimal` rates; do not depend on the smallest rate or a precision-derived expanded table.
- Support keys for both the default strategy pool (`strategyId`) and weighted sub-pools (`strategyId:weight`).
- Add tests before production code for Alias table construction and dispatch behavior.

---

### Task 1: Add and test the Alias Table algorithm

**Files:**
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/service/armory/algorithm/AliasTable.java`
- Create: `bm-domain/src/main/java/com/jay/domain/strategy/service/armory/algorithm/impl/AliasMethodAlgorithm.java`
- Test: `bm-app/src/test/java/com/jay/test/domain/strategy/AliasMethodAlgorithmTest.java`

**Interfaces:**
- Produces: `AliasTable build(List<StrategyAwardEntity> awards)` and `Integer dispatch(AliasTable table, Random random)`.
- `AliasTable` exposes `getAwardIds()`, `getProbabilities()`, `getAliases()`, and `size()`.

- [ ] Write failing tests for a single prize, invalid/zero total probabilities, and a two-prize table whose deterministic random values exercise both the primary and alias branches.
- [ ] Run `mvn -pl bm-app -am -Dtest=AliasMethodAlgorithmTest test` and verify the tests fail because the algorithm classes do not exist.
- [ ] Implement `AliasTable` and Alias Method construction using small/large deques, `BigDecimal` normalization, and only final `double` thresholds.
- [ ] Run the same test command and verify it passes.

### Task 2: Persist Alias Tables through the strategy repository

**Files:**
- Modify: `bm-domain/src/main/java/com/jay/domain/strategy/repository/IStrategyRepository.java`
- Modify: `bm-infrastructure/src/main/java/com/jay/infrastructure/adapter/repository/StrategyRepository.java`
- Test: `bm-app/src/test/java/com/jay/test/domain/strategy/StrategyArmoryDispatchTest.java`

**Interfaces:**
- Consumes: `AliasTable` from Task 1.
- Produces: `void storeStrategyAliasTable(String key, AliasTable table)` and `AliasTable getStrategyAliasTable(String key)`.

- [ ] Write a failing dispatch test that verifies the repository receives an Alias table instead of an expanded rate map.
- [ ] Run the test and verify failure because repository Alias methods do not exist.
- [ ] Store the three arrays and table size in a Redis value under a dedicated Alias key; deserialize them on read and retain the existing expanded-table methods only for backward compatibility.
- [ ] Run the test and verify it passes.

### Task 3: Route strategy assembly and dispatch through Alias Method

**Files:**
- Modify: `bm-domain/src/main/java/com/jay/domain/strategy/service/armory/StrategyArmoryDispatch.java`
- Test: `bm-app/src/test/java/com/jay/test/domain/strategy/StrategyArmoryDispatchTest.java`

**Interfaces:**
- Consumes: repository Alias methods and `AliasMethodAlgorithm` from Tasks 1-2.
- Produces: unchanged `getRandomAwardId(Long)` and `getRandomAwardId(Long, Long)` behavior backed by Alias tables.

- [ ] Write failing tests for default-pool and weighted-pool assembly/dispatch.
- [ ] Run the focused tests and verify they fail while the expanded-table implementation remains active.
- [ ] Replace `assembleLotteryStrategy` probability-table creation with Alias table creation and replace both random-award methods with Alias dispatch; keep stock caching and weighted pool key construction unchanged.
- [ ] Run focused tests and verify they pass.

### Task 4: Verify regression safety

**Files:**
- Test: `bm-app/src/test/java/com/jay/test/domain/strategy/AliasMethodAlgorithmTest.java`
- Test: existing strategy tests under `bm-app/src/test/java/com/jay/test/domain/strategy/`

- [ ] Add a statistical sanity test with a deterministic seed and a sufficiently large sample to verify a 1:3 distribution is within a fixed tolerance.
- [ ] Run `mvn -pl bm-app -am -Dtest=AliasMethodAlgorithmTest,StrategyArmoryDispatchTest test`.
- [ ] Run `mvn -pl bm-app -am test` and record any existing infrastructure-dependent failures separately from the Alias changes.
- [ ] Review `git diff --check` and `git diff` before handoff.
