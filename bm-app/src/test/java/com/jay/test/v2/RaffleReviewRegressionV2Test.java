package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.service.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.admission.RedisRequestAdmission;
import com.jay.trigger.listener.v2.V2MessageListener;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Ignore;
import org.junit.Test;

public class RaffleReviewRegressionV2Test {
  @Test
  public void lateHeldAfterCompletedCancellationIsSafeTerminalReentry() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "late-held-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, r);
          var service = DrawAcceptanceV2Test.service(h);
          service.cancel(u, r.orderId());
          var cancelled =
              StockProtocolV2Test.control(
                  h,
                  () ->
                      StockProtocolV2Test.service(h)
                          .cancel(
                              new CancelCommand(
                                  r.orderId(), u, r.identityHash(), r.requestHash())));
          var draws = new MySqlDrawRepository(h.j.getDataSource());
          var recovery =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("DRAW", draws),
                  h.codec::newEventId);
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  recovery,
                  h.codec::newEventId);
          var cancelEvent = DrawCancellationV2Test.receipt(h, r, "ReservationCancelled", cancelled);
          uow.applyWithResponses(
              "workflow-v2",
              cancelEvent,
              () -> DrawCancellationV2Test.progress(h).handle(cancelEvent));
          var late = DrawCancellationV2Test.receipt(h, r, "ReservationHeld", held);
          var ack = new AtomicBoolean();
          var listener =
              new V2MessageListener(
                  h.runner,
                  uow,
                  new MySqlRetryJobRepository(h.j.getDataSource(), h.runner),
                  recovery,
                  new EventRouting(h.codec),
                  raw ->
                      JsonStore.read(
                          new String(raw, java.nio.charset.StandardCharsets.UTF_8),
                          EventEnvelope.class),
                  new MySqlRawMessageStore(h.j.getDataSource()),
                  Map.of("ReservationHeld", e -> DrawCancellationV2Test.progress(h).handle(e)),
                  p -> {});
          listener.handle(
              "workflow-v2",
              JsonStore.write(late).getBytes(java.nio.charset.StandardCharsets.UTF_8),
              0,
              new V2MessageListener.Delivery() {
                public void ack() {
                  ack.set(true);
                }

                public void pause() {
                  throw new AssertionError("Unexpected pause");
                }
              });
          assertThat(service.get(u, r.orderId()).recoveryStatus()).isEqualTo(RecoveryStatus.ACTIVE);
          assertThat(service.cancel(u, r.orderId()).state()).isEqualTo("CANCELLED");
          assertThat(ack.get()).isTrue();
          h.tx(
              u,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from "
                                + MySqlAssetRepository.table("user_quota_ledger")
                                + " where biz_id=? and biz_type='REFUND:DRAW' and"
                                + " bucket_type='TOTAL'",
                            Integer.class,
                            r.orderId()))
                    .isOne();
                return null;
              });
        });
  }

  @Test
  public void publicationRejectsConflictingFallbackIdentityBeforeAcceptingDraws() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String pool = "collision-" + UUID.randomUUID();
          StockProtocolV2Test.control(
              h,
              () -> {
                StockProtocolV2Test.service(h).createPool(pool, "initial", 1, "budget");
                return null;
              });
          long a = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
          var c = FrozenStrategyV2Test.command(a, pool);
          var pub =
              new StrategyPublicationService(
                  h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()));
          for (var fallback :
              List.of(
                  new PublishStrategyCommand.Prize(
                      101, "NONE", null, null, null, BigDecimal.ONE, 0),
                  new PublishStrategyCommand.Prize(
                      101,
                      "CREDIT",
                      new BigDecimal("4.00"),
                      new BigDecimal("4.00"),
                      null,
                      BigDecimal.ONE,
                      0))) {
            assertThatThrownBy(
                    () ->
                        pub.publish(
                            new PublishStrategyCommand(
                                a,
                                a,
                                "OPEN",
                                c.beginsAt(),
                                c.endsAt(),
                                c.prizes(),
                                fallback,
                                Map.of(),
                                null,
                                "frozen")))
                .isInstanceOf(IllegalArgumentException.class);
          }
        });
  }

  @Ignore("legacy cancel flow replaced by P0 405 tombstone; cleanup task")
  @Test
  public void exhaustedCreateAdmissionDoesNotBlockAuthorizedCancellation() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "cancel-budget-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          var admission = new RedisRequestAdmission(null, 1, 1, 1, 1, 1, 1);
          var controller =
              new com.jay.trigger.http.v2.DrawController(
                  new com.jay.trigger.security.CurrentUser(),
                  DrawAcceptanceV2Test.service(h),
                  admission,
                  new AssetWriteGate(true, "V2"));
          var jwt =
              org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test")
                  .header("alg", "RS256")
                  .subject(u)
                  .build();
          org.springframework.security.core.context.SecurityContextHolder.getContext()
              .setAuthentication(
                  new org.springframework.security.oauth2.server.resource.authentication
                      .JwtAuthenticationToken(jwt, List.of()));
          try (var full = admission.enter(u, new Deadline(Instant.now().plusSeconds(5)))) {
            assertThatThrownBy(() -> admission.enter(u, new Deadline(Instant.now().plusSeconds(5))))
                .isInstanceOf(BudgetExceededException.class);
            assertThat(controller.cancel(r.orderId()).getStatusCode().value()).isEqualTo(202);
          } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
          }
        });
  }

  @Test
  public void noneAwardCreditDriftIsDetectedEvenWhenBalanceMatchesLedger() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "none-drift-" + UUID.randomUUID();
          long a = DrawAcceptanceV2Test.publish(h, null);
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update("insert into v2_asset_blacklist values(?)", u);
                return null;
              });
          var r = DrawCancellationV2Test.accepted(h, u, a);
          var held = DrawCancellationV2Test.held(h, r);
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(20)),
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
              new Deadline(Instant.now().plusSeconds(20)),
              () -> {
                DrawCancellationV2Test.progress(h)
                    .onConfirmed(
                        DrawCancellationV2Test.receipt(h, r, "ReservationConfirmed", fact));
                new AwardDeliveryService(
                        new MySqlDrawRepository(h.j.getDataSource()), h.assets, p -> {})
                    .deliver(
                        DrawService.event(
                            r, "AwardDelivery", h.codec.routeUser(u), h.codec.newEventId(), "{}"));
                return null;
              });
          assertThatThrownBy(
                  () ->
                      h.runner.inUser(
                          u,
                          WorkClass.RECOVERY,
                          new Deadline(Instant.now().plusSeconds(20)),
                          () -> {
                            h.assets.lockAccounts(u, List.of(a));
                            h.assets.credit(
                                u,
                                new BusinessId(
                                    "DRAW_AWARD", r.orderId(), held.finalSnapshot().configHash()),
                                BigDecimal.ONE);
                            var report =
                                new MySqlRaffleReconciliation(h.j.getDataSource())
                                    .check(h.codec.routeUser(u));
                            assertThat(report.creditMismatch()).isZero();
                            assertThat(report.duplicateDelivery()).isPositive();
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
}
