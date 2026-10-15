package com.jay.domain.asset.model;
public record QuotaDelta(long totalDelta,long dayLimitDelta,long monthLimitDelta){public QuotaDelta{if(totalDelta<0||dayLimitDelta<0||monthLimitDelta<0)throw new IllegalArgumentException("Nonnegative quota required");}}

