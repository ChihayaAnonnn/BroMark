package com.jay.domain.strategy.repository;

import com.jay.domain.strategy.model.*;

public interface StrategyVersionRepository {
  record DrawAdmission(FrozenStrategy strategy, boolean blacklisted, java.time.Instant expiresAt) {}

  default DrawAdmission admit(
      String user, long activity, com.jay.domain.asset.model.Deadline deadline) {
    return new DrawAdmission(
        current(activity),
        false,
        java.time.Instant.now().plusSeconds(3).isBefore(deadline.expiresAt())
            ? java.time.Instant.now().plusSeconds(3)
            : deadline.expiresAt());
  }

  void publish(FrozenStrategy strategy);

  FrozenStrategy load(long strategy, String version);

  FrozenStrategy current(long activity);
}
