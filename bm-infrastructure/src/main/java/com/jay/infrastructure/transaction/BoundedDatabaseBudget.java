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
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition idle = lock.newCondition();
  private volatile boolean accepting = true;
  private int inFlight;

  public BoundedDatabaseBudget(Map<String, Map<WorkClass, Integer>> limits, MeterRegistry metrics) {
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
        if (!semaphore.tryAcquire(
            Math.min(remaining.toNanos(), TimeUnit.MILLISECONDS.toNanos(50)), TimeUnit.NANOSECONDS))
          continue;
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
