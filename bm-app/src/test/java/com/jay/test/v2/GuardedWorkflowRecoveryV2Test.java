package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.model.DrawCommand;
import com.jay.domain.raffle.service.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("legacy six-stage/cancel flow replaced by P0; cleanup task")
public class GuardedWorkflowRecoveryV2Test {
  private static final ThreadLocal<io.micrometer.core.instrument.simple.SimpleMeterRegistry>
      TEST_METRICS = ThreadLocal.withInitial(io.micrometer.core.instrument.simple.SimpleMeterRegistry::new);
  @org.junit.Rule public org.junit.rules.TestName observationTestName = new org.junit.rules.TestName();

  @org.junit.Before
  public void resetObservationRegistry() {
    TEST_METRICS.set(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
  }

  private static RelayObservation testObservation() {
    return new com.jay.infrastructure.event.MicrometerRelayObservation(TEST_METRICS.get());
  }

  @org.junit.After
  public void saveActualFixtureObservations() throws Exception {
    var values = new ArrayList<Map<String, Object>>();
    for (var meter : TEST_METRICS.get().getMeters()) {
      if (!(meter instanceof io.micrometer.core.instrument.Counter counter)) continue;
      var tags = new TreeMap<String, String>();
      meter.getId().getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));
      values.add(Map.of("metric", meter.getId().getName(), "tags", tags, "count", counter.count()));
    }
    var directory = java.nio.file.Path.of("target/task8-recovery-observations");
    java.nio.file.Files.createDirectories(directory);
    java.nio.file.Files.writeString(directory.resolve(observationTestName.getMethodName() + ".json"),
        new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
            "testMethod", observationTestName.getMethodName(), "counters", values,
            "provenance", "actual SQL integration fixture; includes deliberate stale/count/failure setup; not runtime workload")));
    TEST_METRICS.remove();
  }

  static WorkflowRedrive guarded(AssetTestSupport h) {
    var ds = h.j.getDataSource();
    var recovery =
        new RecoveryService(
            h.runner,
            new MySqlRecoveryRepository(ds),
            new MySqlOutboxWriter(ds),
            Map.of("DRAW", new MySqlDrawRepository(ds), "PAYMENT", new MySqlPaymentRepository(ds)),
            h.codec::newEventId);
    return new WorkflowRedrive(
        h.runner,
        new MySqlRecoveryDispatchRepository(ds),
        new MySqlDeliveryEvidenceRepository(ds),
        recovery,
        new MySqlOutboxWriter(ds),
        () ->
            h.j
                .queryForObject("select UTC_TIMESTAMP(6)", LocalDateTime.class)
                .toInstant(ZoneOffset.UTC),
        Duration.ofSeconds(30),
        Duration.ofSeconds(30),
        12).withObservation(testObservation());
  }

  @Test
  public void unifiesDrawAndStockRecoveryWithoutChangingCandidate() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "guarded-" + UUID.randomUUID();
          var before =
              DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, before);
          var draws = new MySqlDrawRepository(h.j.getDataSource());
          var stocks = new MySqlStockRepository(h.j.getDataSource());
          h.tx(
              user,
              () -> {
                h.j.update(
                    "update v2_draw_order_"
                        + h.codec.routeUser(user).tableKey()
                        + " set last_progress_at=UTC_TIMESTAMP(6)-interval 60 second where"
                        + " order_id=?",
                    before.orderId());
                h.j.update(
                    "update outbox set state='PUBLISHED',published_at=UTC_TIMESTAMP(6)-interval 60"
                        + " second where aggregate_id=?",
                    before.orderId());
                return null;
              });
          var service =
              new DrawRecoveryService(
                  h.runner,
                  draws,
                  stocks,
                  h.codec,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  guarded(h));
          assertThat(service.inspect(before.orderId()).actions())
              .extracting(EventEnvelope::eventType)
              .containsExactly("ReservationHeld");
          assertThat(service.inspect(before.orderId()).actions()).isEmpty();
          assertThat(h.tx(user, () -> draws.find(before.orderId(), false)).candidate())
              .isEqualTo(before.candidate());
          assertThat(StockProtocolV2Test.control(h, () -> stocks.fact(before.orderId())))
              .isEqualTo(held);
        });
  }

  @Test
  public void actualCooldownAndRemoteRetryEmitFiniteCounters() {
    AssetTestSupport.run(h -> {
      DrawAcceptanceV2Test.schema(h);
      RelayOptimizationTestSupport.schema(h);
      String user = "observed-reasons-" + UUID.randomUUID();
      long activity = DrawAcceptanceV2Test.publish(h, null);
      DrawAcceptanceV2Test.grant(h, user, activity);
      var accepted = DrawAcceptanceV2Test.service(h).create(new DrawCommand(user,
          UUID.randomUUID().toString(), activity, new Deadline(Instant.now().plusSeconds(30))));
      stale(h, user, accepted.orderId());
      var recovery = service(h);
      var emitted = recovery.inspect(accepted.orderId()).actions().getFirst();
      assertThat(emitted.eventType()).isEqualTo("PrepareDraw");
      h.tx(user, () -> { published(h, accepted.orderId()); return null; });
      assertThat(recovery.inspect(accepted.orderId()).actions()).isEmpty();
      assertThat(TEST_METRICS.get().get("bm.v2.relay.count").tag("action", "suppressed")
          .tag("reason", "cooldown").counter().count()).isEqualTo(1);
      String remoteUser = "observed-remote-" + UUID.randomUUID();
      var candidate = DrawCancellationV2Test.accepted(h, remoteUser, activity);
      stale(h, remoteUser, candidate.orderId());
      var remoteCommand = recovery.inspect(candidate.orderId()).actions().getFirst();
      assertThat(remoteCommand.eventType()).isEqualTo("ReserveAward");
      h.tx(remoteUser, () -> { published(h, candidate.orderId()); return null; });
      StockProtocolV2Test.control(h, () -> {
        new MySqlRetryJobRepository(h.j.getDataSource(), h.runner)
            .schedule(remoteCommand, 1, Instant.now().plusSeconds(100));
        return null;
      });
      assertThat(recovery.inspect(candidate.orderId()).actions()).isEmpty();
      assertThat(TEST_METRICS.get().get("bm.v2.relay.count").tag("action", "suppressed")
          .tag("reason", "remote_retry").counter().count()).isEqualTo(1);
      assertThat(TEST_METRICS.get().get("bm.v2.relay.count").tag("action", "redriven")
          .tag("event_type", "PrepareDraw").counter().count()).isEqualTo(1);
      assertThat(h.tx(user, () -> new MySqlDrawRepository(h.j.getDataSource())
          .find(accepted.orderId(), false).state())).isEqualTo("ACCEPTED");
    });
  }

  @Test
  public void scanContractHasFixedUpperAndWraps() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String user = "scan-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          h.tx(
              user,
              () -> {
                var progress = repo.lastProgressAt(r.orderId());
                assertThat(progress).isBeforeOrEqualTo(Instant.now().plusSeconds(2));
                var page = repo.scanStale("CANDIDATE_FIXED", null, Instant.now().plusSeconds(2), 1);
                assertThat(page.next().upper()).isNotNull();
                return null;
              });
        });
  }

  static DrawRecoveryService service(AssetTestSupport h) {
    return new DrawRecoveryService(
        h.runner,
        new MySqlDrawRepository(h.j.getDataSource()),
        new MySqlStockRepository(h.j.getDataSource()),
        h.codec,
        new MySqlOutboxWriter(h.j.getDataSource()),
        guarded(h));
  }

  static void stale(AssetTestSupport h, String user, String order) {
    h.tx(
        user,
        () -> {
          h.j.update(
              "update v2_draw_order_"
                  + h.codec.routeUser(user).tableKey()
                  + " set last_progress_at=UTC_TIMESTAMP(6)-interval 120 second where order_id=?",
              order);
          published(h, order);
          return null;
        });
  }

  static void published(AssetTestSupport h, String order) {
    h.j.update(
        "update outbox set state='PUBLISHED',published_at=UTC_TIMESTAMP(6)-interval 90 second where"
            + " aggregate_id=?",
        order);
  }

  @Test
  public void paidUsesFrozenCommandHashAndHonorsAccountPause() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "paid-guard-" + UUID.randomUUID();
          var command = PaymentDeliveryV2Test.paid(p, user);
          p.control(
              () -> {
                h.j.update(
                    "update payment_order set last_progress_at=UTC_TIMESTAMP(6)-interval 120 second"
                        + " where order_id=?",
                    command.aggregateId());
                published(h, command.aggregateId());
                return null;
              });
          var service =
              new com.jay.domain.payment.service.PaymentRecoveryService(
                  h.runner,
                  p.repo,
                  p.provider,
                  p.facts,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  guarded(h));
          h.tx(
              user,
              () -> {
                h.j.update("update user_credit_account set status='REVIEW' where user_id=?", user);
                return null;
              });
          assertThat(service.inspect(command.aggregateId()).actions()).isEmpty();
          h.runner.inUser(
              user,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("update user_credit_account set status='ACTIVE' where user_id=?", user);
                return null;
              });
          h.tx(
              user,
              () -> {
                new MySqlRecoveryRepository(h.j.getDataSource())
                    .open(
                        "ASSET_RECONCILIATION",
                        new RecoveryAggregate(user, "ACCOUNT", RecoveryStatus.REVIEW, 1),
                        RecoveryStatus.REVIEW,
                        "account audit");
                return null;
              });
          assertThat(service.inspect(command.aggregateId()).actions()).isEmpty();
          h.tx(
              user,
              () -> {
                var cases = new MySqlRecoveryRepository(h.j.getDataSource());
                cases.close(
                    cases.active("ASSET_RECONCILIATION", user), "operator", "verified account");
                return null;
              });
          var event = service.inspect(command.aggregateId()).actions().getFirst();
          assertThat(event.requestHash()).isEqualTo(command.requestHash());
          assertThat(event.requestHash())
              .isNotEqualTo(
                  p.control(() -> p.repo.find(command.aggregateId(), false)).requestHash());
          assertThat(service.inspect(command.aggregateId()).actions()).isEmpty();
        });
  }

  @Test
  public void exhaustionDefersActualEventAndAuditedResumeResetsBudget() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "exhaust-" + UUID.randomUUID();
          var order =
              DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          DrawCancellationV2Test.held(h, order);
          stale(h, user, order.orderId());
          var service = service(h);
          var event = service.inspect(order.orderId()).actions().getFirst();
          h.tx(
              user,
              () -> {
                published(h, order.orderId());
                h.j.update(
                    "update v2_recovery_dispatch set"
                        + " redrive_count=12,last_redrive_at=UTC_TIMESTAMP(6)-interval 90"
                        + " second,next_redrive_at=UTC_TIMESTAMP(6)-interval 60 second where"
                        + " aggregate_type='DRAW' and aggregate_id=?",
                    order.orderId());
                return null;
              });
          assertThat(service.inspect(order.orderId()).actions()).isEmpty();
          var draws = new MySqlDrawRepository(h.j.getDataSource());
          var paused = h.tx(user, () -> draws.find(order.orderId(), false));
          assertThat(paused.recoveryStatus()).isEqualTo(RecoveryStatus.RETRY_EXHAUSTED);
          assertThat(
                  h.tx(
                      user,
                      () ->
                          h.j.queryForObject(
                              "select event_id from deferred_fact where aggregate_type='DRAW' and"
                                  + " aggregate_id=?",
                              String.class,
                              order.orderId())))
              .isEqualTo(event.eventId());
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("DRAW", draws),
                  h.codec::newEventId);
          recovery.resume(
              h.codec.routeUser(user),
              "DRAW",
              order.orderId(),
              paused.objectVersion(),
              "operator",
              "verified original fact");
          assertThat(
                  h.tx(
                          user,
                          () ->
                              new MySqlRecoveryDispatchRepository(h.j.getDataSource())
                                  .lock("DRAW", order.orderId()))
                      .count())
              .isZero();
          h.tx(
              user,
              () -> {
                published(h, order.orderId());
                return null;
              });
          var after = service.inspect(order.orderId()).actions();
          assertThat(after).hasSize(1);
          assertThat(after.getFirst().payload()).isEqualTo(event.payload());
          assertThat(after.getFirst().eventId()).isNotEqualTo(event.eventId());
          assertThat(h.tx(user, () -> draws.find(order.orderId(), false)).candidate())
              .isEqualTo(order.candidate());
        });
  }

  @Test
  public void fixedUpperExcludesNewHighKeysThenWrapFindsLateSmallKey() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          var slot = new RouteSlot("01", "000");
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                String prefix = "cursor-" + UUID.randomUUID();
                for (String id : List.of("b", "d"))
                  h.j.update(
                      "insert into"
                          + " v2_draw_order_000(order_id,user_id,activity_id,state,snapshot,last_progress_at)"
                          + " values(?,? ,1,'ACCEPTED','{}','2001-01-01 00:00:00')",
                      prefix + id,
                      prefix);
                var repo = new MySqlDrawRepository(h.j.getDataSource());
                var first =
                    repo.scanStale("ACCEPTED", null, Instant.parse("2002-01-01T00:00:00Z"), 1);
                assertThat(first.ids()).containsExactly(prefix + "b");
                assertThat(first.exhausted()).isFalse();
                for (String id : List.of("a", "z"))
                  h.j.update(
                      "insert into"
                          + " v2_draw_order_000(order_id,user_id,activity_id,state,snapshot,last_progress_at)"
                          + " values(?,? ,1,'ACCEPTED','{}','2001-01-01 00:00:00')",
                      prefix + id,
                      prefix);
                var second =
                    repo.scanStale(
                        "ACCEPTED", first.next(), Instant.parse("2002-01-01T00:00:00Z"), 8);
                assertThat(second.ids()).containsExactly(prefix + "d");
                assertThat(second.exhausted()).isTrue();
                var wrapped =
                    repo.scanStale("ACCEPTED", null, Instant.parse("2002-01-01T00:00:00Z"), 8);
                assertThat(wrapped.ids()).contains(prefix + "a", prefix + "z");
                h.j.update("delete from v2_draw_order_000 where user_id=?", prefix);
                return null;
              });
        });
  }

  @Test
  public void scansAllEightTablesAndCountsActualEmissionsWithTimeBudget() {
    var scanner = new WorkflowRecoveryScan();
    var partitions = WorkflowRecoveryScan.userPartitions(List.of("ACCEPTED", "COMMITTED"));
    var visited = new HashSet<RouteSlot>();
    var checked = new java.util.concurrent.atomic.AtomicInteger();
    var emitted =
        scanner.run(
            partitions,
            (p, c) -> {
              visited.add(p.slot());
              return new RecoveryScanPage(List.of("id"), null, true);
            },
            id -> {
              checked.incrementAndGet();
              return 0;
            });
    assertThat(visited).hasSize(8);
    assertThat(checked.get()).isEqualTo(64);
    assertThat(emitted).isZero();
    checked.set(0);
    scanner.run(
        partitions,
        (p, c) -> new RecoveryScanPage(List.of("id"), null, true),
        id -> {
          checked.incrementAndGet();
          return 1;
        });
    assertThat(checked.get()).isEqualTo(8);
    checked.set(0);
    scanner.budget(64, 8, Duration.ofMillis(20));
    scanner.run(
        partitions,
        (p, c) -> {
          try {
            Thread.sleep(30);
          } catch (InterruptedException e) {
            throw new IllegalStateException(e);
          }
          return new RecoveryScanPage(List.of("id"), null, true);
        },
        id -> {
          checked.incrementAndGet();
          return 1;
        });
    assertThat(checked.get()).isZero();
  }

  @Test
  public void freshProgressAndPausedStockNeverEmitAndHeldIsRetained() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "paused-stock-" + UUID.randomUUID();
          var order =
              DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, order);
          var service = service(h);
          h.tx(
              user,
              () -> {
                published(h, order.orderId());
                return null;
              });
          assertThat(service.inspect(order.orderId()).actions()).isEmpty();
          stale(h, user, order.orderId());
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update(
                    "update award_reservation set recovery_status='WAITING_FIX' where"
                        + " draw_order_id=?",
                    order.orderId());
                return null;
              });
          assertThatThrownBy(() -> service.inspect(order.orderId()))
              .isInstanceOf(IllegalStateException.class);
          assertThat(
                  StockProtocolV2Test.control(
                      h, () -> new MySqlStockRepository(h.j.getDataSource()).fact(order.orderId())))
              .isEqualTo(held);
        });
  }

  @Test
  public void actualTwoProcessesDrawAndStockShareOneOwnerGuard() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "process-" + UUID.randomUUID();
          h.overrideTime = BusinessTime.at(Instant.parse("2026-01-31T15:59:59Z"));
          var order =
              DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, order);
          stale(h, user, order.orderId());
          long activity = DrawAcceptanceV2Test.publish(h, null);
          DrawAcceptanceV2Test.grant(h, user, activity);
          var accepted =
              DrawAcceptanceV2Test.service(h)
                  .create(
                      new com.jay.domain.raffle.model.DrawCommand(
                          user,
                          UUID.randomUUID().toString(),
                          activity,
                          new Deadline(Instant.now().plusSeconds(30))));
          stale(h, user, accepted.orderId());
          var directory =
              java.nio.file.Files.createTempDirectory(
                      java.nio.file.Path.of(
                          "../.superpowers/sdd/2026-10-16-outbox-relay-recovery-optimization"),
                      "task6-process-")
                  .toAbsolutePath();
          var children = new ArrayList<Process>();
          try {
            for (String mode : List.of("draw", "stock")) {
              children.add(
                  new ProcessBuilder(
                          java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java")
                              .toString(),
                          "-Xmx128m",
                          "-cp",
                          System.getProperty("java.class.path"),
                          GuardedRecoveryChild.class.getName(),
                          mode,
                          order.orderId() + "," + accepted.orderId(),
                          directory.toString())
                      .redirectErrorStream(true)
                      .redirectOutput(directory.resolve(mode + ".log").toFile())
                      .start());
            }
            org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(
                    () ->
                        java.nio.file.Files.exists(directory.resolve("draw.ready"))
                            && java.nio.file.Files.exists(directory.resolve("stock.ready")));
            java.nio.file.Files.writeString(directory.resolve("go"), "go");
            for (int i = 0; i < children.size(); i++) {
              assertThat(children.get(i).waitFor(30, java.util.concurrent.TimeUnit.SECONDS))
                  .isTrue();
              assertThat(children.get(i).exitValue())
                  .withFailMessage(
                      java.nio.file.Files.readString(
                          directory.resolve(i == 0 ? "draw.log" : "stock.log")))
                  .isZero();
            }
          } finally {
            for (var child : children) if (child.isAlive()) child.destroyForcibly();
          }
          assertThat(
                  h.tx(
                      user,
                      () ->
                          h.j.queryForObject(
                              "select count(*) from outbox where aggregate_id=? and"
                                  + " event_type='ReservationHeld'",
                              Integer.class,
                              order.orderId())))
              .isEqualTo(1);
          assertThat(
                  h.tx(
                      user,
                      () ->
                          h.j.queryForObject(
                              "select count(*) from outbox where aggregate_id=? and"
                                  + " event_type='PrepareDraw'",
                              Integer.class,
                              accepted.orderId())))
              .isEqualTo(2);
          var after =
              h.tx(
                  user,
                  () -> new MySqlDrawRepository(h.j.getDataSource()).find(order.orderId(), false));
          assertThat(after.candidate()).isEqualTo(order.candidate());
          assertThat(after.time()).isEqualTo(order.time());
          assertThat(
                  StockProtocolV2Test.control(
                      h, () -> new MySqlStockRepository(h.j.getDataSource()).fact(order.orderId())))
              .isEqualTo(held);
        });
  }

  @Test
  public void allEightTablesKeepIndependentStateUpperBoundsAndSeeLateCommits() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          var cursors = new HashMap<WorkflowRecoveryScan.Partition, RecoveryScanPosition>();
          for (var partition :
              WorkflowRecoveryScan.userPartitions(List.of("ACCEPTED", "COMMITTED"))) {
            h.runner.inSlot(
                partition.slot(),
                WorkClass.RECOVERY,
                RelayOptimizationTestSupport.deadline(),
                () -> {
                  String prefix = "eight-" + UUID.randomUUID();
                  String table = "v2_draw_order_" + partition.slot().tableKey();
                  String sql =
                      "insert into "
                          + table
                          + "(order_id,user_id,activity_id,state,snapshot,last_progress_at)"
                          + " values(?,?,1,?,'{}','1999-01-01 00:00:00')";
                  for (String id : List.of("b", "d"))
                    h.j.update(sql, prefix + id, prefix, partition.state());
                  var repo = new MySqlDrawRepository(h.j.getDataSource());
                  var cutoff = Instant.parse("2000-01-01T00:00:00Z");
                  var first = repo.scanStale(partition.state(), null, cutoff, 1);
                  cursors.put(partition, first.next());
                  assertThat(first.ids()).containsExactly(prefix + "b");
                  try (var late = h.j.getDataSource().getConnection()) {
                    late.setAutoCommit(false);
                    try (var stmt = late.prepareStatement(sql)) {
                      stmt.setString(1, prefix + "a");
                      stmt.setString(2, prefix);
                      stmt.setString(3, partition.state());
                      stmt.executeUpdate();
                    }
                    for (int i = 0; i < 3; i++)
                      h.j.update(sql, prefix + "z" + i, prefix, partition.state());
                    var end = repo.scanStale(partition.state(), cursors.get(partition), cutoff, 8);
                    assertThat(end.ids()).containsExactly(prefix + "d");
                    assertThat(end.exhausted()).isTrue();
                    late.commit();
                    var wrap = repo.scanStale(partition.state(), null, cutoff, 8);
                    assertThat(wrap.ids())
                        .contains(prefix + "a", prefix + "z0", prefix + "z1", prefix + "z2");
                  } catch (java.sql.SQLException e) {
                    throw new IllegalStateException(e);
                  }
                  h.j.update("delete from " + table + " where user_id=?", prefix);
                  return null;
                });
          }
          assertThat(cursors).hasSize(16);
          assertThat(cursors.keySet().stream().map(WorkflowRecoveryScan.Partition::slot).distinct())
              .hasSize(8);
        });
  }

  @Test
  public void guardedStartupRequires007WhileLegacyStillStarts() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          var config = new com.jay.config.V2RabbitConfig();
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  h.codec::newEventId);
          var policy = new com.jay.config.V2RecoveryRedriveProperties();
          policy.setMode("guarded");
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.execute("rename table v2_recovery_dispatch to task6_dispatch_saved");
                return null;
              });
          try {
            new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(
                    WorkflowRedrive.class,
                    () ->
                        config.workflowRedrive(
                            h.j.getDataSource(),
                            h.runner,
                            recovery,
                            new MySqlOutboxWriter(h.j.getDataSource()),
                            policy))
                .run(
                    c -> {
                      assertThat(c).hasFailed();
                      assertThat(c.getStartupFailure())
                          .hasStackTraceContaining("v2_recovery_dispatch");
                    });
            policy.setMode("legacy");
            new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(
                    WorkflowRedrive.class,
                    () ->
                        config.workflowRedrive(
                            h.j.getDataSource(),
                            h.runner,
                            recovery,
                            new MySqlOutboxWriter(h.j.getDataSource()),
                            policy))
                .run(c -> assertThat(c).hasNotFailed());
          } finally {
            StockProtocolV2Test.control(
                h,
                () -> {
                  h.j.execute("rename table task6_dispatch_saved to v2_recovery_dispatch");
                  return null;
                });
          }
        });
  }

  @Test
  public void mismatchedAuthorityIsPausedInsteadOfReplayed() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "mismatch-authority-" + UUID.randomUUID();
          var order =
              DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, order);
          stale(h, user, order.orderId());
          var bad =
              new com.jay.domain.stock.model.ReservationResult(
                  held.drawOrderId(),
                  held.userId(),
                  "b".repeat(64),
                  held.requestHash(),
                  held.state(),
                  held.finalSnapshot(),
                  held.quantity());
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update(
                    "update award_reservation set snapshot=? where draw_order_id=?",
                    JsonStore.write(bad),
                    order.orderId());
                return null;
              });
          assertThat(service(h).inspect(order.orderId()).actions()).isEmpty();
          assertThat(
                  h.tx(
                          user,
                          () ->
                              new MySqlDrawRepository(h.j.getDataSource())
                                  .find(order.orderId(), false))
                      .recoveryStatus())
              .isEqualTo(RecoveryStatus.REVIEW);
        });
  }

  @Test
  public void allowedActionsFollowFrozenStateAndAuthority() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "states-" + UUID.randomUUID();
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          var r = DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          stale(h, user, r.orderId());
          assertThat(service(h).inspect(r.orderId()).actions())
              .extracting(EventEnvelope::eventType)
              .containsExactly("ReserveAward");
          var held = DrawCancellationV2Test.held(h, r);
          h.tx(
              user,
              () -> {
                DrawCancellationV2Test.progress(h)
                    .onHeld(DrawCancellationV2Test.receipt(h, r, "ReservationHeld", held));
                return null;
              });
          var recorded = h.tx(user, () -> repo.find(r.orderId(), false));
          stale(h, user, r.orderId());
          h.tx(
              user,
              () -> {
                h.j.update(
                    "update v2_recovery_dispatch set next_redrive_at=UTC_TIMESTAMP(6)-interval 1"
                        + " second,last_redrive_at=UTC_TIMESTAMP(6)-interval 60 second where"
                        + " aggregate_id=?",
                    r.orderId());
                return null;
              });
          assertThat(service(h).inspect(r.orderId()).actions())
              .extracting(EventEnvelope::eventType)
              .containsExactly("ConfirmReservation");
          var confirmed =
              StockProtocolV2Test.control(
                  h,
                  () ->
                      StockProtocolV2Test.service(h)
                          .confirm(
                              new com.jay.domain.stock.model.ConfirmCommand(
                                  r.orderId(),
                                  user,
                                  r.identityHash(),
                                  r.requestHash(),
                                  recorded.finalAward().configHash())));
          h.tx(
              user,
              () -> {
                published(h, r.orderId());
                h.j.update(
                    "update v2_recovery_dispatch set next_redrive_at=UTC_TIMESTAMP(6)-interval 1"
                        + " second,last_redrive_at=UTC_TIMESTAMP(6)-interval 60 second where"
                        + " aggregate_id=?",
                    r.orderId());
                return null;
              });
          assertThat(service(h).inspect(r.orderId()).actions())
              .extracting(EventEnvelope::eventType)
              .containsExactly("ReservationConfirmed");
          h.tx(
              user,
              () -> {
                DrawCancellationV2Test.progress(h)
                    .onConfirmed(
                        DrawCancellationV2Test.receipt(
                            h, recorded, "ReservationConfirmed", confirmed));
                return null;
              });
          stale(h, user, r.orderId());
          h.tx(
              user,
              () -> {
                h.j.update(
                    "update v2_recovery_dispatch set next_redrive_at=UTC_TIMESTAMP(6)-interval 1"
                        + " second,last_redrive_at=UTC_TIMESTAMP(6)-interval 60 second where"
                        + " aggregate_id=?",
                    r.orderId());
                return null;
              });
          assertThat(service(h).inspect(r.orderId()).actions())
              .extracting(EventEnvelope::eventType)
              .containsExactly("AwardDelivery");
          var cancel =
              DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          DrawAcceptanceV2Test.service(h).cancel(user, cancel.orderId());
          stale(h, user, cancel.orderId());
          assertThat(service(h).inspect(cancel.orderId()).actions())
              .extracting(EventEnvelope::eventType)
              .containsExactly("CancelReservation");
          StockProtocolV2Test.control(h, () -> StockProtocolV2Test.service(h).cancel(
              new com.jay.domain.stock.model.CancelCommand(cancel.orderId(), user,
                  cancel.identityHash(), cancel.requestHash())));
          h.tx(user, () -> {
            published(h, cancel.orderId());
            h.j.update("update v2_recovery_dispatch set next_redrive_at=UTC_TIMESTAMP(6)-interval 1 second,"
                + "last_redrive_at=UTC_TIMESTAMP(6)-interval 60 second where aggregate_id=?", cancel.orderId());
            return null;
          });
          assertThat(service(h).inspect(cancel.orderId()).actions())
              .extracting(EventEnvelope::eventType).containsExactly("ReservationCancelled");
          assertThat(h.tx(user, () -> repo.find(r.orderId(), false)).candidate())
              .isEqualTo(r.candidate());
        });
  }

  @Test
  public void remoteReadFailureRetainsHeldWithoutNewEvent() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "remote-failure-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, r);
          stale(h, user, r.orderId());
          var ds = h.j.getDataSource();
          var actual = new MySqlDeliveryEvidenceRepository(ds);
          var unavailable =
              new com.jay.domain.task.repository.DeliveryEvidenceRepository() {
                public DeliveryEvidence local(
                    String type, String id, String eventType, String hash) {
                  return actual.local(type, id, eventType, hash);
                }

                public DeliveryEvidence target(EventEnvelope e) {
                  throw new IllegalStateException("authority timeout");
                }

                public boolean operationCompleted(String c, EventEnvelope e) {
                  return actual.operationCompleted(c, e);
                }
              };
          var recover =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(ds),
                  new MySqlOutboxWriter(ds),
                  Map.of("DRAW", new MySqlDrawRepository(ds)),
                  h.codec::newEventId);
          var guard =
              new WorkflowRedrive(
                  h.runner,
                  new MySqlRecoveryDispatchRepository(ds),
                  unavailable,
                  recover,
                  new MySqlOutboxWriter(ds),
                  () ->
                      h.j
                          .queryForObject("select UTC_TIMESTAMP(6)", LocalDateTime.class)
                          .toInstant(ZoneOffset.UTC),
                  Duration.ofSeconds(30),
                  Duration.ofSeconds(30),
                  12).withObservation(testObservation());
          var service =
              new DrawRecoveryService(
                  h.runner,
                  new MySqlDrawRepository(ds),
                  new MySqlStockRepository(ds),
                  h.codec,
                  new MySqlOutboxWriter(ds),
                  guard);
          assertThat(service.inspect(r.orderId()).actions()).isEmpty();
          assertThat(
                  StockProtocolV2Test.control(
                      h, () -> new MySqlStockRepository(ds).fact(r.orderId())))
              .isEqualTo(held);
        });
  }

  @Test
  public void appendFailureRollsBackEventAndDispatchTogether() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "rollback-guard-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          DrawCancellationV2Test.held(h, r);
          stale(h, user, r.orderId());
          var ds = h.j.getDataSource();
          var writer = new MySqlOutboxWriter(ds);
          var draws = new MySqlDrawRepository(ds);
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(ds),
                  writer,
                  Map.of("DRAW", draws),
                  h.codec::newEventId);
          var guard =
              new WorkflowRedrive(
                  h.runner,
                  new MySqlRecoveryDispatchRepository(ds),
                  new MySqlDeliveryEvidenceRepository(ds),
                  recovery,
                  e -> {
                    writer.append(e);
                    throw new IllegalStateException("after append rollback");
                  },
                  () ->
                      h.j
                          .queryForObject("select UTC_TIMESTAMP(6)", LocalDateTime.class)
                          .toInstant(ZoneOffset.UTC),
                  Duration.ofSeconds(30),
                  Duration.ofSeconds(30),
                  12).withObservation(testObservation());
          var service =
              new DrawRecoveryService(
                  h.runner, draws, new MySqlStockRepository(ds), h.codec, writer, guard);
          assertThatThrownBy(() -> service.inspect(r.orderId()))
              .hasMessage("after append rollback");
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from outbox where aggregate_id=? and"
                                + " event_type='ReservationHeld'",
                            Integer.class,
                            r.orderId()))
                    .isZero();
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from v2_recovery_dispatch where aggregate_type='DRAW'"
                                + " and aggregate_id=?",
                            Integer.class,
                            r.orderId()))
                    .isZero();
                return null;
              });
        });
  }

  @Test
  public void dedicatedScanReplaysDeferredWithoutScanningNormalActiveOrders() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "dedicated-scan-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
          var ds = h.j.getDataSource();
          var draws = new MySqlDrawRepository(ds);
          var scans = new java.util.concurrent.atomic.AtomicInteger();
          var port =
              new com.jay.domain.task.repository.RecoveryAggregatePort() {
                public RecoveryAggregate lock(String id) {
                  return draws.lock(id);
                }

                public boolean updateStatus(String id, long version, RecoveryStatus status) {
                  return draws.updateStatus(id, version, status);
                }

                public List<String> scanActive(int limit) {
                  scans.incrementAndGet();
                  return List.of();
                }
              };
          var cases = new MySqlRecoveryRepository(ds);
          var service =
              new RecoveryService(
                      h.runner,
                      cases,
                      new MySqlOutboxWriter(ds),
                      Map.of("DRAW", port),
                      h.codec::newEventId)
                  .dedicatedWorkflows(true);
          service.pause(
              h.codec.routeUser(user), "DRAW", r.orderId(), RecoveryStatus.REVIEW, "audit");
          var paused = h.tx(user, () -> draws.find(r.orderId(), false));
          service.resume(
              h.codec.routeUser(user),
              "DRAW",
              r.orderId(),
              paused.objectVersion(),
              "operator",
              "verified");
          var event =
              DrawService.event(
                  r, "PrepareDraw", h.codec.routeUser(user), h.codec.newEventId(), "{}");
          h.tx(
              user,
              () -> {
                cases.defer("workflow-v2", "DRAW", event);
                return null;
              });
          service.scan(h.codec.routeUser(user), 128);
          assertThat(scans.get()).isZero();
          assertThat(
                  h.tx(
                      user,
                      () ->
                          h.j.queryForObject(
                              "select replayed_event_id from deferred_fact where event_id=?",
                              String.class,
                              event.eventId())))
              .isNotBlank()
              .isNotEqualTo(event.eventId());
        });
  }

  @Test
  public void guardedConfigurationRejectsMoreThanTwoSecondsAndKeepsLegacyCompatible() {
    var scan = new WorkflowRecoveryScan();
    assertThatThrownBy(() -> scan.budget(64, 8, Duration.ofSeconds(3)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> scan.budget(1, 2, Duration.ofSeconds(2)))
        .isInstanceOf(IllegalArgumentException.class);
    var properties = new com.jay.config.V2RecoveryRedriveProperties();
    properties.setRoundTimeoutSeconds(3);
    new org.springframework.boot.test.context.runner.ApplicationContextRunner()
        .withBean(com.jay.config.V2RecoveryRedriveProperties.class, () -> properties)
        .run(c -> assertThat(c).hasNotFailed());
    properties.setMode("guarded");
    new org.springframework.boot.test.context.runner.ApplicationContextRunner()
        .withBean(com.jay.config.V2RecoveryRedriveProperties.class, () -> properties)
        .run(c -> assertThat(c).hasFailed());
  }

  @Test
  public void localPublishedPayloadMustMatchCurrentFrozenCommand() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          RelayOptimizationTestSupport.schema(h);
          String user = "local-mismatch-" + UUID.randomUUID();
          var original = PaymentDeliveryV2Test.paid(p, user);
          var payload = com.jay.domain.payment.model.PaymentPayload.read(original.payload());
          var changed =
              new com.jay.domain.payment.model.PaymentPayload(
                  payload.paymentOrderId(),
                  payload.userId(),
                  payload.routeSlot(),
                  new java.math.BigDecimal("999.00"),
                  payload.requestHash(),
                  payload.paymentVersion(),
                  null);
          var corrupted =
              new EventEnvelope(
                  original.eventId(),
                  original.eventType(),
                  original.aggregateId(),
                  original.bizId(),
                  original.userId(),
                  original.routeSlot(),
                  original.requestHash(),
                  original.aggregateVersion(),
                  original.schemaVersion(),
                  com.jay.domain.payment.model.PaymentPayload.json(changed));
          p.control(
              () -> {
                h.j.update(
                    "update payment_order set last_progress_at=UTC_TIMESTAMP(6)-interval 120 second"
                        + " where order_id=?",
                    original.aggregateId());
                h.j.update(
                    "update outbox set"
                        + " payload=?,payload_hash=?,state='PUBLISHED',published_at=UTC_TIMESTAMP(6)-interval"
                        + " 90 second where event_id=?",
                    JsonStore.write(corrupted),
                    new com.jay.domain.asset.service.RequestHasher()
                        .hash("EVENT", Map.of("envelope", JsonStore.write(corrupted))),
                    original.eventId());
                return null;
              });
          var service =
              new com.jay.domain.payment.service.PaymentRecoveryService(
                  h.runner,
                  p.repo,
                  p.provider,
                  p.facts,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  guarded(h));
          assertThat(service.inspect(original.aggregateId()).actions()).isEmpty();
          assertThat(p.control(() -> p.repo.find(original.aggregateId(), false)).recoveryStatus())
              .isEqualTo(RecoveryStatus.REVIEW);
        });
  }

  @Test
  public void committedWithHeldOrMissingStockIsReviewNotDelivery() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          RelayOptimizationTestSupport.schema(h);
          for (boolean missing : List.of(false, true)) {
            String user = "committed-authority-" + UUID.randomUUID();
            var r = DrawCancellationV2Test.accepted(h, user, DrawAcceptanceV2Test.publish(h, null));
            var held = DrawCancellationV2Test.held(h, r);
            h.tx(
                user,
                () -> {
                  DrawCancellationV2Test.progress(h)
                      .onHeld(DrawCancellationV2Test.receipt(h, r, "ReservationHeld", held));
                  return null;
                });
            var recorded =
                h.tx(
                    user,
                    () -> new MySqlDrawRepository(h.j.getDataSource()).find(r.orderId(), false));
            var confirmed =
                StockProtocolV2Test.control(
                    h,
                    () ->
                        StockProtocolV2Test.service(h)
                            .confirm(
                                new com.jay.domain.stock.model.ConfirmCommand(
                                    r.orderId(),
                                    user,
                                    r.identityHash(),
                                    r.requestHash(),
                                    recorded.finalAward().configHash())));
            h.tx(
                user,
                () -> {
                  DrawCancellationV2Test.progress(h)
                      .onConfirmed(
                          DrawCancellationV2Test.receipt(
                              h, recorded, "ReservationConfirmed", confirmed));
                  return null;
                });
            StockProtocolV2Test.control(
                h,
                () -> {
                  if (missing)
                    h.j.update("delete from award_reservation where draw_order_id=?", r.orderId());
                  else
                    h.j.update(
                        "update award_reservation set state='HELD',snapshot=? where"
                            + " draw_order_id=?",
                        JsonStore.write(held),
                        r.orderId());
                  return null;
                });
            stale(h, user, r.orderId());
            assertThat(service(h).inspect(r.orderId()).actions()).isEmpty();
            assertThat(
                    h.tx(
                            user,
                            () ->
                                new MySqlDrawRepository(h.j.getDataSource())
                                    .find(r.orderId(), false))
                        .recoveryStatus())
                .isEqualTo(RecoveryStatus.REVIEW);
          }
        });
  }
}
