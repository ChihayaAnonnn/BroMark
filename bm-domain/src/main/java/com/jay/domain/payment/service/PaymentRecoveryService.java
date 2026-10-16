package com.jay.domain.payment.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.payment.adapter.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.repository.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.time.*;
import java.util.*;

public final class PaymentRecoveryService {
  private final UserTransactionRunner tx;
  private final PaymentRepository repo;
  private final PaymentProvider provider;
  private final PaymentFactService facts;
  private final OutboxWriter out;

  public PaymentRecoveryService(
      UserTransactionRunner tx,
      PaymentRepository repo,
      PaymentProvider provider,
      PaymentFactService facts,
      OutboxWriter out) {
    this.tx = tx;
    this.repo = repo;
    this.provider = provider;
    this.facts = facts;
    this.out = out;
  }

  private com.jay.domain.task.service.WorkflowRedrive guarded;
  private final com.jay.domain.task.service.WorkflowRecoveryScan scanner =
      new com.jay.domain.task.service.WorkflowRecoveryScan();

  public PaymentRecoveryService(
      UserTransactionRunner tx,
      PaymentRepository repo,
      PaymentProvider provider,
      PaymentFactService facts,
      OutboxWriter out,
      com.jay.domain.task.service.WorkflowRedrive guarded) {
    this(tx, repo, provider, facts, out);
    this.guarded = guarded;
  }

  public PaymentRecoveryService scanBudget(int candidates, int emissions, Duration timeout) {
    if (guarded != null) scanner.budget(candidates, emissions, timeout);
    return this;
  }

  private <T> T control(java.util.function.Supplier<T> s) {
    return tx.inControl(WorkClass.RECOVERY, new Deadline(Instant.now().plusSeconds(10)), s);
  }

  public RecoveryDecision inspect(String id) {
    var r =
        control(
            () -> {
              repo.touch(id);
              return repo.find(id, false);
            });
    if (r.recoveryStatus() != RecoveryStatus.ACTIVE) return new RecoveryDecision(List.of());
    if (r.state().equals("CREATED")) {
      var stored = control(() -> repo.facts(id));
      var paid = stored.stream().filter(PaymentFact::paid).findFirst();
      if (paid.isPresent()) {
        facts.apply(paid.get().event());
        return new RecoveryDecision(List.of());
      }
      var answer = provider.query(id);
      if (answer.status().equals("NOT_FOUND")) {
        ProviderOrder created;
        try {
          created = provider.create(id, r.product().amount(), r.product().currency());
          if (!id.equals(created.merchantOrderId()))
            throw new IllegalArgumentException("Create merchant mismatch");
        } catch (RuntimeException unknown) {
          created = new ProviderOrder(id, "UNKNOWN", Map.of());
        }
        var response = created;
        control(
            () -> {
              var current = repo.find(id, true);
              if (current.recoveryStatus() == RecoveryStatus.ACTIVE
                  && !Set.of("READY", "CLOSED").contains(current.providerCreateStatus()))
                repo.update(
                    current,
                    current.change(
                        current.state(),
                        response.status(),
                        current.recoveryStatus(),
                        response.parameters(),
                        current.receipt()));
              return null;
            });
        return new RecoveryDecision(List.of());
      }
      if (!answer.merchantOrderId().equals(id))
        throw new IllegalArgumentException("Query merchant mismatch");
      if (answer.fact() != null) {
        var e = answer.fact();
        if (!e.merchantOrderId().equals(id)
            || !e.provider().equals(provider.name())
            || !e.providerAccount().equals(provider.account())
            || !e.environment().equals(provider.environment()))
          throw new IllegalArgumentException("Query fact mismatch");
        facts.apply(e);
      }
      if (!answer.status().equals("UNKNOWN"))
        control(
            () -> {
              var current = repo.find(id, true);
              if (current.recoveryStatus() == RecoveryStatus.ACTIVE
                  && !current.providerCreateStatus().equals("READY")) {
                repo.update(
                    current,
                    current.change(
                        current.state(),
                        answer.status().equals("CLOSED") ? "CLOSED" : "READY",
                        current.recoveryStatus(),
                        answer.status().equals("CLOSED") ? Map.of() : Map.of("sandboxOrder", id),
                        current.receipt()));
              }
              return null;
            });
      return new RecoveryDecision(List.of());
    }
    if (!r.state().equals("PAID")) return new RecoveryDecision(List.of());
    if (guarded != null) {
      var command = facts.grant(r);
      var remote = guarded.observe(command);
      return control(
          () -> {
            repo.lock(id);
            var current = repo.find(id, false);
            if (current.version() != r.version()
                || current.recoveryStatus() != RecoveryStatus.ACTIVE
                || !current.state().equals("PAID")) return new RecoveryDecision(List.of());
            return guarded.commit(
                "PAYMENT",
                id,
                current.state(),
                current.version(),
                repo.lastProgressAt(id),
                command,
                remote);
          });
    }
    return control(
        () -> {
          var current = repo.find(id, true);
          if (current.version() != r.version()
              || current.recoveryStatus() != RecoveryStatus.ACTIVE
              || !current.state().equals("PAID")) return new RecoveryDecision(List.of());
          var e = facts.grant(current);
          out.append(e);
          return new RecoveryDecision(List.of(e));
        });
  }

  public void tick() {
    if (guarded != null) {
      scanner.run(
          List.of(
              new com.jay.domain.task.service.WorkflowRecoveryScan.Partition(
                  RouteSlot.control(), "CREATED"),
              new com.jay.domain.task.service.WorkflowRecoveryScan.Partition(
                  RouteSlot.control(), "PAID")),
          (p, c) -> control(() -> repo.scanStale(p.state(), c, guarded.cutoff(), 1)),
          id -> inspect(id).actions().size());
      return;
    }
    for (var id : control(() -> repo.scanActive(8)))
      try {
        inspect(id);
      } catch (RuntimeException e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Payment recovery retained order {}", id);
      }
  }
}
