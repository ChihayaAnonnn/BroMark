package com.jay.domain.payment.model;

import com.jay.domain.task.model.RecoveryStatus;

public record PaymentView(
    String orderId,
    String state,
    String providerCreateStatus,
    com.jay.domain.task.model.RecoveryStatus recoveryStatus,
    java.util.Map<String, String> parameters,
    com.jay.domain.asset.model.AssetReceipt receipt) {
  public PaymentView {
    parameters = java.util.Map.copyOf(parameters);
  }
}
