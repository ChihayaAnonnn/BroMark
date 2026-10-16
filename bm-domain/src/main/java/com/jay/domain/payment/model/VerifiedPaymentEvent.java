package com.jay.domain.payment.model;

import com.jay.domain.asset.service.RequestHasher;
import java.math.BigDecimal;
import java.util.*;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record VerifiedPaymentEvent(
    String provider,
    String providerAccount,
    String environment,
    String eventId,
    String transactionId,
    String merchantOrderId,
    String type,
    BigDecimal amount,
    String currency,
    String userBinding,
    String canonicalHash) {
  public VerifiedPaymentEvent {
    for (String s :
        List.of(
            provider,
            providerAccount,
            environment,
            eventId,
            transactionId,
            merchantOrderId,
            type,
            currency,
            userBinding))
      if (s.isBlank() || s.length() > 128)
        throw new IllegalArgumentException("Invalid payment identity");
    if (!Set.of("PAID", "REVERSED").contains(type))
      throw new IllegalArgumentException("Unknown event type");
    amount = RequestHasher.amount(amount);
    if (amount.signum() <= 0 || !currency.matches("[A-Z]{3}"))
      throw new IllegalArgumentException("Invalid amount/currency");
    String hash =
        new RequestHasher()
            .hash(
                "PROVIDER_FACT",
                Map.of(
                    "provider",
                    provider,
                    "account",
                    providerAccount,
                    "environment",
                    environment,
                    "transaction",
                    transactionId,
                    "merchant",
                    merchantOrderId,
                    "type",
                    type,
                    "amount",
                    amount.toPlainString(),
                    "currency",
                    currency,
                    "user",
                    userBinding));
    if (canonicalHash != null && !canonicalHash.equals(hash))
      throw new IllegalArgumentException("Canonical hash mismatch");
    canonicalHash = hash;
  }
}
