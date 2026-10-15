package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class DrawCancellationV2Test {
  static DrawProgressService progress(AssetTestSupport h) {
    return new DrawProgressService(
        new MySqlDrawRepository(h.j.getDataSource()),
        h.assets,
        new com.jay.domain.asset.service.QuotaKernel(h.repo),
        h.codec,
        new MySqlOutboxWriter(h.j.getDataSource()));
  }

  static DrawRecord accepted(AssetTestSupport h, String user, long a) {
    var svc = DrawAcceptanceV2Test.service(h);
    DrawAcceptanceV2Test.grant(h, user, a);
    var view =
        svc.create(
            new DrawCommand(
                user,
                UUID.randomUUID().toString(),
                a,
                new Deadline(Instant.now().plusSeconds(30))));
    var repo = new MySqlDrawRepository(h.j.getDataSource());
    h.runner.inUser(
        user,
        WorkClass.WORKER,
        new Deadline(Instant.now().plusSeconds(30)),
        () -> {
          var r = repo.find(view.orderId(), false);
          new CandidateWorker(
                  repo,
                  null,
                  new com.jay.domain.strategy.service.FrozenRaffleEngine(),
                  h.codec,
                  new MySqlOutboxWriter(h.j.getDataSource()))
              .prepareFrozen(r.orderId(), r.strategy(), new Random());
          return null;
        });
    return h.tx(user, () -> repo.find(view.orderId(), false));
  }

  static ReservationResult held(AssetTestSupport h, DrawRecord r) {
    return StockProtocolV2Test.control(
        h,
        () ->
            StockProtocolV2Test.service(h)
                .reserve(
                    new ReserveCommand(
                        r.orderId(),
                        r.userId(),
                        r.identityHash(),
                        r.requestHash(),
                        r.strategy().strategyId(),
                        r.version(),
                        r.candidate(),
                        r.fallback())));
  }

  static EventEnvelope receipt(
      AssetTestSupport h, DrawRecord r, String type, ReservationResult result) {
    return DrawService.event(
        r,
        type,
        h.codec.routeUser(r.userId()),
        h.codec.newEventId(),
        com.jay.domain.asset.service.ReceiptJson.write(result));
  }

  @Test
  public void cancelAndHeldHaveExactlyOneWinnerAndRefundOriginalPeriod() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "cancel-" + UUID.randomUUID();
          long a = DrawAcceptanceV2Test.publish(h, null);
          h.overrideTime = BusinessTime.at(Instant.parse("2026-01-31T15:59:59Z"));
          var r = accepted(h, u, a);
          var held = held(h, r);
          var svc = DrawAcceptanceV2Test.service(h);
          var index = new java.util.concurrent.atomic.AtomicInteger();
          var outcomes =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  2,
                  () -> {
                    if (index.getAndIncrement() == 0) {
                      try {
                        svc.cancel(u, r.orderId());
                      } catch (AssetConflictException e) {
                      }
                    } else
                      h.runner.inUser(
                          u,
                          WorkClass.WORKER,
                          new Deadline(Instant.now().plusSeconds(30)),
                          () -> {
                            progress(h).onHeld(receipt(h, r, "ReservationHeld", held));
                            return null;
                          });
                    return true;
                  });
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          var next = h.tx(u, () -> repo.find(r.orderId(), false));
          assertThat(Set.of("RESULT_RECORDED", "CANCELLING")).contains(next.state());
          if (next.state().equals("CANCELLING")) {
            var cancelled =
                StockProtocolV2Test.control(
                    h,
                    () ->
                        StockProtocolV2Test.service(h)
                            .cancel(
                                new CancelCommand(
                                    r.orderId(), u, r.identityHash(), r.requestHash())));
            h.overrideTime = BusinessTime.at(Instant.parse("2026-02-01T01:00:00Z"));
            h.runner.inUser(
                u,
                WorkClass.WORKER,
                new Deadline(Instant.now().plusSeconds(30)),
                () -> {
                  progress(h).onCancelled(receipt(h, r, "ReservationCancelled", cancelled));
                  progress(h).onCancelled(receipt(h, r, "ReservationCancelled", cancelled));
                  return null;
                });
            assertThat(svc.get(u, r.orderId()).state()).isEqualTo("CANCELLED");
            assertThat(
                    h.tx(
                        u,
                        () -> {
                          h.assets.lockAccounts(u, List.of(a));
                          return h.repo.quotaState(u, a, r.time()).total();
                        }))
                .isEqualTo(10);
          } else
            assertThatThrownBy(() -> svc.cancel(u, r.orderId()))
                .isInstanceOf(AssetConflictException.class);
        });
  }

  @Test
  public void confirmedReceiptAdvancesOnlyRecordedOriginalResult() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "confirm-" + UUID.randomUUID();
          var r = accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          var held = held(h, r);
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                progress(h).onHeld(receipt(h, r, "ReservationHeld", held));
                return null;
              });
          var confirmed =
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
                progress(h).onConfirmed(receipt(h, r, "ReservationConfirmed", confirmed));
                progress(h).onConfirmed(receipt(h, r, "ReservationConfirmed", confirmed));
                return null;
              });
          assertThat(DrawAcceptanceV2Test.service(h).get(u, r.orderId()).state())
              .isEqualTo("COMMITTED");
        });
  }

  @Test
  public void acceptedCancellationRefundsOldBucketsAndCannotFixLateCandidate() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "cross-month-" + UUID.randomUUID();
          long a = DrawAcceptanceV2Test.publish(h, null);
          h.overrideTime = BusinessTime.at(Instant.parse("2026-01-31T15:59:59Z"));
          DrawAcceptanceV2Test.grant(h, u, a);
          var svc = DrawAcceptanceV2Test.service(h);
          var view =
              svc.create(
                  new DrawCommand(u, "old-month", a, new Deadline(Instant.now().plusSeconds(30))));
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          var original = h.tx(u, () -> repo.find(view.orderId(), false));
          assertThat(svc.cancel(u, original.orderId()).state()).isEqualTo("CANCELLING");
          var cancelled =
              StockProtocolV2Test.control(
                  h,
                  () ->
                      StockProtocolV2Test.service(h)
                          .cancel(
                              new CancelCommand(
                                  original.orderId(),
                                  u,
                                  original.identityHash(),
                                  original.requestHash())));
          h.overrideTime = BusinessTime.at(Instant.parse("2026-02-01T00:00:00Z"));
          var today = h.overrideTime;
          h.tx(
              u,
              () -> {
                h.assets.lockAccounts(u, List.of(a));
                assertThat(h.repo.quotaState(u, a, today).dayRemaining()).isEqualTo(10);
                return null;
              });
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                new CandidateWorker(
                        repo,
                        null,
                        new com.jay.domain.strategy.service.FrozenRaffleEngine(),
                        h.codec,
                        new MySqlOutboxWriter(h.j.getDataSource()))
                    .prepareFrozen(original.orderId(), original.strategy(), new Random());
                progress(h).onCancelled(receipt(h, original, "ReservationCancelled", cancelled));
                progress(h).onCancelled(receipt(h, original, "ReservationCancelled", cancelled));
                return null;
              });
          h.tx(
              u,
              () -> {
                h.assets.lockAccounts(u, List.of(a));
                assertThat(h.repo.quotaState(u, a, original.time()).dayRemaining()).isEqualTo(10);
                assertThat(h.repo.quotaState(u, a, today).dayRemaining()).isEqualTo(10);
                assertThat(h.repo.quotaState(u, a, today).effective()).isZero();
                assertThat(repo.find(original.orderId(), false).candidate()).isNull();
                return null;
              });
        });
  }
}
