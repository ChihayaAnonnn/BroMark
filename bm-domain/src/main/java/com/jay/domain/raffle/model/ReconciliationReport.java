package com.jay.domain.raffle.model;

import java.util.Set;

public record ReconciliationReport(
    long creditMismatch,
    long quotaMismatch,
    long stockMismatch,
    long duplicateDelivery,
    long paymentMismatch,
    long signinMismatch,
    Set<String> checkedScopes) {
  public ReconciliationReport {
    checkedScopes = Set.copyOf(checkedScopes);
  }
}
