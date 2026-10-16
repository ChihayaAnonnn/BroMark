package com.jay.domain.payment.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.repository.*;
import com.jay.domain.raffle.model.ReconciliationReport;
import com.jay.domain.task.model.*;
import java.time.*;
import java.util.*;

public final class PaymentReconciliationService {
  private final UserTransactionRunner tx;
  private final PaymentRepository repo;
  private final AssetRepository assets;
  private final PaymentFactService facts;

  public PaymentReconciliationService(
      UserTransactionRunner tx,
      PaymentRepository repo,
      AssetRepository assets,
      PaymentFactService facts) {
    this.tx = tx;
    this.repo = repo;
    this.assets = assets;
    this.facts = facts;
  }

  private <T> T control(java.util.function.Supplier<T> s) {
    return tx.inControl(WorkClass.RECOVERY, new Deadline(Instant.now().plusSeconds(10)), s);
  }

  public boolean checkOrder(String id) {
    var r = control(() -> repo.find(id, false));
    if (r.recoveryStatus() != RecoveryStatus.ACTIVE) return false;
    var p = facts.grantPayload(r);
    var biz = new BusinessId("CHARGE", "charge:" + id, p.requestHash());
    var ledger =
        tx.inUser(
            r.userId(),
            WorkClass.RECOVERY,
            new Deadline(Instant.now().plusSeconds(10)),
            () -> assets.creditReceipt(r.userId(), biz));
    boolean wrong =
        ledger.isPresent()
            && (!ledger.get().bizId().equals(biz)
                || ledger.get().delta().compareTo(p.amount()) != 0
                || r.state().equals("CREATED"));
    if (r.state().equals("DELIVERED"))
      wrong |= ledger.isEmpty() || r.receipt() == null || !ledger.get().equals(r.receipt());
    boolean mismatch = wrong;
    return control(
        () -> {
          var current = repo.find(id, true);
          if (current.version() != r.version() || current.recoveryStatus() != RecoveryStatus.ACTIVE)
            return false;
          boolean bad = mismatch || (!r.state().equals("CREATED") && repo.paidFacts(id) != 1);
          if (bad)
            repo.review(
                id,
                "Payment reconciliation mismatch",
                Map.of(
                    "state", r.state(), "ledger", ledger.map(Object::toString).orElse("MISSING")));
          return bad;
        });
  }

  public ReconciliationReport check() {
    var rows = control(repo::all);
    long mismatch = 0;
    for (var r : rows) if (checkOrder(r.orderId())) mismatch++;
    return new ReconciliationReport(0, 0, 0, 0, mismatch, 0, Set.of("PAYMENT"));
  }
}
