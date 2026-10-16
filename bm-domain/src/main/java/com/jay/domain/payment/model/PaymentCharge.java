package com.jay.domain.payment.model;

public record PaymentCharge(String userId, com.jay.domain.asset.model.AssetReceipt receipt) {}
