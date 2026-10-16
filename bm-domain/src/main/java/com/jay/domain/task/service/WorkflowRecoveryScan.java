package com.jay.domain.task.service;

import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.task.model.*;
import java.time.Duration;
import java.util.*;
import java.util.function.*;

/** 每个服务一次tick共用预算；分区轮转防止前表饥饿，空分区也受64次查询上界。 */
public final class WorkflowRecoveryScan {
  public record Partition(RouteSlot slot, String state) {}

  private final Map<Partition, RecoveryScanPosition> cursors = new HashMap<>();
  private int next, candidates = 64, emissions = 8;
  private Duration timeout = Duration.ofSeconds(2);

  public void budget(int candidates, int emissions, Duration timeout) {
    if (candidates < 1
        || candidates > 64
        || emissions < 1
        || emissions > 8
        || emissions > candidates
        || timeout == null
        || timeout.isNegative()
        || timeout.isZero()
        || timeout.compareTo(Duration.ofSeconds(2)) > 0)
      throw new IllegalArgumentException("Recovery scan budget");
    this.candidates = candidates;
    this.emissions = emissions;
    this.timeout = timeout;
  }

  public synchronized int run(
      List<Partition> partitions,
      BiFunction<Partition, RecoveryScanPosition, RecoveryScanPage> scan,
      ToIntFunction<String> inspect) {
    long started = System.nanoTime();
    int emitted = 0, checked = 0;
    for (int queries = 0;
        queries < candidates && checked < candidates && emitted < emissions;
        queries++) {
      if (System.nanoTime() - started >= timeout.toNanos() || partitions.isEmpty()) break;
      var p = partitions.get(Math.floorMod(next++, partitions.size()));
      try {
        var page = scan.apply(p, cursors.get(p));
        if (page.exhausted()) cursors.remove(p);
        else cursors.put(p, page.next());
        for (String id : page.ids()) {
          if (checked >= candidates
              || emitted >= emissions
              || System.nanoTime() - started >= timeout.toNanos()) break;
          checked++;
          emitted += inspect.applyAsInt(id);
        }
      } catch (RuntimeException unavailable) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Recovery partition retained {}", p, unavailable);
      }
    }
    return emitted;
  }

  public static List<Partition> userPartitions(List<String> states) {
    var result = new ArrayList<Partition>();
    for (String state : states)
      for (String db : List.of("01", "02"))
        for (int tb = 0; tb < 4; tb++)
          result.add(new Partition(new RouteSlot(db, String.format("%03d", tb)), state));
    return List.copyOf(result);
  }
}
