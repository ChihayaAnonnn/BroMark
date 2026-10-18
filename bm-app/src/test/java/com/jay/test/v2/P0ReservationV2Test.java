package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.stock.service.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.repository.*;
import com.jay.domain.strategy.service.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.cache.P0AdmissionCache;
import com.jay.infrastructure.event.MicrometerRelayObservation;
import com.jay.infrastructure.id.BlockOrderIdGenerator;
import com.jay.trigger.listener.v2.StockCommandListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class P0ReservationV2Test {
  private static final String USER = "p0-reservation-user";

  @Test
  public void commitMovesAvailableToAllocatedAndFallsBackToNone() {
    AssetTestSupport.run(
        h -> {
          prepare(h);
          var svc = StockProtocolV2Test.service(h);
          String pool = pool(h, 1);
          var engine = new FrozenRaffleEngine();
          var firstCommand = cmd(h, pool, engine);
          var first = StockProtocolV2Test.control(h, () -> svc.confirmOnReserve(firstCommand));
          assertThat(first.state()).isEqualTo("CONFIRMED");
          assertThat(first.finalSnapshot().type()).isEqualTo("CREDIT");
          assertThat(first.quantity()).isEqualTo(1);
          assertThat(stock(h, pool))
              .containsEntry("available", 0L)
              .containsEntry("allocated", 1L)
              .containsEntry("reserved", 0L);
          var secondCommand = cmd(h, pool, engine);
          var second = StockProtocolV2Test.control(h, () -> svc.confirmOnReserve(secondCommand));
          assertThat(second.state()).isEqualTo("CONFIRMED");
          assertThat(second.finalSnapshot().type()).isEqualTo("NONE");
          assertThat(second.quantity()).isZero();
          assertThat(stock(h, pool))
              .containsEntry("available", 0L)
              .containsEntry("allocated", 1L);
        });
  }

  @Test
  public void duplicateConfirmOnReserveDoesNotAllocateTwice() {
    AssetTestSupport.run(
        h -> {
          prepare(h);
          var svc = StockProtocolV2Test.service(h);
          String pool = pool(h, 1);
          var command = cmd(h, pool, new FrozenRaffleEngine());
          var first = StockProtocolV2Test.control(h, () -> svc.confirmOnReserve(command));
          var second = StockProtocolV2Test.control(h, () -> svc.confirmOnReserve(command));
          assertThat(first.state()).isEqualTo("CONFIRMED");
          assertThat(second).isEqualTo(first);
          assertThat(stock(h, pool))
              .containsEntry("available", 0L)
              .containsEntry("allocated", 1L)
              .containsEntry("reserved", 0L);
        });
  }

  @Test
  public void pausedPoolIsTechnicalFailureNotConfirmedNone() {
    AssetTestSupport.run(
        h -> {
          prepare(h);
          var svc = StockProtocolV2Test.service(h);
          String pool = pool(h, 1);
          var command = cmd(h, pool, new FrozenRaffleEngine());
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update(
                    "update award_stock set recovery_status='REVIEW' where stock_pool_id=?", pool);
                return null;
              });
          assertThatThrownBy(
                  () -> StockProtocolV2Test.control(h, () -> svc.confirmOnReserve(command)))
              .isInstanceOf(AssetConflictException.class)
              .hasMessageContaining("Stock pool paused");
          assertThat(
                  count(
                      h,
                      "select count(*) from award_reservation where draw_order_id=? and"
                          + " state='CONFIRMED'",
                      command.drawOrderId()))
              .isZero();
          assertThat(
                  count(
                      h,
                      "select count(*) from stock_change_log where stock_pool_id=? and"
                          + " kind='COMMIT'",
                      pool))
              .isZero();
          assertThat(stock(h, pool)).containsEntry("available", 1L).containsEntry("allocated", 0L);
        });
  }

  @Test
  public void missingPoolIsTechnicalFailureNotConfirmedNone() {
    AssetTestSupport.run(
        h -> {
          prepare(h);
          var svc = StockProtocolV2Test.service(h);
          String pool = pool(h, 1);
          var command = cmd(h, pool, new FrozenRaffleEngine());
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update(
                    "delete from strategy_stock_binding where strategy_id=? and version=?",
                    command.strategyId(),
                    command.version());
                h.j.update("delete from stock_change_log where stock_pool_id=?", pool);
                h.j.update("delete from award_stock where stock_pool_id=?", pool);
                return null;
              });
          assertThatThrownBy(
                  () -> StockProtocolV2Test.control(h, () -> svc.confirmOnReserve(command)))
              .isInstanceOf(AssetConflictException.class)
              .hasMessageContaining("Stock pool missing");
          assertThat(
                  count(
                      h,
                      "select count(*) from award_reservation where draw_order_id=?",
                      command.drawOrderId()))
              .isZero();
        });
  }

  @Test
  public void publicationRejectsInvalidStockAndFallback() {
    AssetTestSupport.run(
        h -> {
          FrozenStrategyV2Test.schema(h);
          var pub =
              new StrategyPublicationService(
                  h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()));
          long activity = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
          assertThatThrownBy(() -> pub.publish(FrozenStrategyV2Test.command(activity, null)))
              .isInstanceOf(IllegalArgumentException.class);
          String pool = pool(h, 1);
          var valid = FrozenStrategyV2Test.command(activity, pool);
          var creditFallback =
              new PublishStrategyCommand.Prize(
                  0,
                  "CREDIT",
                  new BigDecimal("1.00"),
                  new BigDecimal("1.00"),
                  pool,
                  BigDecimal.ONE,
                  0);
          assertThatThrownBy(
                  () ->
                      pub.publish(
                          new PublishStrategyCommand(
                              valid.activityId(),
                              valid.strategyId(),
                              "OPEN",
                              valid.beginsAt(),
                              valid.endsAt(),
                              valid.prizes(),
                              creditFallback,
                              Map.of(),
                              null,
                              "p0")))
              .isInstanceOf(IllegalArgumentException.class);
        });
  }

  @Test
  public void reserveAwardEmitsReservationConfirmedSchemaTwo() {
    AssetTestSupport.run(
        h -> {
          prepare(h);
          var svc = StockProtocolV2Test.service(h);
          String pool = pool(h, 1);
          var command = cmd(h, pool, new FrozenRaffleEngine());
          var event =
              new EventEnvelope(
                  "evt-" + UUID.randomUUID(),
                  "ReserveAward",
                  command.drawOrderId(),
                  new BusinessId("DRAW", command.drawOrderId(), command.requestHash()),
                  command.userId(),
                  RouteSlot.control(),
                  command.requestHash(),
                  1,
                  2,
                  ReceiptJson.write(command));
          var registry = new SimpleMeterRegistry();
          var listener =
              new StockCommandListener(
                  svc, h.codec, new MicrometerRelayObservation(registry));
          var events = StockProtocolV2Test.control(h, () -> listener.apply(event));
          assertThat(events).hasSize(1);
          assertThat(events.getFirst().eventType()).isEqualTo("ReservationConfirmed");
          assertThat(events.getFirst().schemaVersion()).isEqualTo(2);
          assertThat(events.getFirst().aggregateId()).isEqualTo(command.drawOrderId());
          assertThat(
                  registry
                      .find("p0_stage_seconds")
                      .tag("stage", "reserve")
                      .tag("result", "success")
                      .timer()
                      .count())
              .isOne();
          assertThat(
                  registry
                      .find("p0_event_total")
                      .tag("type", "ReservationConfirmed")
                      .tag("result", "emitted")
                      .counter()
                      .count())
              .isOne();
          var result = ReceiptJson.read(events.getFirst().payload(), ReservationResult.class);
          assertThat(result.state()).isEqualTo("CONFIRMED");
          assertThat(result.finalSnapshot().type()).isEqualTo("CREDIT");
          assertThat(stock(h, pool))
              .containsEntry("available", 0L)
              .containsEntry("allocated", 1L)
              .containsEntry("reserved", 0L);
        });
  }

  @Test
  public void invalidateActivityEvictsEveryUserAdmissionForActivity() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String pool = pool(h, 1);
          long activity = DrawAcceptanceV2Test.publish(h, pool);
          var repo = new MySqlStrategyVersionRepository(h.j.getDataSource());
          var calls = new java.util.concurrent.atomic.AtomicInteger();
          var counted =
              new StrategyVersionRepository() {
                public void publish(FrozenStrategy f) {
                  repo.publish(f);
                }

                public FrozenStrategy load(long id, String v) {
                  return repo.load(id, v);
                }

                public FrozenStrategy current(long id) {
                  return repo.current(id);
                }

                public StrategyVersionRepository.DrawAdmission admit(
                    String user, long id, Deadline deadline) {
                  calls.incrementAndGet();
                  return repo.admit(user, id, deadline);
                }
              };
          var cache = new P0AdmissionCache(h.runner, counted, Duration.ofSeconds(60));
          var deadline = new Deadline(Instant.now().plusSeconds(30));
          cache.admit("a", activity, deadline);
          cache.admit("b", activity, deadline);
          assertThat(calls.get()).isEqualTo(2);
          cache.admit("a", activity, deadline);
          assertThat(calls.get()).isEqualTo(2);
          cache.invalidateActivity(activity);
          cache.admit("a", activity, deadline);
          cache.admit("b", activity, deadline);
          assertThat(calls.get()).isEqualTo(4);
        });
  }

  private static void prepare(AssetTestSupport h) throws Exception {
    FrozenStrategyV2Test.schema(h);
    var ids = new BlockOrderIdGenerator(h.runner, h.j, "p0reserve");
    ids.prefetch();
    h.codec.withGenerator(ids);
  }

  private static String pool(AssetTestSupport h, long total) {
    String pool = "p0-" + UUID.randomUUID();
    StockProtocolV2Test.control(
        h,
        () -> {
          StockProtocolV2Test.service(h).createPool(pool, "init", total, "p0");
          return null;
        });
    return pool;
  }

  private static Map<String, Long> stock(AssetTestSupport h, String pool) {
    return StockProtocolV2Test.control(
        h,
        () -> {
          var row =
              h.j.queryForMap(
                  "select available,reserved,allocated from award_stock where stock_pool_id=?",
                  pool);
          return Map.of(
              "available", ((Number) row.get("available")).longValue(),
              "reserved", ((Number) row.get("reserved")).longValue(),
              "allocated", ((Number) row.get("allocated")).longValue());
        });
  }

  private static int count(AssetTestSupport h, String sql, Object... args) {
    return StockProtocolV2Test.control(h, () -> h.j.queryForObject(sql, Integer.class, args));
  }

  private static ReserveCommand cmd(
      AssetTestSupport h, String pool, FrozenRaffleEngine engine) {
    long strategy = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
    var strategies = new MySqlStrategyVersionRepository(h.j.getDataSource());
    String version =
        new StrategyPublicationService(h.runner, strategies)
            .publish(FrozenStrategyV2Test.command(strategy, pool));
    var frozen = StockProtocolV2Test.control(h, () -> strategies.load(strategy, version));
    return new ReserveCommand(
        h.codec.newOrderId("DR", USER),
        USER,
        "a".repeat(64),
        "b".repeat(64),
        strategy,
        version,
        engine.choose(
            frozen, new RuleInputSnapshot(Instant.now(), 1, 1, false, "p0"), new Random()),
        engine.fallback(frozen, new Random()));
  }
}
