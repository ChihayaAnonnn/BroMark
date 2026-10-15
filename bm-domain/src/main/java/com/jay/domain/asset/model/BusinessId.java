package com.jay.domain.asset.model;

public record BusinessId(String type, String id, String hash) {
  public BusinessId {
    if (type == null
        || !type.matches("[A-Z_]{1,32}")
        || id == null
        || id.isBlank()
        || id.length() > 128
        || hash == null
        || !hash.matches("[a-f0-9]{64}"))
      throw new IllegalArgumentException("Invalid business identity");
  }
}
