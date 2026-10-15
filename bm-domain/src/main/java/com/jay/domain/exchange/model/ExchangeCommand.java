package com.jay.domain.exchange.model;

import com.jay.domain.asset.model.Deadline;

public record ExchangeCommand(
    String userId, String requestId, long sku, String configVersion, Deadline deadline) {}
