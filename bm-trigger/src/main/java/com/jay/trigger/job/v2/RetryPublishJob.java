package com.jay.trigger.job.v2;

import com.jay.domain.task.service.OutboxRelay;

public final class RetryPublishJob {
  private final OutboxRelayJob delegate;

  public RetryPublishJob(OutboxRelay relay) {
    delegate = new OutboxRelayJob(relay);
  }

  public void tick() {
    delegate.tick();
  }

  public void stop() {
    delegate.stop();
  }
}
