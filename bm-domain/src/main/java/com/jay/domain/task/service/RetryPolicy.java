package com.jay.domain.task.service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public final class RetryPolicy {
  public static final int MAX_ATTEMPTS = 12;

  public Instant next(int attempt, Instant now) {
    if (attempt < 1) throw new IllegalArgumentException("Positive attempt required");
    long millis = Math.min(300000L, 1000L << Math.min(attempt - 1, 9));
    // Downward jitter keeps the total interval at or below five minutes.
    return now.plusMillis(
        ThreadLocalRandom.current().nextLong(Math.max(1, millis * 4 / 5), millis + 1));
  }
}
