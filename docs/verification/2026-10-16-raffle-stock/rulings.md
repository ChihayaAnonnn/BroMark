# 实施裁决与代价

Ruling: Continue existing JDBC repository pattern rather than unused MyBatis wrappers — single transaction-bound access implementation — cost if wrong: adapter rewrite.

Ruling: Separate control and user 004 scripts in manifest — runner scopes cannot mix control-only and four user tables in one file — cost if wrong: extra migration file.

Ruling: Prior legacy infinite-wait demo remains a known full-suite limitation; run bounded full-suite once and report it — do not change unrelated demo behavior — cost if wrong: later legacy failures remain unobserved.

Ruling: Persist complete immutable strategy alongside accepted order — prevents cross-db/cache I/O inside Phase3 consumer transaction — cost if wrong: larger per-order JSON storage.

Ruling: quantity=0 means no stock movement, not no credit — unlimited CREDIT still delivers its frozen amount; NONE alone has no credit ledger — cost if wrong: business meaning requires redesign.

Ruling: Explicit reconciliation is a maintenance operation that locks accounts/pools for one physical database — consistent fact/ledger inspection before pausing only affected identities — cost if wrong: database-sized lock duration; do not run as an unbounded scheduled job.

Ruling: Durable recovery scan cursor is separate from last_progress_at — rotating failed authorities must not hide oldest HELD age — cost if wrong: extra technical rows.

Ruling: Daily unlock uses frozen dayEffectiveCount, weighted subpool selection uses frozen totalEffectiveCount — preserves the legacy rule's business meaning without its live query — cost if wrong: product rule naming/config requires migration.

Ruling: Public cachedAvailable is null when no stock projection exists — do not display authoritative or invented inventory — cost if wrong: no stock estimate until a projection is added.

Ruling: Record whole application heap usage and version entry count, not a fabricated cache-exclusive retained size — true isolated cache footprint/capacity belongs to Phase6 measurement — cost if wrong: 128-version tuning remains provisional.

Ruling: EventHandler beans call the domain services directly, without redundant forwarding listener/DAO wrappers — Phase3 consumer UoW remains the single Inbox/Outbox transaction boundary — cost if wrong: file names differ from plan.

Final: Ruling: future payment and sign-in reconciliation — checkedScopes excludes them, their zero fields are not evidence — cost if wrong: those domains require separate acceptance.

Final: Ruling: production deployment/migration/switch — no external deployment or merge, Phase6 remains required — cost if wrong: no production readiness conclusion.

Final: Ruling: long-term throughput/cache retained heap/capacity — preserve functional evidence and coarse process heap only — cost if wrong: capacity remains unknown.

Final: Ruling: database-sized maintenance reconciliation — retain explicitly manual maintenance boundary, no periodic whole-db scan — cost if wrong: large lock duration pending Phase6 batching.

Final: Ruling: JDBC, split migrations, direct handlers and copied strategy — retain earlier ledgered choices — cost if wrong: storage/adapter maintenance overhead.

Final: Ruling: unlimited CREDIT quantity0 and missing public stock projection — deliver credit, show null for unavailable display stock — cost if wrong: product contract clarification needed.

Ruling: cancellation has its own Redis/fallback rate key and semaphore plus explicit cancel-rps/cancel-max-in-flight when raffle enabled; database cancellation uses existing RECOVERY budget independent of create TRADE — preserve bounded physical budget without inventing extra connections — cost if wrong: cancellation shares capacity with repair scans.

审查4项Important均修复；无延期Minor。
