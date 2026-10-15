package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class RaffleReconciliationV2Test {
  @Test
  public void stockLogDriftIsDetectedAndOnlyAffectedPoolIsPaused() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String pool = "recon-" + UUID.randomUUID();
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update("insert into award_stock values(?,5,5,0,0,0,'ACTIVE')", pool);
                h.j.update(
                    "insert into"
                        + " stock_change_log(stock_pool_id,biz_id,kind,total_delta,available_delta,reserved_delta,allocated_delta,reason)"
                        + " values(?,?,'INIT',5,5,0,0,'new budget')",
                    pool,
                    pool);
                return null;
              });
          var checks = new MySqlRaffleReconciliation(h.j.getDataSource());
          assertThatThrownBy(
                  () ->
                      StockProtocolV2Test.control(
                          h,
                          () -> {
                            h.j.update(
                                "update award_stock set total=6,available=6 where stock_pool_id=?",
                                pool);
                            var report = checks.check(RouteSlot.control());
                            assertThat(report.stockMismatch()).isPositive();
                            assertThat(report.checkedScopes()).containsExactly("stock");
                            assertThat(
                                    h.j.queryForObject(
                                        "select recovery_status from award_stock where"
                                            + " stock_pool_id=?",
                                        String.class,
                                        pool))
                                .isEqualTo("REVIEW");
                            throw new IllegalStateException("test rollback");
                          }))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("test rollback");
        });
  }

  @Test
  public void detectsMissingDrawConsumptionAndPausesOnlyAffectedUser() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "recon-user-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          assertThatThrownBy(
                  () ->
                      h.runner.inUser(
                          u,
                          WorkClass.RECOVERY,
                          new Deadline(Instant.now().plusSeconds(30)),
                          () -> {
                            h.j.update(
                                "delete from "
                                    + MySqlAssetRepository.table("user_quota_ledger")
                                    + " where biz_id=? and biz_type='CONSUME:DRAW'",
                                r.orderId());
                            h.j.update(
                                "update raffle_activity_account set"
                                    + " total_remaining=total_remaining+1,effective_draw_count=effective_draw_count-1"
                                    + " where user_id=? and activity_id=?",
                                u,
                                r.activityId());
                            h.j.update(
                                "update raffle_activity_account_day set"
                                    + " remaining=remaining+1,effective_draw_count=effective_draw_count-1"
                                    + " where user_id=? and activity_id=?",
                                u,
                                r.activityId());
                            h.j.update(
                                "update raffle_activity_account_month set remaining=remaining+1"
                                    + " where user_id=? and activity_id=?",
                                u,
                                r.activityId());
                            var report =
                                new MySqlRaffleReconciliation(h.j.getDataSource())
                                    .check(h.codec.routeUser(u));
                            assertThat(report.quotaMismatch()).isPositive();
                            assertThat(
                                    h.j.queryForObject(
                                        "select status from user_credit_account where user_id=?",
                                        String.class,
                                        u))
                                .isEqualTo("REVIEW");
                            throw new IllegalStateException("test rollback");
                          }))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("test rollback");
        });
  }

  @Test
  public void auditedPoolResumeRestoresActiveWithoutChangingCounters() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String pool = "resume-pool-" + UUID.randomUUID();
          StockProtocolV2Test.control(
              h,
              () -> {
                StockProtocolV2Test.service(h).createPool(pool, "initial", 2, "budget");
                return null;
              });
          var port = new MySqlReconciliationRecoveryPort(h.j.getDataSource(), true);
          var recovery =
              new com.jay.domain.task.service.RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("STOCK_RECONCILIATION", port),
                  () -> "resume-" + UUID.randomUUID());
          recovery.pause(
              RouteSlot.control(),
              "STOCK_RECONCILIATION",
              pool,
              com.jay.domain.task.model.RecoveryStatus.REVIEW,
              "verify budget");
          long version =
              h.runner.inControl(
                  WorkClass.RECOVERY,
                  new Deadline(Instant.now().plusSeconds(30)),
                  () -> port.lock(pool).version());
          recovery.resume(
              RouteSlot.control(),
              "STOCK_RECONCILIATION",
              pool,
              version,
              "tester",
              "budget verified");
          StockProtocolV2Test.control(
              h,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select recovery_status from award_stock where stock_pool_id=?",
                            String.class,
                            pool))
                    .isEqualTo("ACTIVE");
                assertThat(
                        h.j.queryForObject(
                            "select available from award_stock where stock_pool_id=?",
                            Long.class,
                            pool))
                    .isEqualTo(2);
                return null;
              });
        });
  }
}
