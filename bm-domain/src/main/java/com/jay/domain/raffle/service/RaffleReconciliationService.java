package com.jay.domain.raffle.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.raffle.model.*;
import java.time.*;
import java.util.function.Function;

public final class RaffleReconciliationService {
  private final UserTransactionRunner tx;
  private final Function<RouteSlot, ReconciliationReport> checks;

  public RaffleReconciliationService(
      UserTransactionRunner t, Function<RouteSlot, ReconciliationReport> c) {
    tx = t;
    checks = c;
  }

  public ReconciliationReport check(RouteSlot slot) {
    return tx.inSlot(
        slot,
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(30)),
        () -> checks.apply(slot));
  }
}
