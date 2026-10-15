package com.jay.domain.asset.service;

import com.jay.domain.asset.model.LegacyWriteBlockedException;

public final class AssetWriteGate {
  private final boolean enabled;
  private final String mode;

  public AssetWriteGate(boolean enabled, String mode) {
    if (!"LEGACY".equals(mode) && !"V2".equals(mode) || "V2".equals(mode) && !enabled)
      throw new IllegalArgumentException("Invalid asset write mode");
    this.enabled = enabled;
    this.mode = mode;
  }

  public boolean legacyAllowed() {
    return "LEGACY".equals(mode);
  }

  public void check(String entry) {
    if (entry.startsWith("v2")) {
      if (!enabled || legacyAllowed()) throw new LegacyWriteBlockedException();
    } else if (!legacyAllowed()) throw new LegacyWriteBlockedException();
  }
}
