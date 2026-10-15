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

  @Scheduled(fixedDelayString = "${bm.v2.raffle.recovery-delay-ms:10000}")
  public void tick() {
    ids.prefetch();
    recovery.tick();
  }
}
