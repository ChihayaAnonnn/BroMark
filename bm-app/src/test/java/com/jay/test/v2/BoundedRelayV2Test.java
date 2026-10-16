package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.RequestHasher;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.JsonStore;
import com.jay.infrastructure.adapter.repository.v2.MySqlConsumerInbox;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;

public class BoundedRelayV2Test {
  static class Repo implements BatchOutboxRepository {
    List<ClaimedPublish> rows;
    final Set<String> released = ConcurrentHashMap.newKeySet();
    final List<PublishResolution> resolved = new CopyOnWriteArrayList<>();
    final Set<String> quarantined = ConcurrentHashMap.newKeySet();

    Repo(int n) {
      rows = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        var e = RelayOptimizationTestSupport.event("wave-" + i, "PrepareDraw");
        rows.add(
            new ClaimedPublish(
                RouteSlot.control(),
                e.eventId(),
                e.aggregateId(),
                1,
                e.eventType(),
                JsonStore.write(e),
                new RequestHasher().hash("EVENT", Map.of("envelope", JsonStore.write(e))),
                "token-" + i,
                Instant.now().plusSeconds(30),
                1,
                0,
                "",
                Instant.now()));
      }
    }

    public List<ClaimedPublish> claimBatch(RouteSlot s, RelayPolicy p, Deadline d) {
      var out = rows;
      rows = List.of();
      return out;
    }

    public BatchSettleReport settleBatch(RouteSlot s, List<PublishResolution> r, Deadline d) {
      resolved.addAll(r);
      return new BatchSettleReport(
          r.stream()
              .filter(x -> x.outcome() == PublishOutcome.CONFIRMED)
              .map(x -> x.task().eventId())
              .collect(java.util.stream.Collectors.toSet()),
          Set.of(),
          Set.of(),
          Set.of(),
          Instant.now());
    }

    public Set<String> releaseUnsent(RouteSlot s, List<ClaimedPublish> tasks, Deadline d) {
      tasks.forEach(t -> released.add(t.eventId()));
      return Set.copyOf(released);
    }

    public boolean quarantineMalformed(ClaimedPublish t, String reason, Deadline d) {
      quarantined.add(t.eventId());
      return true;
    }

    public List<OutboxLease> claim(RouteSlot s, int n, Duration l) {
      throw new AssertionError("legacy claim");
    }

    public boolean complete(OutboxLease l) {
      throw new AssertionError("per-row complete");
    }

