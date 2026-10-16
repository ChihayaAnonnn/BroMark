package com.jay.trigger.job.v2;

import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;

/** 同一键最多一个排队/运行任务，失败或拒绝后下一次tick可重试。 */
public final class CoalescingJobDispatcher {
  private final ExecutorService executor;
  private final Set<String> pending = new HashSet<>();
  private boolean accepting = true;

  public CoalescingJobDispatcher(ExecutorService executor) {
    if (executor instanceof ThreadPoolExecutor pool
        && pool.getRejectedExecutionHandler() instanceof ThreadPoolExecutor.CallerRunsPolicy)
      throw new IllegalArgumentException("Recovery must not run on scheduling caller");
    this.executor = Objects.requireNonNull(executor);
  }

  public synchronized boolean submit(String key, Runnable work) {
    if (!accepting || !pending.add(key)) return false;
    try {
      executor.execute(
          () -> {
            try {
              synchronized (this) {
                if (!accepting) return;
              }
              work.run();
            } catch (RuntimeException e) {
              LoggerFactory.getLogger(getClass()).warn("V2 scheduled work failed: {}", key, e);
            } finally {
              synchronized (this) {
                pending.remove(key);
              }
            }
          });
      return true;
    } catch (RejectedExecutionException e) {
      pending.remove(key);
      return false;
    }
  }

  public synchronized void quiesce() {
    accepting = false;
  }
}
