package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;
import java.util.List;

public interface RecoveryAggregatePort {
  RecoveryAggregate lock(String id);

  boolean updateStatus(String id, long expectedVersion, RecoveryStatus status);

  default boolean terminal(RecoveryAggregate aggregate) {
    return false;
  }

  default List<String> scanActive(int limit) {
    return List.of();
  }

  default RecoveryDecision recoveryDecision(RecoveryAggregate aggregate) {
    return new RecoveryDecision(List.of());
  }
}
