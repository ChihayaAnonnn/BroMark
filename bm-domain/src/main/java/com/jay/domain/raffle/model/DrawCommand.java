package com.jay.domain.raffle.model;

public record DrawCommand(
    String userId,
    String requestId,
    long activityId,
    com.jay.domain.asset.model.Deadline deadline) {}
