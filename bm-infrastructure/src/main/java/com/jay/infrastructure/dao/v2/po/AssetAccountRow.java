package com.jay.infrastructure.dao.v2.po;

import java.math.BigDecimal;

public record AssetAccountRow(
    String userId, BigDecimal balance, BigDecimal openingBalance, String status, long version) {}
