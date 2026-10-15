package com.jay.domain.stock.model;
public record ReservationResult(String drawOrderId,String userId,String identityHash,String requestHash,String state,com.jay.domain.strategy.model.AwardSnapshot finalSnapshot,long quantity) {}
