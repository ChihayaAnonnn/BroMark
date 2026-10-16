package com.jay.trigger.job.v2;

import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.task.service.OutboxRelay;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.LoggerFactory;

public final class OutboxRelayJob {
  public static final List<RouteSlot> SOURCES =
      List.of(RouteSlot.control(), new RouteSlot("01", "000"), new RouteSlot("02", "000"));
  private final OutboxRelay relay;
  private final AtomicBoolean busy = new AtomicBoolean();
  private volatile boolean running = true;

  public OutboxRelayJob(OutboxRelay relay) {
    this.relay = relay;
  }

  public void tick() {
    if (!running || !busy.compareAndSet(false, true)) return;
    try {
      for (var slot : SOURCES) {
        if (!running) break;
        try {
          relay.runOnce(slot, 8);
        } catch (Exception e) {
          LoggerFactory.getLogger(getClass()).warn("V2 relay failed at source {}", slot.dbKey(), e);
        }
      }
    } finally {
      busy.set(false);
    }
  }

  public void quiesce() {
    running = false;
    relay.quiesce();
  }

  public void stopAndDrain(Duration timeout) {
    quiesce();
    relay.stopAndDrain(timeout);
  }

  public void stop() {
    stopAndDrain(Duration.ofSeconds(30));
  }
}
