package com.jay.domain.payment.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.repository.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;

public final class PaymentFactService {
  private final UserTransactionRunner tx;
  private final PaymentRepository repo;
  private final RouteCodec codec;
  private final OutboxWriter out;

  public PaymentFactService(
      UserTransactionRunner tx, PaymentRepository repo, RouteCodec codec, OutboxWriter out) {
    this.tx = tx;
    this.repo = repo;
    this.codec = codec;
    this.out = out;
  }

  public void apply(VerifiedPaymentEvent e) {
    tx.inControl(
        WorkClass.WORKER,
        new Deadline(Instant.now().plusSeconds(10)),
        () -> {
          applyInTransaction(e);
          return null;
        });
  }

  public void applyInTransaction(VerifiedPaymentEvent e) {
    var f = repo.lockFact(e);
    PaymentRecord r;
    try {
      r = repo.find(e.merchantOrderId(), true);
    } catch (AssetNotFoundException missing) {
      repo.review(e.merchantOrderId(), "Unknown merchant order", e);
      return;
    }
    boolean binding =
        f.get("order_id").equals(e.merchantOrderId())
            && f.get("user_id").equals(e.userBinding())
            && ((BigDecimal) f.get("amount")).compareTo(e.amount()) == 0
            && f.get("currency").equals(e.currency());
    if (!binding) {
      repo.review((String) f.get("order_id"), "Payment identity rebound", e);
      repo.review(r.orderId(), "Payment identity rebound", e);
      return;
    }
    if (!r.provider().equals(e.provider())
        || !r.providerAccount().equals(e.providerAccount())
        || !r.environment().equals(e.environment())
        || !r.userId().equals(e.userBinding())
        || r.product().amount().compareTo(e.amount()) != 0
        || !r.product().currency().equals(e.currency())) {
      repo.review(r.orderId(), "Payment snapshot mismatch", e);
      return;
    }
    repo.markFact(e);
    if (e.type().equals("REVERSED")
        || Boolean.TRUE.equals(f.get("reversed"))
        || "1".equals(f.get("reversed").toString())) {
      repo.review(r.orderId(), "Payment reversal requires manual review", e);
      return;
    }
    if (r.recoveryStatus() != RecoveryStatus.ACTIVE) return;
    if (repo.paidFacts(r.orderId()) != 1) {
      repo.review(r.orderId(), "Multiple payment identities for order", e);
      return;
    }
    if (r.state().equals("CREATED")) {
      var next =
          r.change("PAID", r.providerCreateStatus(), RecoveryStatus.ACTIVE, r.parameters(), null);
      repo.update(r, next);
      out.append(grant(next));
    }
  }

  public EventEnvelope grant(PaymentRecord r) {
    var slot = codec.routeUser(r.userId());
    String hash = PaymentPayload.hash(r.orderId(), r.userId(), slot, r.product().credit());
    var p = new PaymentPayload(r.orderId(), r.userId(), slot, r.product().credit(), hash, 1, null);
    return new EventEnvelope(
        codec.newEventId(),
        "GrantCredit",
        r.orderId(),
        new BusinessId("CHARGE", "charge:" + r.orderId(), hash),
        r.userId(),
        slot,
        hash,
        1,
        1,
        PaymentPayload.json(p));
  }

  public void onCreditGranted(EventEnvelope e) {
    var r = repo.find(e.aggregateId(), true);
    var p = PaymentPayload.read(e.payload());
    var expected = grantPayload(r);
    if (!e.eventType().equals("CreditGranted")
        || !e.routeSlot().equals(RouteSlot.control())
        || !e.userId().equals(r.userId())
        || !e.bizId()
            .equals(new BusinessId("CHARGE", "charge:" + r.orderId(), expected.requestHash()))
        || !e.requestHash().equals(expected.requestHash())
        || !p.paymentOrderId().equals(r.orderId())
        || !p.userId().equals(r.userId())
        || !p.routeSlot().equals(expected.routeSlot())
        || p.amount().compareTo(expected.amount()) != 0
        || !p.requestHash().equals(expected.requestHash())
        || p.receipt() == null
        || !p.receipt().bizId().equals(e.bizId())
        || p.receipt().delta().compareTo(expected.amount()) != 0
        || e.aggregateVersion() != 1) throw new AssetConflictException("Credit receipt mismatch");
    if (r.recoveryStatus() != RecoveryStatus.ACTIVE)
      throw new AssetConflictException("Payment paused");
    if (r.state().equals("DELIVERED")) {
      if (!r.receipt().equals(p.receipt()))
        throw new AssetConflictException("Original receipt changed");
      return;
    }
    if (!r.state().equals("PAID")) throw new AssetConflictException("Payment not paid");
    repo.update(
        r,
        r.change(
            "DELIVERED",
            r.providerCreateStatus(),
            r.recoveryStatus(),
            r.parameters(),
            p.receipt()));
  }

  public PaymentPayload grantPayload(PaymentRecord r) {
    var slot = codec.routeUser(r.userId());
    return new PaymentPayload(
        r.orderId(),
        r.userId(),
        slot,
        r.product().credit(),
        PaymentPayload.hash(r.orderId(), r.userId(), slot, r.product().credit()),
        1,
        null);
  }
}
