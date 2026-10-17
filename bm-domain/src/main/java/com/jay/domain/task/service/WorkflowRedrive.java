package com.jay.domain.task.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** 唯一owner提交协议；调用commit前必须已按账户→订单持有本地业务锁。 */
public final class WorkflowRedrive {
  private final UserTransactionRunner tx;
  private final RecoveryDispatchRepository dispatch;
  private final DeliveryEvidenceRepository evidence;
  private final RecoveryService recovery;
  private final OutboxWriter out;
  private final Supplier<Instant> clock;
  private final RecoveryRedriveGuard guard;
  private final Duration stale, interval;
  private RelayObservation observation = RelayObservation.NOOP;

  public WorkflowRedrive(
      UserTransactionRunner tx,
      RecoveryDispatchRepository dispatch,
      DeliveryEvidenceRepository evidence,
      RecoveryService recovery,
      OutboxWriter out,
      Supplier<Instant> clock,
      Duration stale,
      Duration interval,
      int max) {
    this.tx = tx;
    this.dispatch = dispatch;
    this.evidence = evidence;
    this.recovery = recovery;
    this.out = out;
    this.clock = clock;
    this.stale = stale;
    this.interval = interval;
    guard = new RecoveryRedriveGuard(stale, interval, max);
  }

  /** 仅在装配期设置；原构造器保持测试和 legacy 调用兼容。 */
  public WorkflowRedrive withObservation(RelayObservation observation) {
    this.observation = Objects.requireNonNull(observation);
    return this;
  }

  public void conflict(String type, String id, String reason) {
    recovery.pauseInCurrentTransaction(type, id, RecoveryStatus.REVIEW, reason);
  }

  public Instant now() {
    return clock.get();
  }

  public Instant cutoff() {
    return now().minus(stale);
  }

  public DeliveryEvidence observe(EventEnvelope event) {
    try {
      return tx.inSlot(
          event.routeSlot(),
          WorkClass.RECOVERY,
          new Deadline(Instant.now().plusSeconds(5)),
          () -> evidence.target(event));
    } catch (RuntimeException unavailable) {
      return new DeliveryEvidence(false, null, false, false, false, true, false);
    }
  }

  public RecoveryDecision commit(
      String type,
      String id,
      String state,
      long version,
      Instant progress,
      EventEnvelope event,
      DeliveryEvidence remote) {
    var saved = dispatch.lock(type, id);
    // INSERT初始next_redrive_at是DB_NOW，必须在锁后重新采样。
    Instant now = clock.get();
    var local = evidence.local(type, id, event);
    var all =
        new DeliveryEvidence(
            local.localInFlight(),
            local.latestPublishedAt(),
            remote.remoteRetryUnfinished(),
            remote.operationCompleted(),
            local.blocked() || remote.blocked(),
            local.uncertain() || remote.uncertain(),
            local.conflicting() || remote.conflicting());
    var decision = guard.decide(saved, state, version, progress, all, now);
    var slot = event.routeSlot();
    var eventType = event.eventType();
    observation.count("checked", slot, eventType, "success", 1);
    if (decision == RedriveDecision.EXHAUSTED) {
      var frozen =
          evidence
              .localEvent(saved.lastEventId())
              .orElseThrow(() -> new IllegalStateException("Missing persisted redrive event"));
      recovery.pauseRedrive(
          type, id, frozen, "Automatic redrive stage exhausted: " + saved.lastEventId());
    } else if (decision == RedriveDecision.CONFLICT)
      recovery.pauseInCurrentTransaction(
          type, id, RecoveryStatus.REVIEW, "Recovery evidence digest conflict");
    if (decision != RedriveDecision.EMIT) {
      if (decision == RedriveDecision.EXHAUSTED)
        observation.count("exhausted", slot, eventType, "review", 1);
      else observation.count("suppressed", slot, eventType, suppressionReason(decision), 1);
      return new RecoveryDecision(List.of());
    }
    out.append(event);
    dispatch.record(type, id, state, version, event, now, interval, progress);
    observation.count("redriven", slot, eventType, "success", 1);
    return new RecoveryDecision(List.of(event));
  }

  private static String suppressionReason(RedriveDecision decision) {
    return switch (decision) {
      case FRESH -> "no_progress";
      case LOCAL_IN_FLIGHT, OBSERVATION_WINDOW -> "active_task";
      case REMOTE_RETRY -> "remote_retry";
      case COOLDOWN -> "cooldown";
      case PAUSED -> "paused";
      case UNKNOWN -> "uncertain_fact";
      case CONFLICT -> "review";
      default -> throw new IllegalArgumentException("Not suppressed: " + decision);
    };
  }
}
