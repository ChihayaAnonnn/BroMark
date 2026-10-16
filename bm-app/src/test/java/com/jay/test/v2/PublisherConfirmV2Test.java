package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.event.V2ConfirmedEventPublisher;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.*;

public class PublisherConfirmV2Test {
  static CachingConnectionFactory connection() {
    var c =
        new CachingConnectionFactory(
            System.getenv().getOrDefault("V2_RABBIT_HOST", "host.docker.internal"),
            Integer.parseInt(System.getenv().getOrDefault("V2_RABBIT_PORT", "15686")));
    c.setUsername("bromark_it");
    c.setPassword(Objects.requireNonNull(System.getenv("V2_RABBIT_PASSWORD")));
    c.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    c.setPublisherReturns(true);
    c.setConnectionTimeout(1000);
    return c;
  }

  static EventEnvelope event(String id) {
    return new EventEnvelope(
        id,
        "ExchangeCompleted",
        "order",
        new BusinessId("EX", id, "a".repeat(64)),
        "owner",
        new RouteSlot("01", "000"),
        "a".repeat(64),
        1,
        1,
        "{}");
  }

  @Test
  public void requiresConfirmWithoutReturnAndRetriesUnknownCommit() throws Exception {
    var cf = connection();
    try {
      var admin = new RabbitAdmin(cf);
      String ex = "v2.it." + UUID.randomUUID(), q = ex + ".q";
      admin.declareExchange(new DirectExchange(ex, true, false));
      admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
      admin.declareBinding(
          new Binding(q, Binding.DestinationType.QUEUE, ex, "ExchangeCompleted", null));
      var template = new RabbitTemplate(cf);
      template.setMandatory(true);
      try (var publisher = new V2ConfirmedEventPublisher(template, ex, 4, Duration.ofSeconds(2))) {
        assertThat(
                publisher
                    .publish(event("ok"), new Deadline(Instant.now().plusSeconds(3)))
                    .toCompletableFuture()
                    .get(4, TimeUnit.SECONDS))
            .isEqualTo(PublishOutcome.CONFIRMED);
        var wrong =
            new EventEnvelope(
                "bad",
                "NoBinding",
                "order",
                new BusinessId("EX", "bad", "a".repeat(64)),
                null,
                RouteSlot.control(),
                "a".repeat(64),
                1,
                1,
                "{}");
        assertThat(
                publisher
                    .publish(wrong, new Deadline(Instant.now().plusSeconds(3)))
                    .toCompletableFuture()
                    .get(4, TimeUnit.SECONDS))
            .isEqualTo(PublishOutcome.RETURNED);
        AssetTestSupport.run(
            h -> {
              var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
              var writer = new MySqlOutboxWriter(h.j.getDataSource());
              h.runner.inControl(
                  WorkClass.RECOVERY,
                  new Deadline(Instant.now().plusSeconds(30)),
                  () -> {
                    h.j.update("delete from outbox");
                    writer.append(event("confirm-" + UUID.randomUUID()));
                    writer.append(wrong);
                    return null;
                  });
              var relay =
                  new OutboxRelay(
                      repo,
                      publisher,
                      new RetryPolicy(),
                      point -> {
                        if (point.equals("relay.after-confirm"))
                          throw new IllegalStateException("crash before completion");
                      });
              assertThatThrownBy(() -> relay.runOnce(RouteSlot.control(), 2))
                  .isInstanceOf(IllegalStateException.class);
              h.j.update(
                  "update outbox set lease_until=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND where"
                      + " state='PROCESSING'");
              new OutboxRelay(repo, publisher, new RetryPolicy(), point -> {})
                  .runOnce(RouteSlot.control(), 2);
              assertThat(
                      h.j.queryForObject(
                          "select state from outbox where event_id='bad'", String.class))
                  .isEqualTo("PENDING");
              assertThat(
                      h.j.queryForObject(
                          "select count(*) from outbox where state='PUBLISHED'", Integer.class))
                  .isOne();
              assertThat(admin.getQueueInfo(q).getMessageCount()).isGreaterThanOrEqualTo(3);
            });
        assertThat(
                publisher
                    .publish(event("expired"), new Deadline(Instant.now().minusSeconds(1)))
                    .toCompletableFuture()
                    .join())
            .isEqualTo(PublishOutcome.UNKNOWN);
      } finally {
        admin.deleteQueue(q);
        admin.deleteExchange(ex);
      }
    } finally {
      cf.destroy();
    }
  }

