package com.jay.domain.asset.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class RequestHasher {
  public String hash(String operation, Map<String, String> fields) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var sorted = new TreeMap<>(fields);
      sorted.put("_operation", operation);
      sorted.put("_schema", "1");
      for (var e : sorted.entrySet())
        for (String value : List.of(e.getKey(), Objects.requireNonNull(e.getValue()))) {
          byte[] b = value.getBytes(StandardCharsets.UTF_8);
          digest.update(java.nio.ByteBuffer.allocate(4).putInt(b.length).array());
          digest.update(b);
        }
      return HexFormat.of().formatHex(digest.digest());
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static BigDecimal amount(BigDecimal value) {
    if (value == null
        || value.signum() <= 0
        || value.scale() > 2
        || value.compareTo(new BigDecimal("9999999999999999.99")) > 0)
      throw new IllegalArgumentException("Positive DECIMAL(18,2) required");
    return value.setScale(2);
  }
}
