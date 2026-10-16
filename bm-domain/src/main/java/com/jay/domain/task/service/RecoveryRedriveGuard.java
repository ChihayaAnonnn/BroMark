package com.jay.domain.task.service;

import com.jay.domain.task.model.*;
import java.time.*;
import java.util.Objects;

/** 纯判定；不跨库、不提交，不把版本递增当作业务进展。 */
public final class RecoveryRedriveGuard {
  private final Duration staleAfter, interval;
  private final int maxPerStage;

  public RecoveryRedriveGuard(Duration staleAfter, Duration interval, int maxPerStage) {
    if (staleAfter == null
        || interval == null
        || staleAfter.isNegative()
        || staleAfter.isZero()
        || interval.isNegative()
        || interval.isZero()
        || maxPerStage < 1) throw new IllegalArgumentException("Invalid redrive policy");
    this.staleAfter = staleAfter;
    this.interval = interval;
    this.maxPerStage = maxPerStage;
  }

  public RedriveDecision decide(
      RedriveState guard,
      String state,
      long version,
      Instant lastProgressAt,
      DeliveryEvidence e,
      Instant now) {
    if (e == null || lastProgressAt == null || now == null) return RedriveDecision.UNKNOWN;
    if (e.conflicting()) return RedriveDecision.CONFLICT;
    if (e.uncertain()) return RedriveDecision.UNKNOWN;
    if (e.blocked()) return RedriveDecision.PAUSED;
    if (now.isBefore(lastProgressAt.plus(staleAfter))) return RedriveDecision.FRESH;
    if (e.localInFlight()) return RedriveDecision.LOCAL_IN_FLIGHT;
    if (e.latestPublishedAt() != null && now.isBefore(e.latestPublishedAt().plus(staleAfter)))
      return RedriveDecision.OBSERVATION_WINDOW;
    if (e.remoteRetryUnfinished()) return RedriveDecision.REMOTE_RETRY;
    if (guard == null) return RedriveDecision.UNKNOWN;
    boolean progressed =
        !Objects.equals(guard.observedState(), state)
            && guard.lastRedriveAt() != null
            && lastProgressAt.isAfter(guard.lastRedriveAt());
    if (guard.count() >= maxPerStage && !progressed) return RedriveDecision.EXHAUSTED;
    Instant next = guard.nextRedriveAt();
    if (guard.lastRedriveAt() != null
        && (next == null || next.isBefore(guard.lastRedriveAt().plus(interval))))
      next = guard.lastRedriveAt().plus(interval);
    if (next != null && now.isBefore(next)) return RedriveDecision.COOLDOWN;
    return RedriveDecision.EMIT;
  }
}
