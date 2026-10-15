package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class RaffleDeliveryV2Test {
  @Test
  public void awardDeliveryIsAtomicAndIdempotentAfterAckLoss() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "delivery-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          var held = DrawCancellationV2Test.held(h, r);
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          var delivery = new AwardDeliveryService(repo, h.assets, x -> {});
          assertThatThrownBy(
                  () ->
                      h.runner.inUser(
                          u,
                          WorkClass.WORKER,
                          new Deadline(Instant.now().plusSeconds(30)),
                          () -> {
                            delivery.deliver(
                                DrawService.event(
                                    r,
                                    "AwardDelivery",
                                    h.codec.routeUser(u),
                                    h.codec.newEventId(),
                                    "{}"));
                            return null;
                          }))
              .isInstanceOf(AssetConflictException.class);
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                DrawCancellationV2Test.progress(h)
                    .onHeld(DrawCancellationV2Test.receipt(h, r, "ReservationHeld", held));
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
                DrawCancellationV2Test.progress(h)
                    .onConfirmed(
                        DrawCancellationV2Test.receipt(h, r, "ReservationConfirmed", confirmed));
                delivery.deliver(
                    DrawService.event(
                        r, "AwardDelivery", h.codec.routeUser(u), h.codec.newEventId(), "{}"));
                delivery.deliver(
                    DrawService.event(
                        r, "AwardDelivery", h.codec.routeUser(u), h.codec.newEventId(), "{}"));
                return null;
              });
          assertThat(h.balance(u)).isEqualByComparingTo(held.finalSnapshot().amount());
          assertThat(DrawAcceptanceV2Test.service(h).get(u, r.orderId()).state())
              .isEqualTo("DELIVERED");
          assertThatThrownBy(() -> DrawAcceptanceV2Test.service(h).get("other-user", r.orderId()))
              .isInstanceOf(AssetOwnershipException.class);
        });
  }

  @Test
  public void noneAwardCompletesWithoutCreditLedger() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "none-delivery-" + UUID.randomUUID();
          long a = DrawAcceptanceV2Test.publish(h, null);
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update("insert into v2_asset_blacklist values(?)", u);
                return null;
              });
          var r = DrawCancellationV2Test.accepted(h, u, a);
          assertThat(r.candidate().type()).isEqualTo("NONE");
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
                DrawCancellationV2Test.progress(h)
                    .onConfirmed(
                        DrawCancellationV2Test.receipt(h, r, "ReservationConfirmed", confirmed));
                new AwardDeliveryService(
                        new MySqlDrawRepository(h.j.getDataSource()), h.assets, x -> {})
                    .deliver(
                        DrawService.event(
                            r, "AwardDelivery", h.codec.routeUser(u), h.codec.newEventId(), "{}"));
                return null;
              });
          assertThat(h.balance(u)).isEqualByComparingTo("0.00");
          assertThat(DrawAcceptanceV2Test.service(h).get(u, r.orderId()).state())
              .isEqualTo("DELIVERED");
        });
  }
}
