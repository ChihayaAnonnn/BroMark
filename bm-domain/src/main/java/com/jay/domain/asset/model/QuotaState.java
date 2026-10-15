package com.jay.domain.asset.model;
public record QuotaState(long total,long dayLimit,long monthLimit,long effective,long dayRemaining,long monthRemaining,long dayEffective) {}

