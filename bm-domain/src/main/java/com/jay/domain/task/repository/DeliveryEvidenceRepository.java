package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;

/** local/target 分别要求调用者先建立所属库/目标库的独立短事务。 */
public interface DeliveryEvidenceRepository {
  DeliveryEvidence local(
      String aggregateType, String aggregateId, String eventType, String requestHash);

  DeliveryEvidence target(EventEnvelope command);

  boolean operationCompleted(String consumer, EventEnvelope event);
}
