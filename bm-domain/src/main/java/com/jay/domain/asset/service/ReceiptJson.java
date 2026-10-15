package com.jay.domain.asset.service;

public final class ReceiptJson {
  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
      new com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(
              new com.fasterxml.jackson.databind.module.SimpleModule()
                  .addSerializer(
                      java.math.BigDecimal.class,
                      com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance))
          .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private ReceiptJson() {}

  public static String write(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid receipt", e);
    }
  }
}
