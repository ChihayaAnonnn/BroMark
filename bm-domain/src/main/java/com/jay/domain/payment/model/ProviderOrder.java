package com.jay.domain.payment.model;

public record ProviderOrder(
    String merchantOrderId, String status, java.util.Map<String, String> parameters) {
  public ProviderOrder {
    parameters = java.util.Map.copyOf(parameters);
  }
}
