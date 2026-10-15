package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class OutboxLeaseV2Test {
  @Test
  public void claimsEachSourceOnceAndFencesExpiredOwner() {
    AssetTestSupport.run(
        h -> {
          var slot = RouteSlot.control();
          var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
          var writer = new MySqlOutboxWriter(h.j.getDataSource());
          String prefix = "lease-" + UUID.randomUUID();
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                h.j.update("delete from outbox");
                for (int i = 0; i < 20; i++)
                  writer.append(
                      new EventEnvelope(
                          prefix + i,
                          i == 19 ? "SignInCompleted" : "ExchangeCompleted",
                          "order",
                          new BusinessId("EX", "biz", "a".repeat(64)),
                          "u",
                          new RouteSlot("02", "003"),
                          "a".repeat(64),
                          1,
                          1,
                          "{}"));
                return null;
              });
          var claims =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  2, () -> repo.claim(slot, 10, Duration.ofSeconds(30)));
          var all = claims.stream().flatMap(List::stream).toList();
          assertThat(all).hasSize(20);
          assertThat(all.stream().map(x -> x.event().eventId()).distinct().count()).isEqualTo(20);
          assertThat(all.stream().anyMatch(x -> x.event().eventType().equals("SignInCompleted")))
              .isTrue();
          var old = all.getFirst();
          assertThat(old.ownerSlot()).isEqualTo(slot);
          assertThat(old.event().routeSlot()).isEqualTo(new RouteSlot("02", "003"));
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                h.j.update(
                    "update outbox set lease_until=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND where"
                        + " event_id=?",
                    old.event().eventId());
                return null;
              });
          var current = repo.claim(slot, 1, Duration.ofSeconds(30)).getFirst();
          assertThat(current.attempt()).isEqualTo(2);
          assertThat(repo.complete(old)).isFalse();
          assertThat(repo.retry(old, Instant.now(), "late")).isFalse();
          assertThat(repo.complete(current)).isTrue();
          assertThat(repo.complete(current)).isFalse();
          assertThat(
                  h.j
                      .queryForList(
                          "EXPLAIN SELECT event_id FROM outbox FORCE INDEX(pending) WHERE"
                              + " state='PENDING' AND next_attempt_at<=UTC_TIMESTAMP(6) ORDER BY"
                              + " next_attempt_at,event_id LIMIT 10 FOR UPDATE SKIP LOCKED")
                      .getFirst()
                      .get("key"))
              .isEqualTo("pending");
        });
  }

  @Test
  public void exhaustsIntoAuditedDeadTask() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
          String id = "dead-" + UUID.randomUUID();
          h.runner.inControl(
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                h.j.update("delete from outbox");
                new MySqlOutboxWriter(h.j.getDataSource()).append(PublisherConfirmV2Test.event(id));
                h.j.update("update outbox set attempt=11 where event_id=?", id);
                return null;
              });
          var lease = repo.claim(RouteSlot.control(), 1, Duration.ofSeconds(30)).getFirst();
          assertThat(repo.retry(lease, Instant.now(), "RETURNED")).isTrue();
          assertThat(
                  h.j.queryForObject("select state from outbox where event_id=?", String.class, id))
              .isEqualTo("DEAD");
          assertThat(
                  h.j.queryForObject(
                      "select count(*) from recovery_case where aggregate_type='OUTBOX_TASK' and"
                          + " aggregate_id=? and active_key is not null",
                      Integer.class,
                      id))
              .isOne();
          assertThat(repo.claim(RouteSlot.control(), 1, Duration.ofSeconds(30))).isEmpty();
        });
  }

  @Test
  public void lockedTaskDoesNotBlockClaimOfAnotherTask() {
    AssetTestSupport.run(
        h -> {
          String locked = "lock-" + UUID.randomUUID(), free = "free-" + UUID.randomUUID();
          h.runner.inControl(
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                h.j.update("delete from outbox");
                var w = new MySqlOutboxWriter(h.j.getDataSource());
                w.append(PublisherConfirmV2Test.event(locked));
                w.append(PublisherConfirmV2Test.event(free));
                return null;
              });
          var acquired = new java.util.concurrent.CountDownLatch(1);
          var release = new java.util.concurrent.CountDownLatch(1);
          try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var owner =
                pool.submit(
                    () ->
                        h.runner.inControl(
                            WorkClass.TRADE,
                            new Deadline(Instant.now().plusSeconds(30)),
                            () -> {
                              h.j.queryForObject(
                                  "select event_id from outbox where event_id=? for update",
                                  String.class,
                                  locked);
                              acquired.countDown();
                              try {
                                release.await(5, java.util.concurrent.TimeUnit.SECONDS);
                              } catch (InterruptedException e) {
                                throw new IllegalStateException(e);
                              }
                              return null;
                            }));
            assertThat(acquired.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            try {
              var claims =
                  new MySqlOutboxRepository(h.j.getDataSource(), h.runner)
                      .claim(RouteSlot.control(), 2, Duration.ofSeconds(30));
              assertThat(claims).hasSize(1);
              assertThat(claims.getFirst().event().eventId()).isEqualTo(free);
            } finally {
              release.countDown();
            }
            owner.get(5, java.util.concurrent.TimeUnit.SECONDS);
          }
        });
  }

  @Test
  public void retryAndLeaseInstantsRemainUtcWithShanghaiJvmTimezone() {
    var previous = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    try {
      AssetTestSupport.run(
          h -> {
            ConsumerInboxV2Test.schema(h);
            var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
            String id = "timezone-" + UUID.randomUUID();
            h.j.update("delete from outbox");
            h.runner.inControl(
                WorkClass.RECOVERY,
                new Deadline(Instant.now().plusSeconds(30)),
                () -> {
                  new MySqlOutboxWriter(h.j.getDataSource())
                      .append(PublisherConfirmV2Test.event(id));
                  return null;
                });
            var lease = repo.claim(RouteSlot.control(), 1, Duration.ofSeconds(30)).getFirst();
            assertThat(Duration.between(Instant.now(), lease.leaseUntil()).toSeconds())
                .isBetween(25L, 35L);
            repo.retry(lease, Instant.parse("2026-10-15T12:00:00Z"), "UNKNOWN");
            assertThat(
                    h.j.queryForObject(
                        "select next_attempt_at from outbox where event_id=?",
                        LocalDateTime.class,
                        id))
                .isEqualTo(LocalDateTime.parse("2026-10-15T12:00:00"));
          });
    } finally {
      TimeZone.setDefault(previous);
    }
  }

  @Test
  public void futureClassesCannotStarveDueBusinessClass() {
    AssetTestSupport.run(
        h -> {
          h.j.update("delete from outbox");
          var writer = new MySqlOutboxWriter(h.j.getDataSource());
          h.runner.inControl(
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                for (int i = 0; i < 64; i++)
                  writer.append(
                      new EventEnvelope(
                          "future-" + i,
                          "AAA" + i,
                          "a",
                          new BusinessId("EX", "a", "a".repeat(64)),
                          null,
                          RouteSlot.control(),
                          "a".repeat(64),
                          1,
                          1,
                          "{}"));
                h.j.update("update outbox set next_attempt_at=UTC_TIMESTAMP(6)+INTERVAL 1 DAY");
                writer.append(PublisherConfirmV2Test.event("due-fairness"));
                return null;
              });
          var claims =
              new MySqlOutboxRepository(h.j.getDataSource(), h.runner)
                  .claim(RouteSlot.control(), 8, Duration.ofSeconds(30));
          assertThat(claims).hasSize(1);
          assertThat(claims.getFirst().event().eventId()).isEqualTo("due-fairness");
        });
  }
}
