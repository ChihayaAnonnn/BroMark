package com.jay.infrastructure.transaction;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.DatabaseBudget;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;

public final class BoundedDatabaseBudget implements DatabaseBudget {
  private final Map<String, Map<WorkClass, Semaphore>> pools = new HashMap<>();
  private final MeterRegistry metrics;
  private final long tradeMaxWaitNanos;
  private final boolean boundedTradeWait;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition idle = lock.newCondition();
  private volatile boolean accepting = true;
  private int inFlight;

  public BoundedDatabaseBudget(Map<String, Map<WorkClass, Integer>> limits, MeterRegistry metrics) {
    // 保留已有直接构造调用的 deadline 语义；应用通过配置启用前台短等待。
    this(limits, metrics, Duration.ofNanos(Long.MAX_VALUE), false);
  }

  public BoundedDatabaseBudget(
      Map<String, Map<WorkClass, Integer>> limits, MeterRegistry metrics, Duration tradeMaxWait) {
    this(limits, metrics, tradeMaxWait, true);
  }

  private BoundedDatabaseBudget(
      Map<String, Map<WorkClass, Integer>> limits,
      MeterRegistry metrics,
      Duration tradeMaxWait,
      boolean boundedTradeWait) {
    this.boundedTradeWait = boundedTradeWait;
    if (tradeMaxWait == null || tradeMaxWait.isZero() || tradeMaxWait.isNegative())
      throw new IllegalArgumentException("Positive TRADE permit wait required");
    this.tradeMaxWaitNanos = tradeMaxWait.toNanos();
    this.metrics = metrics;
    if (limits.isEmpty()) throw new IllegalArgumentException("Physical database budgets required");
    limits.forEach(
        (db, classes) -> {
          var buckets = new EnumMap<WorkClass, Semaphore>(WorkClass.class);
          for (var work : WorkClass.values()) {
            Integer n = classes.get(work);
            if (n == null || n < 1)
              throw new IllegalArgumentException(
                  "Every work class needs an independent positive limit");
            buckets.put(work, new Semaphore(n, true));
          }
          pools.put(db, buckets);
        });
  }

  private Semaphore pool(String db, WorkClass work) {
    var classes = pools.get(db);
    if (classes == null || work == null)
      throw new IllegalArgumentException("Unknown physical database/work class");
    return classes.get(work);
  }

  public int available(String db, WorkClass work) {
    return pool(db, work).availablePermits();
  }

  public Permit acquire(String db, WorkClass work, Deadline deadline) {
    Semaphore semaphore = pool(db, work);
    long started = System.nanoTime();
    String outcome = "acquired";
    try {
      while (true) {
        if (!accepting) throw new RejectedExecutionException("Database budget is stopping");
        Duration remaining = deadline.remaining();
        if (remaining.isNegative() || remaining.isZero())
          throw new BudgetExceededException("Database permit deadline exceeded");
        long waitNanos = remaining.toNanos();
        // 前台交易只短暂排队；后台任务仍遵守原有恢复/关闭期限。
        if (work == WorkClass.TRADE && boundedTradeWait) {
          waitNanos = Math.min(waitNanos, tradeMaxWaitNanos - (System.nanoTime() - started));
          if (waitNanos <= 0)
            throw new BudgetExceededException("TRADE permit wait budget exceeded");
        }
        // 有界前台等待保留公平队列位置，避免每50ms退出、重新排队。
        long attemptNanos =
            work == WorkClass.TRADE && boundedTradeWait
                ? waitNanos
                : Math.min(waitNanos, TimeUnit.MILLISECONDS.toNanos(50));
        if (!semaphore.tryAcquire(attemptNanos, TimeUnit.NANOSECONDS)) continue;
        lock.lock();
        try {
          if (!accepting) {
            semaphore.release();
            throw new RejectedExecutionException("Database budget is stopping");
          }
          inFlight++;
        } finally {
          lock.unlock();
        }
        AtomicBoolean released = new AtomicBoolean();
        return () -> {
          if (released.compareAndSet(false, true)) {
            semaphore.release();
            lock.lock();
            try {
              inFlight--;
              if (inFlight == 0) idle.signalAll();
            } finally {
              lock.unlock();
            }
          }
        };
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      outcome = "interrupted";
      throw new RejectedExecutionException("Interrupted waiting for database permit", e);
    } catch (RuntimeException e) {
      outcome = "rejected";
      metrics.counter("bm.v2.database.rejected", "work", work.name()).increment();
      throw e;
    } finally {
      metrics
          .timer("bm.v2.database.permit.wait", "work", work.name(), "result", outcome)
          .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
    }
  }

  public boolean isIdle() {
    lock.lock();
    try {
      return inFlight == 0;
    } finally {
      lock.unlock();
    }
  }

  public void stopAccepting() {
    lock.lock();
    try {
      accepting = false;
    } finally {
      lock.unlock();
    }
  }

  public boolean awaitIdle(Duration timeout) throws InterruptedException {
    long remaining = timeout.toNanos();
    lock.lock();
    try {
      while (inFlight > 0) {
        if (remaining <= 0) return false;
        remaining = idle.awaitNanos(remaining);
      }
      return true;
    } finally {
      lock.unlock();
    }
  }
}
