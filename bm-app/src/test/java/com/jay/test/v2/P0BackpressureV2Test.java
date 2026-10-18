package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.V2MetricsConfig;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.service.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.cache.P0AdmissionCache;
import com.jay.infrastructure.event.MicrometerRelayObservation;
import com.jay.infrastructure.id.BlockOrderIdGenerator;
import com.jay.infrastructure.observability.P0BacklogProbe;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/** Task 9：入口背压、黑名单受理和 P0 指标。 */
public class P0BackpressureV2Test {

  @Test
  public void rejectsNewDrawOnlyWhenPendingExceedsBacklogAndRecoversAutomatically() {
    var gate = new P0AdmissionGate();
    gate.update(new P0BacklogSnapshot(20_001, 10));
    assertThat(gate.tryEnter("u1", 1)).isFalse();
    assertThat(gate.inflight()).isZero();

    /** oldestAge 只用于指标/告警：单笔卡单不能在全局范围拒绝所有 draw。 */
    gate.update(new P0BacklogSnapshot(0, 121));
    assertThat(gate.tryEnter("u1", 1)).isTrue();
    gate.leave("u1");

    /** 阈值是保护线：等于 20,000 仍然放行；age 再大也不参与拒绝。 */
    gate.update(new P0BacklogSnapshot(20_000, 120));
    assertThat(gate.tryEnter("u1", 1)).isTrue();
    gate.leave("u1");

    gate.update(new P0BacklogSnapshot(0, 0));
    assertThat(gate.tryEnter("u1", 1)).isTrue();
    gate.leave("u1");
  }

  @Test
  public void perUserLimitReleasesAfterAdmission() {
    var gate = new P0AdmissionGate();
    gate.update(new P0BacklogSnapshot(0, 0));
    assertThat(gate.tryEnter("u1", 1)).isTrue();
    assertThat(gate.tryEnter("u1", 1)).isFalse();
    assertThat(gate.inflight()).isOne();

    gate.leave("u1");
    assertThat(gate.inflight()).isZero();
    assertThat(gate.tryEnter("u1", 1)).isTrue();
    gate.leave("u1");

    /** 同一 requestId 的并发重试共享一次在途许可，不同 requestId 被拒绝。 */
    assertThat(gate.tryEnter("u2", 1, "request-1")).isTrue();
    assertThat(gate.tryEnter("u2", 1, "request-1")).isTrue();
    assertThat(gate.tryEnter("u2", 1, "request-2")).isFalse();
    gate.leave("u2");
    assertThat(gate.inflight()).isOne();
    gate.leave("u2");
    assertThat(gate.inflight()).isZero();
  }

  @Test
  public void probeAggregatesPendingOrdersAndKeepsLastSnapshotOnFailure() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          clearOrders(h);
          insertOrder(h, "01", 0, "old-pending", "PENDING_RESERVATION", 200);
          insertOrder(h, "02", 3, "new-pending", "PENDING_RESERVATION", 5);
          insertOrder(h, "01", 1, "delivered", "DELIVERED", 1_000);

