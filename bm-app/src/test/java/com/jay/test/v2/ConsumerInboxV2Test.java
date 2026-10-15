package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class ConsumerInboxV2Test {
  static void schema(AssetTestSupport h) throws Exception {
    for (var slot : com.jay.trigger.job.v2.OutboxRelayJob.SOURCES)
      h.runner.inSlot(
          slot,
          WorkClass.RECOVERY,
          new Deadline(Instant.now().plusSeconds(30)),
          () -> {
            try {
              for (String f :
                  slot.dbKey().equals("00")
                      ? List.of("001_outbox_base.sql", "001_id_block.sql")
                      : List.<String>of())
                for (String ddl :
                    java.nio.file.Files.readString(
                            java.nio.file.Path.of("../docs/dev-ops/mysql/migrations/" + f))
                        .split(";")) if (!ddl.isBlank()) h.j.execute(ddl);
              for (String s :
                  java.nio.file.Files.readString(
                          java.nio.file.Path.of(
                              "../docs/dev-ops/mysql/migrations/003_messaging_recovery.sql"))
                      .split(";")) if (!s.isBlank()) h.j.execute(s);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
            return null;
          });
  }

  static EventEnvelope event(AssetTestSupport h, String id, String user, String biz) {
    return new EventEnvelope(
        id,
        "GrantCredit",
        biz,
        new BusinessId("GRANT", biz, "a".repeat(64)),
        user,
        h.codec.routeUser(user),
        "a".repeat(64),
        1,
        1,
        "{\"amount\":\"1.00\"}");
  }

  @Test
  public void atomicallyDeduplicatesTransportAndBusinessAndReplaysOriginalResponse() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          String user = "inbox-" + UUID.randomUUID(), biz = UUID.randomUUID().toString();
          var inbox = new MySqlConsumerInbox(h.j.getDataSource());
          var writer = new MySqlOutboxWriter(h.j.getDataSource());
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  inbox,
                  writer,
                  new EventRouting(h.codec),
                  null,
                  () -> "response-" + UUID.randomUUID());
          var first = event(h, "in-" + UUID.randomUUID(), user, biz);
          java.util.function.Supplier<List<EventEnvelope>> effect =
              () -> {
                h.assets.lockAccounts(user, List.of());
                h.assets.credit(user, first.bizId(), BigDecimal.ONE);
                return List.of(
                    new EventEnvelope(
                        "response-" + UUID.randomUUID(),
                        "CreditGranted",
                        biz,
                        first.bizId(),
                        user,
                        RouteSlot.control(),
                        first.requestHash(),
                        1,
                        1,
                        "{\"balanceAfter\":\"1.00\"}"));
              };
          assertThat(uow.applyWithResponses("workflow-v2", first, effect))
              .isEqualTo(ConsumeOutcome.APPLIED);
          // Ack may be lost: a redelivery observes the committed Inbox and never calls the asset
          // handler.
          assertThat(
                  uow.applyWithResponses(
                      "workflow-v2",
                      first,
                      () -> {
                        throw new AssertionError("duplicate handler");
                      }))
              .isEqualTo(ConsumeOutcome.DUPLICATE);
          var replay = event(h, "recovery-" + UUID.randomUUID(), user, biz);
          assertThat(
                  uow.applyWithResponses(
                      "workflow-v2",
                      replay,
                      () -> {
                        throw new AssertionError("business replay handler");
                      }))
              .isEqualTo(ConsumeOutcome.APPLIED);
          assertThat(h.balance(user)).isEqualByComparingTo("1.00");
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from consumer_inbox where event_id=?",
                            Integer.class,
                            first.eventId()))
                    .isOne();
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from outbox where aggregate_id=?", Integer.class, biz))
                    .isEqualTo(2);
                return null;
              });
          String failingBiz = UUID.randomUUID().toString();
          var failing = event(h, "fail-" + UUID.randomUUID(), user, failingBiz);
          assertThatThrownBy(
                  () ->
                      uow.apply(
                          "workflow-v2",
                          failing,
                          () -> {
                            h.assets.lockAccounts(user, List.of());
                            h.assets.credit(user, failing.bizId(), BigDecimal.ONE);
                            throw new IllegalStateException("wrong stage");
                          }))
              .isInstanceOf(IllegalStateException.class);
          assertThat(h.balance(user)).isEqualByComparingTo("1.00");
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from consumer_inbox where event_id=?",
                            Integer.class,
                            failing.eventId()))
                    .isZero();
                return null;
              });
          var changed =
              new EventEnvelope(
                  first.eventId(),
                  first.eventType(),
                  biz,
                  first.bizId(),
                  user,
                  first.routeSlot(),
                  first.requestHash(),
                  1,
                  1,
                  "{\"amount\":\"2.00\"}");
          assertThatThrownBy(() -> uow.apply("workflow-v2", changed, () -> {}))
              .isInstanceOf(AssetConflictException.class);
        });
  }

  @Test
  public void durableRetryKeepsFailureCountAndFencesLeases() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          String user = "retry-" + UUID.randomUUID();
          var e = event(h, "retry-" + UUID.randomUUID(), user, UUID.randomUUID().toString());
          var jobs = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
          var slot = e.routeSlot();
          h.runner.inSlot(
              slot,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                jobs.schedule(e, 3, Instant.now().minusSeconds(1));
                return null;
              });
          var claimed =
              jobs.claim(slot, 128, Duration.ofSeconds(30)).stream()
                  .filter(x -> x.event().eventId().equals(e.eventId()))
                  .findFirst()
                  .orElseThrow();
          assertThat(jobs.failureCount(claimed)).isEqualTo(3);
          assertThat(jobs.complete(claimed)).isTrue();
          h.runner.inSlot(
              slot,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                jobs.schedule(e, 4, Instant.now().minusSeconds(1));
                jobs.schedule(e, 2, Instant.now().minusSeconds(1));
                return null;
              });
          var next =
              jobs.claim(slot, 128, Duration.ofSeconds(30)).stream()
                  .filter(x -> x.event().eventId().equals(e.eventId()))
                  .findFirst()
                  .orElseThrow();
          assertThat(jobs.failureCount(next)).isEqualTo(4);
          assertThat(jobs.complete(claimed)).isFalse();
        });
  }

  @Test
  public void retryPublicationExhaustionCannotBeBypassedByNewEventId() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          String user = "retry-dead-" + UUID.randomUUID(), biz = UUID.randomUUID().toString();
          var e = event(h, "retry-dead-" + UUID.randomUUID(), user, biz);
          var jobs = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
          h.runner.inSlot(
              e.routeSlot(),
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                jobs.schedule(e, 1, Instant.now().minusSeconds(1));
                h.j.update("update retry_job set attempt=11 where event_id=?", e.eventId());
                return null;
              });
          var lease =
              jobs.claim(e.routeSlot(), 128, Duration.ofSeconds(30)).stream()
                  .filter(l -> l.event().eventId().equals(e.eventId()))
                  .findFirst()
                  .orElseThrow();
          jobs.retry(lease, Instant.now(), "UNKNOWN");
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  () -> "replay-" + UUID.randomUUID());
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  recovery,
                  () -> "reply-" + UUID.randomUUID());
          assertThat(
                  uow.apply(
                      "workflow-v2", event(h, "new-" + UUID.randomUUID(), user, biz), () -> {}))
              .isEqualTo(ConsumeOutcome.DEFERRED);
        });
  }

  @Test
  public void twoInstancesSerializeDifferentEventsForSameBusinessReceipt() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          String user = "parallel-inbox-" + UUID.randomUUID(), biz = UUID.randomUUID().toString();
          var ds = h.j.getDataSource();
          var second =
              new com.jay.infrastructure.transaction.RoutedTransactionRunner(
                  ds,
                  new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),
                  h.codec);
          var runners =
              List.<com.jay.domain.asset.repository.UserTransactionRunner>of(h.runner, second);
          var index = new java.util.concurrent.atomic.AtomicInteger();
          var effects = new java.util.concurrent.atomic.AtomicInteger();
          var outcomes =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  2,
                  () -> {
                    int i = index.getAndIncrement();
                    var e = event(h, "parallel-" + i + "-" + biz, user, biz);
                    var uow =
                        new ConsumerUnitOfWork(
                            runners.get(i),
                            new MySqlConsumerInbox(ds),
                            new MySqlOutboxWriter(ds),
                            new EventRouting(h.codec),
                            null,
                            () -> "reply-" + UUID.randomUUID());
                    return uow.applyWithResponses(
                        "workflow-v2",
                        e,
                        () -> {
                          effects.incrementAndGet();
                          h.assets.lockAccounts(user, List.of());
                          h.assets.credit(user, e.bizId(), BigDecimal.ONE);
                          return List.of(
                              new EventEnvelope(
                                  "parallel-reply-" + UUID.randomUUID(),
                                  "CreditGranted",
                                  biz,
                                  e.bizId(),
                                  user,
                                  RouteSlot.control(),
                                  e.requestHash(),
                                  1,
                                  1,
                                  "{}"));
                        });
                  });
          assertThat(outcomes).containsOnly(ConsumeOutcome.APPLIED);
          assertThat(effects.get()).isOne();
          assertThat(h.balance(user)).isEqualByComparingTo("1.00");
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from outbox where aggregate_id=?", Integer.class, biz))
                    .isEqualTo(2);
                return null;
              });
        });
  }
}
