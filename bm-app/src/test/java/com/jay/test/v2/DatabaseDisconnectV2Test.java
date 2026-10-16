package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.EventEnvelope;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;

public class DatabaseDisconnectV2Test {
  @Test
  public void mysqlKillBeforeCommitCannotPublishUncommittedAssetsOrOutbox() {
    AssetTestSupport.run(
        h -> {
          String user = "kill-db-" + UUID.randomUUID();
          var ready = new CountDownLatch(1);
          var killed = new CountDownLatch(1);
          var connection = new java.util.concurrent.atomic.AtomicLong();
          try (var worker = Executors.newSingleThreadExecutor()) {
            var result =
                worker.submit(
                    () ->
                        h.tx(
                            user,
                            () -> {
                              h.assets.credit(
                                  user,
                                  new BusinessId("FAULT", user, "a".repeat(64)),
                                  new BigDecimal("12.00"));
                              new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                                      h.j.getDataSource())
                                  .append(
                                      new EventEnvelope(
                                          "kill-event-" + user,
                                          "ExchangeCompleted",
                                          user,
                                          new BusinessId("FAULT", user, "a".repeat(64)),
                                          user,
                                          h.codec.routeUser(user),
                                          "a".repeat(64),
                                          1,
                                          1,
                                          "{}"));
                              connection.set(
                                  h.j.queryForObject("SELECT CONNECTION_ID()", Long.class));
                              ready.countDown();
                              try {
                                if (!killed.await(10, TimeUnit.SECONDS))
                                  throw new IllegalStateException("kill barrier timeout");
                              } catch (InterruptedException e) {
                                throw new IllegalStateException(e);
                              }
                              h.j.queryForObject("SELECT 1", Integer.class);
                              return null;
                            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            h.runner.inControl(
                WorkClass.RECOVERY,
                new Deadline(Instant.now().plusSeconds(10)),
                () -> {
                  h.j.execute("KILL CONNECTION " + connection.get());
                  return null;
                });
            killed.countDown();
            assertThatThrownBy(() -> result.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
            assertThat(h.balance(user)).isEqualByComparingTo("0.00");
            h.tx(
                user,
                () -> {
                  assertThat(
                          h.j.queryForObject(
                              "SELECT COUNT(*) FROM outbox WHERE event_id=?",
                              Long.class,
                              "kill-event-" + user))
                      .isZero();
                  return null;
                });
          } finally {
            killed.countDown();
          }
        });
  }
}
