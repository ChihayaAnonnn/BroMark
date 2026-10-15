package com.jay.trigger.job.v2;

import com.jay.domain.task.service.RecoveryService;
import org.slf4j.LoggerFactory;

public final class RecoveryScanJob {
  private final RecoveryService recovery;
  private volatile boolean running = true;

  public RecoveryScanJob(RecoveryService recovery) {
    this.recovery = recovery;
  }

  public synchronized void tick() {
    if (!running) return;
    for (var slot : OutboxRelayJob.SOURCES) {
      if (!running) break;
      try {
        recovery.scan(slot, 8);
      } catch (RuntimeException e) {
        LoggerFactory.getLogger(getClass())
            .warn("Recovery scan unavailable in {}", slot.dbKey(), e);
      }
    }
  }

  public synchronized void stop() {
    running = false;
  }
}
