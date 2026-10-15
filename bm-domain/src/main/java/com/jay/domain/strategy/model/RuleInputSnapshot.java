package com.jay.domain.strategy.model;
public record RuleInputSnapshot(java.time.Instant businessTime,long totalEffectiveCount,long dayEffectiveCount,boolean blacklisted,String blacklistVersion) {}
