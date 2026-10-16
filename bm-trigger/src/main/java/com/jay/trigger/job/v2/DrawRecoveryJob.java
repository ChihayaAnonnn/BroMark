package com.jay.trigger.job.v2;

import com.jay.domain.asset.service.OrderIdGenerator;
import com.jay.domain.raffle.service.DrawRecoveryService;
import org.springframework.scheduling.annotation.Scheduled;

public final class DrawRecoveryJob {
  private final DrawRecoveryService recovery;
  private final OrderIdGenerator ids;

  public DrawRecoveryJob(DrawRecoveryService r, OrderIdGenerator i) {
    recovery = r;
    ids = i;
  }

  private CoalescingJobDispatcher dispatcher;

  public DrawRecoveryJob dispatchWith(CoalescingJobDispatcher dispatcher) {
    this.dispatcher = dispatcher;
    return this;
  }

  @Scheduled(fixedDelayString = "${bm.v2.raffle.recovery-delay-ms:10000}")
  public void tick() {
    if (dispatcher == null) runWork();
    else dispatcher.submit("draw", this::runWork);
  }

  private void runWork() {
    ids.prefetch();
    recovery.tick();
  }
}
