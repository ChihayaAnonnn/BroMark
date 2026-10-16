package com.jay.domain.task.model;

import com.jay.domain.asset.model.RouteSlot;
import java.time.Instant;

public record ClaimedPublish(
    RouteSlot ownerSlot,
    String eventId,
    String aggregateId,
    long aggregateVersion,
    String eventType,
    String rawEnvelope,
    String payloadHash,
    String leaseToken,
    Instant leaseUntil,
    int attempt,
    int failureCount,
    String consumerName,
    Instant claimedAt) {}
