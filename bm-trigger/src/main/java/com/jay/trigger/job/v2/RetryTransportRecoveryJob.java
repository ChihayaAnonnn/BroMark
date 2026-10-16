package com.jay.trigger.job.v2;

import com.jay.domain.task.service.RetryTransportRecovery;

/** 与其他恢复任务共用现有执行器和合并调度器。 */
public final class RetryTransportRecoveryJob {
  private final RetryTransportRecovery recovery;
  private final CoalescingJobDispatcher dispatcher;
  private volatile boolean running = true;

  public RetryTransportRecoveryJob(
      RetryTransportRecovery recovery, CoalescingJobDispatcher dispatcher) {
    this.recovery = recovery;
    this.dispatcher = dispatcher;
  }

  public void tick() {
    if (running) dispatcher.submit("retry-transport-recovery", () -> {
      if (running) recovery.tick();
    });
  }

  public void stop() {
    running = false;
  }
}
