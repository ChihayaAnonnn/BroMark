package com.jay.domain.asset.model;

public record AssetReceipt(
    BusinessId bizId, java.math.BigDecimal delta, java.math.BigDecimal balanceAfter) {}
