package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import com.jay.domain.task.model.*;
import java.util.*;
import org.junit.Test;

public class PaymentBoundaryV2Test {
  @Test
  public void lateTimeoutCannotEraseAuthoritativeReadyQuery() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "late-create-timeout-" + UUID.randomUUID();
          p.provider.mode("TIMEOUT_CREATED");
          p.provider.beforeResponse(
              id ->
                  new PaymentRecoveryService(
                          h.runner,
                          p.repo,
                          p.provider,
                          p.facts,
                          new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                              h.j.getDataSource()))
                      .inspect(id));
          var o = p.service.create(p.command(u, "req", "sku"));
          assertThat(o.providerCreateStatus()).isEqualTo("READY");
        });
  }

  @Test
  public void queryResolvesCreatedUnknownWithoutMakingAnotherMerchant() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "query-ready-" + UUID.randomUUID();
          p.provider.mode("TIMEOUT_CREATED");
          var o = p.service.create(p.command(u, "req", "sku"));
          p.provider.mode("NORMAL");
          new PaymentRecoveryService(
                  h.runner,
                  p.repo,
                  p.provider,
                  p.facts,
                  new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                      h.j.getDataSource()))
              .inspect(o.orderId());
          assertThat(p.service.get(u, o.orderId()).providerCreateStatus()).isEqualTo("READY");
        });
  }

  @Test
  public void alteredFirstGrantCannotBypassBenefitDigest() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "first-grant-forged-" + UUID.randomUUID();
          var e = PaymentDeliveryV2Test.paid(p, u);
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
          var credit = new PaymentCreditService(h.assets, h.codec, x -> {});
          assertThatThrownBy(
                  () ->
                      PaymentDeliveryV2Test.work(p)
                          .applyWithResponses("payment-grant", forged, () -> credit.handle(forged)))
              .isInstanceOf(AssetConflictException.class);
          assertThat(h.balance(u)).isEqualByComparingTo("0.00");
        });
  }

  @Test
  public void concurrentRequestsAcrossInstancesCreateExactlyOneLocalAndChannelOrder() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "payment-concurrent-" + UUID.randomUUID();
          var other =
              new PaymentTestSupport(
                  new AssetTestSupport(
                      h.j.getDataSource(),
                      new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                          h.j.getDataSource())));
          var counter = new java.util.concurrent.atomic.AtomicInteger();
          var views =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  20,
                  () ->
                      (counter.incrementAndGet() % 2 == 0 ? p : other)
                          .service.create(p.command(u, "req", "sku")));
          assertThat(views.stream().map(PaymentView::orderId).distinct()).hasSize(1);
          String id = views.getFirst().orderId();
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from sandbox_payment where merchant_order_id=?",
                              Long.class,
                              id)))
              .isOne();
        });
  }
}
