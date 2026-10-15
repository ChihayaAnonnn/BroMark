package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.trigger.listener.v2.V2MessageListener;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.springframework.dao.OptimisticLockingFailureException;

public class MessagingReviewRegressionV2Test {
  static final class Fixture {
    final AssetTestSupport h;
    final MySqlRetryJobRepository retries;
    final RecoveryService recovery;
    final ConsumerUnitOfWork uow;

    Fixture(AssetTestSupport h) {
      this.h = h;
      retries = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
      recovery =
          new RecoveryService(
              h.runner,
              new MySqlRecoveryRepository(h.j.getDataSource()),
              new MySqlOutboxWriter(h.j.getDataSource()),
              Map.of(),
              () -> "review-replay-" + UUID.randomUUID());
      uow =
          new ConsumerUnitOfWork(
              h.runner,
              new MySqlConsumerInbox(h.j.getDataSource()),
              new MySqlOutboxWriter(h.j.getDataSource()),
              new EventRouting(h.codec),
              recovery,
              () -> "review-reply-" + UUID.randomUUID());
    }

    V2MessageListener listener(EventHandler handler) {
      return new V2MessageListener(
          h.runner,
          uow,
          retries,
          recovery,
          new EventRouting(h.codec),
          raw ->
              JsonStore.read(
                  new String(raw, java.nio.charset.StandardCharsets.UTF_8), EventEnvelope.class),
          new MySqlRawMessageStore(h.j.getDataSource()),
          Map.of("GrantCredit", handler),
          point -> {});
    }
  }

