package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.trigger.listener.v2.V2MessageListener;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;

public class MessageListenerV2Test {
  @Test
  public void failedRetryHandoffLeavesBrokerDeliveryUnackedAndStopsConsumer() throws Exception {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var cf = PublisherConfirmV2Test.connection();
          try {
            var conn = cf.createConnection();
            var ch = conn.createChannel(false);
            String q = "v2.it.listener." + UUID.randomUUID();
            ch.queueDeclare(q, true, false, false, null);
            var e =
                ConsumerInboxV2Test.event(
                    h,
                    "listener-" + UUID.randomUUID(),
                    "listener-user",
                    UUID.randomUUID().toString());
            byte[] body = JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ch.basicPublish("", q, null, body);
            var delivery = ch.basicGet(q, false);
            assertThat(delivery).isNotNull();
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
            var jobs = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
            var paused = new AtomicBoolean();
            var listener =
                new V2MessageListener(
                    h.runner,
                    uow,
                    jobs,
                    recovery,
                    new EventRouting(h.codec),
                    raw ->
                        JsonStore.read(
                            new String(raw, java.nio.charset.StandardCharsets.UTF_8),
                            EventEnvelope.class),
                    new MySqlRawMessageStore(h.j.getDataSource()),
                    Map.of(
                        "GrantCredit",
                        event -> {
                          throw new IllegalStateException("temporary database failure");
                        }),
                    point -> {
                      if (point.equals("consumer.before-retry-persist"))
                        throw new IllegalStateException("retry storage unavailable");
                    });
            listener.handle(
                "workflow-v2",
                body,
                0,
                new V2MessageListener.Delivery() {
                  public void ack() throws Exception {
                    ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                  }

                  public void pause() {
                    paused.set(true);
                  }
                });
            assertThat(paused.get()).isTrue();
            assertThat(ch.basicGet(q, false))
                .isNull(); // delivery still unacked, not republished/requeued
            ((org.springframework.amqp.rabbit.connection.ChannelProxy) ch)
                .getTargetChannel()
                .close();
            var verify = conn.createChannel(false);
            org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .until(() -> verify.messageCount(q) == 1);
            assertThat(verify.basicGet(q, true)).isNotNull();
            verify.queueDelete(q);
            verify.close();
          } finally {
            cf.destroy();
          }
        });
  }

  @Test
  public void retriesPersistBeforeAckAndExhaustionBlocksNewTransportIdentity() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          String user = "exhaust-" + UUID.randomUUID(), biz = UUID.randomUUID().toString();
          var e = ConsumerInboxV2Test.event(h, "exhaust-" + UUID.randomUUID(), user, biz);
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  () -> "replay-" + UUID.randomUUID());
          var jobs = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  recovery,
                  () -> "reply-" + UUID.randomUUID());
          var listener =
              new V2MessageListener(
                  h.runner,
                  uow,
                  jobs,
                  recovery,
                  new EventRouting(h.codec),
                  raw ->
                      JsonStore.read(
                          new String(raw, java.nio.charset.StandardCharsets.UTF_8),
                          EventEnvelope.class),
                  new MySqlRawMessageStore(h.j.getDataSource()),
                  Map.of(
                      "GrantCredit",
                      event -> {
                        throw new IllegalStateException("temporary failure");
                      }),
                  point -> {});
          var acks = new AtomicInteger();
          var delivery =
              new V2MessageListener.Delivery() {
                public void ack() {
                  h.runner.inSlot(
                      e.routeSlot(),
                      WorkClass.QUERY,
                      new Deadline(Instant.now().plusSeconds(10)),
                      () -> {
                        assertThat(
                                h.j.queryForObject(
                                    "select count(*) from retry_job where event_id=?",
                                    Integer.class,
                                    e.eventId()))
                            .isOne();
                        return null;
                      });
                  acks.incrementAndGet();
                }

                public void pause() {
                  throw new AssertionError("healthy handoff paused");
                }
              };
          listener.handle(
              "workflow-v2",
              JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8),
              0,
              delivery);
          for (int generation = 1; generation < 12; generation++) {
            h.runner.inSlot(
                e.routeSlot(),
                WorkClass.RECOVERY,
                new Deadline(Instant.now().plusSeconds(10)),
                () -> {
                  h.j.update(
                      "update retry_job set next_attempt_at=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND"
                          + " where event_id=?",
                      e.eventId());
                  return null;
                });
            var lease =
                jobs.claim(e.routeSlot(), 128, Duration.ofSeconds(30)).stream()
                    .filter(l -> l.event().eventId().equals(e.eventId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(jobs.failureCount(lease)).isEqualTo(generation);
            listener.handle(
                "workflow-v2",
                JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                generation,
                delivery);
            assertThat(jobs.complete(lease))
                .isFalse(); // consumer may reschedule before publisher completion
          }
          assertThat(acks.get()).isEqualTo(12);
          h.runner.inSlot(
              e.routeSlot(),
              WorkClass.QUERY,
              new Deadline(Instant.now().plusSeconds(10)),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select state from retry_job where event_id=?",
                            String.class,
                            e.eventId()))
                    .isEqualTo("DEAD");
                assertThat(
                        h.j.queryForObject(
                            "select failure_count from retry_job where event_id=?",
                            Integer.class,
                            e.eventId()))
                    .isEqualTo(12);
                return null;
              });
          var late = ConsumerInboxV2Test.event(h, "late-dead-" + UUID.randomUUID(), user, biz);
          assertThat(
                  uow.apply(
                      "workflow-v2",
                      late,
                      () -> {
                        throw new AssertionError("DEAD bypassed with new eventId");
                      }))
              .isEqualTo(ConsumeOutcome.DEFERRED);
        });
  }

  @Test
  public void quarantinesUnknownSchemaWithoutExecutingPayload() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  () -> "reply-" + UUID.randomUUID());
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  recovery,
                  () -> "reply-" + UUID.randomUUID());
          var listener =
              new V2MessageListener(
                  h.runner,
                  uow,
                  new MySqlRetryJobRepository(h.j.getDataSource(), h.runner),
                  recovery,
                  new EventRouting(h.codec),
                  raw ->
                      JsonStore.read(
                          new String(raw, java.nio.charset.StandardCharsets.UTF_8),
                          EventEnvelope.class),
                  new MySqlRawMessageStore(h.j.getDataSource()),
                  Map.of(
                      "GrantCredit",
                      e -> {
                        throw new AssertionError("poison handler");
                      }),
                  point -> {});
          var e =
              ConsumerInboxV2Test.event(
                  h, "poison-" + UUID.randomUUID(), "poison-user", UUID.randomUUID().toString());
          byte[] body =
              JsonStore.write(e)
                  .replace("\"schemaVersion\":1", "\"schemaVersion\":99")
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          var ack = new AtomicBoolean();
          listener.handle(
              "workflow-v2",
              body,
              0,
              new V2MessageListener.Delivery() {
                public void ack() {
                  assertThat(
                          h.j.queryForObject(
                              "select count(*) from message_quarantine where body_hash=?",
                              Integer.class,
                              MySqlRawMessageStore.hash(body)))
                      .isOne();
                  assertThat(
                          h.j.queryForObject(
                              "select count(*) from recovery_case where"
                                  + " aggregate_type='RAW_MESSAGE' and aggregate_id=?",
                              Integer.class,
                              MySqlRawMessageStore.hash(body)))
                      .isOne();
                  ack.set(true);
                }

                public void pause() {
                  throw new AssertionError("quarantine failed");
                }
              });
          assertThat(ack.get()).isTrue();
        });
  }

  @Test
  public void actualClosedDatabasePoolCannotAckOrCreateSuccessfulInbox() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          String user = "db-outage-" + UUID.randomUUID();
          var e =
              ConsumerInboxV2Test.event(
                  h, "db-outage-" + UUID.randomUUID(), user, UUID.randomUUID().toString());
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  () -> "reply-" + UUID.randomUUID());
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  recovery,
                  () -> "reply-" + UUID.randomUUID());
          var listener =
              new V2MessageListener(
                  h.runner,
                  uow,
                  new MySqlRetryJobRepository(h.j.getDataSource(), h.runner),
                  recovery,
                  new EventRouting(h.codec),
                  raw ->
                      JsonStore.read(
                          new String(raw, java.nio.charset.StandardCharsets.UTF_8),
                          EventEnvelope.class),
                  new MySqlRawMessageStore(h.j.getDataSource()),
                  Map.of(
                      "GrantCredit",
                      event -> {
                        throw new AssertionError("database down handler");
                      }),
                  point -> {});
          var dynamic =
              (cn.bugstack.middleware.db.router.dynamic.DynamicDataSource) h.j.getDataSource();
          ((com.zaxxer.hikari.HikariDataSource)
                  dynamic.getResolvedDataSources().get("db" + e.routeSlot().dbKey()))
              .close();
          var ack = new AtomicBoolean();
          var pause = new AtomicBoolean();
          listener.handle(
              "workflow-v2",
              JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8),
              0,
              new V2MessageListener.Delivery() {
                public void ack() {
                  ack.set(true);
                }

                public void pause() {
                  pause.set(true);
                }
              });
          assertThat(ack.get()).isFalse();
          assertThat(pause.get()).isTrue();
        });
  }
}
