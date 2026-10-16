package com.jay.domain.payment.model;

public record PaymentFact(
    String provider,
    String account,
    String environment,
    String transactionId,
    String orderId,
    String userId,
    java.math.BigDecimal amount,
    String currency,
    boolean paid,
    boolean reversed) {
  public boolean matches(PaymentRecord r) {
    return provider.equals(r.provider())
        && account.equals(r.providerAccount())
        && environment.equals(r.environment())
        && orderId.equals(r.orderId())
        && userId.equals(r.userId())
        && amount.compareTo(r.product().amount()) == 0
        && currency.equals(r.product().currency());
  }

  public VerifiedPaymentEvent event() {
    return new VerifiedPaymentEvent(
        provider,
        account,
        environment,
        "stored-"
            + new com.jay.domain.asset.service.RequestHasher()
                .hash(
                    "STORED_PAYMENT",
                    java.util.Map.of(
                        "provider",
                        provider,
                        "account",
                        account,
                        "environment",
                        environment,
                        "transaction",
                        transactionId)),
        transactionId,
        orderId,
        reversed ? "REVERSED" : "PAID",
        amount,
        currency,
        userId,
        null);
  }
}
