package com.jay.domain.asset.model;
public record QuotaReceipt(BusinessId bizId,long activityId,long totalRemaining,long dayRemaining,long monthRemaining,BusinessTime businessTime) {}

