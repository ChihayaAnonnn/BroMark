package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.trigger.listener.v2.V2MessageListener;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;

public class RaffleUnsupportedV2Test {
  @Test
  public void unsupportedHistoricalAwardWaitsForFixWithoutAcknowledgingBusinessSuccess() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String u = "unsupported-" + UUID.randomUUID();
          var r = DrawCancellationV2Test.accepted(h, u, DrawAcceptanceV2Test.publish(h, null));
          h.tx(
              u,
              () -> {
                h.j.update(
                    "update "
                        + MySqlAssetRepository.table("v2_draw_order")
                        + " set state='COMMITTED',snapshot=json_set(snapshot,'$.finalAward',json_extract(snapshot,'$.candidate'),'$.finalAward.type','PHYSICAL')"
                        + " where order_id=?",
                    r.orderId());
                return null;
              });
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
          var e =
              DrawService.event(
                  r, "AwardDelivery", h.codec.routeUser(u), h.codec.newEventId(), "{}");
          var ack = new AtomicBoolean();
          var paused = new AtomicBoolean();
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
                  Map.of(
                      "AwardDelivery",
                      event -> {
                        new AwardDeliveryService(draws, h.assets, p -> {}).deliver(event);
                        return List.of();
                      }),
                  p -> {});
          listener.handle(
              "workflow-v2",
              JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8),
              0,
              new V2MessageListener.Delivery() {
                public void ack() {
                  ack.set(true);
                }

                public void pause() {
                  paused.set(true);
                }
              });
          h.tx(
              u,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select recovery_status from "
                                + MySqlAssetRepository.table("v2_draw_order")
                                + " where order_id=?",
                            String.class,
                            r.orderId()))
                    .isEqualTo("WAITING_FIX");
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from consumer_inbox where event_id=?",
                            Integer.class,
                            e.eventId()))
                    .isZero();
                return null;
              });
          var view = DrawAcceptanceV2Test.service(h).get(u, r.orderId());
          assertThat(view.recoveryStatus()).isEqualTo(RecoveryStatus.WAITING_FIX);
          assertThat(view.finalAward()).isNull();
          assertThat(ack.get()).isTrue();
          assertThat(paused.get()).isFalse();
          assertThat(h.balance(u)).isEqualByComparingTo("0");
          h.tx(
              u,
              () -> {
                h.j.update(
                    "update "
                        + MySqlAssetRepository.table("v2_draw_order")
                        + " set state='CANDIDATE_FIXED',recovery_status='ACTIVE',snapshot=json_remove(snapshot,'$.finalAward')"
                        + " where order_id=?",
                    r.orderId());
                return null;
              });
        });
  }
}
