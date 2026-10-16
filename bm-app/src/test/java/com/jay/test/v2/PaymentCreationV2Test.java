package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import java.util.*;
import org.junit.Test;

public class PaymentCreationV2Test {
  @Test
  public void stableMerchantSurvivesTimeoutAndChangedProductIsRejected() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String user = "payment-create-" + UUID.randomUUID();
          p.provider.mode("TIMEOUT_CREATED");
          var first = p.service.create(p.command(user, "request", "sku"));
          assertThat(first.providerCreateStatus()).isEqualTo("UNKNOWN");
          var next = p.service.create(p.command(user, "request", "sku"));
          assertThat(next.orderId()).isEqualTo(first.orderId());
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from sandbox_payment where merchant_order_id=?",
                              Long.class,
                              first.orderId())))
              .isOne();
          assertThatThrownBy(() -> p.service.create(p.command(user, "request", "other")))
              .isInstanceOf(AssetConflictException.class);
        });
  }

  @Test
  public void callbackBeforeCreateResponseCannotOverwritePaidState() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String user = "early-" + UUID.randomUUID();
          p.provider.beforeResponse(
              order ->
                  p.facts.apply(
                      p.event(order, "evt-" + order, "txn-" + order, "PAID", "10.00", "CNY")));
          var view = p.service.create(p.command(user, "req", "sku"));
          assertThat(view.state()).isEqualTo("PAID");
          assertThat(view.providerCreateStatus()).isEqualTo("READY");
        });
  }
}
