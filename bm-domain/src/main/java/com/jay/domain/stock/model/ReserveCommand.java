package com.jay.domain.stock.model;
public record ReserveCommand(String drawOrderId,String userId,String identityHash,String requestHash,long strategyId,String version,com.jay.domain.strategy.model.AwardSnapshot candidate,com.jay.domain.strategy.model.AwardSnapshot fallback) {}
