package com.jay.domain.task.repository;
import com.jay.domain.task.model.*;
import java.time.Instant;
public interface RetryJobRepository extends OutboxRepository {
  void schedule(EventEnvelope event,int attempt,Instant next);
  int failureCount(OutboxLease lease);
}
