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
    PaymentRecord r;
    try {
      r = control(() -> repo.find(id, false));
    } catch (IllegalStateException | AssetNotFoundException invalid) {
      control(
          () -> {
            repo.review(id, "Missing/corrupt payment source snapshot", Map.of("order", id));
            return null;
          });
      return true;
    }
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
          boolean bad =
              mismatch || (!r.state().equals("CREATED") && !validPaidFacts(r, repo.facts(id)));
          if (bad)
            repo.review(
                id,
                "Payment reconciliation mismatch",
                Map.of(
                    "state", r.state(), "ledger", ledger.map(Object::toString).orElse("MISSING")));
          return bad;
        });
  }

  private boolean validPaidFacts(PaymentRecord r, List<PaymentFact> facts) {
    var paid = facts.stream().filter(PaymentFact::paid).toList();
    return paid.size() == 1 && !paid.getFirst().reversed() && paid.getFirst().matches(r);
  }

  private boolean orphanFact(String id) {
    return control(
        () -> {
          try {
            repo.find(id, false);
            return false;
          } catch (AssetNotFoundException | IllegalStateException absent) {
            repo.review(id, "Payment fact has missing/corrupt source order", Map.of("order", id));
            return true;
          }
        });
  }

  private boolean checkCharge(PaymentCharge c) {
    String biz = c.receipt().bizId().id();
    String id =
        biz.startsWith("charge:")
            ? biz.substring(7)
            : "CHARGE-"
                + new com.jay.domain.asset.service.RequestHasher()
                    .hash("ORPHAN_CHARGE", Map.of("biz", biz, "user", c.userId()));
    return control(
        () -> {
          PaymentRecord r;
          try {
            r = repo.find(id, true);
          } catch (AssetNotFoundException | IllegalStateException missing) {
            repo.review(id, "Charge ledger has missing/corrupt source order", c);
            return true;
          }
          var p = facts.grantPayload(r);
          boolean mismatch =
              !r.userId().equals(c.userId())
                  || !r.state().equals("PAID") && !r.state().equals("DELIVERED")
                  || !c.receipt().bizId().hash().equals(p.requestHash())
                  || c.receipt().delta().compareTo(p.amount()) != 0;
          if (mismatch) repo.review(id, "Charge ledger source binding mismatch", c);
          return mismatch;
        });
  }

  public ReconciliationReport check() {
    var rows = control(repo::orderIds);
    Set<String> mismatches = new HashSet<>();
    for (var id : rows) if (checkOrder(id)) mismatches.add(id);
    for (var id : control(repo::factOrderIds)) if (orphanFact(id)) mismatches.add(id);
    for (String db : List.of("01", "02"))
      for (int suffix = 0; suffix < 4; suffix++) {
        var slot = new RouteSlot(db, String.format("%03d", suffix));
        var charges =
            tx.inSlot(
                slot,
                WorkClass.RECOVERY,
                new Deadline(Instant.now().plusSeconds(10)),
                repo::charges);
        for (var c : charges) if (checkCharge(c)) mismatches.add(c.receipt().bizId().id());
      }
    return new ReconciliationReport(
        0,
        0,
        0,
        0,
        mismatches.size(),
        0,
        Set.of("PAYMENT", "PAYMENT_ORDERS", "PAYMENT_FACTS", "PAYMENT_CHARGES"));
  }
}
