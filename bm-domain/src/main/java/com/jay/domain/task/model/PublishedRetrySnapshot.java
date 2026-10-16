package com.jay.domain.task.model;

import java.time.Instant;

/** 原 Retry 运输身份与发布代际；不把 Rabbit Confirm 当成业务回执。 */
public record PublishedRetrySnapshot(
    EventEnvelope event, String consumerName, int attempt, int failureCount, Instant publishedAt) {}
