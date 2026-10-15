package com.jay.infrastructure.dao.v2.po;

public record QuotaAccountRow(
    String userId,
    long activityId,
    long totalRemaining,
    long dayLimit,
    long monthLimit,
    long effectiveDrawCount,
    long version) {}
