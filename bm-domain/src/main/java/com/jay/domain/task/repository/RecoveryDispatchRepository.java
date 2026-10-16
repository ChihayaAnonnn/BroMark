package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;
import java.time.*;

/** 所有方法要求调用者持有所有者库事务；now 来自该库 UTC_TIMESTAMP(6)。 */
public interface RecoveryDispatchRepository {
  RedriveState lock(String type, String id);

  void record(
      String type,
      String id,
      String state,
      long version,
      EventEnvelope event,
      Instant now,
      Duration interval);

  /** 仅阶段变化且进展时间晚于前次实际补发可重置计数。 */
  default void record(
      String type,
      String id,
      String state,
      long version,
      EventEnvelope event,
      Instant now,
      Duration interval,
      Instant lastProgressAt) {
    record(type, id, state, version, event, now, interval);
  }
}
