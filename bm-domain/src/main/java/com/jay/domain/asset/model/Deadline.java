package com.jay.domain.asset.model;

import java.time.*;

public record Deadline(Instant expiresAt) {
  public Deadline {
    java.util.Objects.requireNonNull(expiresAt);
  }

  public Duration remaining() {
    return Duration.between(Instant.now(), expiresAt);
  }
}
