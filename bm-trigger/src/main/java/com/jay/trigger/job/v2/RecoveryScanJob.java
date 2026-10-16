package com.jay.trigger.job.v2;

import com.jay.domain.task.service.RecoveryService;
import org.slf4j.LoggerFactory;

public final class RecoveryScanJob {
  private final RecoveryService recovery;
  private volatile boolean running = true;

  public RecoveryScanJob(RecoveryService recovery) {
    this.recovery = recovery;
  }

  private CoalescingJobDispatcher dispatcher;
  private Runnable prefetch = () -> {};

  public RecoveryScanJob prefetchWith(Runnable prefetch) {
    this.prefetch = prefetch;
    return this;
  }

  public RecoveryScanJob dispatchWith(CoalescingJobDispatcher dispatcher) {
    this.dispatcher = dispatcher;
    return this;
  }

  public void tick() {
    if (dispatcher == null) runWork();
    else dispatcher.submit("recovery-scan", this::runWork);
  }

  private void runWork() {
    if (!running) return;
    prefetch.run();
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

  public void stop() {
    running = false;
  }
}
