package com.jay.domain.payment.model;

public record PaymentCommand(
    String userId,
    String requestId,
    String productId,
    String provider,
    com.jay.domain.asset.model.Deadline deadline) {}
