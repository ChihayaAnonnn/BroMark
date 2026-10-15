package com.jay.domain.task.model;

public record RecoveryAggregate(
    String id, String state, RecoveryStatus recoveryStatus, long version) {}
