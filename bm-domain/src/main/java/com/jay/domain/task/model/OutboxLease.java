package com.jay.domain.task.model;

import com.jay.domain.asset.model.RouteSlot;
import java.time.Instant;

public record OutboxLease(
    RouteSlot ownerSlot, EventEnvelope event, String leaseToken, Instant leaseUntil, int attempt) {}
