package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;

/** local/target 分别要求调用者先建立所属库/目标库的独立短事务。 */
public interface DeliveryEvidenceRepository {
  DeliveryEvidence local(
      String aggregateType, String aggregateId, String eventType, String requestHash);

  /** guarded调用携带当前冻结命令；旧fake继续委托旧签名。 */
  default DeliveryEvidence local(String aggregateType, String aggregateId, EventEnvelope command) {
    return local(aggregateType, aggregateId, command.eventType(), command.requestHash());
  }

  DeliveryEvidence target(EventEnvelope command);

  default java.util.Optional<EventEnvelope> localEvent(String eventId) {
    return java.util.Optional.empty();
  }

  boolean operationCompleted(String consumer, EventEnvelope event);

  /** 只读目标业务暂停证据；调用方已在目标库事务内。 */
  default boolean targetPaused(EventEnvelope event) {
    return false;
  }
}
