package com.jay.domain.task.model;

import java.time.Instant;

public record DeliveryEvidence(
    boolean localInFlight,
    Instant latestPublishedAt,
    boolean remoteRetryUnfinished,
    boolean operationCompleted,
    boolean blocked,
    boolean uncertain,
    boolean conflicting) {}
