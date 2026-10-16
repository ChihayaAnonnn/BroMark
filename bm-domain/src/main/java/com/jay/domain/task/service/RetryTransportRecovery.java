package com.jay.domain.task.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;

/** 已发布 Retry 的原身份运输修复；每个目标库都只在本库短事务中更新。 */
public final class RetryTransportRecovery {
  private static final class StaleReview extends RuntimeException {}
  private final UserTransactionRunner tx;
  private final RetryJobRepository retries;
  private final DeliveryEvidenceRepository evidence;
  private final RecoveryService recovery;
  private final WorkflowRecoveryScan scan = new WorkflowRecoveryScan();
  private final List<WorkflowRecoveryScan.Partition> partitions =
      List.of(
          new WorkflowRecoveryScan.Partition(RouteSlot.control(), "PUBLISHED"),
          new WorkflowRecoveryScan.Partition(new RouteSlot("01", "000"), "PUBLISHED"),
          new WorkflowRecoveryScan.Partition(new RouteSlot("02", "000"), "PUBLISHED"));

  public RetryTransportRecovery(
      UserTransactionRunner tx,
      RetryJobRepository retries,
      DeliveryEvidenceRepository evidence,
      RecoveryService recovery) {
    this.tx = Objects.requireNonNull(tx);
    this.retries = Objects.requireNonNull(retries);
    this.evidence = Objects.requireNonNull(evidence);
    this.recovery = Objects.requireNonNull(recovery);
  }

  public void budget(int candidates, int repairs, Duration timeout) {
    scan.budget(candidates, repairs, timeout);
  }

  private Deadline deadline() {
    return new Deadline(Instant.now().plusSeconds(5));
  }

  /** true 仅代表原任务 PUBLISHED→PENDING；不代表消费者完成。 */
  public boolean inspect(RouteSlot target, String eventId) {
    return inspectOutcome(target, eventId) == 1;
  }

  private int inspectOutcome(RouteSlot target, String eventId) {
    PublishedRetrySnapshot observed;
    try {
      observed =
          tx.inSlot(
              target,
              WorkClass.RECOVERY,
              deadline(),
              () -> retries.findPublished(eventId).orElse(null));
    } catch (AssetConflictException invalid) {
      try {
        return tx.inSlot(target, WorkClass.RECOVERY, deadline(), () -> quarantine(eventId));
      } catch (StaleReview changed) {
        return 0;
      }
    }
    if (observed == null || !target.dbKey().equals(observed.event().routeSlot().dbKey()))
      return 0;
    EventEnvelope event = observed.event();
    try {
      return tx.inSlot(
          event.routeSlot(),
          WorkClass.RECOVERY,
          deadline(),
          () -> {
          boolean terminal = observed.attempt() == 12;
          if (terminal) {
            if (!recovery.transportPresentForTerminal(event)) return 0;
          } else if (evidence.targetPaused(event) || !recovery.transportAllowed(event)) return 0;
          PublishedRetrySnapshot current;
          try {
            current = retries.findPublished(eventId).orElse(null);
          } catch (AssetConflictException invalid) {
            return quarantine(eventId);
          }
          if (current == null
              || current.attempt() != observed.attempt()
              || current.failureCount() != observed.failureCount()
              || !current.publishedAt().equals(observed.publishedAt())
              || !current.consumerName().equals(observed.consumerName())
              || !current.event().equals(observed.event())) return 0;
          if (retries.databaseNow().isBefore(current.publishedAt().plusSeconds(30))) return 0;
          try {
            if (evidence.operationCompleted(current.consumerName(), current.event())) return 0;
          } catch (AssetConflictException conflict) {
            recovery.pauseTransportRetry(
                current.consumerName(),
                event,
                RecoveryStatus.REVIEW,
                "Published Retry receipt digest conflict: " + event.eventId());
            return 2;
          }
          if (current.attempt() == 12) {
            recovery.pauseTransportRetry(
                current.consumerName(),
                event,
                RecoveryStatus.RETRY_EXHAUSTED,
                "Published Retry exhausted without receipt: " + event.eventId());
            if (!retries.deadPublished(
                eventId, current.attempt(), current.failureCount(), current.publishedAt()))
              throw new OptimisticLockingFailureException("Published Retry generation lost");
            return 2;
          }
          return retries.rearmPublished(
                  eventId, current.attempt(), current.failureCount(), current.publishedAt())
              ? 1
              : 0;
          });
    } catch (StaleReview changed) {
      return 0;
    }
  }

  private int quarantine(String eventId) {
    recovery.pauseInCurrentTransaction(
        "RETRY_TASK", eventId, RecoveryStatus.WAITING_FIX, "Invalid published Retry identity");
    try {
      retries.findPublishedForReview(eventId);
      throw new StaleReview();
    } catch (AssetConflictException stillInvalid) {
      // 只有持有原任务锁后仍是坏证据，才把该代任务转入持久核查。
    }
    if (!retries.quarantinePublished(eventId))
      throw new OptimisticLockingFailureException("Published Retry changed during review");
    return 3;
  }

  public void tick() {
    var owners = new HashMap<String, RouteSlot>();
    scan.run(
        partitions,
        (partition, cursor) ->
            tx.inSlot(
                partition.slot(),
                WorkClass.RECOVERY,
                deadline(),
                () -> {
                  var page =
                      retries.scanPublished(
                          cursor, retries.databaseNow().minusSeconds(30), 8);
                  page.ids().forEach(id -> owners.put(id, partition.slot()));
                  return page;
                }),
        id -> inspectOutcome(owners.get(id), id) == 0 ? 0 : 1);
  }
}
