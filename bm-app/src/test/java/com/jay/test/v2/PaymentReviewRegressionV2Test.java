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

public class PaymentReviewRegressionV2Test {
  static RecoveryService recovery(PaymentTestSupport p) {
    return new RecoveryService(
        p.h.runner,
        new MySqlRecoveryRepository(p.h.j.getDataSource()),
        new MySqlOutboxWriter(p.h.j.getDataSource()),
        Map.of("PAYMENT", p.repo),
        p.h.codec::newEventId);
  }

  static PaymentRecoveryService paymentRecovery(PaymentTestSupport p) {
    return new PaymentRecoveryService(
        p.h.runner, p.repo, p.provider, p.facts, new MySqlOutboxWriter(p.h.j.getDataSource()));
  }

  @Test
  public void unboundPaymentReviewCanBeAuditedClosed() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String id = "PAY-00.000-missing-" + Math.abs(UUID.randomUUID().getLeastSignificantBits());
          var e =
              new VerifiedPaymentEvent(
                  "sandbox",
                  "merchant-it",
                  "test",
                  "unknown-" + UUID.randomUUID(),
                  "unknown-tx-" + UUID.randomUUID(),
                  id,
                  "PAID",
                  new java.math.BigDecimal("10.00"),
                  "CNY",
                  "missing-user",
                  null);
          p.facts.apply(e);
          var cases = new MySqlRecoveryRepository(h.j.getDataSource());
          long version = p.control(() -> cases.active("PAYMENT", id).version());
          recovery(p)
              .resume(
                  RouteSlot.control(),
                  "PAYMENT",
                  id,
                  version,
                  "test-operator",
                  "verified unknown source evidence; no fulfillment");
          assertThat(p.control(() -> cases.active("PAYMENT", id))).isNull();
        });
  }

  @Test
  public void recoveryCreatesChannelWithSameMerchantAfterLocalCommitOnly() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "pre-channel-crash-" + UUID.randomUUID();
          var o = p.service.create(p.command(u, "req", "sku"));
          p.control(
              () -> {
                h.j.update("delete from sandbox_payment where merchant_order_id=?", o.orderId());
                h.j.update(
                    "update payment_order set provider_create_status='PENDING' where order_id=?",
                    o.orderId());
                return null;
              });
          paymentRecovery(p).inspect(o.orderId());
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from sandbox_payment where merchant_order_id=?",
                              Long.class,
                              o.orderId())))
              .isOne();
          assertThat(p.service.get(u, o.orderId()).providerCreateStatus()).isEqualTo("READY");
        });
  }

  @Test
  public void pausedVerifiedPaymentContinuesEvenWhenChannelQueryIsUnknown() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "paused-fact-" + UUID.randomUUID();
          var o = p.service.create(p.command(u, "req", "sku"));
          var r = recovery(p);
          r.pause(
              RouteSlot.control(),
              "PAYMENT",
              o.orderId(),
              RecoveryStatus.REVIEW,
              "pause before notification");
          var event =
              p.event(
                  o.orderId(),
                  "paused-event-" + o.orderId(),
                  "tx-" + o.orderId(),
                  "PAID",
                  "10.00",
                  "CNY");
          var n = new PaymentNotificationService(h.runner, p.repo, p.provider);
          byte[] raw = PaymentFactV2Test.JsonBytes.event(event);
          n.receive("sandbox", raw, p.provider.sign(raw));
          new com.jay.trigger.job.v2.PaymentInboxJob(
                  h.runner,
                  p.repo,
                  p.facts,
                  new com.jay.infrastructure.id.BlockOrderIdGenerator(
                      h.runner, h.j, "reviewworker"))
              .tick();
          assertThat(p.service.get(u, o.orderId()).state()).isEqualTo("CREATED");
          long version = p.control(() -> p.repo.find(o.orderId(), false).version());
          r.resume(
              RouteSlot.control(),
              "PAYMENT",
              o.orderId(),
              version,
              "test-operator",
              "validated payment snapshot");
          p.provider.mode("QUERY_UNKNOWN");
          paymentRecovery(p).inspect(o.orderId());
          assertThat(p.service.get(u, o.orderId()).state()).isEqualTo("PAID");
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from outbox where aggregate_id=? and"
                                  + " event_type='GrantCredit'",
                              Long.class,
                              o.orderId())))
              .isOne();
          var grant =
              p.control(
                  () ->
                      JsonStore.read(
                          h.j.queryForObject(
                              "select payload from outbox where aggregate_id=? and"
                                  + " event_type='GrantCredit'",
                              String.class,
                              o.orderId()),
                          EventEnvelope.class));
          var credit = new PaymentCreditService(h.assets, h.codec, x -> {});
          PaymentDeliveryV2Test.work(p)
              .applyWithResponses("payment-grant", grant, () -> credit.handle(grant));
          var receipt =
              h.tx(
                  u,
                  () ->
                      JsonStore.read(
                          h.j.queryForObject(
                              "select payload from outbox where aggregate_id=? and"
                                  + " event_type='CreditGranted'",
                              String.class,
                              o.orderId()),
                          EventEnvelope.class));
          PaymentDeliveryV2Test.work(p)
              .apply("payment-receipt", receipt, () -> p.facts.onCreditGranted(receipt));
          assertThat(p.service.get(u, o.orderId()).state()).isEqualTo("DELIVERED");
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
        });
  }

  @Test
  public void reconciliationDetectsWrongPaidFactDespiteMatchingCreditReceipt() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "bad-paid-fact-" + UUID.randomUUID();
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
          p.control(
              () -> {
                p.facts.onCreditGranted(response);
                h.j.update(
                    "update provider_payment_fact set currency='USD' where order_id=?",
                    e.aggregateId());
                return null;
              });
          assertThat(
                  new PaymentReconciliationService(h.runner, p.repo, h.repo, p.facts)
                      .checkOrder(e.aggregateId()))
              .isTrue();
          assertThat(h.balance(u)).isEqualByComparingTo("100.00");
        });
  }

  @Test
  public void corruptInboxDoesNotBlockFollowingValidNotification() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "inbox-poison-" + UUID.randomUUID();
          var o = p.service.create(p.command(u, "req", "sku"));
          var event =
              p.event(
                  o.orderId(), "valid-" + o.orderId(), "tx-" + o.orderId(), "PAID", "10.00", "CNY");
          var n = new PaymentNotificationService(h.runner, p.repo, p.provider);
          byte[] raw = PaymentFactV2Test.JsonBytes.event(event);
          n.receive("sandbox", raw, p.provider.sign(raw));
          String bad = "corrupt-" + UUID.randomUUID();
          p.control(
              () -> {
                h.j.update(
                    "insert into"
                        + " provider_inbox(provider,account,environment,event_id,canonical_hash,payload,next_attempt_at)"
                        + " values('sandbox','merchant-it','test',?,?,'{}','1970-01-01')",
                    bad,
                    "a".repeat(64));
                return null;
              });
          var job =
              new com.jay.trigger.job.v2.PaymentInboxJob(
                  h.runner,
                  p.repo,
                  p.facts,
                  new com.jay.infrastructure.id.BlockOrderIdGenerator(
                      h.runner, h.j, "poisonworker"));
          job.tick();
          assertThat(p.service.get(u, o.orderId()).state()).isEqualTo("PAID");
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select state from provider_inbox where event_id=?",
                              String.class,
                              bad)))
              .isEqualTo("DEAD");
        });
  }

  @Test
  public void notificationPreservesHashOfExactSignedBytes() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var o = p.service.create(p.command("raw-hash-" + UUID.randomUUID(), "req", "sku"));
          var e =
              p.event(
                  o.orderId(), "raw-" + o.orderId(), "tx-" + o.orderId(), "PAID", "10.00", "CNY");
          byte[] raw = PaymentFactV2Test.JsonBytes.event(e);
          new PaymentNotificationService(h.runner, p.repo, p.provider)
              .receive("sandbox", raw, p.provider.sign(raw));
          String expected =
              java.util.HexFormat.of()
                  .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw));
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select json_unquote(json_extract(payload,'$.rawSha256')) from"
                                  + " provider_inbox where event_id=?",
                              String.class,
                              e.eventId())))
              .isEqualTo(expected);
        });
  }

  @Test
  public void reconciliationFindsChargeWithoutAnySourceOrder() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String u = "orphan-charge-" + UUID.randomUUID();
          String id = "PAY-00.000-orphan-" + Math.abs(UUID.randomUUID().getLeastSignificantBits());
          String hash =
              PaymentPayload.hash(id, u, h.codec.routeUser(u), new java.math.BigDecimal("100.00"));
          h.tx(
              u,
              () ->
                  h.assets.credit(
                      u,
                      new BusinessId("CHARGE", "charge:" + id, hash),
                      new java.math.BigDecimal("100.00")));
          new PaymentReconciliationService(h.runner, p.repo, h.repo, p.facts).check();
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select count(*) from recovery_case where aggregate_type='PAYMENT'"
                                  + " and aggregate_id=? and active_key is not null",
                              Long.class,
                              id)))
              .isOne();
        });
  }

  @Test
  public void persistentInboxFailureHasFiniteRetryBudget() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          var o = p.service.create(p.command("inbox-exhausted-" + UUID.randomUUID(), "req", "sku"));
          var e =
              p.event(
                  o.orderId(),
                  "exhausted-" + o.orderId(),
                  "tx-" + o.orderId(),
                  "PAID",
                  "10.00",
                  "CNY");
          byte[] raw = PaymentFactV2Test.JsonBytes.event(e);
          new PaymentNotificationService(h.runner, p.repo, p.provider)
              .receive("sandbox", raw, p.provider.sign(raw));
          p.control(
              () -> {
                h.j.update("update payment_order set snapshot='{}' where order_id=?", o.orderId());
                return null;
              });
          var job =
              new com.jay.trigger.job.v2.PaymentInboxJob(
                  h.runner,
                  p.repo,
                  p.facts,
                  new com.jay.infrastructure.id.BlockOrderIdGenerator(
                      h.runner, h.j, "failureworker"));
          for (int n = 0; n < 12; n++) {
            p.control(
                () -> {
                  h.j.update(
                      "update provider_inbox set"
                          + " lease_until='1970-01-01',next_attempt_at='1970-01-01' where"
                          + " event_id=?",
                      e.eventId());
                  return null;
                });
            try {
              job.tick();
            } catch (RuntimeException expectedOldPoisonBehavior) {
            }
          }
          assertThat(
                  p.control(
                      () ->
                          h.j.queryForObject(
                              "select state from provider_inbox where event_id=?",
                              String.class,
                              e.eventId())))
              .isEqualTo("DEAD");
        });
  }

  @Test
  public void missingSourceCreditReceiptIsDurablyHandedOffBeforeAck() {
    AssetTestSupport.run(
        h -> {
          var p = new PaymentTestSupport(h);
          String id =
              "PAY-00.000-receiptmissing-" + Math.abs(UUID.randomUUID().getLeastSignificantBits());
          String hash = "a".repeat(64);
          var e =
              new EventEnvelope(
                  h.codec.newEventId(),
                  "CreditGranted",
                  id,
                  new BusinessId("CHARGE", "charge:" + id, hash),
                  "missing-receipt-user",
                  RouteSlot.control(),
                  hash,
                  1,
                  1,
                  "{}");
          var recovery = recovery(p);
          var work =
              new ConsumerUnitOfWork(
                  h.runner,
                  new MySqlConsumerInbox(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  recovery,
                  h.codec::newEventId);
          var ack = new java.util.concurrent.atomic.AtomicBoolean();
          var paused = new java.util.concurrent.atomic.AtomicBoolean();
          var listener =
              new com.jay.trigger.listener.v2.V2MessageListener(
                  h.runner,
                  work,
                  new MySqlRetryJobRepository(h.j.getDataSource(), h.runner),
                  recovery,
                  new EventRouting(h.codec),
                  raw ->
                      JsonStore.read(
                          new String(raw, java.nio.charset.StandardCharsets.UTF_8),
                          EventEnvelope.class),
                  new MySqlRawMessageStore(h.j.getDataSource()),
                  Map.of(
                      "CreditGranted",
                      event -> {
                        p.facts.onCreditGranted(event);
                        return List.of();
                      }),
                  x -> {});
          listener.handle(
              "workflow-v2",
              JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8),
              0,
              new com.jay.trigger.listener.v2.V2MessageListener.Delivery() {
                public void ack() {
                  assertThat(
                          p.control(
                              () ->
                                  h.j.queryForObject(
                                      "select count(*) from retry_job where event_id=?",
                                      Long.class,
                                      e.eventId())))
                      .isOne();
                  ack.set(true);
                }

                public void pause() {
                  paused.set(true);
                }
              });
          assertThat(ack.get()).isTrue();
          assertThat(paused.get()).isFalse();
        });
  }
}
