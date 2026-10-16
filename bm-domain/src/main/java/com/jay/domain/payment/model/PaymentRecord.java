package com.jay.domain.payment.model;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.RecoveryStatus;

public record PaymentRecord(
    String orderId,
    String userId,
    String requestId,
    String requestHash,
    PaymentProduct product,
    String provider,
    String providerAccount,
    String environment,
    String state,
    String providerCreateStatus,
    RecoveryStatus recoveryStatus,
    long version,
    java.util.Map<String, String> parameters,
    AssetReceipt receipt) {
  public PaymentRecord {
    parameters = java.util.Map.copyOf(parameters);
  }

  public PaymentView view() {
    return new PaymentView(
        orderId, state, providerCreateStatus, recoveryStatus, parameters, receipt);
  }

  public PaymentRecord change(
      String state,
      String create,
      RecoveryStatus recovery,
      java.util.Map<String, String> params,
      AssetReceipt receipt) {
    return new PaymentRecord(
        orderId,
        userId,
        requestId,
        requestHash,
        product,
        provider,
        providerAccount,
        environment,
        state,
        create,
        recovery,
        version + 1,
        params,
        receipt);
  }
}
