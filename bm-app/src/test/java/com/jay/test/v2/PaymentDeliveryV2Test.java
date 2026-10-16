package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.util.*;
import org.junit.Test;

public class PaymentDeliveryV2Test {
  static EventEnvelope paid(PaymentTestSupport p, String u) {
    var o = p.service.create(p.command(u, "req", "sku"));
    p.facts.apply(p.event(o.orderId(), "paid", "tx-" + o.orderId(), "PAID", "10.00", "CNY"));
    return p.control(
        () ->
            JsonStore.read(
                p.h.j.queryForObject(
                    "select payload from outbox where aggregate_id=? and event_type='GrantCredit'",
                    String.class,
                    o.orderId()),
                EventEnvelope.class));
  }

  static ConsumerUnitOfWork work(PaymentTestSupport p) {
    return new ConsumerUnitOfWork(
        p.h.runner,
        new MySqlConsumerInbox(p.h.j.getDataSource()),
        new MySqlOutboxWriter(p.h.j.getDataSource()),
        new EventRouting(p.h.codec),
        null,
        p.h.codec::newEventId);
  }

  static EventEnvelope restate(PaymentTestSupport p, EventEnvelope e) {
    return new EventEnvelope(
        p.h.codec.newEventId(),
        e.eventType(),
        e.aggregateId(),
        e.bizId(),
        e.userId(),
        e.routeSlot(),
        e.requestHash(),
        e.aggregateVersion(),
        1,
        e.payload());
  }

  @Test
  public void newEventReplaysOriginalReceiptAndCreditsOnce() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "delivery-" + UUID.randomUUID();
          var e = paid(p, u);
          var credit = new PaymentCreditService(h.assets, h.codec, x -> {});
          var work = work(p);
          work.applyWithResponses("payment-grant", e, () -> credit.handle(e));
          var replay = restate(p, e);
          work.applyWithResponses("payment-grant", replay, () -> credit.handle(replay));
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
          var responses =
              h.tx(
                  u,
                  () ->
                      h.j.query(
                          "select payload from outbox where aggregate_id=? and"
                              + " event_type='CreditGranted' order by event_id",
                          (rs, n) -> JsonStore.read(rs.getString(1), EventEnvelope.class),
                          e.aggregateId()));
          assertThat(responses).hasSize(2);
          assertThat(PaymentPayload.read(responses.get(0).payload()).receipt())
              .isEqualTo(PaymentPayload.read(responses.get(1).payload()).receipt());
          for (var r : responses)
            work.apply("payment-receipt", r, () -> p.facts.onCreditGranted(r));
          assertThat(p.service.get(u, e.aggregateId()).state()).isEqualTo("DELIVERED");
        });
  }

  @Test
  public void creditFaultRollsBackInboxLedgerAndResponse() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "credit-fault-" + UUID.randomUUID();
          var e = paid(p, u);
          var work = work(p);
          var bad =
              new PaymentCreditService(
                  h.assets,
                  h.codec,
                  x -> {
                    throw new IllegalStateException("fault");
                  });
          assertThatThrownBy(() -> work.applyWithResponses("payment-grant", e, () -> bad.handle(e)))
              .isInstanceOf(IllegalStateException.class);
          assertThat(h.balance(u)).isEqualByComparingTo("0.00");
          var good = new PaymentCreditService(h.assets, h.codec, x -> {});
          work.applyWithResponses("payment-grant", e, () -> good.handle(e));
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
        });
  }

  @Test
  public void forgedRouteAndChangedAmountCannotCreditAgain() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "credit-forge-" + UUID.randomUUID();
          var e = paid(p, u);
          var credit = new PaymentCreditService(h.assets, h.codec, x -> {});
          var work = work(p);
          work.applyWithResponses("payment-grant", e, () -> credit.handle(e));
          var original = PaymentPayload.read(e.payload());
          var changed =
              new PaymentPayload(
                  e.aggregateId(),
                  u,
                  e.routeSlot(),
                  new java.math.BigDecimal("200.00"),
                  e.requestHash(),
                  1,
                  null);
          var forged =
              new EventEnvelope(
                  h.codec.newEventId(),
                  e.eventType(),
                  e.aggregateId(),
                  e.bizId(),
                  u,
                  e.routeSlot(),
                  e.requestHash(),
                  1,
                  1,
                  PaymentPayload.json(changed));
          assertThatThrownBy(
                  () ->
                      work.applyWithResponses("payment-grant", forged, () -> credit.handle(forged)))
              .isInstanceOf(AssetConflictException.class);
          var wrong =
              new EventEnvelope(
                  h.codec.newEventId(),
                  e.eventType(),
                  e.aggregateId(),
                  e.bizId(),
                  u,
                  RouteSlot.control(),
                  e.requestHash(),
                  1,
                  1,
                  e.payload());
          assertThatThrownBy(
                  () -> work.applyWithResponses("payment-grant", wrong, () -> credit.handle(wrong)))
              .isInstanceOf(IllegalArgumentException.class);
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
        });
  }
}
