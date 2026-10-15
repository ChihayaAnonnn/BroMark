package com.jay.domain.exchange.model;
import com.jay.domain.asset.model.*;
public record ExchangeResult(String orderId,String state,AssetReceipt creditReceipt,QuotaReceipt quotaReceipt,String reason) {}

