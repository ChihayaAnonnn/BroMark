package com.jay.domain.stock.model;

public record ConfirmCommand(
    String drawOrderId, String userId, String identityHash, String requestHash, String finalHash) {}
