package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;
import java.util.List;

public interface RecoveryAggregatePort {
  RecoveryAggregate lock(String id);

  /** 仅核查/暂停元数据；默认与业务锁相同，禁用账户等特例可覆盖。 */
  default RecoveryAggregate lockForPause(String id) {
    return lock(id);
  }

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
