package com.jay.domain.asset.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.AssetRepository;
import java.util.*;

public final class QuotaKernel {
  private final AssetRepository repo;

  public QuotaKernel(AssetRepository repo) {
    this.repo = repo;
  }

  private Optional<QuotaReceipt> replay(String u, long a, BusinessId id, String kind) {
    repo.requireLocked(u, a);
    var old = repo.quotaReceipt(u, a, id, kind);
    if (old.isPresent() && !old.get().bizId().hash().equals(id.hash()))
      throw new AssetConflictException("Quota business hash changed");
    return old;
  }

  public QuotaReceipt grant(String u, long a, BusinessId id, QuotaDelta d, BusinessTime t) {
    var old = replay(u, a, id, "GRANT");
    if (old.isPresent()) {
      if (!d.equals(old.get().granted())) throw new AssetConflictException("Grant amount changed");
      return old.get();
    }
    var s = repo.quotaState(u, a, t);
    var n =
        new QuotaState(
            Math.addExact(s.total(), d.totalDelta()),
            Math.addExact(s.dayLimit(), d.dayLimitDelta()),
            Math.addExact(s.monthLimit(), d.monthLimitDelta()),
            s.effective(),
            Math.addExact(s.dayRemaining(), d.dayLimitDelta()),
            Math.addExact(s.monthRemaining(), d.monthLimitDelta()),
            s.dayEffective());
    return write(u, a, id, "GRANT", d, s, n, t);
  }

  public Optional<QuotaReceipt> consume(String u, long a, BusinessId id, BusinessTime t) {
    var old = replay(u, a, id, "CONSUME");
    if (old.isPresent()) return old;
    var s = repo.quotaState(u, a, t);
    if (s.total() == 0 || s.dayRemaining() == 0 || s.monthRemaining() == 0) return Optional.empty();
    var n =
        new QuotaState(
            s.total() - 1,
            s.dayLimit(),
            s.monthLimit(),
            Math.addExact(s.effective(), 1),
            s.dayRemaining() - 1,
            s.monthRemaining() - 1,
            Math.addExact(s.dayEffective(), 1));
    return Optional.of(write(u, a, id, "CONSUME", new QuotaDelta(0, 0, 0), s, n, t));
  }

  public QuotaReceipt refund(String u, long a, BusinessId original, BusinessTime t) {
    var consumed =
        replay(u, a, original, "CONSUME")
            .orElseThrow(() -> new AssetConflictException("Missing original consumption"));
    if (!consumed.businessTime().equals(t))
      throw new AssetConflictException("Original period mismatch");
    var old = replay(u, a, original, "REFUND");
    if (old.isPresent()) return old.get();
    var s = repo.quotaState(u, a, t);
    if (s.effective() == 0 || s.dayEffective() == 0)
      throw new AssetConflictException("Consumption counter mismatch");
    var n =
        new QuotaState(
            Math.addExact(s.total(), 1),
            s.dayLimit(),
            s.monthLimit(),
            s.effective() - 1,
            Math.addExact(s.dayRemaining(), 1),
            Math.addExact(s.monthRemaining(), 1),
            s.dayEffective() - 1);
    return write(u, a, original, "REFUND", new QuotaDelta(0, 0, 0), s, n, t);
  }

  private QuotaReceipt write(
      String u,
      long a,
      BusinessId id,
      String kind,
      QuotaDelta d,
      QuotaState before,
      QuotaState after,
      BusinessTime t) {
    var receipt =
        new QuotaReceipt(id, a, after.total(), after.dayRemaining(), after.monthRemaining(), t, d);
    repo.writeQuota(u, a, id, kind, d, before, after, receipt);
    return receipt;
  }
}