          var gate = new P0AdmissionGate();
          try (var probe = new P0BacklogProbe(h.j.getDataSource(), gate::update)) {
            var snapshot = probe.refresh();
            assertThat(snapshot.pending()).isEqualTo(2);
            assertThat(snapshot.oldestAgeSeconds()).isBetween(195L, 230L);
            assertThat(gate.snapshot()).isEqualTo(snapshot);

            var failing = new AtomicBoolean();
            try (var flaky =
                new P0BacklogProbe(
                    new DelegatingDataSource(h.j.getDataSource()) {
                      @Override
                      public Connection getConnection() throws SQLException {
                        if (failing.get()) throw new SQLException("probe down");
                        return super.getConnection();
                      }
                    })) {
              var before = flaky.refresh();
              failing.set(true);
              assertThat(flaky.refresh()).isEqualTo(before);
            }
          }
          clearOrders(h);
        });
  }

  @Test
  public void sameRequestReturnsExistingOrderBeforeBacklogRejection() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long activity = DrawAcceptanceV2Test.publish(h, null);
          String user = "p0-idempotent-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, user, activity);
          var gate = new P0AdmissionGate();
          var service = service(h, gate);
          String request = "request-" + UUID.randomUUID();
          var first =
              service.create(
                  new DrawCommand(
                      user, request, activity, new Deadline(Instant.now().plusSeconds(60))));

          gate.update(new P0BacklogSnapshot(20_001, 0));
          var repeat =
              service.create(
                  new DrawCommand(
                      user, request, activity, new Deadline(Instant.now().plusSeconds(60))));
          assertThat(repeat.orderId()).isEqualTo(first.orderId());
          assertThat(gate.inflight()).isZero();

          assertThatThrownBy(
                  () ->
                      service.create(
                          new DrawCommand(
                              user,
                              "different-" + UUID.randomUUID(),
                              activity,
                              new Deadline(Instant.now().plusSeconds(60)))))
              .isInstanceOf(BudgetExceededException.class);
        });
  }

  @Test
  public void inFlightUserRejectsNewRequestButAllowsRetryOfSameRequest() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long activity = DrawAcceptanceV2Test.publish(h, null);
          String user = "p0-inflight-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, user, activity);
          var gate = new P0AdmissionGate();
          var service = service(h, gate);
          String request = "request-" + UUID.randomUUID();

          assertThat(gate.tryEnter(user, activity, request)).isTrue();
          assertThatThrownBy(
                  () ->
                      service.create(
                          new DrawCommand(
                              user,
                              "different-" + UUID.randomUUID(),
                              activity,
                              new Deadline(Instant.now().plusSeconds(60)))))
              .isInstanceOf(BudgetExceededException.class);

          var accepted =
              service.create(
                  new DrawCommand(
                      user, request, activity, new Deadline(Instant.now().plusSeconds(60))));
          assertThat(accepted.state()).isEqualTo("PENDING_RESERVATION");
          assertThat(gate.inflight()).isOne();
          gate.leave(user);
          assertThat(gate.inflight()).isZero();
        });
  }

  @Test
  public void blacklistedDrawPersistsRejectedWithoutQuotaOrOutbox() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long activity = DrawAcceptanceV2Test.publish(h, null);
          String user = "p0-blacklist-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, user, activity);
          h.j.update("insert into v2_asset_blacklist values(?)", user);
          var gate = new P0AdmissionGate();
          var service = service(h, gate);

          var view =
              service.create(
                  new DrawCommand(
                      user,
                      "blocked-" + UUID.randomUUID(),
                      activity,
                      new Deadline(Instant.now().plusSeconds(60))));

          assertThat(view.state()).isEqualTo("REJECTED");
          assertThat(h.balance(user)).isEqualByComparingTo("0.00");
          assertThat(quota(h, user, activity)).isEqualTo(10);
          assertThat(gate.inflight()).isZero();
          h.tx(
              user,
              () -> {
                var record =
                    new MySqlDrawRepository(h.j.getDataSource()).find(view.orderId(), false);
                assertThat(record.state()).isEqualTo("REJECTED");
                assertThat(record.ruleInput().blacklisted()).isTrue();
                assertThat(record.candidate()).isNull();
                assertThat(record.fallback()).isNull();
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from outbox where aggregate_id=?",
                            Integer.class,
                            view.orderId()))
                    .isZero();
                return null;
              });
        });
  }

  @Test
  public void deliveryRecordsP0MetricsAndKeepsExactlyOnceSemantics() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long activity = DrawAcceptanceV2Test.publish(h, null);
          String user = "p0-delivery-metrics-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, user, activity);
          var view =
              DrawAcceptanceV2Test.service(h)
                  .create(
                      new DrawCommand(
                          user,
                          "deliver-" + UUID.randomUUID(),
                          activity,
                          new Deadline(Instant.now().plusSeconds(60))));
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          var record = h.tx(user, () -> repo.find(view.orderId(), false));
          var result =
              new ReservationResult(
                  record.orderId(),
                  record.userId(),
                  record.identityHash(),
                  record.requestHash(),
                  "CONFIRMED",
                  record.candidate(),
                  record.candidate().quantity());
          var event =
              new EventEnvelope(
                  h.codec.newEventId(),
                  "ReservationConfirmed",
                  record.orderId(),
                  record.biz(),
                  record.userId(),
                  h.codec.routeUser(record.userId()),
                  record.requestHash(),
                  record.objectVersion(),
                  2,
                  ReceiptJson.write(result));

          var registry = new SimpleMeterRegistry();
          var delivery =
              new DrawDeliveryService(
                  repo, h.assets, new MicrometerRelayObservation(registry));

          /** 事务回滚不能把 delivery 记成 delivered：指标只在 afterCommit 自增。 */
          assertThatThrownBy(
                  () ->
                      h.tx(
                          user,
                          () -> {
                            delivery.deliver(event);
                            throw new IllegalStateException("force rollback");
                          }))
              .isInstanceOf(IllegalStateException.class);
          assertThat(
                  registry.find("p0_delivery_total").tag("result", "delivered").counter().count())
              .isZero();
          assertThat(
                  registry
                      .find("p0_event_total")
                      .tag("type", "ReservationConfirmed")
                      .tag("result", "delivered")
                      .counter()
                      .count())
              .isZero();
          assertThat(
                  registry
                      .find("p0_stage_seconds")
                      .tag("stage", "deliver")
                      .tag("result", "delivered")
                      .timer()
                      .count())
              .isZero();
          assertThat(h.tx(user, () -> repo.find(view.orderId(), false)).state())
              .isEqualTo("PENDING_RESERVATION");

          h.tx(
              user,
              () -> {
                delivery.deliver(event);
                return null;
              });
          assertThat(
                  registry.find("p0_delivery_total").tag("result", "delivered").counter().count())
              .isOne();
          assertThat(
                  registry
                      .find("p0_event_total")
                      .tag("type", "ReservationConfirmed")
                      .tag("result", "delivered")
                      .counter()
                      .count())
              .isOne();
          assertThat(
                  registry
                      .find("p0_stage_seconds")
                      .tag("stage", "deliver")
                      .tag("result", "delivered")
                      .timer()
                      .count())
              .isOne();
          assertThat(h.tx(user, () -> repo.find(view.orderId(), false)).state())
              .isEqualTo("DELIVERED");

          h.tx(
              user,
              () -> {
                delivery.deliver(event);
                return null;
              });
          assertThat(
                  registry.find("p0_delivery_total").tag("result", "duplicate").counter().count())
              .isOne();
        });
  }

  @Test
  public void p0MetricsExposeCountersTimersAndGauges() {
    var registry = new SimpleMeterRegistry();
    var observation = new MicrometerRelayObservation(registry);
    observation.p0Admission("accepted");
    observation.p0Admission("ratelimited");
    observation.p0Delivery("delivered");
    observation.p0Event("ReservationConfirmed", "delivered");
    observation.p0Stage("deliver", "delivered", 1_000_000);

    assertThat(registry.find("p0_admission_total").tag("result", "accepted").counter().count())
        .isOne();
    assertThat(registry.find("p0_admission_total").tag("result", "ratelimited").counter().count())
        .isOne();
    assertThat(registry.find("p0_delivery_total").tag("result", "delivered").counter().count())
        .isOne();
    assertThat(
            registry
                .find("p0_event_total")
                .tag("type", "ReservationConfirmed")
                .tag("result", "delivered")
                .counter()
                .count())
        .isOne();
    assertThat(registry.find("p0_stage_seconds").tag("stage", "deliver").timer().count()).isOne();

    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          clearOrders(h);
          insertOrder(h, "01", 0, "gauge-pending", "PENDING_RESERVATION", 10);
          var gate = new P0AdmissionGate();
          try (var probe = new P0BacklogProbe(h.j.getDataSource(), gate::update)) {
            var gauges = new SimpleMeterRegistry();
            new V2MetricsConfig().p0Metrics(gauges, probe, gate).afterSingletonsInstantiated();
            assertThat(gauges.find("p0_backlog_orders").tag("state", "PENDING_RESERVATION").gauge().value())
                .isEqualTo(1.0);
            assertThat(
                    gauges
                        .find("p0_oldest_age_seconds")
                        .tag("state", "PENDING_RESERVATION")
                        .gauge()
                        .value())
                .isGreaterThan(0.0);
            assertThat(gauges.find("p0_inflight_orders").gauge().value()).isZero();
            gate.tryEnter("gauge-user", 1);
            assertThat(gauges.find("p0_inflight_orders").gauge().value()).isOne();
            gate.leave("gauge-user");
          }
          clearOrders(h);
        });
  }

  private static DrawService service(AssetTestSupport h, P0AdmissionGate gate) {
    var ids = new BlockOrderIdGenerator(h.runner, h.j, "p0backpressure");
    ids.prefetch();
    h.codec.withGenerator(ids);
    var strategies = new MySqlStrategyVersionRepository(h.j.getDataSource());
    return new DrawService(
        h.runner,
        h.assets,
        new QuotaKernel(h.repo),
        h.repo,
        new MySqlDrawRepository(h.j.getDataSource()),
        h.codec,
        ids,
        new MySqlOutboxWriter(h.j.getDataSource()),
        x -> {},
        com.jay.domain.task.service.RelayObservation.NOOP,
        new P0AdmissionCache(h.runner, strategies, Duration.ofSeconds(2)),
        new FrozenRaffleEngine(),
        gate);
  }

  private static long quota(AssetTestSupport h, String user, long activity) {
    return h.tx(
        user,
        () ->
            h.j.queryForObject(
                "select total_remaining from raffle_activity_account where user_id=? and"
                    + " activity_id=?",
                Long.class,
                user,
                activity));
  }

  private static void clearOrders(AssetTestSupport h) {
    for (String db : List.of("01", "02"))
      for (int table = 0; table < 4; table++) {
        var slot = new RouteSlot(db, String.format("%03d", table));
        h.runner.inSlot(
            slot,
            WorkClass.RECOVERY,
            new Deadline(Instant.now().plusSeconds(30)),
            () -> {
              h.j.update("delete from v2_draw_order_" + slot.tableKey());
              return null;
            });
      }
  }

  private static void insertOrder(
      AssetTestSupport h,
      String db,
      int table,
      String order,
      String state,
      long ageSeconds) {
    var slot = new RouteSlot(db, String.format("%03d", table));
    h.runner.inSlot(
        slot,
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(30)),
        () -> {
          h.j.update(
              "insert into v2_draw_order_"
                  + slot.tableKey()
                  + "(order_id,user_id,activity_id,state,snapshot,last_progress_at)"
                  + " values(?,?,?,?,?,"
                  + (AssetTestSupport.postgres()
                      ? "utc_timestamp(6) - (? * interval '1 second')"
                      : "date_sub(utc_timestamp(6), interval ? second)")
                  + ")",
              order,
              "probe-user",
              1L,
              state,
              "{}",
              ageSeconds);
          return null;
        });
  }
}
