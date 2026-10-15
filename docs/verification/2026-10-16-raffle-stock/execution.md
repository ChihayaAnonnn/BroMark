# SDD ledger — plan: docs/superpowers/plans/2026-10-15-phase-4-raffle-stock.md
Base: f32de58. Current-directory branch feat/phase4-raffle-stock, per user preference.
Tasks: 4.1 immutable strategy/cache; 4.2 stock protocol; 4.3 acceptance/candidate; 4.4 cancellation/recovery; 4.5 delivery/API/reconciliation.
Pre-flight: 4.1→4.2 immutable award binding, shared pool identity; 4.1→4.3 frozen rule input; 4.2→4.4 identity/final digest; 4.3→4.4 legal CAS; 4.4→4.5 confirmed award only; Phase3→all same-slot consumer transactions/recovery ports.
Ruling: Continue existing JDBC repository pattern rather than unused MyBatis wrappers — single transaction-bound access implementation — cost if wrong: adapter rewrite.
Ruling: Separate control and user 004 scripts in manifest — runner scopes cannot mix control-only and four user tables in one file — cost if wrong: extra migration file.
Ruling: Prior legacy infinite-wait demo remains a known full-suite limitation; run bounded full-suite once and report it — do not change unrelated demo behavior — cost if wrong: later legacy failures remain unobserved.
4.1 core RED missing interfaces → GREEN FrozenStrategyV2Test 2/2, log /tmp/phase4-strategy-green.log. Remaining public query/cache resilience covered in integration pass.
4.2 core RED missing interfaces → GREEN StockProtocolV2Test 2/2 plus strategy2/2, log /tmp/phase4-stock-green.log. Remaining transport/recovery validation in subsequent tasks.
Ruling: Persist complete immutable strategy alongside accepted order — prevents cross-db/cache I/O inside Phase3 consumer transaction — cost if wrong: larger per-order JSON storage.
4.3 core RED missing interfaces → GREEN DrawAcceptanceV2Test2/2; strategy2/2 stock2/2. /tmp/phase4-draw-green.log. Test fixture account lock corrected after stack trace identified missing activity lock in quota assertion. Remaining frozen-blacklist/admission and technical rollback cases in acceptance expansion.
4.4 core RED missing DrawProgressService → GREEN cancellation2/2; confirmed receipt loss, pause/defer/resume and rollback in RaffleFault4/4. /tmp/phase4-fault-green.log.
4.5 core RED missing AwardDeliveryService/RaffleReconciliationService → GREEN delivery1/1 and real full application/MQ/HTTP integration2/2. /tmp/phase4-fault-green.log total17/17.
Ruling: quantity=0 means no stock movement, not no credit — unlimited CREDIT still delivers its frozen amount; NONE alone has no credit ledger — cost if wrong: business meaning requires redesign.
Ruling: Explicit reconciliation is a maintenance operation that locks accounts/pools for one physical database — consistent fact/ledger inspection before pausing only affected identities — cost if wrong: database-sized lock duration; do not run as an unbounded scheduled job.
Ruling: Durable recovery scan cursor is separate from last_progress_at — rotating failed authorities must not hide oldest HELD age — cost if wrong: extra technical rows.
Test setup issue: first app run merged default dev profile; stopped owned container immediately on remote Redis PING warning, reran with explicit spring.profiles.active=v2-it. No transaction committed on remote environment.
Pre-review full acceptance: migration4/4, Router28/28, app92/92 /tmp/phase4-full-green.log. First run had only Redis Cluster stale noaddr peer (10923 slots); refreshed the three test-owned peer IPs and hardened verification readiness, without resetting slots or data.
Ruling: Daily unlock uses frozen dayEffectiveCount, weighted subpool selection uses frozen totalEffectiveCount — preserves the legacy rule's business meaning without its live query — cost if wrong: product rule naming/config requires migration.
Ruling: Public cachedAvailable is null when no stock projection exists — do not display authoritative or invented inventory — cost if wrong: no stock estimate until a projection is added.
Ruling: Record whole application heap usage and version entry count, not a fabricated cache-exclusive retained size — true isolated cache footprint/capacity belongs to Phase6 measurement — cost if wrong: 128-version tuning remains provisional.
Task 4.1: complete — strategy immutable copies, actual eviction/reload, weighted/default/blacklist/daily unlock, cents endpoints, query coalescing/negative TTL and best-effort prewarm. Strategy/Frozen9-test boundary subset green /tmp/phase4-boundary-green.log; prior whole suite92 green before two extra stock guard tests.
Task 4.2: complete — real two-instance shared-pool contention, HELD/confirm/cancel/tombstones, fixed digest/quantity0, init/adjustment logs, transaction guard, rejects unlogged preexisting budgets. Stock4/4 green /tmp/phase4-boundary-green.log (RED3 failures across stock/rule regressions /tmp/phase4-budget-red.log).
Task 4.3: complete —100 same-key acceptances, two independently budgeted workers one candidate/outbox, frozen blacklist and day, deterministic rejection and technical rollback. /tmp/phase4-full-green.log + boundary suite.
Task 4.4: complete — single cancellation branch, original-period refund, lost confirmed receipt recovery, real user Hikari closure keeps HELD, pause/defer/audited resume; bounded all 2x4 recovery jobs. /tmp/phase4-full-green.log.
Task 4.5: complete — credit/NONE unique atomic delivery, rollback, real Rabbit seven-stage HTTP integration including stock exhaustion,403/409/202/200, reconciliation drift and audited pool resume, WAITING_FIX and safe query. /tmp/phase4-full-green.log + /tmp/phase4-view-green.log.
Ruling: EventHandler beans call the domain services directly, without redundant forwarding listener/DAO wrappers — Phase3 consumer UoW remains the single Inbox/Outbox transaction boundary — cost if wrong: file names differ from plan.
Final review: /root/raffle_final_review, read-only f32de58..0bf34ee; 0 Critical,4 Important,0 Minor. Re-graded all four Important: late HELD after CANCELLED, conflicting fallback ID, cancellation admission isolation, NONE-credit reconciliation gap. All four reproduced as actual behavior failures in RaffleReviewRegressionV2Test, /tmp/phase4-review-red.log 4/4 RED. One fix pass in progress; no second review.
Final: Ruling: future payment and sign-in reconciliation — checkedScopes excludes them, their zero fields are not evidence — cost if wrong: those domains require separate acceptance.
Final: Ruling: production deployment/migration/switch — no external deployment or merge, Phase6 remains required — cost if wrong: no production readiness conclusion.
Final: Ruling: long-term throughput/cache retained heap/capacity — preserve functional evidence and coarse process heap only — cost if wrong: capacity remains unknown.
Final: Ruling: database-sized maintenance reconciliation — retain explicitly manual maintenance boundary, no periodic whole-db scan — cost if wrong: large lock duration pending Phase6 batching.
Final: Ruling: JDBC, split migrations, direct handlers and copied strategy — retain earlier ledgered choices — cost if wrong: storage/adapter maintenance overhead.
Final: Ruling: unlimited CREDIT quantity0 and missing public stock projection — deliver credit, show null for unavailable display stock — cost if wrong: product contract clarification needed.
Ruling: cancellation has its own Redis/fallback rate key and semaphore plus explicit cancel-rps/cancel-max-in-flight when raffle enabled; database cancellation uses existing RECOVERY budget independent of create TRADE — preserve bounded physical budget without inventing extra connections — cost if wrong: cancellation shares capacity with repair scans.

