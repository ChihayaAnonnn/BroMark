package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.transaction.RoutedTransactionRunner;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.Test;

public class RetryTransportRecoveryV2Test {
  private static RetryTransportRecovery recovery(AssetTestSupport h) {
    var ds = h.j.getDataSource();
    var recovery =
        new RecoveryService(
            h.runner,
            new MySqlRecoveryRepository(ds),
            new MySqlOutboxWriter(ds),
            Map.of(),
            h.codec::newEventId);
    return new RetryTransportRecovery(
        h.runner,
        new MySqlRetryJobRepository(ds, h.runner, recovery),
        new MySqlDeliveryEvidenceRepository(ds),
        recovery);
  }

  private static EventEnvelope published(AssetTestSupport h, String user, int attempt) {
    var event =
        ConsumerInboxV2Test.event(
            h, "retry-transport-" + UUID.randomUUID(), user, "order-" + UUID.randomUUID());
    var repo = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
    h.tx(
        user,
        () -> {
          repo.schedule("workflow-v2", event, 4, Instant.now());
          h.j.update(
              "update retry_job set state='PUBLISHED',attempt=?,published_at=UTC_TIMESTAMP(6)-interval"
                  + " 31 second where event_id=?",
              attempt,
              event.eventId());
          return null;
        });
    return event;
  }

  private static <T> T target(AssetTestSupport h, EventEnvelope e, Supplier<T> work) {
    return h.runner.inSlot(
        e.routeSlot(), WorkClass.RECOVERY, RelayOptimizationTestSupport.deadline(), work);
  }

  private static String state(AssetTestSupport h, EventEnvelope e) {
    return target(
        h,
        e,
        () ->
            h.j.queryForObject(
                "select state from retry_job where event_id=?", String.class, e.eventId()));
  }

