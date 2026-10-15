package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.event.V2ConfirmedEventPublisher;
import com.jay.trigger.job.v2.OutboxRelayJob;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.*;

public class MessagingShutdownV2Test {
  @Test
  public void drainWaitsForConfirmedSourceUpdateWithoutHoldingDatabasePermit() {
    AssetTestSupport.run(
        h -> {
          var cf = PublisherConfirmV2Test.connection();
          String ex = "v2.it.drain." + UUID.randomUUID(), q = ex + ".q";
          var admin = new RabbitAdmin(cf);
          admin.declareExchange(new DirectExchange(ex, true, false));
          admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
          admin.declareBinding(
              new Binding(q, Binding.DestinationType.QUEUE, ex, "ExchangeCompleted", null));
          try (var publisher =
                  new V2ConfirmedEventPublisher(
                      new RabbitTemplate(cf), ex, 4, Duration.ofSeconds(3));
              var pool = Executors.newFixedThreadPool(2)) {
            String id = "drain-" + UUID.randomUUID();
            h.runner.inControl(
                WorkClass.RECOVERY,
                new Deadline(Instant.now().plusSeconds(30)),
                () -> {
                  h.j.update("delete from outbox");
                  new MySqlOutboxWriter(h.j.getDataSource())
                      .append(PublisherConfirmV2Test.event(id));
                  return null;
                });
            var confirmed = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var job =
                new OutboxRelayJob(
                    new OutboxRelay(
                        new MySqlOutboxRepository(h.j.getDataSource(), h.runner),
                        publisher,
                        new RetryPolicy(),
                        point -> {
                          confirmed.countDown();
                          try {
                            if (!release.await(10, TimeUnit.SECONDS))
                              throw new IllegalStateException("test release timeout");
                          } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                          }
                        }));
            var work = pool.submit(job::tick);
            assertThat(confirmed.await(10, TimeUnit.SECONDS)).isTrue();
            var stopped = pool.submit(job::stop);
            // A completion is still pending, while all database classes remain available.
            assertThat(
                    h.runner.inControl(
                        WorkClass.RELAY,
                        new Deadline(Instant.now().plusSeconds(3)),
                        () ->
                            h.j.queryForObject(
                                "select state from outbox where event_id=?", String.class, id)))
                .isEqualTo("PROCESSING");
            assertThat(stopped.isDone()).isFalse();
            release.countDown();
            work.get(10, TimeUnit.SECONDS);
            stopped.get(10, TimeUnit.SECONDS);
            assertThat(
                    h.j.queryForObject(
                        "select state from outbox where event_id=?", String.class, id))
                .isEqualTo("PUBLISHED");
            job.tick();
          } finally {
            admin.deleteQueue(q);
            admin.deleteExchange(ex);
            cf.destroy();
          }
        });
  }
}
