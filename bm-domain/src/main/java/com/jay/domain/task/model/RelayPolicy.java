package com.jay.domain.task.model;

import java.time.Duration;

/** 不可变的单通道领取、发布及租约预算。 */
public record RelayPolicy(
    int claimLimit,
    int publishWindow,
    int expiredReserve,
    int fairnessSlots,
    Duration idlePoll,
    Duration lease,
    Duration claimTimeout,
    Duration publishTimeout,
    Duration settleTimeout,
    Duration batchTimeout,
    Duration safetyMargin) {
  public void validate(int lanes, int publisherLimit) {
    if (claimLimit < 1
        || claimLimit > 128
        || publishWindow < 1
        || lanes < 1
        || publisherLimit < 1
        || (long) lanes * publishWindow > publisherLimit
        || expiredReserve < 1
        || fairnessSlots < 1
        || (long) expiredReserve + fairnessSlots >= claimLimit)
      throw new IllegalArgumentException("Invalid relay resource budget");
    for (var d :
        new Duration[] {
          idlePoll, lease, claimTimeout, publishTimeout, settleTimeout, batchTimeout, safetyMargin
        })
      if (d == null || d.isZero() || d.isNegative())
        throw new IllegalArgumentException("Relay durations must be positive");
    long waves = (claimLimit + (long) publishWindow - 1) / publishWindow;
    Duration work = claimTimeout.plus(publishTimeout.plus(settleTimeout).multipliedBy(waves));
    if (work.compareTo(batchTimeout) > 0
        || work.plus(safetyMargin).compareTo(lease) >= 0
        || batchTimeout.plus(safetyMargin).compareTo(lease) >= 0)
      throw new IllegalArgumentException("Relay work cannot fit batch deadline and lease margin");
  }
}
