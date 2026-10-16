package com.jay.domain.payment.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.payment.adapter.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.repository.*;
import com.jay.domain.task.model.RecoveryStatus;
import com.jay.domain.task.repository.*;
import java.time.*;
import java.util.*;

public final class PaymentService {
  private final UserTransactionRunner tx;
  private final PaymentRepository repo;
  private final PaymentProvider provider;
  private final RouteCodec codec;
  private final OrderIdGenerator ids;

  public PaymentService(
      UserTransactionRunner tx,
      PaymentRepository repo,
      PaymentProvider provider,
      RouteCodec codec,
      OrderIdGenerator ids,
      OutboxWriter out) {
    this.tx = tx;
    this.repo = repo;
    this.provider = provider;
    this.codec = codec;
    this.ids = ids;
  }

  public PaymentView create(PaymentCommand c) {
    if (!provider.name().equals(c.provider()))
      throw new IllegalArgumentException("Unknown provider");
    var identity =
        new RequestIdentity(
            c.userId(),
            "PAYMENT",
            c.requestId(),
            new RequestHasher()
                .hash("PAYMENT", Map.of("product", c.productId(), "provider", c.provider())));
    var old =
        tx.inControl(
            WorkClass.TRADE,
            c.deadline(),
            () -> repo.request(c.userId(), c.requestId(), identity.requestHash()));
    if (old.isPresent()) return old.get().view();
    ids.prefetch();
    String id = ids.next("PAY", RouteSlot.control());
    PaymentRecord r;
    try {
      r =
          tx.inControl(
              WorkClass.TRADE,
              c.deadline(),
              () -> {
                var repeat = repo.request(c.userId(), c.requestId(), identity.requestHash());
                if (repeat.isPresent()) return repeat.get();
                var p = repo.product(c.productId());
                var row =
                    new PaymentRecord(
                        id,
                        c.userId(),
                        c.requestId(),
                        identity.requestHash(),
                        p,
                        provider.name(),
                        provider.account(),
                        provider.environment(),
                        "CREATED",
                        "PENDING",
                        RecoveryStatus.ACTIVE,
                        0,
                        Map.of(),
                        null);
                repo.insert(row);
                return row;
              });
    } catch (org.springframework.dao.DuplicateKeyException race) {
      return tx.inControl(
          WorkClass.TRADE,
          c.deadline(),
          () ->
              repo.request(c.userId(), c.requestId(), identity.requestHash()).orElseThrow().view());
    }
    if (!r.orderId().equals(id)) return r.view();
    ProviderOrder answer;
    try {
      answer = provider.create(id, r.product().amount(), r.product().currency());
      if (!id.equals(answer.merchantOrderId()))
        throw new IllegalArgumentException("Merchant mismatch");
    } catch (RuntimeException unknown) {
      answer = new ProviderOrder(id, "UNKNOWN", Map.of());
    }
    var response = answer;
    return tx.inControl(
        WorkClass.TRADE,
        c.deadline(),
        () -> {
          var current = repo.find(id, true);
          if (current.providerCreateStatus().equals("CLOSED")
              || (current.providerCreateStatus().equals("READY")
                  && response.status().equals("UNKNOWN"))) return current.view();
          var next =
              current.change(
                  current.state(),
                  response.status(),
                  current.recoveryStatus(),
                  response.status().equals("READY") ? response.parameters() : Map.of(),
                  current.receipt());
          repo.update(current, next);
          return next.view();
        });
  }

  public PaymentView get(String user, String id) {
    return tx.inControl(
        WorkClass.QUERY,
        new Deadline(Instant.now().plusSeconds(5)),
        () -> {
          var r = repo.find(id, false);
          if (!r.userId().equals(user)) throw new AssetOwnershipException();
          return r.view();
        });
  }
}
