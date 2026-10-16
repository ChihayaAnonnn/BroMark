package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;
import java.time.Instant;
import java.util.Optional;

public interface RetryJobRepository extends OutboxRepository {
  void schedule(EventEnvelope event, int attempt, Instant next);

  int failureCount(OutboxLease lease);

  int failures(EventEnvelope event);

  boolean mayApply(String consumer, EventEnvelope event, int generation);

  void schedule(String consumer, EventEnvelope event, int attempt, Instant next);

  void dead(String consumer, EventEnvelope event, int attempt);

  Instant databaseNow();

  RecoveryScanPage scanPublished(RecoveryScanPosition cursor, Instant cutoff, int limit);

  Optional<PublishedRetrySnapshot> findPublished(String eventId);

  /** 核查事务已先持有任务审计guard，随后锁原任务并重新校验。 */
  Optional<PublishedRetrySnapshot> findPublishedForReview(String eventId);

  boolean rearmPublished(
      String eventId, int expectedAttempt, int expectedFailureCount, Instant expectedPublishedAt);

  boolean deadPublished(
      String eventId, int expectedAttempt, int expectedFailureCount, Instant expectedPublishedAt);

  boolean quarantinePublished(String eventId);
}
