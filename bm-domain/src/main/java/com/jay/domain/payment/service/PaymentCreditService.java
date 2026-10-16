package com.jay.domain.payment.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.task.model.*;
import java.util.*;

public final class PaymentCreditService {
  private final AssetKernel assets;
  private final RouteCodec codec;
  private final FaultInjector faults;

  public PaymentCreditService(AssetKernel assets, RouteCodec codec, FaultInjector faults) {
    this.assets = assets;
    this.codec = codec;
    this.faults = faults;
  }

  public AssetReceipt grant(EventEnvelope e) {
    var p = PaymentPayload.read(e.payload());
    if (!p.requestHash()
            .equals(PaymentPayload.hash(p.paymentOrderId(), p.userId(), p.routeSlot(), p.amount()))
        || !"GrantCredit".equals(e.eventType())
        || !e.aggregateId().equals(p.paymentOrderId())
        || !e.userId().equals(p.userId())
        || !codec.routeUser(e.userId()).equals(e.routeSlot())
        || !e.routeSlot().equals(p.routeSlot())
        || !e.requestHash().equals(p.requestHash())
        || !e.bizId()
            .equals(new BusinessId("CHARGE", "charge:" + p.paymentOrderId(), p.requestHash()))
        || e.aggregateVersion() != 1
        || p.receipt() != null) throw new AssetConflictException("Grant identity mismatch");
    assets.lockAccounts(e.userId(), List.of());
    var receipt = assets.credit(e.userId(), e.bizId(), p.amount());
    faults.hit("payment.after-credit");
    return receipt;
  }

  public List<EventEnvelope> handle(EventEnvelope e) {
    var receipt = grant(e);
    var p = PaymentPayload.read(e.payload());
    var response =
        new PaymentPayload(
            p.paymentOrderId(), p.userId(), p.routeSlot(), p.amount(), p.requestHash(), 1, receipt);
    return List.of(
        new EventEnvelope(
            codec.newEventId(),
            "CreditGranted",
            e.aggregateId(),
            e.bizId(),
            e.userId(),
            RouteSlot.control(),
            e.requestHash(),
            1,
            1,
            PaymentPayload.json(response)));
  }
}
