package com.jay.domain.asset.model;

public final class LegacyWriteBlockedException extends RuntimeException {
  public LegacyWriteBlockedException() {
    super("Asset entry is fenced; migration identity required");
  }
}