    public boolean retry(OutboxLease l, Instant n, String r) {
      throw new AssertionError("per-row retry");
    }
  }

  @Test
  public void publishesThirtyTwoInFourBoundedWaves() throws Exception {
    var repo = new Repo(32);
    var queue = new LinkedBlockingQueue<CompletableFuture<PublishOutcome>>();
    var active = new AtomicInteger();
    var max = new AtomicInteger();
    var attempted = ConcurrentHashMap.<String>newKeySet();
    ConfirmedEventPublisher publisher =
        (e, d) -> {
          attempted.add(e.eventId());
          max.accumulateAndGet(active.incrementAndGet(), Math::max);
          var f = new CompletableFuture<PublishOutcome>();
          f.whenComplete((v, t) -> active.decrementAndGet());
          queue.add(f);
          return f;
        };
    var relay =
        new BoundedOutboxRelay(repo, publisher, RelayOptimizationTestSupport.policy(), p -> {});
    try (var executor = Executors.newSingleThreadExecutor()) {
      var run = executor.submit(() -> relay.runOnce(RouteSlot.control()));
      for (int wave = 0; wave < 4; wave++) {
        var futures = new ArrayList<CompletableFuture<PublishOutcome>>();
        for (int i = 0; i < 8; i++)
          futures.add(Objects.requireNonNull(queue.poll(3, TimeUnit.SECONDS)));
        assertThat(queue).isEmpty();
        assertThat(run.isDone()).isFalse();
        futures.forEach(f -> f.complete(PublishOutcome.CONFIRMED));
      }
      assertThat(run.get(5, TimeUnit.SECONDS)).isEqualTo(32);
    }
    assertThat(max).hasValue(8);
    assertThat(repo.resolved).hasSize(32);
    assertThat(repo.released).doesNotContainAnyElementsOf(attempted);
  }

  @Test
  public void unknownWaveSharesDeadlineAndReleasesOnlyNeverSubmittedRows() {
    var repo = new Repo(32);
    var attempted = ConcurrentHashMap.<String>newKeySet();
    var p =
        new RelayPolicy(
            32,
            8,
            4,
            8,
            Duration.ofMillis(1),
            Duration.ofSeconds(30),
            Duration.ofSeconds(1),
            Duration.ofMillis(100),
            Duration.ofSeconds(1),
            Duration.ofSeconds(20),
            Duration.ofSeconds(5));
    ConfirmedEventPublisher publisher =
        (e, d) -> {
          attempted.add(e.eventId());
          return new CompletableFuture<>();
        };
    long start = System.nanoTime();
    assertThat(new BoundedOutboxRelay(repo, publisher, p, x -> {}).runOnce(RouteSlot.control()))
        .isEqualTo(32);
    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
    assertThat(attempted).hasSize(8);
    assertThat(repo.released).hasSize(24).doesNotContainAnyElementsOf(attempted);
    assertThat(repo.resolved).allMatch(r -> r.outcome() == PublishOutcome.UNKNOWN);
  }

  @Test
  public void sharedDeadlineAndMixedOutcomesPreserveAttemptGeneration() {
    var repo = new Repo(8);
    var deadlines = new ArrayList<Deadline>();
    var generations = new ArrayList<Integer>();
    var publisher =
        new ConfirmedEventPublisher() {
          public CompletionStage<PublishOutcome> publish(EventEnvelope e, Deadline d) {
            throw new AssertionError("snapshot path required");
          }

          public CompletionStage<PublishOutcome> publishAttempt(
              EventEnvelope e, String token, int count, Deadline d) {
            deadlines.add(d);
            generations.add(count);
            return switch (deadlines.size()) {
              case 1 -> CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
              case 2 -> CompletableFuture.completedFuture(PublishOutcome.RETURNED);
              default -> new CompletableFuture<>();
            };
          }
        };
    var p =
        new RelayPolicy(
            32,
            8,
            4,
            8,
            Duration.ofMillis(1),
            Duration.ofSeconds(30),
            Duration.ofSeconds(1),
            Duration.ofMillis(100),
            Duration.ofSeconds(1),
            Duration.ofSeconds(20),
            Duration.ofSeconds(5));
    new BoundedOutboxRelay(repo, publisher, p, x -> {}).runOnce(RouteSlot.control());
    assertThat(deadlines).hasSize(8).allMatch(d -> d == deadlines.getFirst());
    assertThat(generations).containsOnly(0);
    assertThat(repo.resolved.stream().filter(r -> r.outcome() == PublishOutcome.CONFIRMED))
        .hasSize(1);
    assertThat(repo.resolved.stream().filter(r -> r.outcome() == PublishOutcome.UNKNOWN))
        .hasSize(6);
    assertThat(repo.released).isEmpty();
  }

  @Test
  public void conservativeDatabaseSampleStopsWaveWithoutUsingHostClock() {
    var repo = new Repo(8);
    var base = Instant.now().minusSeconds(3600);
    repo.rows =
        repo.rows.stream()
            .map(
                t ->
                    new ClaimedPublish(
                        t.ownerSlot(),
                        t.eventId(),
                        t.aggregateId(),
                        t.aggregateVersion(),
                        t.eventType(),
                        t.rawEnvelope(),
                        t.payloadHash(),
                        t.leaseToken(),
                        base.plusSeconds(5),
                        t.attempt(),
                        0,
                        "",
                        base))
            .toList();
    var sent = new AtomicInteger();
    new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              sent.incrementAndGet();
              return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
            },
            RelayOptimizationTestSupport.policy(),
            x -> {})
        .runOnce(RouteSlot.control());
    assertThat(sent).hasValue(0);
    assertThat(repo.released).hasSize(8);
  }

  @Test
  public void identityAndBothHashSchemesAreCheckedPerRow() {
    var repo = new Repo(4);
    var first = repo.rows.get(0);
    var second = repo.rows.get(1);
    var retry = repo.rows.get(2);
    repo.rows.set(
        0,
        new ClaimedPublish(
            first.ownerSlot(),
            "wrong-id",
            first.aggregateId(),
            1,
            first.eventType(),
            first.rawEnvelope(),
            first.payloadHash(),
            first.leaseToken(),
            first.leaseUntil(),
            1,
            0,
            "",
            first.claimedAt()));
    repo.rows.set(
        1,
        new ClaimedPublish(
            second.ownerSlot(),
            second.eventId(),
            second.aggregateId(),
            1,
            second.eventType(),
            second.rawEnvelope(),
            "0".repeat(64),
            second.leaseToken(),
            second.leaseUntil(),
            1,
            0,
            "",
            second.claimedAt()));
    repo.rows.set(
        2,
        new ClaimedPublish(
            retry.ownerSlot(),
            retry.eventId(),
            retry.aggregateId(),
            1,
            retry.eventType(),
            retry.rawEnvelope(),
            MySqlConsumerInbox.digest(retry.rawEnvelope()),
            retry.leaseToken(),
            retry.leaseUntil(),
            3,
            7,
            "credit",
            retry.claimedAt()));
    var sent = new AtomicInteger();
    var counts = new ArrayList<Integer>();
    var publisher =
        new ConfirmedEventPublisher() {
          public CompletionStage<PublishOutcome> publish(EventEnvelope e, Deadline d) {
            throw new AssertionError();
          }

          public CompletionStage<PublishOutcome> publishAttempt(
              EventEnvelope e, String token, int count, Deadline d) {
            counts.add(count);
            sent.incrementAndGet();
            return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
          }
        };
    new BoundedOutboxRelay(repo, publisher, RelayOptimizationTestSupport.policy(), x -> {})
        .runOnce(RouteSlot.control());
    assertThat(sent).hasValue(2);
    assertThat(counts).containsExactly(7, 0);
    assertThat(repo.quarantined).containsExactlyInAnyOrder("wrong-id", second.eventId());
  }

  @Test
  public void badEnvelopeDoesNotBlockOtherRows() {
    var repo = new Repo(2);
    var t = repo.rows.getFirst();
    repo.rows.set(
        0,
        new ClaimedPublish(
            t.ownerSlot(),
            t.eventId(),
            t.aggregateId(),
            1,
            t.eventType(),
            "{}",
            t.payloadHash(),
            t.leaseToken(),
            t.leaseUntil(),
            1,
            0,
            "",
            t.claimedAt()));
    var sent = new AtomicInteger();
    new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              sent.incrementAndGet();
              return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
            },
            RelayOptimizationTestSupport.policy(),
            x -> {})
        .runOnce(RouteSlot.control());
    assertThat(sent).hasValue(1);
    assertThat(repo.quarantined).containsExactly(t.eventId());
    assertThat(repo.resolved).hasSize(1);
  }

  @Test
  public void stopDrainsCurrentWaveAndReleasesOnlyUnsubmittedRows() throws Exception {
    var repo = new Repo(32);
    var queue = new LinkedBlockingQueue<CompletableFuture<PublishOutcome>>();
    ConfirmedEventPublisher publisher =
        (e, d) -> {
          var f = new CompletableFuture<PublishOutcome>();
          queue.add(f);
          return f;
        };
    var relay =
        new BoundedOutboxRelay(repo, publisher, RelayOptimizationTestSupport.policy(), x -> {});
    try (var executor = Executors.newFixedThreadPool(2)) {
      var run = executor.submit(() -> relay.runOnce(RouteSlot.control()));
      var wave = new ArrayList<CompletableFuture<PublishOutcome>>();
      for (int i = 0; i < 8; i++) wave.add(Objects.requireNonNull(queue.poll(3, TimeUnit.SECONDS)));
      var stopping = executor.submit(() -> relay.stopAndDrain(Duration.ofSeconds(3)));
      // 新领取已停止，活动波未确认时关闭不能完成。
      Thread.sleep(50);
      assertThat(stopping.isDone()).isFalse();
      wave.forEach(f -> f.complete(PublishOutcome.CONFIRMED));
      assertThat(run.get(3, TimeUnit.SECONDS)).isEqualTo(32);
      stopping.get(3, TimeUnit.SECONDS);
      assertThat(repo.resolved).hasSize(8);
      assertThat(repo.released).hasSize(24);
      assertThat(queue).isEmpty();
      assertThat(relay.runOnce(RouteSlot.control())).isZero();
    }
  }

  @Test
  public void uncertainSettleAndAfterConfirmCrashNeverReleaseAttemptedRows() {
    var repo =
        new Repo(32) {
          @Override
          public BatchSettleReport settleBatch(RouteSlot s, List<PublishResolution> r, Deadline d) {
            throw new IllegalStateException("uncertain commit");
          }
        };
    var attempted = new HashSet<String>();
    ConfirmedEventPublisher publisher =
        (e, d) -> {
          attempted.add(e.eventId());
          return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
        };
    assertThatThrownBy(
            () ->
                new BoundedOutboxRelay(
                        repo, publisher, RelayOptimizationTestSupport.policy(), x -> {})
                    .runOnce(RouteSlot.control()))
        .hasMessage("uncertain commit");
    assertThat(attempted).hasSize(8);
    assertThat(repo.released).hasSize(24).doesNotContainAnyElementsOf(attempted);
    var crashRepo = new Repo(32);
    assertThatThrownBy(
            () ->
                new BoundedOutboxRelay(
                        crashRepo,
                        publisher,
                        RelayOptimizationTestSupport.policy(),
                        x -> {
                          throw new IllegalStateException("after-confirm");
                        })
                    .runOnce(RouteSlot.control()))
        .hasMessage("after-confirm");
    assertThat(crashRepo.resolved).isEmpty();
    assertThat(crashRepo.released).hasSize(24).doesNotContainAnyElementsOf(attempted);
  }

  @Test
  public void settledDatabaseSampleCanEndLeaseBudgetBeforeNextWave() {
    var repo =
        new Repo(32) {
          @Override
          public BatchSettleReport settleBatch(RouteSlot s, List<PublishResolution> r, Deadline d) {
            var report = super.settleBatch(s, r, d);
            return new BatchSettleReport(
                report.completed(), Set.of(), Set.of(), Set.of(), Instant.now().plusSeconds(25));
          }
        };
    var sent = new AtomicInteger();
    new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              sent.incrementAndGet();
              return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
            },
            RelayOptimizationTestSupport.policy(),
            x -> {})
        .runOnce(RouteSlot.control());
    assertThat(sent).hasValue(8);
    assertThat(repo.released).hasSize(24);
  }

  @Test
  public void twoLanesShareSixteenRealRabbitSlotsWithoutOverflowUnknown() throws Exception {
    var cf = PublisherConfirmV2Test.connection();
    var admin = new org.springframework.amqp.rabbit.core.RabbitAdmin(cf);
    String ex = "v2.it.waves." + UUID.randomUUID(), q = ex + ".q";
    admin.declareExchange(new org.springframework.amqp.core.DirectExchange(ex, true, false));
    admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
    admin.declareBinding(
        new org.springframework.amqp.core.Binding(
            q,
            org.springframework.amqp.core.Binding.DestinationType.QUEUE,
            ex,
            "PrepareDraw",
            null));
    var sixteen = new CountDownLatch(16);
    var active = new AtomicInteger();
    var max = new AtomicInteger();
    var template =
        new org.springframework.amqp.rabbit.core.RabbitTemplate(cf) {
          @Override
          public void send(
              String exchange,
              String key,
              org.springframework.amqp.core.Message message,
              org.springframework.amqp.rabbit.connection.CorrelationData cd) {
            try {
              if (!sixteen.await(2, TimeUnit.SECONDS))
                throw new IllegalStateException("lanes did not fill first wave");
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(e);
            }
            super.send(exchange, key, message, cd);
          }
        };
    try (var real =
            new com.jay.infrastructure.event.V2ConfirmedEventPublisher(
                template, ex, 16, Duration.ofSeconds(3));
        var executor = Executors.newFixedThreadPool(2)) {
      var publisher =
          new ConfirmedEventPublisher() {
            public CompletionStage<PublishOutcome> publish(EventEnvelope e, Deadline d) {
              return publishAttempt(e, "token", 0, d);
            }

            public boolean isAttemptComplete(CompletionStage<PublishOutcome> a) {
              return real.isAttemptComplete(a);
            }

            public CompletionStage<PublishOutcome> publishAttempt(
                EventEnvelope e, String token, int count, Deadline d) {
              max.accumulateAndGet(active.incrementAndGet(), Math::max);
              var result = real.publishAttempt(e, token, count, d);
              result.whenComplete((v, t) -> active.decrementAndGet());
              sixteen.countDown();
              return result;
            }
          };
      var first = new Repo(32);
      var second = new Repo(32);
      var a =
          executor.submit(
              () ->
                  new BoundedOutboxRelay(
                          first, publisher, RelayOptimizationTestSupport.policy(), x -> {})
                      .runOnce(RouteSlot.control()));
      var b =
          executor.submit(
              () ->
                  new BoundedOutboxRelay(
                          second, publisher, RelayOptimizationTestSupport.policy(), x -> {})
                      .runOnce(RouteSlot.control()));
      assertThat(a.get(8, TimeUnit.SECONDS)).isEqualTo(32);
      assertThat(b.get(8, TimeUnit.SECONDS)).isEqualTo(32);
      assertThat(max).hasValue(16);
      assertThat(first.resolved).hasSize(32).allMatch(r -> r.outcome() == PublishOutcome.CONFIRMED);
      assertThat(second.resolved)
          .hasSize(32)
          .allMatch(r -> r.outcome() == PublishOutcome.CONFIRMED);
      assertThat(admin.getQueueInfo(q).getMessageCount()).isEqualTo(64);
      System.out.println(
          "bounded-relay two-lane real Rabbit maxInFlight="
              + max.get()
              + " confirmed=64 unknown=0");
    } finally {
      sixteen.countDown();
      admin.deleteQueue(q);
      admin.deleteExchange(ex);
      cf.destroy();
    }
  }

  @Test
  public void databaseClockOffsetDoesNotInvalidateFreshDatabaseLease() {
    var repo = new Repo(8);
    var base = Instant.now().minusSeconds(3600);
    repo.rows =
        repo.rows.stream()
            .map(
                t ->
                    new ClaimedPublish(
                        t.ownerSlot(),
                        t.eventId(),
                        t.aggregateId(),
                        t.aggregateVersion(),
                        t.eventType(),
                        t.rawEnvelope(),
                        t.payloadHash(),
                        t.leaseToken(),
                        base.plusSeconds(30),
                        t.attempt(),
                        0,
                        "",
                        base))
            .toList();
    var sent = new AtomicInteger();
    new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              sent.incrementAndGet();
              return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
            },
            RelayOptimizationTestSupport.policy(),
            x -> {})
        .runOnce(RouteSlot.control());
    assertThat(sent).hasValue(8);
    assertThat(repo.released).isEmpty();
  }

  @Test
  public void claimSamplingUncertaintyConsumesLeaseBeforeAnyPublisherCall() {
    var repo =
        new Repo(8) {
          @Override
          public List<ClaimedPublish> claimBatch(
              RouteSlot slot, RelayPolicy policy, Deadline deadline) {
            try {
              Thread.sleep(150);
            } catch (InterruptedException e) {
              throw new IllegalStateException(e);
            }
            return super.claimBatch(slot, policy, deadline);
          }
        };
    repo.rows =
        repo.rows.stream()
            .map(
                t ->
                    new ClaimedPublish(
                        t.ownerSlot(),
                        t.eventId(),
                        t.aggregateId(),
                        t.aggregateVersion(),
                        t.eventType(),
                        t.rawEnvelope(),
                        t.payloadHash(),
                        t.leaseToken(),
                        t.claimedAt().plusMillis(9050),
                        t.attempt(),
                        0,
                        "",
                        t.claimedAt()))
            .toList();
    var sent = new AtomicInteger();
    new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              sent.incrementAndGet();
              return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
            },
            RelayOptimizationTestSupport.policy(),
            x -> {})
        .runOnce(RouteSlot.control());
    assertThat(sent).hasValue(0);
    assertThat(repo.released).hasSize(8);
  }

  @Test
  public void batchDeadlineStopsNewWaveAfterSlowSettlement() {
    var repo =
        new Repo(32) {
          @Override
          public BatchSettleReport settleBatch(RouteSlot s, List<PublishResolution> r, Deadline d) {
            try {
              Thread.sleep(210);
            } catch (InterruptedException e) {
              throw new IllegalStateException(e);
            }
            return super.settleBatch(s, r, d);
          }
        };
    var p =
        new RelayPolicy(
            32,
            8,
            4,
            8,
            Duration.ofMillis(1),
            Duration.ofSeconds(30),
            Duration.ofMillis(10),
            Duration.ofMillis(20),
            Duration.ofMillis(20),
            Duration.ofMillis(200),
            Duration.ofMillis(10));
    var sent = new AtomicInteger();
    new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              sent.incrementAndGet();
              return CompletableFuture.completedFuture(PublishOutcome.CONFIRMED);
            },
            p,
            x -> {})
        .runOnce(RouteSlot.control());
    assertThat(sent).hasValue(8);
    assertThat(repo.released).hasSize(24);
  }

  @Test
  public void realDatabaseQuarantinesBadRowsWhileOutboxAndRetryPublishWithOriginalHashes()
      throws Exception {
    var cf = PublisherConfirmV2Test.connection();
    var admin = new org.springframework.amqp.rabbit.core.RabbitAdmin(cf);
    String ex = "v2.it.db-relay." + UUID.randomUUID(), q = ex + ".q";
    admin.declareExchange(new org.springframework.amqp.core.DirectExchange(ex, true, false));
    admin.declareQueue(new org.springframework.amqp.core.Queue(q, true));
    admin.declareBinding(
        new org.springframework.amqp.core.Binding(
            q,
            org.springframework.amqp.core.Binding.DestinationType.QUEUE,
            ex,
            "PrepareDraw",
            null));
    var template = new org.springframework.amqp.rabbit.core.RabbitTemplate(cf);
    try (var publisher =
        new com.jay.infrastructure.event.V2ConfirmedEventPublisher(
            template, ex, 16, Duration.ofSeconds(3))) {
      AssetTestSupport.run(
          h -> {
            RelayOptimizationTestSupport.schema(h);
            var writer =
                new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                    h.j.getDataSource());
            var outbox =
                new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxRepository(
                    h.j.getDataSource(), h.runner);
            var retry =
                new com.jay.infrastructure.adapter.repository.v2.MySqlRetryJobRepository(
                    h.j.getDataSource(), h.runner);
            String prefix = "db-relay-" + UUID.randomUUID();
            h.runner.inControl(
                WorkClass.RECOVERY,
                RelayOptimizationTestSupport.deadline(),
                () -> {
                  h.j.update("delete from outbox");
                  h.j.update("delete from retry_job");
                  for (String suffix : List.of("json", "identity", "hash", "good"))
                    writer.append(
                        RelayOptimizationTestSupport.event(prefix + suffix, "PrepareDraw"));
                  h.j.update("update outbox set payload='{}' where event_id=?", prefix + "json");
                  h.j.update(
                      "update outbox set aggregate_version=2 where event_id=?",
                      prefix + "identity");
                  h.j.update(
                      "update outbox set payload_hash=? where event_id=?",
                      "0".repeat(64),
                      prefix + "hash");
                  retry.schedule(
                      "workflow-v2",
                      RelayOptimizationTestSupport.event(prefix + "retry", "PrepareDraw"),
                      7,
                      Instant.now().minusSeconds(1));
                  return null;
                });
            assertThat(
                    new BoundedOutboxRelay(
                            outbox, publisher, RelayOptimizationTestSupport.policy(), x -> {})
                        .runOnce(RouteSlot.control()))
                .isEqualTo(4);
            assertThat(
                    new BoundedOutboxRelay(
                            retry, publisher, RelayOptimizationTestSupport.policy(), x -> {})
                        .runOnce(RouteSlot.control()))
                .isEqualTo(1);
            h.runner.inControl(
                WorkClass.RECOVERY,
                RelayOptimizationTestSupport.deadline(),
                () -> {
                  assertThat(
                          h.j.queryForObject(
                              "select count(*) from outbox where event_id like ? and state='DEAD'",
                              Integer.class,
                              prefix + "%"))
                      .isEqualTo(3);
                  assertThat(
                          h.j.queryForObject(
                              "select state from outbox where event_id=?",
                              String.class,
                              prefix + "good"))
                      .isEqualTo("PUBLISHED");
                  assertThat(
                          h.j.queryForObject(
                              "select state from retry_job where event_id=?",
                              String.class,
                              prefix + "retry"))
                      .isEqualTo("PUBLISHED");
                  assertThat(
                          h.j.queryForObject(
                              "select failure_count from retry_job where event_id=?",
                              Integer.class,
                              prefix + "retry"))
                      .isEqualTo(7);
                  assertThat(
                          h.j.queryForObject(
                              "select count(*) from recovery_case where aggregate_id like ? and"
                                  + " recovery_status='WAITING_FIX'",
                              Integer.class,
                              prefix + "%"))
                      .isEqualTo(3);
                  return null;
                });
            var first = template.receive(q, 2000);
            var second = template.receive(q, 2000);
            assertThat(first).isNotNull();
            assertThat(second).isNotNull();
            assertThat(
                    List.of(
                        first.getMessageProperties().getHeaders().get("x-v2-failure-count"),
                        second.getMessageProperties().getHeaders().get("x-v2-failure-count")))
                .containsExactlyInAnyOrder(0, 7);
            assertThat(template.receive(q)).isNull();
          });
    } finally {
      admin.deleteQueue(q);
      admin.deleteExchange(ex);
      cf.destroy();
    }
  }

  @Test
  public void priorUnknownPhysicalWaveBlocksNextClaimUntilItsOwnAttemptsFinish() {
    var claims = new AtomicInteger();
    var repo =
        new Repo(8) {
          @Override
          public List<ClaimedPublish> claimBatch(RouteSlot s, RelayPolicy p, Deadline d) {
            claims.incrementAndGet();
            return super.claimBatch(s, p, d);
          }
        };
    var futures = new ArrayList<CompletableFuture<PublishOutcome>>();
    var p =
        new RelayPolicy(
            32,
            8,
            4,
            8,
            Duration.ofMillis(1),
            Duration.ofSeconds(30),
            Duration.ofSeconds(1),
            Duration.ofMillis(50),
            Duration.ofSeconds(1),
            Duration.ofSeconds(20),
            Duration.ofSeconds(5));
    var relay =
        new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              var f = new CompletableFuture<PublishOutcome>();
              futures.add(f);
              return f;
            },
            p,
            x -> {});
    assertThat(relay.runOnce(RouteSlot.control())).isEqualTo(8);
    repo.rows = new Repo(8).rows;
    assertThat(relay.runOnce(RouteSlot.control())).isZero();
    assertThat(claims).hasValue(1);
    assertThatThrownBy(() -> relay.stopAndDrain(Duration.ofMillis(30)))
        .isInstanceOf(IllegalStateException.class);
    futures.forEach(f -> f.complete(PublishOutcome.CONFIRMED));
    relay.stopAndDrain(Duration.ofMillis(100));
    assertThat(repo.rows).hasSize(8);
  }

  @Test
  public void concurrentCallerCannotClaimSecondBatchForSameLane() throws Exception {
    var claims = new AtomicInteger();
    var repo =
        new Repo(8) {
          @Override
          public List<ClaimedPublish> claimBatch(RouteSlot s, RelayPolicy p, Deadline d) {
            if (claims.incrementAndGet() > 1) throw new AssertionError("second simultaneous claim");
            return super.claimBatch(s, p, d);
          }
        };
    var queue = new LinkedBlockingQueue<CompletableFuture<PublishOutcome>>();
    var relay =
        new BoundedOutboxRelay(
            repo,
            (e, d) -> {
              var f = new CompletableFuture<PublishOutcome>();
              queue.add(f);
              return f;
            },
            RelayOptimizationTestSupport.policy(),
            x -> {});
    try (var executor = Executors.newSingleThreadExecutor()) {
      var run = executor.submit(() -> relay.runOnce(RouteSlot.control()));
      var wave = new ArrayList<CompletableFuture<PublishOutcome>>();
      for (int i = 0; i < 8; i++) wave.add(Objects.requireNonNull(queue.poll(3, TimeUnit.SECONDS)));
      try {
        assertThat(relay.runOnce(RouteSlot.control())).isZero();
      } finally {
        wave.forEach(f -> f.complete(PublishOutcome.CONFIRMED));
      }
      assertThat(run.get(3, TimeUnit.SECONDS)).isEqualTo(8);
      assertThat(claims).hasValue(1);
    }
  }
}