  @Test
  public void lostConfirmConnectionIsUnknownEvenWhenBrokerAcceptedMessage() throws Exception {
    var adminCf = connection();
    var admin = new RabbitAdmin(adminCf);
    String ex = "v2.it.unknown." + UUID.randomUUID(), q = ex + ".q";
    admin.declareExchange(new DirectExchange(ex, true, false));
    admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
    admin.declareBinding(
        new Binding(q, Binding.DestinationType.QUEUE, ex, "ExchangeCompleted", null));
    try (var proxy =
        new com.jay.test.v2.support.ConfirmDropProxy(adminCf.getHost(), adminCf.getPort())) {
      var cf = connection();
      cf.setHost("127.0.0.1");
      cf.setPort(proxy.port());
      try (var publisher =
          new V2ConfirmedEventPublisher(new RabbitTemplate(cf), ex, 2, Duration.ofSeconds(2))) {
        assertThat(
                publisher
                    .publish(
                        event("warmup-" + UUID.randomUUID()),
                        new Deadline(Instant.now().plusSeconds(3)))
                    .toCompletableFuture()
                    .get(4, TimeUnit.SECONDS))
            .isEqualTo(PublishOutcome.CONFIRMED);
        admin.purgeQueue(q, false);
        proxy.arm();
        var outcome =
            publisher
                .publish(
                    event("unknown-" + UUID.randomUUID()),
                    new Deadline(Instant.now().plusSeconds(3)))
                .toCompletableFuture();
        assertThat(proxy.dropped.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(admin.getQueueInfo(q).getMessageCount()).isEqualTo(1);
        assertThat(outcome.get(4, TimeUnit.SECONDS)).isEqualTo(PublishOutcome.UNKNOWN);
      } finally {
        cf.destroy();
      }
    } finally {
      admin.deleteQueue(q);
      admin.deleteExchange(ex);
      adminCf.destroy();
    }
  }

  @Test
  public void timeoutRetainsPhysicalSlotUntilLateRealSendAndConfirmFinish() throws Exception {
    var cf = connection();
    var admin = new RabbitAdmin(cf);
    String ex = "v2.it.late." + UUID.randomUUID(), q = ex + ".q";
    admin.declareExchange(new DirectExchange(ex, true, false));
    admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
    admin.declareBinding(
        new Binding(q, Binding.DestinationType.QUEUE, ex, "ExchangeCompleted", null));
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var template =
        new RabbitTemplate(cf) {
          @Override
          public void send(
              String exchange,
              String key,
              Message message,
              org.springframework.amqp.rabbit.connection.CorrelationData data) {
            calls.incrementAndGet();
            entered.countDown();
            try {
              if (!release.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException("test send not released");
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(e);
            }
            super.send(exchange, key, message, data);
          }
        };
    try (var publisher = new V2ConfirmedEventPublisher(template, ex, 1, Duration.ofMillis(100))) {
      var first =
          publisher.publish(
              event("late-" + UUID.randomUUID()), new Deadline(Instant.now().plusSeconds(2)));
      assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(first.toCompletableFuture().get(2, TimeUnit.SECONDS))
          .isEqualTo(PublishOutcome.UNKNOWN);
      assertThat(publisher.isAttemptComplete(first)).isFalse();
      assertThat(
              publisher
                  .publish(
                      event("rejected-" + UUID.randomUUID()),
                      new Deadline(Instant.now().plusSeconds(2)))
                  .toCompletableFuture()
                  .join())
          .isEqualTo(PublishOutcome.UNKNOWN);
      assertThat(calls).hasValue(1);
      release.countDown();
      long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
      while (!publisher.isAttemptComplete(first) && System.nanoTime() < end) Thread.sleep(10);
      assertThat(publisher.isAttemptComplete(first)).isTrue();
      assertThat(admin.getQueueInfo(q).getMessageCount()).isEqualTo(1);
      assertThat(first.toCompletableFuture().join()).isEqualTo(PublishOutcome.UNKNOWN);
      assertThat(
              publisher
                  .publish(
                      event("after-" + UUID.randomUUID()),
                      new Deadline(Instant.now().plusSeconds(2)))
                  .toCompletableFuture()
                  .get(2, TimeUnit.SECONDS))
          .isEqualTo(PublishOutcome.CONFIRMED);
    } finally {
      release.countDown();
      admin.deleteQueue(q);
      admin.deleteExchange(ex);
      cf.destroy();
    }
  }

  @Test
  public void publisherRejectsCallsInsideDatabaseTransactions() throws Exception {
    var cf = connection();
    try (var publisher =
        new V2ConfirmedEventPublisher(
            new RabbitTemplate(cf), "unused", 16, Duration.ofSeconds(2))) {
      org.springframework.transaction.support.TransactionSynchronizationManager
          .setActualTransactionActive(true);
      try {
        assertThatThrownBy(
                () ->
                    publisher.publish(
                        event("transaction"), new Deadline(Instant.now().plusSeconds(2))))
            .isInstanceOf(IllegalStateException.class);
      } finally {
        org.springframework.transaction.support.TransactionSynchronizationManager
            .setActualTransactionActive(false);
      }
    } finally {
      cf.destroy();
    }
  }

  @Test
  public void confirmBeforeSendReturnsStillRetainsPhysicalSlot() throws Exception {
    var cf = connection();
    var admin = new RabbitAdmin(cf);
    String ex = "v2.it.confirm-before-exit." + UUID.randomUUID(), q = ex + ".q";
    admin.declareExchange(new DirectExchange(ex, true, false));
    admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
    admin.declareBinding(
        new Binding(q, Binding.DestinationType.QUEUE, ex, "ExchangeCompleted", null));
    var confirmed = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var template =
        new RabbitTemplate(cf) {
          @Override
          public void send(
              String exchange,
              String key,
              Message message,
              org.springframework.amqp.rabbit.connection.CorrelationData cd) {
            super.send(exchange, key, message, cd);
            try {
              assertThat(cd.getFuture().get(2, TimeUnit.SECONDS).isAck()).isTrue();
              confirmed.countDown();
              if (!release.await(4, TimeUnit.SECONDS))
                throw new IllegalStateException("test did not release send exit");
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          }
        };
    try (var publisher = new V2ConfirmedEventPublisher(template, ex, 1, Duration.ofSeconds(1))) {
      var first =
          publisher.publish(
              event("confirm-before-exit"), new Deadline(Instant.now().plusSeconds(3)));
      assertThat(confirmed.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(first.toCompletableFuture().isDone()).isFalse();
      assertThat(publisher.isAttemptComplete(first)).isFalse();
      assertThat(first.toCompletableFuture().get(2, TimeUnit.SECONDS))
          .isEqualTo(PublishOutcome.UNKNOWN);
      assertThat(
              publisher
                  .publish(event("overflow"), new Deadline(Instant.now().plusSeconds(2)))
                  .toCompletableFuture()
                  .join())
          .isEqualTo(PublishOutcome.UNKNOWN);
      release.countDown();
      long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
      while (!publisher.isAttemptComplete(first) && System.nanoTime() < end) Thread.sleep(10);
      assertThat(publisher.isAttemptComplete(first)).isTrue();
      assertThat(admin.getQueueInfo(q).getMessageCount()).isEqualTo(1);
    } finally {
      release.countDown();
      admin.deleteQueue(q);
      admin.deleteExchange(ex);
      cf.destroy();
    }
  }

  @Test
  public void sendExceptionAfterRunnableExitRestoresLocalCapacity() throws Exception {
    var cf = connection();
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var template =
        new RabbitTemplate(cf) {
          @Override
          public void send(
              String ex,
              String key,
              Message message,
              org.springframework.amqp.rabbit.connection.CorrelationData cd) {
            calls.incrementAndGet();
            throw new org.springframework.amqp.AmqpException(
                "cannot establish whether transport accepted publish");
          }
        };
    try (var publisher =
        new V2ConfirmedEventPublisher(template, "unused", 1, Duration.ofMillis(100))) {
      var first =
          publisher.publish(event("uncertain-send"), new Deadline(Instant.now().plusSeconds(1)));
      assertThat(first.toCompletableFuture().get(1, TimeUnit.SECONDS))
          .isEqualTo(PublishOutcome.UNKNOWN);
      long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
      while (!publisher.isAttemptComplete(first) && System.nanoTime() < end) Thread.sleep(10);
      assertThat(publisher.isAttemptComplete(first)).isTrue();
      assertThat(
              publisher
                  .publish(event("overflow-send"), new Deadline(Instant.now().plusSeconds(1)))
                  .toCompletableFuture()
                  .join())
          .isEqualTo(PublishOutcome.UNKNOWN);
      assertThat(calls).hasValue(2);
    } finally {
      cf.destroy();
    }
  }

  @Test
  public void realConnectionRefusalThenRecoveryReusesSamePublisherWithoutRestart()
      throws Exception {
    var adminCf = connection();
    var admin = new RabbitAdmin(adminCf);
    String ex = "v2.it.reconnect." + UUID.randomUUID(), q = ex + ".q";
    admin.declareExchange(new DirectExchange(ex, true, false));
    admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
    admin.declareBinding(
        new Binding(q, Binding.DestinationType.QUEUE, ex, "ExchangeCompleted", null));
    int refusedPort;
    try (var unused = new java.net.ServerSocket(0)) {
      refusedPort = unused.getLocalPort();
    }
    var cf = connection();
    cf.setHost("127.0.0.1");
    cf.setPort(refusedPort);
    try (var publisher =
        new V2ConfirmedEventPublisher(new RabbitTemplate(cf), ex, 1, Duration.ofSeconds(2))) {
      var refused =
          publisher.publish(
              event("connect-refused-" + UUID.randomUUID()),
              new Deadline(Instant.now().plusSeconds(3)));
      assertThat(refused.toCompletableFuture().get(3, TimeUnit.SECONDS))
          .isEqualTo(PublishOutcome.UNKNOWN);
      long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
      while (!publisher.isAttemptComplete(refused) && System.nanoTime() < end) Thread.sleep(10);
      assertThat(publisher.isAttemptComplete(refused)).isTrue();
      cf.setHost(adminCf.getHost());
      cf.setPort(adminCf.getPort());
      cf.resetConnection();
      assertThat(
              publisher
                  .publish(
                      event("after-reconnect-" + UUID.randomUUID()),
                      new Deadline(Instant.now().plusSeconds(3)))
                  .toCompletableFuture()
                  .get(3, TimeUnit.SECONDS))
          .isEqualTo(PublishOutcome.CONFIRMED);
      assertThat(admin.getQueueInfo(q).getMessageCount()).isEqualTo(1);
    } finally {
      cf.destroy();
      admin.deleteQueue(q);
      admin.deleteExchange(ex);
      adminCf.destroy();
    }
  }
}
