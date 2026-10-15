package com.jay.domain.strategy.model;
import java.time.Instant;import java.math.BigDecimal;import java.util.*;
public record PublishStrategyCommand(long activityId,long strategyId,String state,Instant beginsAt,Instant endsAt,List<Prize> prizes,Prize fallback,Map<Long,List<Integer>> weightedPools,Integer blacklistAwardId,String blacklistVersion) {
 public record Prize(int awardId,String type,BigDecimal minAmount,BigDecimal maxAmount,String stockPoolId,BigDecimal rate,long unlockAfter) {}
}
