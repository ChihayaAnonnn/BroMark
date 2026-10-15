package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class RaffleFaultV2Test {
  @Test
  public void changedValidCandidateConflictsAndStockAdjustmentIsIdempotent() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "stock-conflict-" + UUID.randomUUID();
          String pool = "fault-" + UUID.randomUUID();
          StockProtocolV2Test.control(
              h,
              () -> {
                StockProtocolV2Test.service(h).createPool(pool, "initial", 1, "budget");
                return null;
              });
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, pool));
          var held = DrawCancellationV2Test.held(h, r);
          StockProtocolV2Test.control(
              h,
              () -> {
                var svc = StockProtocolV2Test.service(h);
                var amount =
                    r.candidate().amount().compareTo(new java.math.BigDecimal("1.00")) == 0
                        ? new java.math.BigDecimal("2.00")
                        : new java.math.BigDecimal("1.00");
                String hash =
                    new RequestHasher()
                        .hash(
                            "AWARD",
                            Map.of(
                                "version",
                                r.version(),
                                "award",
                                "101",
                                "type",
                                "CREDIT",
                                "amount",
                                amount.toPlainString(),
                                "pool",
                                pool));
                var different =
                    new com.jay.domain.strategy.model.AwardSnapshot(
                        101, "CREDIT", amount, pool, 1, hash);
                assertThatThrownBy(
                        () ->
                            svc.reserve(
                                new ReserveCommand(
                                    r.orderId(),
                                    u,
                                    r.identityHash(),
                                    r.requestHash(),
                                    r.strategy().strategyId(),
                                    r.version(),
                                    different,
                                    r.fallback())))
                    .isInstanceOf(AssetConflictException.class);
                svc.addStock(pool, "adjust", 1, "budget");
                svc.addStock(pool, "adjust", 1, "budget");
                assertThatThrownBy(() -> svc.addStock(pool, "minus", -2, "overdraw"))
                    .isInstanceOf(AssetConflictException.class);
                assertThat(
                        h.j.queryForObject(
                            "select total from award_stock where stock_pool_id=?",
                            Long.class,
                            pool))
                    .isEqualTo(2);
                return null;
              });
        });
  }

  @Test
  public void pausedDrawDefersLateReceiptThenAuditedResumeContinuesSameAward() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "paused-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, r);
          var draws = new MySqlDrawRepository(h.j.getDataSource());
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("DRAW", draws),
                  h.codec::newEventId);
          recovery.pause(
              h.codec.routeUser(u), "DRAW", r.orderId(), RecoveryStatus.REVIEW, "test mismatch");
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  recovery,
                  h.codec::newEventId);
          var event = DrawCancellationV2Test.receipt(h, r, "ReservationHeld", held);
          assertThat(
                  uow.applyWithResponses(
                      "workflow-v2", event, () -> DrawCancellationV2Test.progress(h).handle(event)))
              .isEqualTo(ConsumeOutcome.DEFERRED);
          assertThat(DrawAcceptanceV2Test.service(h).get(u, r.orderId()).state())
              .isEqualTo("CANDIDATE_FIXED");
          long version = h.tx(u, () -> draws.find(r.orderId(), false).objectVersion());
          recovery.resume(
              h.codec.routeUser(u), "DRAW", r.orderId(), version, "tester", "verified reservation");
          assertThat(
                  uow.applyWithResponses(
                      "workflow-v2", event, () -> DrawCancellationV2Test.progress(h).handle(event)))
              .isEqualTo(ConsumeOutcome.APPLIED);
          assertThat(DrawAcceptanceV2Test.service(h).get(u, r.orderId()).state())
              .isEqualTo("RESULT_RECORDED");
        });
  }

  @Test
  public void recoveryRestatesOriginalCandidateAfterConfirmedReceiptLoss() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "recover-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, r);
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                DrawCancellationV2Test.progress(h)
                    .onHeld(DrawCancellationV2Test.receipt(h, r, "ReservationHeld", held));
                return null;
              });
          StockProtocolV2Test.control(
              h,
              () ->
                  StockProtocolV2Test.service(h)
                      .confirm(
                          new ConfirmCommand(
                              r.orderId(),
                              u,
                              r.identityHash(),
                              r.requestHash(),
                              held.finalSnapshot().configHash())));
          var service =
              new DrawRecoveryService(
                  h.runner,
                  new MySqlDrawRepository(h.j.getDataSource()),
                  new MySqlStockRepository(h.j.getDataSource()),
                  h.codec,
                  new MySqlOutboxWriter(h.j.getDataSource()));
          var events = service.inspect(r.orderId()).actions();
          assertThat(events).hasSize(1);
          assertThat(events.getFirst().eventType()).isEqualTo("ReservationConfirmed");
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                DrawCancellationV2Test.progress(h).onConfirmed(events.getFirst());
                return null;
              });
          assertThat(DrawAcceptanceV2Test.service(h).get(u, r.orderId()).finalAward())
              .isEqualTo(held.finalSnapshot());
        });
  }

  @Test
  public void technicalAwardFailureRollsBackCreditReceiptAndStage() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "rollback-award-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, r);
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                DrawCancellationV2Test.progress(h)
                    .onHeld(DrawCancellationV2Test.receipt(h, r, "ReservationHeld", held));
                return null;
              });
          var fact =
              StockProtocolV2Test.control(
                  h,
                  () ->
                      StockProtocolV2Test.service(h)
                          .confirm(
                              new ConfirmCommand(
                                  r.orderId(),
                                  u,
                                  r.identityHash(),
                                  r.requestHash(),
                                  held.finalSnapshot().configHash())));
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                DrawCancellationV2Test.progress(h)
                    .onConfirmed(
                        DrawCancellationV2Test.receipt(h, r, "ReservationConfirmed", fact));
                return null;
              });
          var e =
              DrawService.event(
                  r, "AwardDelivery", h.codec.routeUser(u), h.codec.newEventId(), "{}");
          assertThatThrownBy(
                  () ->
                      h.runner.inUser(
                          u,
                          WorkClass.WORKER,
                          new Deadline(Instant.now().plusSeconds(30)),
                          () -> {
                            new AwardDeliveryService(
                                    new MySqlDrawRepository(h.j.getDataSource()),
                                    h.assets,
                                    p -> {
                                      throw new IllegalStateException("crash");
                                    })
                                .deliver(e);
                            return null;
                          }))
              .isInstanceOf(IllegalStateException.class);
          assertThat(h.balance(u)).isEqualByComparingTo("0");
          assertThat(DrawAcceptanceV2Test.service(h).get(u, r.orderId()).state())
              .isEqualTo("COMMITTED");
        });
  }

  @Test
  public void technicalAcceptanceFailureLeavesNoOrderOrQuotaConsumption() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "accept-rollback-" + UUID.randomUUID();
          long a = DrawAcceptanceV2Test.publish(h, null);
          DrawAcceptanceV2Test.grant(h, u, a);
          var ids = new com.jay.infrastructure.id.BlockOrderIdGenerator(h.runner, h.j, "fault");
          h.codec.withGenerator(ids);
          var svc =
              new DrawService(
                  h.runner,
                  h.assets,
                  new QuotaKernel(h.repo),
                  h.repo,
                  new MySqlDrawRepository(h.j.getDataSource()),
                  new MySqlStrategyVersionRepository(h.j.getDataSource()),
                  h.codec,
                  ids,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  p -> {
                    throw new IllegalStateException("crash after consume");
                  });
          assertThatThrownBy(
                  () ->
                      svc.create(
                          new DrawCommand(
                              u, "rollback", a, new Deadline(Instant.now().plusSeconds(30)))))
              .isInstanceOf(IllegalStateException.class);
          h.tx(
              u,
              () -> {
                var t = h.assets.lockAccounts(u, List.of(a));
                assertThat(h.repo.quotaState(u, a, t).total()).isEqualTo(10);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from "
                                + MySqlAssetRepository.table("v2_draw_order")
                                + " where user_id=?",
                            Integer.class,
                            u))
                    .isZero();
                return null;
              });
        });
  }

  @Test
  public void unavailableUserDatabaseNeverReleasesHeldStock() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "held-unavailable-" + UUID.randomUUID();
          String pool = "held-pool-" + UUID.randomUUID();
          StockProtocolV2Test.control(
              h,
              () -> {
                StockProtocolV2Test.service(h).createPool(pool, "initial", 1, "budget");
                return null;
              });
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, pool));
          DrawCancellationV2Test.held(h, r);
          var ds =
              (org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource)
                  h.j.getDataSource();
          ((com.zaxxer.hikari.HikariDataSource)
                  ds.getResolvedDataSources().get("db" + h.codec.routeUser(u).dbKey()))
              .close();
          var job =
              new com.jay.trigger.job.v2.StockRecoveryJob(
                  h.runner,
                  new MySqlStockRepository(h.j.getDataSource()),
                  new MySqlDrawRepository(h.j.getDataSource()),
                  h.codec,
                  new com.jay.domain.asset.service.OrderIdGenerator() {
                    public void prefetch() {}

                    public String next(String type, RouteSlot slot) {
                      return "unused";
                    }
                  },
                  new MySqlOutboxWriter(h.j.getDataSource()));
          assertThatThrownBy(() -> job.inspect(r.orderId())).isInstanceOf(RuntimeException.class);
          StockProtocolV2Test.control(
              h,
              () -> {
                assertThat(new MySqlStockRepository(h.j.getDataSource()).fact(r.orderId()).state())
                    .isEqualTo("HELD");
                assertThat(
                        h.j.queryForObject(
                            "select reserved from award_stock where stock_pool_id=?",
                            Long.class,
                            pool))
                    .isOne();
                return null;
              });
        });
  }
}
