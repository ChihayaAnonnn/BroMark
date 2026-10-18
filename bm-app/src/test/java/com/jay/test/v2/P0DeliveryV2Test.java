package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.service.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.Test;

public class P0DeliveryV2Test {
  @Test
  public void confirmedCreditDeliversOnceAndNoneDoesNotCredit() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long a = publish(h);
          String u = "p0-delivery-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, u, a);
          var svc = DrawAcceptanceV2Test.service(h);

          String order = create(svc, u, a);
          var event = eventFor(h, u, order);
          deliver(h, u, event);
          assertThat(h.balance(u)).isEqualByComparingTo("10.00");
          assertThat(record(h, u, order).state()).isEqualTo("DELIVERED");
          assertThat(record(h, u, order).finalAward().type()).isEqualTo("CREDIT");

          deliver(h, u, event); // duplicate delivery must not credit twice
          assertThat(h.balance(u)).isEqualByComparingTo("10.00");
          assertThat(record(h, u, order).state()).isEqualTo("DELIVERED");

          // 售罄时 control 侧确认 fallback NONE；deliver 后订单完成但不加积分。
          String noneOrder = create(svc, u, a);
          var none = record(h, u, noneOrder);
          deliver(h, u, eventFor(h, none, none.fallback()));
          assertThat(h.balance(u)).isEqualByComparingTo("10.00");
          var delivered = record(h, u, noneOrder);
          assertThat(delivered.state()).isEqualTo("DELIVERED");
          assertThat(delivered.finalAward().type()).isEqualTo("NONE");
        });
  }

  @Test
  public void finalAwardMismatchLeavesOrderPending() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long a = publish(h);
          String u = "p0-mismatch-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, u, a);
          var svc = DrawAcceptanceV2Test.service(h);
          String order = create(svc, u, a);
          var r = record(h, u, order);
          var tampered =
              new AwardSnapshot(
                  r.candidate().awardId(),
                  "CREDIT",
                  new BigDecimal("9.99"),
                  r.candidate().stockPoolId(),
                  1,
                  "f".repeat(64));

          assertThatThrownBy(() -> deliver(h, u, eventFor(h, r, tampered)))
              .isInstanceOf(AssetConflictException.class);
          assertThat(record(h, u, order).state()).isEqualTo("PENDING_RESERVATION");
          assertThat(record(h, u, order).finalAward()).isNull();
          assertThat(h.balance(u)).isEqualByComparingTo("0.00");
        });
  }

  @Test
  public void requestHashMismatchLeavesOrderPending() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long a = publish(h);
          String u = "p0-hash-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, u, a);
          var svc = DrawAcceptanceV2Test.service(h);
          String order = create(svc, u, a);
          var r = record(h, u, order);
          var result =
              new ReservationResult(
                  r.orderId(),
                  r.userId(),
                  r.identityHash(),
                  "c".repeat(64),
                  "CONFIRMED",
                  r.candidate(),
                  r.candidate().quantity());
          var event =
              new EventEnvelope(
                  h.codec.newEventId(),
                  "ReservationConfirmed",
                  r.orderId(),
                  new BusinessId("DRAW", r.orderId(), "c".repeat(64)),
                  r.userId(),
                  h.codec.routeUser(r.userId()),
                  "c".repeat(64),
                  r.objectVersion(),
                  2,
                  ReceiptJson.write(result));

          assertThatThrownBy(() -> deliver(h, u, event))
              .isInstanceOf(AssetConflictException.class);
          assertThat(record(h, u, order).state()).isEqualTo("PENDING_RESERVATION");
          assertThat(h.balance(u)).isEqualByComparingTo("0.00");
        });
  }

  @Test
  public void configHashMismatchLeavesOrderPending() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long a = publish(h);
          String u = "p0-config-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, u, a);
          var svc = DrawAcceptanceV2Test.service(h);
          String order = create(svc, u, a);
          var r = record(h, u, order);
          var tampered =
              new AwardSnapshot(
                  r.candidate().awardId(),
                  r.candidate().type(),
                  r.candidate().amount(),
                  r.candidate().stockPoolId(),
                  r.candidate().quantity(),
                  "f".repeat(64));

          assertThatThrownBy(() -> deliver(h, u, eventFor(h, r, tampered)))
              .isInstanceOf(AssetConflictException.class);
          assertThat(record(h, u, order).state()).isEqualTo("PENDING_RESERVATION");
          assertThat(h.balance(u)).isEqualByComparingTo("0.00");
        });
  }

  @Test
  public void heldReceiptIsRejectedAndLeavesOrderPending() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long a = publish(h);
          String u = "p0-held-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, u, a);
          var svc = DrawAcceptanceV2Test.service(h);
          String order = create(svc, u, a);
          var r = record(h, u, order);
          var held =
              new ReservationResult(
                  r.orderId(),
                  r.userId(),
                  r.identityHash(),
                  r.requestHash(),
                  "HELD",
                  r.candidate(),
                  r.candidate().quantity());
          var event =
              new EventEnvelope(
                  h.codec.newEventId(),
                  "ReservationConfirmed",
                  r.orderId(),
                  r.biz(),
                  r.userId(),
                  h.codec.routeUser(r.userId()),
                  r.requestHash(),
                  r.objectVersion(),
                  2,
                  ReceiptJson.write(held));

          assertThatThrownBy(() -> deliver(h, u, event))
              .isInstanceOf(AssetConflictException.class);
          assertThat(record(h, u, order).state()).isEqualTo("PENDING_RESERVATION");
          assertThat(h.balance(u)).isEqualByComparingTo("0.00");
        });
  }

  private static long publish(AssetTestSupport h) {
    long activity = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
    String pool = "p0-delivery-pool-" + UUID.randomUUID();
    StockProtocolV2Test.control(
        h,
        () -> {
          StockProtocolV2Test.service(h).createPool(pool, "initial", 100, "p0");
          return null;
        });
    new StrategyPublicationService(
            h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()))
        .publish(
            new PublishStrategyCommand(
                activity,
                activity,
                "OPEN",
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"),
                new ArrayList<>(
                    List.of(
                        new PublishStrategyCommand.Prize(
                            101,
                            "CREDIT",
                            new BigDecimal("10.00"),
                            new BigDecimal("10.00"),
                            pool,
                            BigDecimal.ONE,
                            0))),
                new PublishStrategyCommand.Prize(0, "NONE", null, null, null, BigDecimal.ONE, 0),
                Map.of(),
                null,
                "p0-delivery"));
    return activity;
  }

  private static String create(DrawService svc, String user, long activity) {
    return svc.create(
            new DrawCommand(
                user,
                "req-" + UUID.randomUUID(),
                activity,
                new Deadline(Instant.now().plusSeconds(60))))
        .orderId();
  }

  private static DrawDeliveryService delivery(AssetTestSupport h) {
    return new DrawDeliveryService(new MySqlDrawRepository(h.j.getDataSource()), h.assets);
  }

  private static void deliver(AssetTestSupport h, String user, EventEnvelope e) {
    h.tx(
        user,
        () -> {
          delivery(h).deliver(e);
          return null;
        });
  }

  private static DrawRecord record(AssetTestSupport h, String user, String order) {
    return h.tx(user, () -> new MySqlDrawRepository(h.j.getDataSource()).find(order, false));
  }

  private static EventEnvelope eventFor(AssetTestSupport h, String user, String order) {
    var r = record(h, user, order);
    return eventFor(h, r, r.candidate());
  }

  private static EventEnvelope eventFor(AssetTestSupport h, DrawRecord r, AwardSnapshot award) {
    var result =
        new ReservationResult(
            r.orderId(),
            r.userId(),
            r.identityHash(),
            r.requestHash(),
            "CONFIRMED",
            award,
            award.quantity());
    return new EventEnvelope(
        h.codec.newEventId(),
        "ReservationConfirmed",
        r.orderId(),
        r.biz(),
        r.userId(),
        h.codec.routeUser(r.userId()),
        r.requestHash(),
        r.objectVersion(),
        2,
        ReceiptJson.write(result));
  }
}
