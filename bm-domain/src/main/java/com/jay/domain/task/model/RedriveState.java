package com.jay.domain.task.model;

import java.time.Instant;

public record RedriveState(
    String type,
    String id,
    String observedState,
    long observedVersion,
    String lastEventType,
    String lastEventId,
    Instant lastRedriveAt,
    Instant nextRedriveAt,
    int count) {}
