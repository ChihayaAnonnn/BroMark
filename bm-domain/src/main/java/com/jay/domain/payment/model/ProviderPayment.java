package com.jay.domain.payment.model;

public record ProviderPayment(String merchantOrderId, String status, VerifiedPaymentEvent fact) {}
