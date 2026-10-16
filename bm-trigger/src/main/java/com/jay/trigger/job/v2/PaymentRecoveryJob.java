package com.jay.trigger.job.v2;

import com.jay.domain.asset.service.*;
import com.jay.domain.payment.service.*;
import org.springframework.scheduling.annotation.Scheduled;

public final class PaymentRecoveryJob {
  private final PaymentRecoveryService service;
  private final OrderIdGenerator ids;

  public PaymentRecoveryJob(PaymentRecoveryService s, OrderIdGenerator ids) {
    service = s;
    this.ids = ids;
  }

  @Scheduled(fixedDelayString = "${bm.v2.payment.recovery-delay-ms:10000}")
  public void tick() {
    ids.prefetch();
    service.tick();
  }
}
