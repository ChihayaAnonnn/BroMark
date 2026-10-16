package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class RecoveryRedriveGuardV2Test {
  final RecoveryRedriveGuard guard =
      new RecoveryRedriveGuard(Duration.ofSeconds(30), Duration.ofSeconds(30), 12);
  final DeliveryEvidence clear =
      new DeliveryEvidence(false, null, false, false, false, false, false);

  @Test
  public void respectsBoundariesAndDoesNotResetOnVersionOnly() {
    Instant now = Instant.parse("2026-10-16T00:00:00Z");
    var fresh = new RedriveState("DRAW", "id", "", 0, null, null, null, now, 0);
    assertThat(guard.decide(fresh, "ACCEPTED", 1, now.minusMillis(29999), clear, now))
        .isEqualTo(RedriveDecision.FRESH);
    assertThat(guard.decide(fresh, "ACCEPTED", 1, now.minusSeconds(30), clear, now))
        .isEqualTo(RedriveDecision.EMIT);
    var spent =
        new RedriveState(
            "DRAW",
            "id",
            "ACCEPTED",
            1,
            "PrepareDraw",
            "e",
            now.minusSeconds(60),
            now.minusSeconds(30),
            12);
    assertThat(guard.decide(spent, "ACCEPTED", 2, now.minusSeconds(40), clear, now))
        .isEqualTo(RedriveDecision.EXHAUSTED);
    assertThat(guard.decide(spent, "NEXT", 2, now.minusSeconds(40), clear, now))
        .isEqualTo(RedriveDecision.EMIT);
    assertThat(guard.decide(spent, "NEXT", 2, now.minusSeconds(70), clear, now))
        .isEqualTo(RedriveDecision.EXHAUSTED);
    assertThat(
            guard.decide(
                spent,
                "NEXT",
                2,
                now.minusSeconds(40),
                new DeliveryEvidence(true, null, false, false, false, false, false),
                now))
        .isEqualTo(RedriveDecision.LOCAL_IN_FLIGHT);
    assertThat(
            guard.decide(
                fresh,
                "ACCEPTED",
                1,
                now.minusSeconds(60),
                new DeliveryEvidence(
                    false, now.minusSeconds(29), false, false, false, false, false),
                now))
        .isEqualTo(RedriveDecision.OBSERVATION_WINDOW);
    assertThat(
            guard.decide(
                fresh,
                "ACCEPTED",
                1,
                now.minusSeconds(60),
                new DeliveryEvidence(false, null, true, false, false, false, false),
                now))
        .isEqualTo(RedriveDecision.REMOTE_RETRY);
    assertThat(
            guard.decide(
                fresh,
                "ACCEPTED",
                1,
                now.minusSeconds(60),
                new DeliveryEvidence(false, null, true, true, false, false, false),
                now))
        .isEqualTo(RedriveDecision.REMOTE_RETRY);
    assertThat(
            guard.decide(
                fresh,
                "ACCEPTED",
                1,
                now.minusSeconds(60),
                new DeliveryEvidence(false, null, false, false, true, false, false),
                now))
        .isEqualTo(RedriveDecision.PAUSED);
    assertThat(
            guard.decide(
                fresh,
                "ACCEPTED",
                1,
                now.minusSeconds(60),
                new DeliveryEvidence(false, null, false, false, false, true, false),
                now))
        .isEqualTo(RedriveDecision.UNKNOWN);
    assertThat(
            guard.decide(
                fresh,
                "ACCEPTED",
                1,
                now.minusSeconds(60),
                new DeliveryEvidence(false, null, false, false, false, false, true),
                now))
        .isEqualTo(RedriveDecision.CONFLICT);
    var cooling =
        new RedriveState(
            "DRAW",
            "id",
            "ACCEPTED",
            1,
            "PrepareDraw",
            "e",
            now.minusSeconds(29),
            now.plusSeconds(1),
            1);
    assertThat(guard.decide(cooling, "ACCEPTED", 2, now.minusSeconds(60), clear, now))
        .isEqualTo(RedriveDecision.COOLDOWN);
    assertThat(
            guard.decide(cooling, "ACCEPTED", 2, now.minusSeconds(60), clear, now.plusSeconds(1)))
        .isEqualTo(RedriveDecision.EMIT);
  }

  @Test
  public void rollsBackOutboxAndCooldownTogether() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          var ds = h.j.getDataSource();
          var repo = new MySqlRecoveryDispatchRepository(ds);
          var writer = new MySqlOutboxWriter(ds);
          String id = UUID.randomUUID().toString();
          var event = RelayOptimizationTestSupport.event(id, "PrepareDraw");
          java.util.function.Supplier<Boolean> emit =
              () -> {
                var state = repo.lock("DRAW", event.aggregateId());
                Instant now =
                    h.j
                        .queryForObject("select UTC_TIMESTAMP(6)", java.sql.Timestamp.class)
                        .toInstant();
                if (guard.decide(state, "ACCEPTED", 1, now.minusSeconds(60), clear, now)
                    != RedriveDecision.EMIT) return false;
                writer.append(event);
                repo.record(
                    "DRAW", event.aggregateId(), "ACCEPTED", 1, event, now, Duration.ofSeconds(30));
                return true;
              };
          assertThatThrownBy(
                  () ->
                      h.runner.inSlot(
                          RouteSlot.control(),
                          WorkClass.TRADE,
                          RelayOptimizationTestSupport.deadline(),
                          () -> {
                            emit.get();
                            throw new IllegalStateException("rollback");
                          }))
              .isInstanceOf(IllegalStateException.class);
          h.runner.inSlot(
              RouteSlot.control(),
              WorkClass.TRADE,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                assertThat(repo.lock("DRAW", event.aggregateId()).count()).isZero();
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from outbox where event_id=?", Integer.class, id))
                    .isZero();
                return null;
              });
          var second =
              new com.jay.infrastructure.transaction.RoutedTransactionRunner(
                  ds,
                  new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),
                  h.codec);
          var index = new java.util.concurrent.atomic.AtomicInteger();
          var connections = java.util.concurrent.ConcurrentHashMap.<Long>newKeySet();
          var barrier = new java.util.concurrent.CyclicBarrier(2);
          var results =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  2,
                  () -> {
                    var runner = index.getAndIncrement() == 0 ? h.runner : second;
                    return runner.inSlot(
                        RouteSlot.control(),
                        WorkClass.TRADE,
                        RelayOptimizationTestSupport.deadline(),
                        () -> {
                          connections.add(h.j.queryForObject("select CONNECTION_ID()", Long.class));
                          try {
                            barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                          } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                          }
                          return emit.get();
                        });
                  });
          assertThat(connections).hasSize(2);
          assertThat(results.stream().filter(Boolean::booleanValue).count()).isOne();
          h.runner.inSlot(
              RouteSlot.control(),
              WorkClass.TRADE,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                assertThat(repo.lock("DRAW", event.aggregateId()).count()).isOne();
                return null;
              });
        });
  }

  @Test
  public void verifiesAllVersionsRetryAndCommittedReceipt() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          var ds = h.j.getDataSource();
          var evidence = new MySqlDeliveryEvidenceRepository(ds);
          var writer = new MySqlOutboxWriter(ds);
          var retry = new MySqlRetryJobRepository(ds, h.runner);
          var inbox = new MySqlConsumerInbox(ds);
          String user = "evidence-" + UUID.randomUUID(), biz = UUID.randomUUID().toString();
          var event = ConsumerInboxV2Test.event(h, UUID.randomUUID().toString(), user, biz);
          h.tx(
              user,
              () -> {
                writer.append(event);
                h.j.update(
                    "update outbox set"
                        + " aggregate_version=0,next_attempt_at=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL"
                        + " 1 DAY) where event_id=?",
                    event.eventId());
                assertThat(
                        evidence
                            .local("CREDIT_DELIVERY", biz, event.eventType(), event.requestHash())
                            .localInFlight())
                    .isTrue();
                for (String state : List.of("PROCESSING", "DEAD")) {
                  h.j.update(
                      "update outbox set state=?,lease_until=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1"
                          + " DAY) where event_id=?",
                      state,
                      event.eventId());
                  var found =
                      evidence.local(
                          "CREDIT_DELIVERY", biz, event.eventType(), event.requestHash());
                  assertThat(state.equals("DEAD") ? found.blocked() : found.localInFlight())
                      .isTrue();
                }
                h.j.update(
                    "update outbox set state='PUBLISHED',published_at=UTC_TIMESTAMP(6) where"
                        + " event_id=?",
                    event.eventId());
                assertThat(
                        evidence
                            .local("CREDIT_DELIVERY", biz, event.eventType(), event.requestHash())
                            .latestPublishedAt())
                    .isNotNull();
                retry.schedule(event, 1, Instant.now().plusSeconds(100));
                assertThat(evidence.target(event).remoteRetryUnfinished()).isTrue();
                h.j.update(
                    "update retry_job set state='PUBLISHED',published_at=UTC_TIMESTAMP(6) where"
                        + " event_id=?",
                    event.eventId());
                inbox.tryInsert("workflow-v2", event);
                assertThat(evidence.target(event).remoteRetryUnfinished()).isTrue();
                return null;
              });
          h.tx(
              user,
              () -> {
                inbox.tryStartOperation("workflow-v2", event);
                inbox.completeOperation("workflow-v2", event, List.of());
                return null;
              });
          h.tx(
              user,
              () -> {
                assertThat(evidence.operationCompleted("workflow-v2", event)).isTrue();
                assertThat(evidence.target(event).remoteRetryUnfinished()).isFalse();
                assertThat(evidence.target(event).operationCompleted()).isTrue();
                h.j.update(
                    "update consumer_receipt set payload_hash=? where consumer_name='workflow-v2'"
                        + " and operation_key=?",
                    "b".repeat(64),
                    MySqlConsumerInbox.digest(
                        event.eventType() + ":" + event.bizId().type() + ":" + event.bizId().id()));
                assertThat(evidence.target(event).conflicting()).isTrue();
                h.j.update(
                    "update outbox set payload=JSON_OBJECT('bad',true) where event_id=?",
                    event.eventId());
                assertThat(
                        evidence
                            .local("CREDIT_DELIVERY", biz, event.eventType(), event.requestHash())
                            .uncertain())
                    .isTrue();
                return null;
              });
        });
  }

  @Test
  public void excludesHistoricalDeadOnlyWithAuditedPersistentReplay() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          var ds = h.j.getDataSource();
          var evidence = new MySqlDeliveryEvidenceRepository(ds);
          var recovery = new MySqlRecoveryRepository(ds);
          var writer = new MySqlOutboxWriter(ds);
          var event =
              RelayOptimizationTestSupport.event(UUID.randomUUID().toString(), "PrepareDraw");
          h.runner.inSlot(
              RouteSlot.control(),
              WorkClass.TRADE,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                writer.append(event);
                h.j.update("update outbox set state='DEAD' where event_id=?", event.eventId());
                recovery.open(
                    "DRAW",
                    new RecoveryAggregate(
                        event.aggregateId(), "ACCEPTED", RecoveryStatus.REVIEW, 1),
                    RecoveryStatus.REVIEW,
                    "dead");
                recovery.defer("workflow-v2", "DRAW", event);
                assertThat(
                        evidence
                            .local(
                                "DRAW", event.aggregateId(), event.eventType(), event.requestHash())
                            .blocked())
                    .isTrue();
                recovery.close(recovery.active("DRAW", event.aggregateId()), "operator", "resume");
                assertThat(
                        evidence
                            .local(
                                "DRAW", event.aggregateId(), event.eventType(), event.requestHash())
                            .blocked())
                    .isTrue();
                String replayId = UUID.randomUUID().toString();
                recovery.replayed("DRAW", event.aggregateId(), event.eventId(), replayId);
                assertThat(
                        evidence
                            .local(
                                "DRAW", event.aggregateId(), event.eventType(), event.requestHash())
                            .blocked())
                    .isTrue();
                var replay =
                    new EventEnvelope(
                        replayId,
                        event.eventType(),
                        event.aggregateId(),
                        event.bizId(),
                        event.userId(),
                        event.routeSlot(),
                        event.requestHash(),
                        2,
                        event.schemaVersion(),
                        event.payload());
                writer.append(replay);
                var found =
                    evidence.local(
                        "DRAW", event.aggregateId(), event.eventType(), event.requestHash());
                assertThat(found.blocked()).isFalse();
                assertThat(found.localInFlight()).isTrue();
                var dispatch = new MySqlRecoveryDispatchRepository(ds);
                Instant now =
                    h.j
                        .queryForObject("select UTC_TIMESTAMP(6)", java.sql.Timestamp.class)
                        .toInstant();
                dispatch.lock("DRAW", event.aggregateId());
                dispatch.record(
                    "DRAW",
                    event.aggregateId(),
                    "ACCEPTED",
                    1,
                    event,
                    now.minusSeconds(120),
                    Duration.ofSeconds(30));
                h.j.update(
                    "update v2_recovery_dispatch set redrive_count=12 where aggregate_type='DRAW'"
                        + " and aggregate_id=?",
                    event.aggregateId());
                String replayHash =
                    h.j.queryForObject(
                        "select payload_hash from outbox where event_id=?", String.class, replayId);
                h.j.update(
                    "update outbox set payload_hash=? where event_id=?", "b".repeat(64), replayId);
                assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isEqualTo(12);
                h.j.update(
                    "update outbox set payload_hash=? where event_id=?", replayHash, replayId);
                assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isZero();
                dispatch.record(
                    "DRAW",
                    event.aggregateId(),
                    "ACCEPTED",
                    2,
                    replay,
                    now,
                    Duration.ofSeconds(30));
                assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isOne();
                dispatch.record(
                    "DRAW", event.aggregateId(), "NEXT", 3, replay, now, Duration.ofSeconds(30));
                assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isEqualTo(2);
                dispatch.record(
                    "DRAW",
                    event.aggregateId(),
                    "LAST",
                    4,
                    replay,
                    now.plusSeconds(60),
                    Duration.ofSeconds(30),
                    now.plusSeconds(30));
                assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isOne();
                return null;
              });
        });
  }

  @Test
  public void dispatchAndPublishedEvidenceUseUtcWithShanghaiJvm() {
    var previous = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    try {
      AssetTestSupport.run(
          h -> {
            RelayOptimizationTestSupport.schema(h);
            var event =
                RelayOptimizationTestSupport.event(UUID.randomUUID().toString(), "PrepareDraw");
            var dispatch = new MySqlRecoveryDispatchRepository(h.j.getDataSource());
            var evidence = new MySqlDeliveryEvidenceRepository(h.j.getDataSource());
            h.runner.inControl(
                WorkClass.TRADE,
                RelayOptimizationTestSupport.deadline(),
                () -> {
                  new MySqlOutboxWriter(h.j.getDataSource()).append(event);
                  var notification =
                      RelayOptimizationTestSupport.event(
                          UUID.randomUUID().toString(), "ExchangeCompleted");
                  new MySqlConsumerInbox(h.j.getDataSource())
                      .tryStartOperation("notification-v2", notification);
                  assertThat(evidence.target(notification).operationCompleted()).isTrue();
                  Instant now = Instant.parse("2026-10-16T12:00:00Z");
                  dispatch.lock("DRAW", event.aggregateId());
                  dispatch.record(
                      "DRAW",
                      event.aggregateId(),
                      "ACCEPTED",
                      1,
                      event,
                      now,
                      Duration.ofSeconds(30));
                  assertThat(
                          h.j.queryForObject(
                              "select last_redrive_at from v2_recovery_dispatch where"
                                  + " aggregate_id=?",
                              java.time.LocalDateTime.class,
                              event.aggregateId()))
                      .isEqualTo(java.time.LocalDateTime.parse("2026-10-16T12:00:00"));
                  h.j.update(
                      "update v2_recovery_dispatch set last_redrive_at='2026-10-16"
                          + " 12:00:00',next_redrive_at='2026-10-16 12:00:30' where aggregate_id=?",
                      event.aggregateId());
                  assertThat(dispatch.lock("DRAW", event.aggregateId()).lastRedriveAt())
                      .isEqualTo(now);
                  h.j.update(
                      "update outbox set state='PUBLISHED',published_at='2026-10-16 12:00:00' where"
                          + " event_id=?",
                      event.eventId());
                  assertThat(
                          evidence
                              .local(
                                  "DRAW",
                                  event.aggregateId(),
                                  event.eventType(),
                                  event.requestHash())
                              .latestPublishedAt())
                      .isEqualTo(now);
                  var recovery = new MySqlRecoveryRepository(h.j.getDataSource());
                  recovery.open(
                      "DRAW",
                      new RecoveryAggregate(
                          event.aggregateId(), "ACCEPTED", RecoveryStatus.REVIEW, 1),
                      RecoveryStatus.REVIEW,
                      "timezone proof");
                  recovery.defer("workflow-v2", "DRAW", event);
                  recovery.close(
                      recovery.active("DRAW", event.aggregateId()), "operator", "resume timezone");
                  String replayId = UUID.randomUUID().toString();
                  var replay =
                      new EventEnvelope(
                          replayId,
                          event.eventType(),
                          event.aggregateId(),
                          event.bizId(),
                          event.userId(),
                          event.routeSlot(),
                          event.requestHash(),
                          2,
                          1,
                          event.payload());
                  new MySqlOutboxWriter(h.j.getDataSource()).append(replay);
                  recovery.replayed("DRAW", event.aggregateId(), event.eventId(), replayId);
                  h.j.update(
                      "update v2_recovery_dispatch set redrive_count=12 where aggregate_id=?",
                      event.aggregateId());
                  assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isZero();
                  return null;
                });
          });
    } finally {
      TimeZone.setDefault(previous);
    }
  }

  @Test
  public void actualResumeReplaysBeforeCloseAndResetsGuardOnce() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          var ds = h.j.getDataSource();
          var repository = new MySqlRecoveryRepository(ds);
          var dispatch = new MySqlRecoveryDispatchRepository(ds);
          var evidence = new MySqlDeliveryEvidenceRepository(ds);
          var writer = new MySqlOutboxWriter(ds);
          var event =
              RelayOptimizationTestSupport.event(UUID.randomUUID().toString(), "PrepareDraw");
          h.runner.inControl(
              WorkClass.TRADE,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                writer.append(event);
                dispatch.lock("DRAW", event.aggregateId());
                var now =
                    h.j
                        .queryForObject("select UTC_TIMESTAMP(6)", java.time.LocalDateTime.class)
                        .toInstant(java.time.ZoneOffset.UTC);
                dispatch.record(
                    "DRAW",
                    event.aggregateId(),
                    "ACCEPTED",
                    1,
                    event,
                    now.minusSeconds(60),
                    Duration.ofSeconds(30));
                h.j.update(
                    "update v2_recovery_dispatch set redrive_count=12 where aggregate_id=?",
                    event.aggregateId());
                h.j.update("update outbox set state='DEAD' where event_id=?", event.eventId());
                repository.open(
                    "DRAW",
                    new RecoveryAggregate(
                        event.aggregateId(), "ACCEPTED", RecoveryStatus.REVIEW, 1),
                    RecoveryStatus.REVIEW,
                    "real resume");
                repository.defer("workflow-v2", "DRAW", event);
                return null;
              });
          new RecoveryService(
                  h.runner, repository, writer, Map.of(), () -> UUID.randomUUID().toString())
              .resume(
                  RouteSlot.control(), "DRAW", event.aggregateId(), 1, "operator", "actual resume");
          h.runner.inControl(
              WorkClass.TRADE,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                assertThat(
                        evidence
                            .local(
                                "DRAW", event.aggregateId(), event.eventType(), event.requestHash())
                            .blocked())
                    .isFalse();
                assertThat(
                        evidence
                            .local(
                                "DRAW", event.aggregateId(), event.eventType(), event.requestHash())
                            .localInFlight())
                    .isTrue();
                assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isZero();
                var replayId =
                    h.j.queryForObject(
                        "select replayed_event_id from deferred_fact where event_id=?",
                        String.class,
                        event.eventId());
                var replay =
                    new EventEnvelope(
                        replayId,
                        event.eventType(),
                        event.aggregateId(),
                        event.bizId(),
                        event.userId(),
                        event.routeSlot(),
                        event.requestHash(),
                        1,
                        1,
                        event.payload());
                var now =
                    h.j
                        .queryForObject("select UTC_TIMESTAMP(6)", java.time.LocalDateTime.class)
                        .toInstant(java.time.ZoneOffset.UTC);
                dispatch.record(
                    "DRAW",
                    event.aggregateId(),
                    "ACCEPTED",
                    2,
                    replay,
                    now,
                    Duration.ofSeconds(30));
                assertThat(dispatch.lock("DRAW", event.aggregateId()).count()).isOne();
                return null;
              });
        });
  }
}
