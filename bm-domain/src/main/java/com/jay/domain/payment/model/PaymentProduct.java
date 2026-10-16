package com.jay.domain.payment.model;

public record PaymentProduct(
    String productId, java.math.BigDecimal amount, String currency, java.math.BigDecimal credit) {
  public PaymentProduct {
    com.jay.domain.asset.service.RequestHasher.amount(amount);
    com.jay.domain.asset.service.RequestHasher.amount(credit);
    if (amount.signum() <= 0 || credit.signum() <= 0 || !currency.matches("[A-Z]{3}"))
      throw new IllegalArgumentException("Invalid product");
  }
}
