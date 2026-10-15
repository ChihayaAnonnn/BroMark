package com.jay.domain.raffle.model;
public record DrawView(String orderId,String state,com.jay.domain.task.model.RecoveryStatus recoveryStatus,com.jay.domain.strategy.model.AwardSnapshot finalAward) {}
