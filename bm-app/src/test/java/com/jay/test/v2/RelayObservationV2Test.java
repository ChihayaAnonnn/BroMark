package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.Deadline;
import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.asset.model.WorkClass;
import com.jay.domain.asset.service.RouteCodec;
import com.jay.domain.raffle.service.DrawRecoveryService;
import com.jay.domain.task.model.PublishOutcome;
import com.jay.domain.task.service.BoundedOutboxRelay;
import com.jay.domain.task.service.RelayObservation;
import com.jay.domain.task.service.RelayObservationContext;
import com.jay.infrastructure.adapter.repository.v2.MySqlDrawRepository;
import com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter;
import com.jay.infrastructure.adapter.repository.v2.MySqlStockRepository;
import com.jay.infrastructure.event.MicrometerRelayObservation;
import com.jay.infrastructure.transaction.RoutedTransactionRunner;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class RelayObservationV2Test {
  @Test
  public void identityNeverBecomesAMetricTagAndUnknownCannotComplete() {
    var registry = new SimpleMeterRegistry();
    RelayObservation observation = new MicrometerRelayObservation(registry);
    var slot = new RouteSlot("01", "000");
    observation.duration("claim", slot, "PrepareDraw", "success", 10_000);
    observation.duration("claim", slot, "PrepareDraw", "success", 20_000);
    observation.count("completed", slot, "PrepareDraw", "UNKNOWN", 1);
    assertThat(registry.getMeters()).hasSize(1);
    assertThat(registry.getMeters().getFirst().getId().getTags())
        .noneMatch(tag -> tag.getKey().matches("(?i).*user.*|.*order.*|.*event.*id.*"));
    assertThatThrownBy(() -> observation.count("completed", slot, "PrepareDraw", "event-123", 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void stagesAndReasonsHaveFiniteVocabulary() {
    RelayObservation observation = new MicrometerRelayObservation(new SimpleMeterRegistry());
    var slot = RouteSlot.control();
    for (String stage :
        new String[] {
          "claim", "wave", "confirm", "settle", "permit", "http_admission", "config", "asset_tx"
        }) observation.duration(stage, slot, "PrepareDraw", "success", 1);
    for (String action :
        new String[] {
          "claimed",
          "started",
          "completed",
          "lost_lease",
          "released_unsent",
          "timeout",
          "checked",
          "redriven",
          "suppressed",
          "exhausted"
        }) observation.count(action, slot, "PrepareDraw", "success", 1);
    assertThatThrownBy(() -> observation.duration("order-123", slot, "PrepareDraw", "success", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> observation.count("suppressed", slot, "PrepareDraw", "user-123", 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void boundedRelayEmitsClaimConfirmAndSettlementWithoutUnknownCompletion() {
    var registry = new SimpleMeterRegistry();
    var observation = new MicrometerRelayObservation(registry);
    var repo = new BoundedRelayV2Test.Repo(1);
    var relay =
        new BoundedOutboxRelay(
            repo,
            (event, deadline) -> CompletableFuture.completedFuture(PublishOutcome.CONFIRMED),
            RelayOptimizationTestSupport.policy(),
            point -> {},
            observation);
    assertThat(relay.runOnce(RouteSlot.control())).isOne();
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "claim").timer()).isNotNull();
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "confirm").timer()).isNotNull();
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "settle").timer()).isNotNull();
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "wave").timer().count()).isEqualTo(1);
    assertThat(registry.find("bm.v2.relay.count").tag("action", "started").counter().count()).isEqualTo(1);
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "batch").timer().count()).isEqualTo(1);
    assertThat(registry.find("bm.v2.relay.count").tag("action", "completed").counter().count())
        .isEqualTo(1);
  }

  @Test
  public void explicitNackIsNeverSuccessfulOrUnknown() {
    var registry = new SimpleMeterRegistry();
    var relay = new BoundedOutboxRelay(new BoundedRelayV2Test.Repo(1),
        (event, deadline) -> CompletableFuture.completedFuture(PublishOutcome.NACKED),
        RelayOptimizationTestSupport.policy(), point -> {}, new MicrometerRelayObservation(registry));
    relay.runOnce(RouteSlot.control());
    for (String stage : new String[] {"wave", "confirm"}) {
      assertThat(registry.find("bm.v2.relay.duration").tag("stage", stage).tag("outcome", "nacked").timer().count()).isEqualTo(1);
      assertThat(registry.find("bm.v2.relay.duration").tag("stage", stage).tag("outcome", "UNKNOWN").timer()).isNull();
      assertThat(registry.find("bm.v2.relay.duration").tag("stage", stage).tag("outcome", "success").timer()).isNull();
    }
    assertThat(registry.find("bm.v2.relay.count").tag("action", "completed").counter()).isNull();
  }

  @Test
  public void failedClaimStillRecordsErrorAndReleasesLocalActiveGuard() {
    var registry = new SimpleMeterRegistry();
    var failure = new IllegalStateException("claim failed");
    var repo = new BoundedRelayV2Test.Repo(0) {
      @Override
      public java.util.List<com.jay.domain.task.model.ClaimedPublish> claimBatch(
          RouteSlot slot, com.jay.domain.task.model.RelayPolicy policy, Deadline deadline) {
        throw failure;
      }
    };
    var relay = new BoundedOutboxRelay(repo,
        (event, deadline) -> CompletableFuture.completedFuture(PublishOutcome.CONFIRMED),
        RelayOptimizationTestSupport.policy(), point -> {}, new MicrometerRelayObservation(registry));
    for (int i = 0; i < 2; i++) assertThatThrownBy(() -> relay.runOnce(RouteSlot.control())).isSameAs(failure);
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "claim").tag("outcome", "error").timer().count()).isEqualTo(2);
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "batch").tag("outcome", "error").timer().count()).isEqualTo(2);
  }

  @Test
  public void failedSettlementRecordsWaveErrorWithoutCompletion() {
    var registry = new SimpleMeterRegistry();
    var failure = new IllegalStateException("settle failed");
    var repo = new BoundedRelayV2Test.Repo(1) {
      @Override
      public com.jay.domain.task.model.BatchSettleReport settleBatch(RouteSlot slot,
          java.util.List<com.jay.domain.task.model.PublishResolution> results, Deadline deadline) {
        throw failure;
      }
    };
    var relay = new BoundedOutboxRelay(repo,
        (event, deadline) -> CompletableFuture.completedFuture(PublishOutcome.CONFIRMED),
        RelayOptimizationTestSupport.policy(), point -> {}, new MicrometerRelayObservation(registry));
    assertThatThrownBy(() -> relay.runOnce(RouteSlot.control())).isSameAs(failure);
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "settle").tag("outcome", "error").timer().count()).isEqualTo(1);
    assertThat(registry.find("bm.v2.relay.duration").tag("stage", "wave").tag("outcome", "error").timer().count()).isEqualTo(1);
    assertThat(registry.find("bm.v2.relay.count").tag("action", "completed").counter()).isNull();
  }

  @Test
  public void guardedRecoveryRecordsRealEmissionAndSuppressionReason() {
    var registry = new SimpleMeterRegistry();
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "observed-" + UUID.randomUUID();
          var order =
              DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          DrawCancellationV2Test.held(h, order);
          h.tx(
              user,
              () -> {
                h.j.update(
                    "update v2_draw_order_"
                        + h.codec.routeUser(user).tableKey()
                        + " set last_progress_at=UTC_TIMESTAMP(6)-interval 60 second where"
                        + " order_id=?",
                    order.orderId());
                h.j.update(
                    "update outbox set state='PUBLISHED',published_at=UTC_TIMESTAMP(6)-interval 60"
                        + " second where aggregate_id=?",
                    order.orderId());
                return null;
              });
          var recovery =
              new DrawRecoveryService(
                  h.runner,
                  new MySqlDrawRepository(h.j.getDataSource()),
                  new MySqlStockRepository(h.j.getDataSource()),
                  h.codec,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  GuardedWorkflowRecoveryV2Test.guarded(h)
                      .withObservation(new MicrometerRelayObservation(registry)));
          assertThat(recovery.inspect(order.orderId()).actions()).hasSize(1);
          assertThat(recovery.inspect(order.orderId()).actions()).isEmpty();
          assertThat(registry.find("bm.v2.relay.count").tag("action", "redriven").counter().count())
              .isEqualTo(1);
          assertThat(
                  registry.find("bm.v2.relay.count").tag("action", "suppressed").counter().count())
              .isGreaterThanOrEqualTo(1);
        });
  }

  @Test
  public void parallelConfirmDurationUsesEachAttemptStartAndCompletion() throws Exception {
    var registry = new SimpleMeterRegistry();
    var pending = new CompletableFuture<PublishOutcome>();
    var published = new CountDownLatch(4);
    var relay =
        new BoundedOutboxRelay(
            new BoundedRelayV2Test.Repo(4),
            (event, deadline) -> {
              published.countDown();
              return pending;
            },
            RelayOptimizationTestSupport.policy(),
            point -> {},
            new MicrometerRelayObservation(registry));
    try (var executor = Executors.newSingleThreadExecutor()) {
      var run = executor.submit(() -> relay.runOnce(RouteSlot.control()));
      assertThat(published.await(2, TimeUnit.SECONDS)).isTrue();
      Thread.sleep(100);
      pending.complete(PublishOutcome.CONFIRMED);
      assertThat(run.get(2, TimeUnit.SECONDS)).isEqualTo(4);
    }
    var timer =
        registry
            .find("bm.v2.relay.duration")
            .tag("stage", "confirm")
            .tag("outcome", "confirmed")
            .timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(4);
    assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(300);
  }

  @Test
  public void assetTransactionTimerExcludesDatabasePermitWait() {
    var registry = new SimpleMeterRegistry();
    Java25RouterIntegrationTest.databaseContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var source = context.getBean(javax.sql.DataSource.class);
              var manager =
                  context.getBean(org.springframework.transaction.PlatformTransactionManager.class);
              com.jay.domain.asset.service.DatabaseBudget delayed =
                  new com.jay.domain.asset.service.DatabaseBudget() {
                    @Override
                    public com.jay.domain.asset.model.Permit acquire(
                        String instance, WorkClass work, Deadline deadline) {
                      try {
                        Thread.sleep(150);
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                      }
                      return () -> {};
                    }

                    @Override
                    public int available(String instance, WorkClass work) {
                      return 1;
                    }
                  };
              var runner =
                  new RoutedTransactionRunner(
                      source,
                      manager,
                      new RouteCodec(2, 4),
                      delayed,
                      Map.of("00", "it", "01", "it", "02", "it"),
                      registry,
                      new MicrometerRelayObservation(registry));
              long started = System.nanoTime();
              try (var ignored = RelayObservationContext.asset("PrepareDraw")) {
                runner.inControl(
                    WorkClass.TRADE,
                    new Deadline(Instant.now().plusSeconds(5)),
                    () ->
                        new org.springframework.jdbc.core.JdbcTemplate(source)
                            .queryForObject("select 1", Integer.class));
              }
              long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
              var asset = registry.find("bm.v2.relay.duration").tag("stage", "asset_tx").timer();
              assertThat(asset).isNotNull();
              assertThat(elapsed).isGreaterThanOrEqualTo(150);
              assertThat(asset.totalTime(TimeUnit.MILLISECONDS)).isLessThan(elapsed - 100);
            });
  }
}
