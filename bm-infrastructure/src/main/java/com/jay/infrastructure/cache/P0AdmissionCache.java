package com.jay.infrastructure.cache;

import com.google.common.cache.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.raffle.repository.DrawAdmissionSource;
import com.jay.domain.strategy.repository.StrategyVersionRepository;
import java.time.*;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Short-lived admission cache for the P0 acceptance hot path.
 *
 * <p>Keys are {@code user + ":" + activity}; a miss performs one short control read. The cached
 * admission always expires three seconds after the control read, independent of the caller's
 * deadline, so a request with a tighter deadline cannot poison later cache hits.
 */
public final class P0AdmissionCache implements DrawAdmissionSource {
  private static final Duration CONTROL_ADMISSION = Duration.ofSeconds(3);

  private final LoadingCache<String, StrategyVersionRepository.DrawAdmission> admissions;

  public P0AdmissionCache(
      UserTransactionRunner tx, StrategyVersionRepository repo, Duration ttl) {
    if (ttl == null || ttl.isZero() || ttl.isNegative())
      throw new IllegalArgumentException("Admission cache TTL");
    Objects.requireNonNull(tx);
    Objects.requireNonNull(repo);
    admissions =
        CacheBuilder.newBuilder()
            .expireAfterWrite(ttl)
            .build(
                new CacheLoader<>() {
                  public StrategyVersionRepository.DrawAdmission load(String key) {
                    int split = key.lastIndexOf(':');
                    String user = key.substring(0, split);
                    long activity = Long.parseLong(key.substring(split + 1));
                    var deadline = new Deadline(Instant.now().plus(CONTROL_ADMISSION));
                    return tx.inControl(
                        WorkClass.QUERY, deadline, () -> repo.admit(user, activity, deadline));
                  }
                });
  }

  public StrategyVersionRepository.DrawAdmission admit(
      String user, long activity, Deadline deadline) {
    Objects.requireNonNull(user);
    Objects.requireNonNull(deadline);
    try {
      var admitted = admissions.get(user + ":" + activity);
      if (deadline.expiresAt().isBefore(admitted.expiresAt()))
        return new StrategyVersionRepository.DrawAdmission(
            admitted.strategy(), admitted.blacklisted(), deadline.expiresAt());
      return admitted;
    } catch (ExecutionException e) {
      var cause = e.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new IllegalStateException("Admission load failed", cause);
    }
  }

  public void invalidate(String user, long activity) {
    admissions.invalidate(user + ":" + activity);
  }

  public void invalidateActivity(long activity) {
    String suffix = ":" + activity;
    admissions.asMap().keySet().removeIf(key -> key.endsWith(suffix));
  }
}
