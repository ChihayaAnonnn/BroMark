package com.jay.domain.payment.model;

public record ProviderInboxEntry(
    String provider,
    String account,
    String environment,
    String eventId,
    String canonicalHash,
    String payload) {
  public VerifiedPaymentEvent event() {
    try {
      var e =
          new com.fasterxml.jackson.databind.ObjectMapper()
              .readValue(payload, VerifiedPaymentEvent.class);
      if (!provider.equals(e.provider())
          || !account.equals(e.providerAccount())
          || !environment.equals(e.environment())
          || !eventId.equals(e.eventId())
          || !canonicalHash.equals(e.canonicalHash()))
        throw new IllegalArgumentException("Inbox identity mismatch");
      return e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Corrupt verified inbox", e);
    }
  }
}