Final: fixed late HELD terminal reentry — RaffleReviewRegressionV2Test.lateHeldAfterCompletedCancellationIsSafeTerminalReentry RED→GREEN; 4/4 review regressions green /tmp/phase4-review-green.log.
Final: fixed fallback identity collision — RaffleReviewRegressionV2Test.publicationRejectsConflictingFallbackIdentityBeforeAcceptingDraws RED→GREEN; same suite.
Final: fixed independent cancel admission — RaffleReviewRegressionV2Test.exhaustedCreateAdmissionDoesNotBlockAuthorizedCancellation RED→GREEN; same suite.
Final: fixed NONE incorrect credit reconciliation — RaffleReviewRegressionV2Test.noneAwardCreditDriftIsDetectedEvenWhenBalanceMatchesLedger RED→GREEN; same suite.
Final review deferred minors: none.

Final fix pass whole acceptance: app98/98 (31 new Phase4),Router28/28,migration4/4,failures/errors/skipped0; source75a69a1. /tmp/phase4-final-acceptance.log. All four review fixes covered RED→GREEN and whole suite green. Additive004 has9 applied identities, checksums unchanged; 2x6 asset checks and3 stock checks all0.

Final source75a69a1 unfiltered legacy suite: /tmp/phase4-final-legacy.log,46.128sec,fork timeout40sec; dump main at RaffleActivityControllerTest.test_draw_blacklist:61 CountDownLatch.await. Router28 and one legacy strategy test completed. No merge/push/deployment. Test containers stopped after evidence collection.
