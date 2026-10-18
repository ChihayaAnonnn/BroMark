package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.Application;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class MessagingApplicationV2Test {
  @Test
  public void bootsDedicatedTopologyAndCommitsNotificationBeforeAck() throws Exception {
    bootsMode("legacy");
  }

  @Test
  public void boundedModeOwnsExactlyTwoRelayThreadsAndSharedWakeWriter() throws Exception {
    bootsMode("bounded");
  }

  @Test
  public void sharedWriterRemainsUsableWhenMessagingIsDisabled() throws Exception {
    bootsMode("disabled");
  }

  private void bootsMode(String mode) throws Exception {
    AssetTestSupport.run(h -> ConsumerInboxV2Test.schema(h));
    var settings = new HashMap<String, Object>();
    settings.put("spring.profiles.active", "v2-it");
    settings.put("bm.v2.enabled", true);
    settings.put("bm.v2.asset-write-mode", "V2");
    settings.put("bm.v2.messaging.enabled", !mode.equals("disabled"));
    settings.put("bm.v2.messaging.relay.mode", mode.equals("disabled") ? "legacy" : mode);
    settings.put("bm.v2.messaging.prefix", "bromark.it.msg." + UUID.randomUUID());
    for (String key :
        List.of("user-rps", "activity-rps", "max-in-flight", "query-rps", "query-max-in-flight"))
      settings.put("bm.v2." + key, 20);
    settings.put("bm.v2.local-fallback-rps", 1);
    settings.put("bm.v2.auth.issuer", "https://issuer.test");
    settings.put("bm.v2.auth.audience", "bromark");
    settings.put(
        "bm.v2.auth.public-key",
        Base64.getEncoder().encodeToString(V2IdentityV2Test.publicBytes()));
    try (var context =
        new org.springframework.boot.builder.SpringApplicationBuilder(Application.class)
            .profiles("v2-it")
            .run(
                settings.entrySet().stream()
                    .map(e -> "--" + e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new))) {
      var relayThreads =
          (java.util.concurrent.ThreadPoolExecutor) context.getBean("v2RelayExecutor");
      assertThat(relayThreads.getActiveCount()).isEqualTo(mode.equals("bounded") ? 2 : 0);
      var tx = context.getBean(UserTransactionRunner.class);
      var j = new JdbcTemplate(context.getBean(javax.sql.DataSource.class));
      String user = "notification-" + UUID.randomUUID(), id = "notify-" + UUID.randomUUID();
      var codec = context.getBean(com.jay.domain.asset.service.RouteCodec.class);
      var slot = codec.routeUser(user);
      var e =
          new EventEnvelope(
              id,
              "ExchangeCompleted",
              id,
              new BusinessId("EX", id, "a".repeat(64)),
              user,
              slot,
              "a".repeat(64),
              1,
              1,
              "{\"state\":\"DELIVERED\"}");
      tx.inSlot(
          slot,
          WorkClass.TRADE,
          new Deadline(Instant.now().plusSeconds(30)),
          () -> {
            context.getBean(MySqlOutboxWriter.class).append(e);
            return null;
          });
      if (mode.equals("disabled")) {
        assertThat(context.getBeansOfType(com.jay.config.V2RabbitConfig.MessagingWorkers.class))
            .isEmpty();
        tx.inSlot(
            slot,
            WorkClass.QUERY,
            new Deadline(Instant.now().plusSeconds(5)),
            () -> {
              assertThat(
                      j.queryForObject(
                          "select state from outbox where event_id=?", String.class, id))
                  .isEqualTo("PENDING");
              return null;
            });
        return;
      }
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () ->
                  tx.inSlot(
                      slot,
                      WorkClass.QUERY,
                      new Deadline(Instant.now().plusSeconds(5)),
                      () -> {
                        assertThat(
                                j.queryForObject(
                                    "select state from outbox where event_id=?", String.class, id))
                            .isEqualTo("PUBLISHED");
                        assertThat(
                                j.queryForObject(
                                    "select count(*) from notification_projection where event_id=?",
                                    Integer.class,
                                    id))
                            .isOne();
                        return null;
                      }));
      var registry =
          context.getBean(
              org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry.class);
      assertThat(registry.getListenerContainer("p0-reserve").isRunning()).isTrue();
      assertThat(registry.getListenerContainer("p0-deliver").isRunning()).isTrue();
      assertThat(registry.getListenerContainer("p0-notify").isRunning()).isTrue();
    }
  }

  @Test
  public void applicationCloseKeepsDatabaseOpenUntilConfirmedOutboxUpdateDrains() throws Exception {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          for (var slot : com.jay.trigger.job.v2.OutboxRelayJob.SOURCES)
            h.runner.inSlot(
                slot,
                WorkClass.RECOVERY,
                new Deadline(Instant.now().plusSeconds(30)),
                () -> {
                  h.j.update("delete from outbox");
                  h.j.update("delete from retry_job");
                  return null;
                });
        });
    var settings = new HashMap<String, Object>();
    settings.put("spring.profiles.active", "v2-it");
    settings.put("bm.v2.enabled", true);
    settings.put("bm.v2.asset-write-mode", "V2");
    settings.put("bm.v2.messaging.enabled", true);
    settings.put("bm.v2.messaging.prefix", "bromark.it.msg." + UUID.randomUUID());
    for (String key :
        List.of("user-rps", "activity-rps", "max-in-flight", "query-rps", "query-max-in-flight"))
      settings.put("bm.v2." + key, 20);
    settings.put("bm.v2.local-fallback-rps", 1);
    settings.put("bm.v2.auth.issuer", "https://issuer.test");
    settings.put("bm.v2.auth.audience", "bromark");
    settings.put(
        "bm.v2.auth.public-key",
        Base64.getEncoder().encodeToString(V2IdentityV2Test.publicBytes()));
    var confirmed = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var armed = new java.util.concurrent.atomic.AtomicBoolean();
    var context =
        new org.springframework.boot.builder.SpringApplicationBuilder(Application.class)
            .profiles("v2-it")
            .initializers(
                c ->
                    ((org.springframework.context.support.GenericApplicationContext) c)
                        .registerBean(
                            "messageDrainProbe",
                            com.jay.domain.asset.service.FaultInjector.class,
                            () ->
                                point -> {
                                  if (armed.get() && point.equals("relay.after-confirm")) {
                                    confirmed.countDown();
                                    try {
                                      if (!release.await(20, java.util.concurrent.TimeUnit.SECONDS))
                                        throw new IllegalStateException("probe timeout");
                                    } catch (InterruptedException e) {
                                      Thread.currentThread().interrupt();
                                      throw new IllegalStateException(e);
                                    }
                                  }
                                },
                            bd -> bd.setPrimary(true)))
            .run(
                settings.entrySet().stream()
                    .map(e -> "--" + e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new));
    String id = "close-confirm-" + UUID.randomUUID();
    try (var closer = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var tx = context.getBean(UserTransactionRunner.class);
      var j = new JdbcTemplate(context.getBean(javax.sql.DataSource.class));
      var workers = context.getBean(com.jay.config.V2RabbitConfig.MessagingWorkers.class);
      armed.set(true);
      tx.inControl(
          WorkClass.TRADE,
          new Deadline(Instant.now().plusSeconds(30)),
          () -> {
            new MySqlOutboxWriter(j.getDataSource()).append(PublisherConfirmV2Test.event(id));
            return null;
          });
      assertThat(confirmed.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      var closed = closer.submit(context::close);
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> !workers.isRunning());
      assertThat(
              tx.inControl(
                  WorkClass.RELAY,
                  new Deadline(Instant.now().plusSeconds(3)),
                  () ->
                      j.queryForObject(
                          "select state from outbox where event_id=?", String.class, id)))
          .isEqualTo("PROCESSING");
      assertThat(closed.isDone()).isFalse();
      release.countDown();
      closed.get(15, java.util.concurrent.TimeUnit.SECONDS);
    } finally {
      release.countDown();
      context.close();
    }
    AssetTestSupport.run(
        h ->
            assertThat(
                    h.j.queryForObject(
                        "select state from outbox where event_id=?", String.class, id))
                .isEqualTo("PUBLISHED"));
  }
}
