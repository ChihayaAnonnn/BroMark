package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class PaymentFailureV2Test {
  @Test
  public void changedDeliveryTimestampIsNotChangedBusinessContent() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var o = p.service.create(p.command("delivery-meta-" + UUID.randomUUID(), "req", "sku"));
          var e =
              p.event(
                  o.orderId(),
                  "same-event-" + o.orderId(),
                  "tx-" + o.orderId(),
                  "PAID",
                  "10.00",
                  "CNY");
          var n = new PaymentNotificationService(h.runner, p.repo, p.provider);
          for (String date : List.of("2026-10-15T00:00:00Z", "2026-10-16T00:00:00Z")) {
            var tree =
                (com.fasterxml.jackson.databind.node.ObjectNode)
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(JsonStore.write(e));
            tree.put("deliveredAt", date);
            byte[] raw = tree.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(n.receive("sandbox", raw, p.provider.sign(raw)).httpStatus()).isEqualTo(200);
          }
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from provider_inbox where event_id=?",
                              Long.class,
                              e.eventId())))
              .isOne();
        });
  }

  @Test
  public void inboxCommitFailureDoesNotReturnSuccessfulAck() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var o = p.service.create(p.command("inbox-fault-" + UUID.randomUUID(), "req", "sku"));
          var e =
              p.event(
                  o.orderId(),
                  "fault-event-" + o.orderId(),
                  "tx-" + o.orderId(),
                  "PAID",
                  "10.00",
                  "CNY");
          var n =
              new PaymentNotificationService(
                  h.runner,
                  p.repo,
                  p.provider,
                  x -> {
                    throw new IllegalStateException("before commit");
                  });
          byte[] raw = PaymentFactV2Test.JsonBytes.event(e);
          assertThatThrownBy(() -> n.receive("sandbox", raw, p.provider.sign(raw)))
              .isInstanceOf(IllegalStateException.class);
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from provider_inbox where event_id=?",
                              Long.class,
                              e.eventId())))
              .isZero();
        });
  }

  @Test
  public void pausedSourceDurablyDefersReceiptAndAuditedResumeDelivers() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "paused-pay-" + UUID.randomUUID();
          var e = PaymentDeliveryV2Test.paid(p, u);
          var c = new PaymentCreditService(h.assets, h.codec, x -> {});
          PaymentDeliveryV2Test.work(p).applyWithResponses("payment-grant", e, () -> c.handle(e));
          var response =
              h.tx(
                  u,
                  () ->
                      JsonStore.read(
                          h.j.queryForObject(
                              "select payload from outbox where aggregate_id=? and"
                                  + " event_type='CreditGranted'",
                              String.class,
                              e.aggregateId()),
                          EventEnvelope.class));
          var r =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("PAYMENT", p.repo),
                  h.codec::newEventId);
          r.pause(
              RouteSlot.control(),
              "PAYMENT",
              e.aggregateId(),
              RecoveryStatus.REVIEW,
              "isolated pause evidence");
          var work =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  r,
                  h.codec::newEventId);
          assertThat(
                  work.apply("payment-receipt", response, () -> p.facts.onCreditGranted(response)))
              .isEqualTo(ConsumeOutcome.DEFERRED);
          assertThat(p.service.get(u, e.aggregateId()).state()).isEqualTo("PAID");
          long version = p.control(() -> p.repo.find(e.aggregateId(), false).version());
          r.resume(
              RouteSlot.control(),
              "PAYMENT",
              e.aggregateId(),
              version,
              "test-operator",
              "verified frozen receipt");
          work.apply("payment-receipt", response, () -> p.facts.onCreditGranted(response));
          assertThat(p.service.get(u, e.aggregateId()).state()).isEqualTo("DELIVERED");
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
        });
  }

  @Test
  public void reviewCreatesAuditableRecoveryCase() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var o = p.service.create(p.command("review-case-" + UUID.randomUUID(), "req", "sku"));
          p.facts.apply(
              p.event(o.orderId(), "wrong-currency", "tx-" + o.orderId(), "PAID", "10.00", "USD"));
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from recovery_case where aggregate_type='PAYMENT'"
                                  + " and aggregate_id=? and active_key is not null",
                              Long.class,
                              o.orderId())))
              .isOne();
        });
  }

  @Test
  public void unavailableUserDatabaseDoesNotMarkSourceDelivered() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "down-pay-" + UUID.randomUUID();
          var e = PaymentDeliveryV2Test.paid(p, u);
          var source =
              (cn.bugstack.middleware.db.router.dynamic.DynamicDataSource) h.j.getDataSource();
          ((com.zaxxer.hikari.HikariDataSource)
                  source.getResolvedDataSources().get("db" + e.routeSlot().dbKey()))
              .close();
          var c = new PaymentCreditService(h.assets, h.codec, x -> {});
          assertThatThrownBy(
                  () ->
                      PaymentDeliveryV2Test.work(p)
                          .applyWithResponses("payment-grant", e, () -> c.handle(e)))
              .isInstanceOf(RuntimeException.class);
          assertThat(p.service.get(u, e.aggregateId()).state()).isEqualTo("PAID");
        });
  }
}
