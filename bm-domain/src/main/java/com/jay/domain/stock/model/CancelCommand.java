package com.jay.domain.stock.model;

public record CancelCommand(
    String drawOrderId, String userId, String identityHash, String requestHash) {}
