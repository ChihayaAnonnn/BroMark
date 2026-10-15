# Execution rulings and costs

- Ruling: retain JDBC repository implementation established in phase 1/2, rather than duplicate unused MyBatis DAO/XML — same routed connection and explicit SQL, cost: file layout differs from plan.

- Ruling: schema-invalid raw messages need durable quarantine before decoding EventEnvelope; do not weaken its schema=1 constructor — cost: extra raw-message evidence path.

- Ruling: canonical adapter keys DRAW/RESERVATION/PAYMENT (and CREDIT_DELIVERY/NOTIFICATION) match downstream plan names; missing aggregate is UNBOUND case, never fabricated state — cost: future domain ports must register exact keys and return null for unknown IDs.

- Ruling: source Outbox owner type is not encoded in EventEnvelope; exhausted source publication binds OUTBOX_TASK +original event ID, not an inferred destination order — cost: source-domain adapter reconciliation in phases4/5 must inspect these cases; task is durably DEAD and manual resume restates original fact.

- Ruling: resume replays at most128 facts atomically; RecoveryScanJob continues remaining facts only after audited case closure and ACTIVE check — cost: a large backlog completes over multiple polls.

- Ruling: raw unknown-schema bytes are durable quarantine +RAW_MESSAGE case, never auto-decoded during generic resume — cost: repaired-code audit tool must validate/re-deliver original raw evidence before closing its case.

- Ruling: legacy SendMessageTaskJob/stock jobs are conditional off in V2 write mode after full-app reproduction showed old task-table scans; legacy mode unchanged — cost: old outstanding tasks require phase6 migration handling.

- Ruling: 3.3 and3.4 core delivered in one cohesive commit because the consumer pause gate depends on RecoveryService; wiring/lifecycle/runbook committed separately — cost: task boundaries differ but each final tree compiles.

- Ruling: append 003_recovery_guard.sql instead of modifying already-applied003 script — preserves immutable migration checksums, cost: one extra common coordination table and migration step.

- Final: Ruling: future phase4/5 domain state machines and source-Outbox/domain ownership remain deferred per plan — current valid unhandled commands pause WAITING_FIX, no asset execution — cost: not yet an end-to-end raffle/payment product.

- Final: Ruling: JDBC wrappers and raw-message repair tool remain as documented — no unused MyBatis layer or automatic unknown-payload execution — cost: directory differences and audited repair tool needed.

- Final: Ruling: legacy queue/task/dedupe migration and multi-node quorum/performance acceptance remain phases6/operations — functional single-node evidence only — cost: no production cutover/capacity/HA claim.

- Final: Ruling: existing full legacy-suite hang remains baseline — bounded run and thread stack recorded — cost: remaining old tests unverified, branch not auto-merged.
