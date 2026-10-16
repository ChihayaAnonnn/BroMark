package com.jay.domain.task.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.FaultInjector;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.OutboxRepository;
import java.time.*;
import java.util.concurrent.*;

public final class OutboxRelay {
  private final OutboxRepository repo;
  private final ConfirmedEventPublisher publisher;
  private final RetryPolicy policy;
  private final FaultInjector faults;
  private boolean accepting = true;
  private int active;

  public OutboxRelay(
      OutboxRepository repo,
      ConfirmedEventPublisher publisher,
      RetryPolicy policy,
      FaultInjector faults) {
    this.repo = repo;
    this.publisher = publisher;
    this.policy = policy;
    this.faults = faults;
  }

  public int runOnce(RouteSlot slot, int limit) {
    synchronized (this) {
      if (!accepting) return 0;
      active++;
    }
    try {
      var leases = repo.claim(slot, limit, Duration.ofSeconds(30));
      var stages =
          leases.stream()
              .map(
                  l ->
                      publisher
                          .publishAttempt(
                              l.event(),
                              l.leaseToken(),
                              failureCount(l),
                              new Deadline(Instant.now().plusSeconds(5)))
                          .toCompletableFuture())
              .toList();
      for (int i = 0; i < leases.size(); i++) {
        var lease = leases.get(i);
        PublishOutcome outcome;
        try {
          outcome = stages.get(i).get(6, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(e);
        } catch (ExecutionException | TimeoutException e) {
          outcome = PublishOutcome.UNKNOWN;
        }
        if (outcome == PublishOutcome.CONFIRMED) {
          faults.hit("relay.after-confirm");
          repo.complete(lease);
        } else repo.retry(lease, policy.next(lease.attempt(), Instant.now()), outcome.name());
      }
      return leases.size();
    } finally {
      synchronized (this) {
        active--;
        notifyAll();
      }
    }
  }

  private int failureCount(OutboxLease l) {
    return repo instanceof com.jay.domain.task.repository.RetryJobRepository retry
        ? retry.failureCount(l)
        : 0;
  }

  /** 先对所有通道停止领取，再分别消费共享关闭截止的剩余时间。 */
  public synchronized void quiesce() {
    accepting = false;
  }

  public synchronized void stopAndDrain(Duration timeout) {
    quiesce();
    long end = System.nanoTime() + timeout.toNanos();
    while (active > 0) {
      long left = end - System.nanoTime();
      if (left <= 0)
        throw new IllegalStateException("Relay did not drain; database must remain available");
      try {
        TimeUnit.NANOSECONDS.timedWait(this, left);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
  }
}
