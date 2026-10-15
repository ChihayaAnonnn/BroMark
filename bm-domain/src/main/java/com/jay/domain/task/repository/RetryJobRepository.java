package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;
import java.time.Instant;

public interface RetryJobRepository extends OutboxRepository {
  void schedule(EventEnvelope event, int attempt, Instant next);

  int failureCount(OutboxLease lease);

  int failures(EventEnvelope event);

  boolean mayApply(String consumer, EventEnvelope event, int generation);

  void schedule(String consumer, EventEnvelope event, int attempt, Instant next);

  void dead(String consumer, EventEnvelope event, int attempt);
}
