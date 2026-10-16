package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import com.jay.domain.task.model.*;
import java.util.*;
import org.junit.Test;

public class PaymentReconciliationV2Test {
  @Test
  public void deliveredRequiresExactOriginalUserLedgerAndReviewDoesNotRewriteBalances() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "reconcile-pay-" + UUID.randomUUID();
          var e = PaymentDeliveryV2Test.paid(p, u);
          var credit = new PaymentCreditService(h.assets, h.codec, x -> {});
          PaymentDeliveryV2Test.work(p)
              .applyWithResponses("payment-grant", e, () -> credit.handle(e));
          var receipt =
              h.tx(
                  u,
                  () ->
                      com.jay.infrastructure.adapter.repository.v2.JsonStore.read(
                          h.j.queryForObject(
                              "select payload from outbox where aggregate_id=? and"
                                  + " event_type='CreditGranted'",
                              String.class,
                              e.aggregateId()),
                          EventEnvelope.class));
          p.control(
              () -> {
                p.facts.onCreditGranted(receipt);
                return null;
              });
          var check = new PaymentReconciliationService(h.runner, p.repo, h.repo, p.facts);
          assertThat(check.checkOrder(e.aggregateId())).isFalse();
          h.tx(
              u,
              () -> {
                h.j.update(
                    "update "
                        + com.jay.infrastructure.adapter.repository.v2.MySqlAssetRepository.table(
                            "user_credit_ledger")
                        + " set delta=99.00 where user_id=? and biz_type='CHARGE'",
                    u);
                return null;
              });
          assertThat(check.checkOrder(e.aggregateId())).isTrue();
          assertThat(p.service.get(u, e.aggregateId()).recoveryStatus().name()).isEqualTo("REVIEW");
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
          h.tx(
              u,
              () -> {
                h.j.update(
                    "update "
                        + com.jay.infrastructure.adapter.repository.v2.MySqlAssetRepository.table(
                            "user_credit_ledger")
                        + " set delta=100.00 where user_id=? and biz_type='CHARGE'",
                    u);
                return null;
              });
        });
  }
}