  @Test
  public void rearmsOriginalRetryWithItsFailureGeneration() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-transport-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 3);
          var repo = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
          var before = h.tx(user, () -> repo.findPublished(event.eventId()).orElseThrow());
          assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isTrue();
          var after = h.tx(user, () -> repo.findPublished(event.eventId()));
          assertThat(after).isEmpty();
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select state from retry_job where event_id=?",
                            String.class,
                            event.eventId()))
                    .isEqualTo("PENDING");
                assertThat(
                        h.j.queryForObject(
                            "select attempt from retry_job where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(before.attempt());
                assertThat(
                        h.j.queryForObject(
                            "select failure_count from retry_job where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(before.failureCount());
                return null;
              });
          assertThat(before.event().eventId()).isEqualTo(event.eventId());
          assertThat(before.event().payload()).isEqualTo(event.payload());
        });
  }

  @Test
  public void respectsThirtySecondWindowReceiptAndAccountPause() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-window-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 2);
          target(
              h,
              event,
              () -> {
                h.j.update(
                    "update retry_job set published_at=UTC_TIMESTAMP(6)-interval 29 second where"
                        + " event_id=?",
                    event.eventId());
                return null;
              });
          assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(state(h, event)).isEqualTo("PUBLISHED");
          target(
              h,
              event,
              () -> {
                h.j.update(
                    "update retry_job set published_at=UTC_TIMESTAMP(6)-interval 31 second where"
                        + " event_id=?",
                    event.eventId());
                var inbox = new MySqlConsumerInbox(h.j.getDataSource());
                assertThat(inbox.tryStartOperation("workflow-v2", event)).isTrue();
                inbox.completeOperation("workflow-v2", event, List.of());
                return null;
              });
          assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(state(h, event)).isEqualTo("PUBLISHED");

          var paused = published(h, user, 2);
          target(
              h,
              paused,
              () -> {
                h.j.update("update user_credit_account set status='REVIEW' where user_id=?", user);
                return null;
              });
          assertThat(recovery(h).inspect(paused.routeSlot(), paused.eventId())).isFalse();
          target(
              h,
              paused,
              () -> {
                h.j.update("update user_credit_account set status='ACTIVE' where user_id=?", user);
                new MySqlRecoveryRepository(h.j.getDataSource())
                    .open(
                        "ASSET_RECONCILIATION",
                        new RecoveryAggregate(user, "ACCOUNT", RecoveryStatus.REVIEW, 1),
                        RecoveryStatus.REVIEW,
                        "account review");
                return null;
              });
          assertThat(recovery(h).inspect(paused.routeSlot(), paused.eventId())).isFalse();
          target(
              h,
              paused,
              () -> {
                var cases = new MySqlRecoveryRepository(h.j.getDataSource());
                cases.close(
                    cases.active("ASSET_RECONCILIATION", user), "operator", "checked account");
                return null;
              });
          assertThat(recovery(h).inspect(paused.routeSlot(), paused.eventId())).isTrue();
        });
  }

  @Test
  public void twoConnectionsFenceOldPublishedGeneration() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-cas-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 3);
          var snapshot =
              target(
                  h,
                  event,
                  () ->
                      new MySqlRetryJobRepository(h.j.getDataSource(), h.runner)
                          .findPublished(event.eventId())
                          .orElseThrow());
          var ds = h.j.getDataSource();
          var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds);
          var gate = new CountDownLatch(1);
          var pool = Executors.newFixedThreadPool(2);
          try {
            var calls = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++) {
              calls.add(
                  pool.submit(
                      () -> {
                        var runner = new RoutedTransactionRunner(ds, manager, h.codec);
                        var repo = new MySqlRetryJobRepository(ds, runner);
                        gate.await();
                        return runner.inSlot(
                            event.routeSlot(),
                            WorkClass.RECOVERY,
                            RelayOptimizationTestSupport.deadline(),
                            () ->
                                repo.rearmPublished(
                                    event.eventId(),
                                    snapshot.attempt(),
                                    snapshot.failureCount(),
                                    snapshot.publishedAt()));
                      }));
            }
            gate.countDown();
            assertThat(calls.get(0).get(10, TimeUnit.SECONDS))
                .isNotEqualTo(calls.get(1).get(10, TimeUnit.SECONDS));
          } finally {
            pool.shutdownNow();
          }
          assertThat(state(h, event)).isEqualTo("PENDING");
          target(
              h,
              event,
              () -> {
                h.j.update(
                    "update retry_job set state='PUBLISHED',published_at=UTC_TIMESTAMP(6)-interval"
                        + " 32 second where event_id=?",
                    event.eventId());
                return null;
              });
          assertThat(
                  target(
                      h,
                      event,
                      () ->
                          new MySqlRetryJobRepository(ds, h.runner)
                              .rearmPublished(
                                  event.eventId(),
                                  snapshot.attempt(),
                                  snapshot.failureCount(),
                                  snapshot.publishedAt())))
              .isFalse();
          target(
              h,
              event,
              () -> {
                h.j.update(
                    "update retry_job set attempt=4,failure_count=5 where event_id=?",
                    event.eventId());
                return null;
              });
          var changed = target(h, event, () -> new MySqlRetryJobRepository(ds, h.runner)
              .findPublished(event.eventId()).orElseThrow());
          assertThat(target(h, event, () -> new MySqlRetryJobRepository(ds, h.runner)
              .rearmPublished(event.eventId(), snapshot.attempt(), snapshot.failureCount(),
                  changed.publishedAt()))).isFalse();
        });
  }

  @Test
  public void fixedUpperWrapFindsLateCommittedLowerKey() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-scan-" + UUID.randomUUID();
          h.tx(user, () -> null);
          h.runner.inUser(
              user,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from retry_job");
                return null;
              });
          var first = published(h, user, 1);
          var last = published(h, user, 1);
          target(
              h,
              first,
              () -> {
                h.j.update(
                    "update retry_job set published_at=UTC_TIMESTAMP(6)-interval 90 second"
                        + " where event_id=?",
                    first.eventId());
                h.j.update(
                    "update retry_job set published_at=UTC_TIMESTAMP(6)-interval 60 second"
                        + " where event_id=?",
                    last.eventId());
                return null;
              });
          var repo = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
          var plan =
              target(
                  h,
                  first,
                  () ->
                      h.j.queryForList(
                          "explain select published_at,event_id from retry_job FORCE"
                              + " INDEX(published_recovery) where state='PUBLISHED' and"
                              + " published_at<=UTC_TIMESTAMP(6) order by published_at,event_id"
                              + " limit 8"));
          assertThat(plan).isNotEmpty();
          assertThat(plan.getFirst().get("key")).isEqualTo("published_recovery");
          var page1 =
              target(h, first, () -> repo.scanPublished(null, repo.databaseNow().minusSeconds(30), 1));
          assertThat(page1.ids()).contains(first.eventId());
          var late = published(h, user, 1);
          target(
              h,
              late,
              () -> {
                h.j.update(
                    "update retry_job set published_at=UTC_TIMESTAMP(6)-interval 100 second"
                        + " where event_id=?",
                    late.eventId());
                return null;
              });
          var page2 =
              target(
                  h,
                  first,
                  () -> repo.scanPublished(page1.next(), repo.databaseNow().minusSeconds(30), 1));
          assertThat(page2.ids()).contains(last.eventId());
          assertThat(page2.exhausted()).isTrue();
          var wrapped =
              target(h, first, () -> repo.scanPublished(null, repo.databaseNow().minusSeconds(30), 1));
          assertThat(wrapped.ids()).contains(late.eventId());
        });
  }

  @Test
  public void twelfthPublicationBecomesAuditedDeadWithoutThirteenthSend() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-dead-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 12);
          target(
              h,
              event,
              () -> {
                h.j.update("update user_credit_account set status='REVIEW' where user_id=?", user);
                return null;
              });
          assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(state(h, event)).isEqualTo("DEAD");
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select attempt from retry_job where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(12);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from recovery_case where aggregate_type='CREDIT_DELIVERY'"
                                + " and aggregate_id=? and recovery_status='RETRY_EXHAUSTED'"
                                + " and active_key is not null",
                            Integer.class,
                            event.aggregateId()))
                    .isEqualTo(1);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where consumer_name='workflow-v2'"
                                + " and event_id=? and aggregate_type='CREDIT_DELIVERY'",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(1);
                return null;
              });
        });
  }

  @Test
  public void completedReceiptAtTwelfthPublicationDoesNotExhaustRetry() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-terminal-receipt-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 12);
          target(
              h,
              event,
              () -> {
                var inbox = new MySqlConsumerInbox(h.j.getDataSource());
                assertThat(inbox.tryStartOperation("workflow-v2", event)).isTrue();
                inbox.completeOperation("workflow-v2", event, List.of());
                return null;
              });
          assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(state(h, event)).isEqualTo("PUBLISHED");
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from recovery_case where aggregate_type='CREDIT_DELIVERY'"
                                + " and aggregate_id=? and active_key is not null",
                            Integer.class,
                            event.aggregateId()))
                    .isZero();
                return null;
              });
        });
  }

  @Test
  public void twelfthPublicationSerializesRealConsumerBeforeDeadDecision() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-terminal-race-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 12);
          var ds = h.j.getDataSource();
          var consumerRunner =
              new RoutedTransactionRunner(
                  ds,
                  new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),
                  h.codec);
          var consumerRecovery =
              new RecoveryService(
                  consumerRunner,
                  new MySqlRecoveryRepository(ds),
                  new MySqlOutboxWriter(ds),
                  Map.of(),
                  h.codec::newEventId);
          var uow =
              new ConsumerUnitOfWork(
                  consumerRunner,
                  new MySqlConsumerInbox(ds),
                  new MySqlOutboxWriter(ds),
                  new EventRouting(h.codec),
                  consumerRecovery,
                  h.codec::newEventId);
          var delegate = new MySqlDeliveryEvidenceRepository(ds);
          var pool = Executors.newSingleThreadExecutor();
          var competing = new AtomicReference<Future<ConsumeOutcome>>();
          try {
            var evidence =
                new com.jay.domain.task.repository.DeliveryEvidenceRepository() {
                  public DeliveryEvidence local(String type, String id, String eventType, String hash) {
                    return delegate.local(type, id, eventType, hash);
                  }

                  public DeliveryEvidence target(EventEnvelope command) {
                    return delegate.target(command);
                  }

                  public boolean targetPaused(EventEnvelope command) {
                    return delegate.targetPaused(command);
                  }

                  public boolean operationCompleted(String consumer, EventEnvelope command) {
                    boolean completed = delegate.operationCompleted(consumer, command);
                    if (!completed) {
                      var started = new CountDownLatch(1);
                      competing.set(
                          pool.submit(
                              () -> {
                                started.countDown();
                                return uow.applyAttempt(
                                    "workflow-v2",
                                    event,
                                    4,
                                    new MySqlRetryJobRepository(ds, consumerRunner),
                                    () -> {
                                      h.assets.lockAccounts(user, List.of());
                                      h.assets.credit(user, event.bizId(), java.math.BigDecimal.ONE);
                                      return List.of();
                                    });
                              }));
                      try {
                        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
                        // 事务仍持有原聚合 guard；消费者不能在无回执观察后抢先提交。
                        assertThatThrownBy(() -> competing.get().get(200, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class);
                      } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                      }
                    }
                    return completed;
                  }
                };
            var service =
                new RetryTransportRecovery(
                    h.runner,
                    new MySqlRetryJobRepository(ds, h.runner),
                    evidence,
                    new RecoveryService(
                        h.runner,
                        new MySqlRecoveryRepository(ds),
                        new MySqlOutboxWriter(ds),
                        Map.of(),
                        h.codec::newEventId));
            assertThat(service.inspect(event.routeSlot(), event.eventId())).isFalse();
            assertThat(state(h, event)).isEqualTo("DEAD");
            assertThat(competing.get().get(10, TimeUnit.SECONDS)).isEqualTo(ConsumeOutcome.DEFERRED);
            assertThat(h.balance(user)).isEqualByComparingTo("0.00");
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            pool.shutdownNow();
          }
        });
  }

  @Test
  public void twelfthDrawPublicationPausesWithDisabledAccountAndKeepsEarlierReview() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-draw-disabled-" + UUID.randomUUID();
          var order =
              DrawCancellationV2Test.accepted(
                  h, user, DrawAcceptanceV2Test.publish(h, null));
          var event =
              h.tx(
                  user,
                  () ->
                      JsonStore.read(
                          h.j.queryForObject(
                              "select payload from outbox where aggregate_id=? and"
                                  + " event_type='PrepareDraw' limit 1",
                              String.class,
                              order.orderId()),
                          EventEnvelope.class));
          var ds = h.j.getDataSource();
          var aggregateRecovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(ds),
                  new MySqlOutboxWriter(ds),
                  Map.of("DRAW", new MySqlDrawRepository(ds)),
                  h.codec::newEventId);
          target(
              h,
              event,
              () -> {
                new MySqlRetryJobRepository(ds, h.runner).schedule("workflow-v2", event, 4, Instant.now());
                h.j.update(
                    "update retry_job set state='PUBLISHED',attempt=12,"
                        + " published_at=UTC_TIMESTAMP(6)-interval 31 second where event_id=?",
                    event.eventId());
                aggregateRecovery.pauseInCurrentTransaction(
                    "DRAW", order.orderId(), RecoveryStatus.REVIEW, "prior draw review");
                h.j.update("update user_credit_account set status='REVIEW' where user_id=?", user);
                return null;
              });
          var transport =
              new RetryTransportRecovery(
                  h.runner,
                  new MySqlRetryJobRepository(ds, h.runner, aggregateRecovery),
                  new MySqlDeliveryEvidenceRepository(ds),
                  aggregateRecovery);
          assertThat(transport.inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(state(h, event)).isEqualTo("DEAD");
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select recovery_status from v2_draw_order_"
                                + event.routeSlot().tableKey()
                                + " where order_id=?",
                            String.class,
                            order.orderId()))
                    .isEqualTo("REVIEW");
                assertThat(
                        h.j.queryForObject(
                            "select recovery_status from recovery_case where aggregate_type='DRAW'"
                                + " and aggregate_id=? and active_key is not null",
                            String.class,
                            order.orderId()))
                    .isEqualTo("REVIEW");
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where event_id=? and"
                                + " aggregate_type='DRAW'",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(1);
                return null;
              });
        });
  }

  @Test
  public void conflictingCommittedReceiptPausesOriginalAggregate() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-receipt-conflict-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 2);
          target(
              h,
              event,
              () -> {
                var inbox = new MySqlConsumerInbox(h.j.getDataSource());
                assertThat(inbox.tryStartOperation("workflow-v2", event)).isTrue();
                h.j.update(
                    "update consumer_receipt set payload_hash=? where consumer_name=? and"
                        + " operation_key=?",
                    "0".repeat(64),
                    "workflow-v2",
                    MySqlConsumerInbox.digest(
                        event.eventType() + ":" + event.bizId().type() + ":" + event.bizId().id()));
                return null;
              });
          assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(state(h, event)).isEqualTo("PUBLISHED");
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from recovery_case where aggregate_type='CREDIT_DELIVERY'"
                                + " and aggregate_id=? and recovery_status='REVIEW' and active_key is"
                                + " not null",
                            Integer.class,
                            event.aggregateId()))
                    .isEqualTo(1);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(1);
                return null;
              });
        });
  }

  @Test
  public void invalidPublishedPayloadHashBecomesAuditableDeadTask() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-invalid-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 2);
          target(
              h,
              event,
              () -> {
                h.j.update(
                    "update retry_job set payload_hash=? where event_id=?",
                    "0".repeat(64),
                    event.eventId());
                return null;
              });
          assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(state(h, event)).isEqualTo("DEAD");
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from recovery_case where aggregate_type='RETRY_TASK'"
                                + " and aggregate_id=? and recovery_status='WAITING_FIX' and"
                                + " active_key is not null",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(1);
                return null;
              });
        });
  }

  @Test
  public void invalidQuarantinesConsumeOnlyEightRepairSlotsPerTick() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-invalid-budget-" + UUID.randomUUID();
          h.tx(user, () -> null);
          for (var slot : List.of(RouteSlot.control(), new RouteSlot("01", "000"), new RouteSlot("02", "000")))
            h.runner.inSlot(
                slot,
                WorkClass.RECOVERY,
                RelayOptimizationTestSupport.deadline(),
                () -> {
                  h.j.update("delete from retry_job");
                  return null;
                });
          var events = new ArrayList<EventEnvelope>();
          for (int i = 0; i < 9; i++) events.add(published(h, user, 2));
          target(
              h,
              events.getFirst(),
              () -> {
                for (var event : events)
                  h.j.update(
                      "update retry_job set payload_hash=?,published_at=UTC_TIMESTAMP(6)-interval"
                          + " 31 second where event_id=?",
                      "0".repeat(64),
                      event.eventId());
                return null;
              });
          recovery(h).tick();
          target(
              h,
              events.getFirst(),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from retry_job where state='DEAD' and event_id in ("
                                + "?,?,?,?,?,?,?,?,?)",
                            Integer.class,
                            events.stream().map(EventEnvelope::eventId).toArray()))
                    .isEqualTo(8);
                return null;
              });
        });
  }

  @Test
  public void quarantineRechecksHealthyNewGenerationOnIndependentConnection() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-invalid-race-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 2);
          target(
              h,
              event,
              () -> {
                h.j.update(
                    "update retry_job set payload_hash=? where event_id=?",
                    "0".repeat(64),
                    event.eventId());
                return null;
              });
          var ds = h.j.getDataSource();
          var base = new MySqlRetryJobRepository(ds, h.runner);
          var separate =
              new RoutedTransactionRunner(
                  ds,
                  new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),
                  h.codec);
          var changed = new java.util.concurrent.atomic.AtomicBoolean();
          var probe =
              (com.jay.domain.task.repository.RetryJobRepository)
                  java.lang.reflect.Proxy.newProxyInstance(
                      getClass().getClassLoader(),
                      new Class<?>[] {com.jay.domain.task.repository.RetryJobRepository.class},
                      (proxy, method, args) -> {
                        try {
                          return method.invoke(base, args);
                        } catch (java.lang.reflect.InvocationTargetException reflected) {
                          var cause = reflected.getCause();
                          if (method.getName().equals("findPublished")
                              && cause instanceof AssetConflictException
                              && changed.compareAndSet(false, true)) {
                            CompletableFuture.runAsync(
                                    () ->
                                        separate.inSlot(
                                            event.routeSlot(),
                                            WorkClass.RECOVERY,
                                            RelayOptimizationTestSupport.deadline(),
                                            () -> {
                                              h.j.update(
                                                  "update retry_job set payload_hash=?,attempt=3,"
                                                      + " published_at=UTC_TIMESTAMP(6)-interval"
                                                      + " 31 second where event_id=?",
                                                  MySqlConsumerInbox.digest(JsonStore.write(event)),
                                                  event.eventId());
                                              return null;
                                            }))
                                .join();
                          }
                          throw cause;
                        }
                      });
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(ds),
                  new MySqlOutboxWriter(ds),
                  Map.of(),
                  h.codec::newEventId);
          var service =
              new RetryTransportRecovery(
                  h.runner, probe, new MySqlDeliveryEvidenceRepository(ds), recovery);
          assertThat(service.inspect(event.routeSlot(), event.eventId())).isFalse();
          assertThat(changed).isTrue();
          assertThat(state(h, event)).isEqualTo("PUBLISHED");
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select attempt from retry_job where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(3);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from recovery_case where aggregate_type='RETRY_TASK'"
                                + " and aggregate_id=? and active_key is not null",
                            Integer.class,
                            event.eventId()))
                    .isZero();
                return null;
              });
        });
  }

  @Test
  public void deadCasLossRollsBackPauseAndDeferredFact() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-rollback-" + UUID.randomUUID();
          h.tx(user, () -> null);
          var event = published(h, user, 12);
          var ds = h.j.getDataSource();
          var base = new MySqlRetryJobRepository(ds, h.runner);
          var lost =
              (com.jay.domain.task.repository.RetryJobRepository)
                  java.lang.reflect.Proxy.newProxyInstance(
                      getClass().getClassLoader(),
                      new Class<?>[] {com.jay.domain.task.repository.RetryJobRepository.class},
                      (proxy, method, args) -> {
                        if (method.getName().equals("deadPublished")) return false;
                        try {
                          return method.invoke(base, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                          throw e.getCause();
                        }
                      });
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(ds),
                  new MySqlOutboxWriter(ds),
                  Map.of(),
                  h.codec::newEventId);
          var service =
              new RetryTransportRecovery(
                  h.runner, lost, new MySqlDeliveryEvidenceRepository(ds), recovery);
          assertThatThrownBy(() -> service.inspect(event.routeSlot(), event.eventId()))
              .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
          assertThat(state(h, event)).isEqualTo("PUBLISHED");
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from recovery_case where aggregate_type='CREDIT_DELIVERY'"
                                + " and aggregate_id=? and active_key is not null",
                            Integer.class,
                            event.aggregateId()))
                    .isZero();
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isZero();
                return null;
              });
        });
  }

  @Test
  public void realRabbitRepublishesOriginalFailureHeaderAndRestartsWindow() throws Exception {
    var cf = PublisherConfirmV2Test.connection();
    var admin = new org.springframework.amqp.rabbit.core.RabbitAdmin(cf);
    String ex = "v2.it.retry-recovery." + UUID.randomUUID(), queue = ex + ".q";
    admin.declareExchange(new org.springframework.amqp.core.DirectExchange(ex, true, false));
    admin.declareQueue(new org.springframework.amqp.core.Queue(queue, true));
    admin.declareBinding(
        new org.springframework.amqp.core.Binding(
            queue,
            org.springframework.amqp.core.Binding.DestinationType.QUEUE,
            ex,
            "GrantCredit",
            null));
    var template = new org.springframework.amqp.rabbit.core.RabbitTemplate(cf);
    try (var publisher =
        new com.jay.infrastructure.event.V2ConfirmedEventPublisher(
            template, ex, 16, Duration.ofSeconds(3))) {
      AssetTestSupport.run(
          h -> {
            RelayOptimizationTestSupport.schema(h);
            String user = "retry-rabbit-" + UUID.randomUUID();
            h.tx(user, () -> null);
            h.runner.inUser(
                user,
                WorkClass.RECOVERY,
                RelayOptimizationTestSupport.deadline(),
                () -> {
                  h.j.update("delete from retry_job");
                  return null;
                });
            var event = published(h, user, 3);
            var before =
                target(
                    h,
                    event,
                    () ->
                        new MySqlRetryJobRepository(h.j.getDataSource(), h.runner)
                            .findPublished(event.eventId())
                            .orElseThrow());
            assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isTrue();
            var repo = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
            assertThat(
                    new BoundedOutboxRelay(
                            repo, publisher, RelayOptimizationTestSupport.policy(), point -> {})
                        .runOnce(event.routeSlot()))
                .isEqualTo(1);
            var received = template.receive(queue, 2000);
            assertThat(received).isNotNull();
            assertThat(received.getMessageProperties().getHeaders().get("x-v2-failure-count"))
                .isEqualTo(4);
            assertThat(
                    JsonStore.read(
                            new String(received.getBody(), java.nio.charset.StandardCharsets.UTF_8),
                            EventEnvelope.class)
                        .eventId())
                .isEqualTo(event.eventId());
            var after = target(h, event, () -> repo.findPublished(event.eventId()).orElseThrow());
            assertThat(after.attempt()).isEqualTo(4);
            assertThat(after.failureCount()).isEqualTo(before.failureCount());
            assertThat(after.publishedAt()).isAfter(before.publishedAt());
            assertThat(recovery(h).inspect(event.routeSlot(), event.eventId())).isFalse();
            var duplicateProperties = new org.springframework.amqp.core.MessageProperties();
            duplicateProperties.setHeader("x-v2-failure-count", 4);
            template.send(
                ex,
                "GrantCredit",
                new org.springframework.amqp.core.Message(received.getBody(), duplicateProperties));
            var duplicate = template.receive(queue, 2000);
            assertThat(duplicate).isNotNull();
            assertThat(duplicate.getMessageProperties().getHeaders().get("x-v2-failure-count"))
                .isEqualTo(4);
            var delivered =
                JsonStore.read(
                    new String(received.getBody(), java.nio.charset.StandardCharsets.UTF_8),
                    EventEnvelope.class);
            var deliveredAgain =
                JsonStore.read(
                    new String(duplicate.getBody(), java.nio.charset.StandardCharsets.UTF_8),
                    EventEnvelope.class);
            var consumerRecovery =
                new RecoveryService(
                    h.runner,
                    new MySqlRecoveryRepository(h.j.getDataSource()),
                    new MySqlOutboxWriter(h.j.getDataSource()),
                    Map.of(),
                    h.codec::newEventId);
            var uow =
                new ConsumerUnitOfWork(
                    h.runner,
                    new MySqlConsumerInbox(h.j.getDataSource()),
                    new MySqlOutboxWriter(h.j.getDataSource()),
                    new EventRouting(h.codec),
                    consumerRecovery,
                    h.codec::newEventId);
            Supplier<List<EventEnvelope>> credit =
                () -> {
                  h.assets.lockAccounts(user, List.of());
                  h.assets.credit(user, delivered.bizId(), java.math.BigDecimal.ONE);
                  return List.of();
                };
            assertThat(uow.applyAttempt("workflow-v2", delivered, 4, repo, credit))
                .isEqualTo(ConsumeOutcome.APPLIED);
            assertThat(uow.applyAttempt("workflow-v2", deliveredAgain, 4, repo, credit))
                .isEqualTo(ConsumeOutcome.DUPLICATE);
            assertThat(h.balance(user)).isEqualByComparingTo("1.00");
          });
    } finally {
      admin.deleteQueue(queue);
      admin.deleteExchange(ex);
      cf.destroy();
    }
  }

  @Test
  public void realConsumerRacesRearmAndDuplicateDeliveryCannotGrantTwice() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-consumer-race-" + UUID.randomUUID();
          h.tx(user, () -> null);
          h.runner.inUser(
              user,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from retry_job");
                return null;
              });
          var event = published(h, user, 3);
          var ds = h.j.getDataSource();
          var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds);
          var consumerRunner = new RoutedTransactionRunner(ds, manager, h.codec);
          var consumerRecovery =
              new RecoveryService(
                  consumerRunner,
                  new MySqlRecoveryRepository(ds),
                  new MySqlOutboxWriter(ds),
                  Map.of(),
                  h.codec::newEventId);
          var consumerRetry = new MySqlRetryJobRepository(ds, consumerRunner);
          var uow =
              new ConsumerUnitOfWork(
                  consumerRunner,
                  new MySqlConsumerInbox(ds),
                  new MySqlOutboxWriter(ds),
                  new EventRouting(h.codec),
                  consumerRecovery,
                  h.codec::newEventId);
          Supplier<List<EventEnvelope>> credit =
              () -> {
                h.assets.lockAccounts(user, List.of());
                h.assets.credit(user, event.bizId(), java.math.BigDecimal.ONE);
                return List.of();
              };
          var started = new CountDownLatch(1);
          var pool = Executors.newSingleThreadExecutor();
          var concurrent = new AtomicReference<Future<ConsumeOutcome>>();
          try {
            var delegate = new MySqlDeliveryEvidenceRepository(ds);
            var evidence =
                new com.jay.domain.task.repository.DeliveryEvidenceRepository() {
                  public DeliveryEvidence local(String type, String id, String eventType, String hash) {
                    return delegate.local(type, id, eventType, hash);
                  }

                  public DeliveryEvidence target(EventEnvelope command) {
                    return delegate.target(command);
                  }

                  public boolean targetPaused(EventEnvelope command) {
                    return delegate.targetPaused(command);
                  }

                  public boolean operationCompleted(String consumer, EventEnvelope command) {
                    boolean done = delegate.operationCompleted(consumer, command);
                    if (!done) {
                      concurrent.set(
                          pool.submit(
                              () -> {
                                started.countDown();
                                return uow.applyAttempt("workflow-v2", event, 4, consumerRetry, credit);
                              }));
                      try {
                        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                      }
                    }
                    return done;
                  }

                };
            var service =
                new RetryTransportRecovery(
                    h.runner,
                    new MySqlRetryJobRepository(ds, h.runner),
                    evidence,
                    new RecoveryService(
                        h.runner,
                        new MySqlRecoveryRepository(ds),
                        new MySqlOutboxWriter(ds),
                        Map.of(),
                        h.codec::newEventId));
            assertThat(service.inspect(event.routeSlot(), event.eventId())).isTrue();
            assertThat(state(h, event)).isEqualTo("PENDING");
            // The concurrent consumer was already in flight but cannot pass the original
            // recovery guard before this CAS commits; it defers until the actual republish.
            assertThat(concurrent.get().get(10, TimeUnit.SECONDS)).isEqualTo(ConsumeOutcome.DEFERRED);
            var claim =
                new MySqlRetryJobRepository(ds, h.runner)
                    .claim(event.routeSlot(), 1, Duration.ofSeconds(30))
                    .getFirst();
            assertThat(claim.attempt()).isEqualTo(4);
            assertThat(uow.applyAttempt("workflow-v2", event, 4, consumerRetry, credit))
                .isEqualTo(ConsumeOutcome.APPLIED);
            assertThat(uow.applyAttempt("workflow-v2", event, 4, consumerRetry, credit))
                .isEqualTo(ConsumeOutcome.DUPLICATE);
            assertThat(h.balance(user)).isEqualByComparingTo("1.00");
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            pool.shutdownNow();
          }
        });
  }

  @Test
  public void accountPauseAfterNonlockingCheckDoesNotGrantCredit() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          String user = "retry-pause-race-" + UUID.randomUUID();
          h.tx(user, () -> null);
          h.runner.inUser(
              user,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from retry_job");
                return null;
              });
          var event = published(h, user, 3);
          var ds = h.j.getDataSource();
          var separate =
              new RoutedTransactionRunner(
                  ds,
                  new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),
                  h.codec);
          var delegate = new MySqlDeliveryEvidenceRepository(ds);
          var evidence =
              new com.jay.domain.task.repository.DeliveryEvidenceRepository() {
                public DeliveryEvidence local(String type, String id, String eventType, String hash) {
                  return delegate.local(type, id, eventType, hash);
                }

                public DeliveryEvidence target(EventEnvelope command) {
                  return delegate.target(command);
                }

                public boolean targetPaused(EventEnvelope command) {
                  assertThat(delegate.targetPaused(command)).isFalse();
                  CompletableFuture.runAsync(
                          () ->
                              separate.inUser(
                                  user,
                                  WorkClass.RECOVERY,
                                  RelayOptimizationTestSupport.deadline(),
                                  () -> {
                                    h.j.update(
                                        "update user_credit_account set status='REVIEW' where"
                                            + " user_id=?",
                                        user);
                                    return null;
                                  }))
                      .join();
                  return false;
                }

                public boolean operationCompleted(String consumer, EventEnvelope command) {
                  return delegate.operationCompleted(consumer, command);
                }
              };
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(ds),
                  new MySqlOutboxWriter(ds),
                  Map.of(),
                  h.codec::newEventId);
          var service =
              new RetryTransportRecovery(
                  h.runner, new MySqlRetryJobRepository(ds, h.runner), evidence, recovery);
          assertThat(service.inspect(event.routeSlot(), event.eventId())).isTrue();
          assertThat(state(h, event)).isEqualTo("PENDING");
          var retries = new MySqlRetryJobRepository(ds, h.runner);
          var claimed = retries.claim(event.routeSlot(), 1, Duration.ofSeconds(30)).getFirst();
          assertThat(claimed.event().eventId()).isEqualTo(event.eventId());
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(ds),
                  new MySqlOutboxWriter(ds),
                  new EventRouting(h.codec),
                  recovery,
                  h.codec::newEventId);
          assertThatThrownBy(
                  () ->
                      uow.applyAttempt(
                          "workflow-v2",
                          event,
                          4,
                          retries,
                          () -> {
                            h.assets.lockAccounts(user, List.of());
                            h.assets.credit(user, event.bizId(), java.math.BigDecimal.ONE);
                            return List.of();
                          }))
              .isInstanceOf(AssetConflictException.class);
          target(
              h,
              event,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select balance from user_credit_account where user_id=?",
                            java.math.BigDecimal.class,
                            user))
                    .isEqualByComparingTo("0.00");
                return null;
              });
        });
  }
}
