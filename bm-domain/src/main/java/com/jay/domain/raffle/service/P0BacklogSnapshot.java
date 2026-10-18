package com.jay.domain.raffle.service;

/** 入口背压快照；pending 为 PENDING_RESERVATION 数量，oldestAgeSeconds 为最老进展年龄。 */
public record P0BacklogSnapshot(long pending, long oldestAgeSeconds) {
  public P0BacklogSnapshot {
    if (pending < 0 || oldestAgeSeconds < 0) throw new IllegalArgumentException("Negative backlog");
  }
}