  static byte[] bytes(EventEnvelope e) {
    return JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  @Test
  public void staleCopiesShareOneRetryGenerationAndDoNotSkipBackoff() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var f = new Fixture(h);
          String user = "copy-" + UUID.randomUUID();
          var e =
              ConsumerInboxV2Test.event(
                  h, "copy-" + UUID.randomUUID(), user, UUID.randomUUID().toString());
          var calls = new AtomicInteger();
          var listener =
              f.listener(
                  event -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("temporary");
                  });
          var acks = new AtomicInteger();
          var delivery =
              new V2MessageListener.Delivery() {
                public void ack() {
                  acks.incrementAndGet();
                }

                public void pause() {
                  throw new AssertionError("durable retry exists");
                }
              };
          for (int i = 0; i < 12; i++) listener.handle("workflow-v2", bytes(e), 0, delivery);
          assertThat(acks.get()).isEqualTo(12);
          assertThat(calls.get()).isOne();
          h.runner.inSlot(
              e.routeSlot(),
              WorkClass.QUERY,
              new Deadline(Instant.now().plusSeconds(10)),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select failure_count from retry_job where event_id=?",
                            Integer.class,
                            e.eventId()))
                    .isOne();
                assertThat(
                        h.j.queryForObject(
                            "select state from retry_job where event_id=?",
                            String.class,
                            e.eventId()))
                    .isEqualTo("PENDING");
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from consumer_inbox where event_id=?",
                            Integer.class,
                            e.eventId()))
                    .isZero();
                return null;
              });
        });
  }

  @Test
  public void conflictingDeferredCopyIsQuarantinedWithoutOverwritingFirstFact() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var f = new Fixture(h);
          String user = "conflict-" + UUID.randomUUID(), id = UUID.randomUUID().toString();
          var e = ConsumerInboxV2Test.event(h, "conflict-" + UUID.randomUUID(), user, id);
          f.recovery.pause(
              e.routeSlot(), "CREDIT_DELIVERY", id, RecoveryStatus.REVIEW, "original evidence");
          var listener =
              f.listener(
                  event -> {
                    throw new AssertionError("paused effect");
                  });
          var acks = new AtomicInteger();
          var pauses = new AtomicInteger();
          var delivery =
              new V2MessageListener.Delivery() {
                public void ack() {
                  acks.incrementAndGet();
                }

                public void pause() {
                  pauses.incrementAndGet();
                }
              };
          listener.handle("workflow-v2", bytes(e), 0, delivery);
          var changed =
              new EventEnvelope(
                  e.eventId(),
                  e.eventType(),
                  e.aggregateId(),
                  e.bizId(),
                  e.userId(),
                  e.routeSlot(),
                  e.requestHash(),
                  1,
                  1,
                  "{\"amount\":\"2.00\"}");
          listener.handle("workflow-v2", bytes(changed), 0, delivery);
          assertThat(acks.get()).isEqualTo(2);
          assertThat(pauses.get()).isZero();
          assertThat(
                  h.j.queryForObject(
                      "select count(*) from message_quarantine where body_hash=?",
                      Integer.class,
                      MySqlRawMessageStore.hash(bytes(changed))))
              .isOne();
          h.runner.inSlot(
              e.routeSlot(),
              WorkClass.QUERY,
              new Deadline(Instant.now().plusSeconds(10)),
              () -> {
                assertThat(
                        JsonStore.read(
                                h.j.queryForObject(
                                    "select payload from deferred_fact where"
                                        + " consumer_name='workflow-v2' and event_id=?",
                                    String.class,
                                    e.eventId()),
                                EventEnvelope.class)
                            .payload())
                    .isEqualTo("{\"amount\":\"1.00\"}");
                return null;
              });
        });
  }

  @Test
  public void unboundCaseGenerationRejectsPreviousCycleResume() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var f = new Fixture(h);
          String id = "aba-" + UUID.randomUUID();
          f.recovery.pause(
              RouteSlot.control(), "OUTBOX_TASK", id, RecoveryStatus.REVIEW, "cycle A");
          f.recovery.resume(RouteSlot.control(), "OUTBOX_TASK", id, 1, "operator", "reviewed A");
          f.recovery.pause(
              RouteSlot.control(), "OUTBOX_TASK", id, RecoveryStatus.REVIEW, "cycle B");
          assertThatThrownBy(
                  () ->
                      f.recovery.resume(
                          RouteSlot.control(),
                          "OUTBOX_TASK",
                          id,
                          1,
                          "operator",
                          "late command for A"))
              .isInstanceOf(OptimisticLockingFailureException.class);
        });
  }

  @Test
  public void unboundPauseSerializesWithInFlightConsumer() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var f = new Fixture(h);
          String user = "pause-lock-" + UUID.randomUUID(), id = UUID.randomUUID().toString();
          var e =
              new EventEnvelope(
                  "pause-lock-" + UUID.randomUUID(),
                  "ExchangeCompleted",
                  id,
                  new BusinessId("EX", id, "a".repeat(64)),
                  user,
                  h.codec.routeUser(user),
                  "a".repeat(64),
                  1,
                  1,
                  "{}");
          var entered = new CountDownLatch(1);
          var release = new CountDownLatch(1);
          var pauseEntered = new CountDownLatch(1);
          try (var pool = Executors.newFixedThreadPool(2)) {
            var consuming =
                pool.submit(
                    () ->
                        f.uow.apply(
                            "notification-v2",
                            e,
                            () -> {
                              entered.countDown();
                              try {
                                if (!release.await(10, TimeUnit.SECONDS))
                                  throw new IllegalStateException("release timeout");
                              } catch (InterruptedException x) {
                                throw new IllegalStateException(x);
                              }
                              h.j.update(
                                  "insert into notification_projection"
                                      + " values(?,?,?,?,UTC_TIMESTAMP(6))",
                                  e.eventId(),
                                  e.eventType(),
                                  id,
                                  e.payload());
                            }));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            var pausing =
                pool.submit(
                    () -> {
                      pauseEntered.countDown();
                      f.recovery.pause(
                          e.routeSlot(),
                          "NOTIFICATION",
                          id,
                          RecoveryStatus.REVIEW,
                          "concurrent pause");
                    });
            assertThat(pauseEntered.await(5, TimeUnit.SECONDS)).isTrue();
            try {
              assertThatThrownBy(() -> pausing.get(300, TimeUnit.MILLISECONDS))
                  .isInstanceOf(TimeoutException.class);
            } finally {
              release.countDown();
            }
            consuming.get(5, TimeUnit.SECONDS);
            pausing.get(5, TimeUnit.SECONDS);
            var next =
                new EventEnvelope(
                    "after-pause-" + UUID.randomUUID(),
                    e.eventType(),
                    id,
                    e.bizId(),
                    user,
                    e.routeSlot(),
                    e.requestHash(),
                    1,
                    1,
                    "{}");
            assertThat(
                    f.uow.apply(
                        "notification-v2",
                        next,
                        () -> {
                          throw new AssertionError("paused handler");
                        }))
                .isEqualTo(ConsumeOutcome.DEFERRED);
          } finally {
            release.countDown();
          }
        });
  }

  @Test
  public void unboundPauseSerializesWithRemainingFactScannerAcrossInstances() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          String user = "scan-lock-" + UUID.randomUUID(), id = UUID.randomUUID().toString();
          var slot = h.codec.routeUser(user);
          var repo = new MySqlRecoveryRepository(h.j.getDataSource());
          var entered = new CountDownLatch(1);
          var release = new CountDownLatch(1);
          var armed = new AtomicBoolean();
          var service =
              new RecoveryService(
                  h.runner,
                  repo,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  () -> {
                    if (armed.get()) {
                      entered.countDown();
                      try {
                        if (!release.await(10, TimeUnit.SECONDS))
                          throw new IllegalStateException("release timeout");
                      } catch (InterruptedException x) {
                        throw new IllegalStateException(x);
                      }
                    }
                    return "scan-replay-" + UUID.randomUUID();
                  });
          service.pause(slot, "CREDIT_DELIVERY", id, RecoveryStatus.WAITING_FIX, "repair");
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                for (int i = 0; i < 129; i++)
                  repo.defer(ConsumerInboxV2Test.event(h, "scan-" + i + "-" + id, user, id));
                return null;
              });
          service.resume(slot, "CREDIT_DELIVERY", id, 1, "operator", "fixed");
          armed.set(true);
          var otherRunner =
              new com.jay.infrastructure.transaction.RoutedTransactionRunner(
                  h.j.getDataSource(),
                  new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                      h.j.getDataSource()),
                  h.codec);
          var pauser =
              new RecoveryService(
                  otherRunner,
                  repo,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  () -> "other-" + UUID.randomUUID());
          try (var pool = Executors.newFixedThreadPool(2)) {
            var scanning = pool.submit(() -> service.scan(slot, 8));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            var started = new CountDownLatch(1);
            var pausing =
                pool.submit(
                    () -> {
                      started.countDown();
                      pauser.pause(slot, "CREDIT_DELIVERY", id, RecoveryStatus.REVIEW, "new pause");
                    });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            try {
              assertThatThrownBy(() -> pausing.get(300, TimeUnit.MILLISECONDS))
                  .isInstanceOf(TimeoutException.class);
            } finally {
              release.countDown();
            }
            assertThat(scanning.get(5, TimeUnit.SECONDS)).isOne();
            pausing.get(5, TimeUnit.SECONDS);
          } finally {
            release.countDown();
          }
        });
  }

  @Test
  public void guardUpgradeSeedsGenerationFromExistingHistoricalCases() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var f = new Fixture(h);
          String id = "existing-case-" + UUID.randomUUID();
          h.runner.inControl(
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(10)),
              () -> {
                h.j.update(
                    "insert into"
                        + " recovery_case(case_id,aggregate_type,aggregate_id,business_state,recovery_status,object_version,active_key,evidence,operator_id,resolution_reason,created_at,closed_at)"
                        + " values(?,'OUTBOX_TASK',?,'UNBOUND','REVIEW',7,NULL,'old"
                        + " evidence','operator','old review',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))",
                    UUID.randomUUID().toString(),
                    id);
                return null;
              });
          f.recovery.pause(
              RouteSlot.control(),
              "OUTBOX_TASK",
              id,
              RecoveryStatus.REVIEW,
              "new cycle after upgrade");
          assertThat(
                  h.j.queryForObject(
                      "select object_version from recovery_case where aggregate_id=? and active_key"
                          + " is not null",
                      Long.class,
                      id))
              .isEqualTo(8L);
        });
  }
}
