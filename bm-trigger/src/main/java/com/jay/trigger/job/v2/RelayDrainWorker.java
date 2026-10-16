package com.jay.trigger.job.v2;

import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.task.model.RelayPolicy;
import com.jay.domain.task.service.BoundedOutboxRelay;
import java.time.Duration;
import java.util.function.ToIntFunction;
import org.slf4j.LoggerFactory;

/** 每通道单循环；只有三库整轮为空才等待，信号仅用于提前结束等待。 */
public final class RelayDrainWorker implements Runnable {
  private final ToIntFunction<RouteSlot> runOnce;
  private final Duration idle;
  private volatile boolean accepting = true;
  private boolean signalled;

  public RelayDrainWorker(BoundedOutboxRelay relay, RelayPolicy policy) {
    this(relay::runOnce, policy.idlePoll());
  }

  public RelayDrainWorker(ToIntFunction<RouteSlot> runOnce, Duration idle) {
    if (idle.isNegative() || idle.isZero())
      throw new IllegalArgumentException("Positive idle poll required");
    this.runOnce = runOnce;
    this.idle = idle;
  }

  public void run() {
    int first = 0;
    while (accepting) {
      int claimed = 0;
      for (int i = 0; i < OutboxRelayJob.SOURCES.size() && accepting; i++) {
        var slot = OutboxRelayJob.SOURCES.get((first + i) % OutboxRelayJob.SOURCES.size());
        try {
          claimed += runOnce.applyAsInt(slot);
        } catch (RuntimeException e) {
          LoggerFactory.getLogger(getClass()).warn("V2 drain failed at {}", slot.dbKey(), e);
        }
      }
      first = (first + 1) % OutboxRelayJob.SOURCES.size();
      if (claimed == 0) {
        synchronized (this) {
          if (accepting && !signalled) {
            try {
              java.util.concurrent.TimeUnit.NANOSECONDS.timedWait(this, idle.toNanos());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              return;
            }
          }
          signalled = false;
        }
      }
    }
  }

  public synchronized void wake() {
    signalled = true;
    notifyAll();
  }

  public synchronized void quiesce() {
    accepting = false;
    notifyAll();
  }
}
