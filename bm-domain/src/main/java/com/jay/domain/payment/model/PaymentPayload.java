package com.jay.domain.payment.model;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import java.math.*;

public record PaymentPayload(
    String paymentOrderId,
    String userId,
    RouteSlot routeSlot,
    BigDecimal amount,
    String requestHash,
    long paymentVersion,
    AssetReceipt receipt) {
  public PaymentPayload {
    RequestHasher.amount(amount);
    if (amount.signum() <= 0 || paymentVersion != 1)
      throw new IllegalArgumentException("Invalid payment benefit");
  }

  public static String hash(String id, String user, RouteSlot slot, BigDecimal amount) {
    return new RequestHasher()
        .hash(
            "PAYMENT_BENEFIT",
            java.util.Map.of(
                "order",
                id,
                "user",
                user,
                "slot",
                slot.toString(),
                "credit",
                RequestHasher.amount(amount).toPlainString()));
  }

  public static String json(Object x) {
    try {
      return mapper().writeValueAsString(x);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static PaymentPayload read(String s) {
    try {
      return mapper().readValue(s, PaymentPayload.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid payment payload", e);
    }
  }

  private static com.fasterxml.jackson.databind.ObjectMapper mapper() {
    return new com.fasterxml.jackson.databind.ObjectMapper();
  }
}
