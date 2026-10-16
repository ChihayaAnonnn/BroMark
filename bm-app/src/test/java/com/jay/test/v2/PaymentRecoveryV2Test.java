package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import java.util.*;
import org.junit.Test;

public class PaymentRecoveryV2Test {
  @Test
  public void productionProfileFailsBeanStartupEvenWhenTestIsAlsoActive() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var env = new org.springframework.core.env.StandardEnvironment();
          env.setActiveProfiles("prod", "test");
          new org.springframework.boot.test.context.runner.ApplicationContextRunner()
              .withBean(
                  "sandbox",
                  com.jay.infrastructure.gateway.payment.SandboxPaymentProvider.class,
                  () ->
                      new com.jay.config.V2PaymentConfig()
                          .paymentProvider(
                              h.runner,
                              p.repo,
                              env,
                              "merchant-it",
                              "isolated-signing-key-at-least-32-bytes"))
              .run(
                  c -> {
                    assertThat(c).hasFailed();
                    assertThat(c.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasStackTraceContaining("forbids production");
                  });
        });
  }

  @Test
  public void originalMerchantIsQueriedAndPaidReplaysOnlyFrozenBenefits() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "recover-" + UUID.randomUUID();
          p.provider.mode("TIMEOUT_CREATED");
          var o = p.service.create(p.command(u, "req", "sku"));
          var recovery =
              new PaymentRecoveryService(
                  h.runner,
                  p.repo,
                  p.provider,
                  p.facts,
                  new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                      h.j.getDataSource()));
          p.provider.mode("QUERY_UNKNOWN");
          assertThat(recovery.inspect(o.orderId()).actions()).isEmpty();
          assertThat(p.service.get(u, o.orderId()).providerCreateStatus()).isEqualTo("UNKNOWN");
          p.provider.mode("NORMAL");
          p.provider.record(
              p.event(o.orderId(), "recover-paid", "tx-" + o.orderId(), "PAID", "10.00", "CNY"));
          recovery.inspect(o.orderId());
          assertThat(p.service.get(u, o.orderId()).state()).isEqualTo("PAID");
          p.control(
              () -> {
                h.j.update("update payment_product set credit=999.00 where product_id='sku'");
                return null;
              });
          var decision = recovery.inspect(o.orderId());
          assertThat(decision.actions()).hasSize(1);
          assertThat(PaymentPayload.read(decision.actions().getFirst().payload()).amount())
              .isEqualByComparingTo("100.00");
          assertThatThrownBy(() -> p.service.get("other-user", o.orderId()))
              .isInstanceOf(AssetOwnershipException.class);
        });
  }

  @Test
  public void lateReversalPreservesDeliveredCreditAndStopsAutomaticRecovery() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "late-reversal-" + UUID.randomUUID();
          var e = PaymentDeliveryV2Test.paid(p, u);
          var credit = new PaymentCreditService(h.assets, h.codec, x -> {});
          PaymentDeliveryV2Test.work(p)
              .applyWithResponses("payment-grant", e, () -> credit.handle(e));
          var response =
              h.tx(
                  u,
                  () ->
                      h.j.queryForObject(
                          "select payload from outbox where aggregate_id=? and"
                              + " event_type='CreditGranted'",
                          String.class,
                          e.aggregateId()));
          var receipt =
              com.jay.infrastructure.adapter.repository.v2.JsonStore.read(
                  response, com.jay.domain.task.model.EventEnvelope.class);
          p.control(
              () -> {
                p.facts.onCreditGranted(receipt);
                return null;
              });
          p.facts.apply(
              p.event(e.aggregateId(), "rev", "tx-" + e.aggregateId(), "REVERSED", "10.00", "CNY"));
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
          assertThat(p.service.get(u, e.aggregateId()).state()).isEqualTo("DELIVERED");
          assertThat(p.service.get(u, e.aggregateId()).recoveryStatus().name()).isEqualTo("REVIEW");
          var recovery =
              new PaymentRecoveryService(
                  h.runner,
                  p.repo,
                  p.provider,
                  p.facts,
                  new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                      h.j.getDataSource()));
          assertThat(recovery.inspect(e.aggregateId()).actions()).isEmpty();
        });
  }

  @Test
  public void sandboxIsRejectedWhenAnyProductionProfileIsActive() {
    assertThatThrownBy(
            () -> com.jay.config.V2PaymentConfig.requireSandboxProfiles(Set.of("prod", "test")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> com.jay.config.V2PaymentConfig.requireSandboxProfiles(Set.of("v2-it")))
        .isInstanceOf(IllegalStateException.class);
    com.jay.config.V2PaymentConfig.requireSandboxProfiles(Set.of("v2-it", "test"));
  }
}
