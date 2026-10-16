package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import java.util.*;
import org.junit.Test;

public class PaymentFactV2Test {
  @Test
  public void differentEventsSharePaymentIdentityAndOneGrant() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var o = p.service.create(p.command("fact-" + UUID.randomUUID(), "req", "sku"));
          p.facts.apply(p.event(o.orderId(), "one", "tx-" + o.orderId(), "PAID", "10.00", "CNY"));
          p.facts.apply(p.event(o.orderId(), "two", "tx-" + o.orderId(), "PAID", "10.00", "CNY"));
          assertThat(
                  p.service
                      .get(p.control(() -> p.repo.find(o.orderId(), false)).userId(), o.orderId())
                      .state())
              .isEqualTo("PAID");
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from outbox where aggregate_id=? and"
                                  + " event_type='GrantCredit'",
                              Long.class,
                              o.orderId())))
              .isOne();
        });
  }

  @Test
  public void earlyReversalAndWrongCurrencyNeverGrant() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          for (String scenario : List.of("early", "currency")) {
            var o = p.service.create(p.command("reversal-" + UUID.randomUUID(), "req", "sku"));
            if (scenario.equals("early"))
              p.facts.apply(
                  p.event(o.orderId(), "rev", "tx-" + o.orderId(), "REVERSED", "10.00", "CNY"));
            p.facts.apply(
                p.event(
                    o.orderId(),
                    "paid",
                    "tx-" + o.orderId(),
                    "PAID",
                    "10.00",
                    scenario.equals("currency") ? "USD" : "CNY"));
            assertThat(
                    p.control(
                        () ->
                            h.j.queryForObject(
                                "select count(*) from outbox where aggregate_id=? and"
                                    + " event_type='GrantCredit'",
                                Long.class,
                                o.orderId())))
                .isZero();
            assertThat(p.control(() -> p.repo.find(o.orderId(), false)).recoveryStatus().name())
                .isEqualTo("REVIEW");
          }
        });
  }

  @Test
  public void rawSignatureAndDurableInboxDeduplicateDeliveryMetadata() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var o = p.service.create(p.command("inbox-" + UUID.randomUUID(), "req", "sku"));
          var n = new PaymentNotificationService(h.runner, p.repo, p.provider);
          var e =
              p.event(
                  o.orderId(), "evt-" + o.orderId(), "tx-" + o.orderId(), "PAID", "10.00", "CNY");
          byte[] raw = JsonBytes.event(e);
          var headers = p.provider.sign(raw);
          assertThat(n.receive("sandbox", raw, headers).body()).isEqualTo("SUCCESS");
          assertThat(n.receive("sandbox", raw, headers).httpStatus()).isEqualTo(200);
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from provider_inbox where event_id=?",
                              Long.class,
                              e.eventId())))
              .isOne();
          assertThatThrownBy(
                  () -> n.receive("sandbox", raw, Map.of("x-sandbox-signature", "invalid")))
              .isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(() -> n.receive("sandbox", new byte[65537], headers))
              .isInstanceOf(IllegalArgumentException.class);
          var changed =
              p.event(o.orderId(), e.eventId(), e.transactionId(), "PAID", "11.00", "CNY");
          byte[] conflict = JsonBytes.event(changed);
          assertThat(n.receive("sandbox", conflict, p.provider.sign(conflict)).httpStatus())
              .isEqualTo(200);
          assertThat(p.control(() -> p.repo.find(o.orderId(), false)).recoveryStatus().name())
              .isEqualTo("REVIEW");
        });
  }

  static class JsonBytes {
    static byte[] event(Object e) {
      return com.jay.infrastructure.adapter.repository.v2.JsonStore.write(e)
          .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
  }
}
