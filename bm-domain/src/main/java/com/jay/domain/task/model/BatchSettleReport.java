package com.jay.domain.task.model;

import java.time.Instant;
import java.util.Set;

public record BatchSettleReport(
    Set<String> completed,
    Set<String> retried,
    Set<String> dead,
    Set<String> lost,
    Instant databaseNow) {
  public BatchSettleReport {
    completed = Set.copyOf(completed);
    retried = Set.copyOf(retried);
    dead = Set.copyOf(dead);
    lost = Set.copyOf(lost);
  }
}
