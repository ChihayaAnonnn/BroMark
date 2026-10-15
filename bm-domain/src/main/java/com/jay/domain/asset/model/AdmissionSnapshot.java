package com.jay.domain.asset.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AdmissionSnapshot(
    long activityId,
    String activityState,
    String version,
    Instant beginsAt,
    Instant endsAt,
    Instant readAt,
    Instant expiresAt,
    boolean blacklisted,
    BigDecimal creditCost,
    QuotaDelta quota,
    List<Reward> rewards) {
  public AdmissionSnapshot {
    Objects.requireNonNull(activityState);
    Objects.requireNonNull(version);
    Objects.requireNonNull(beginsAt);
    Objects.requireNonNull(endsAt);
    Objects.requireNonNull(readAt);
    Objects.requireNonNull(expiresAt);
    rewards = List.copyOf(rewards);
  }

  public record Reward(String itemId, long activityId, BigDecimal credit, QuotaDelta quota) {
    public Reward {
      if (itemId == null
          || itemId.isBlank()
          || ((credit == null) == (quota == null))
          || quota != null && activityId <= 0) throw new IllegalArgumentException("Invalid reward");
      if (credit != null) com.jay.domain.asset.service.RequestHasher.amount(credit);
    }
  }

  public boolean accepts(BusinessTime time) {
    if (time.instant().isAfter(expiresAt)) throw new BudgetExceededException("Admission expired");
    return "OPEN".equals(activityState)
        && !blacklisted
        && !time.instant().isBefore(beginsAt)
        && time.instant().isBefore(endsAt);
  }
}
