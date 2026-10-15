package com.jay.domain.task.model;

import com.jay.domain.asset.model.*;

public record EventEnvelope(
    String eventId,
    String eventType,
    String aggregateId,
    BusinessId bizId,
    String userId,
    RouteSlot routeSlot,
    String requestHash,
    long aggregateVersion,
    int schemaVersion,
    String payload) {
  public EventEnvelope {
    if (eventId == null
        || eventId.isBlank()
        || eventId.length() > 128
        || eventType == null
        || eventType.isBlank()
        || aggregateId == null
        || bizId == null
        || routeSlot == null
        || requestHash == null
        || !requestHash.matches("[a-f0-9]{64}")
        || schemaVersion != 1
        || payload == null) throw new IllegalArgumentException("Invalid event");
  }
}
