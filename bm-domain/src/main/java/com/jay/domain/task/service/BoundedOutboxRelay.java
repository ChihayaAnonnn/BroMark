package com.jay.domain.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.FaultInjector;
import com.jay.domain.asset.service.RequestHasher;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.BatchOutboxRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** 领取提交后分波发布；只有确定未调用发布器的本地记录才允许回退领取次数。 */
public final class BoundedOutboxRelay {
  private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
  private final BatchOutboxRepository repo;
  private final ConfirmedEventPublisher publisher;
  private final RelayPolicy policy;
  private final FaultInjector faults;
  private boolean accepting = true;
  private int active;
  private final List<CompletionStage<PublishOutcome>> outstanding = new ArrayList<>();
  private volatile boolean invocationUncertain;

  public BoundedOutboxRelay(
      BatchOutboxRepository repo,
      ConfirmedEventPublisher publisher,
      RelayPolicy policy,
      FaultInjector faults) {
    policy.validate(2, publisher.capacity());
    this.repo = Objects.requireNonNull(repo);
    this.publisher = Objects.requireNonNull(publisher);
    this.policy = policy;
    this.faults = Objects.requireNonNull(faults);
  }

  public int runOnce(RouteSlot slot) {
    synchronized (this) {
      if (!accepting || active > 0 || physicalPending()) return 0;
      active++;
    }
    long start = System.nanoTime(), batchEnd = start + policy.batchTimeout().toNanos();
    var unsent = new LinkedHashMap<String, ClaimedPublish>();
    try {
      var tasks =
          repo.claimBatch(
              slot, policy, deadline(Math.min(batchEnd, start + policy.claimTimeout().toNanos())));
      tasks.forEach(t -> unsent.put(t.eventId(), t));
      if (tasks.isEmpty()) return 0;
      // DB采样点位于claim调用内部，以调用开始作为锚点扣除全部采样不确定度。
      Instant sample =
          tasks.stream().map(ClaimedPublish::claimedAt).max(Instant::compareTo).orElseThrow();
      Instant leaseEnd =
          tasks.stream().map(ClaimedPublish::leaseUntil).min(Instant::compareTo).orElseThrow();
      long sampleStart = start;
      int offset = 0;
      while (offset < tasks.size() && accepting()) {
        long now = System.nanoTime();
        long waveCost = policy.publishTimeout().plus(policy.settleTimeout()).toNanos();
        long leaseLeft = Duration.between(sample, leaseEnd).toNanos() - (now - sampleStart);
        if (batchEnd - now < waveCost || leaseLeft < waveCost + policy.safetyMargin().toNanos())
          break;
        long waveEnd = Math.min(batchEnd, now + policy.publishTimeout().toNanos());
        Deadline publishDeadline = deadline(waveEnd);
        var attempts = new ArrayList<CompletionStage<PublishOutcome>>();
        var submitted = new ArrayList<ClaimedPublish>();
        int stop = Math.min(tasks.size(), offset + policy.publishWindow());
        for (; offset < stop && accepting(); offset++) {
          var task = tasks.get(offset);
          EventEnvelope event;
          try {
            event = decode(task);
          } catch (Exception malformed) {
            if (repo.quarantineMalformed(
                task,
                "Invalid persisted event identity or payload hash",
                deadline(Math.min(batchEnd, System.nanoTime() + policy.settleTimeout().toNanos()))))
              unsent.remove(task.eventId());
            continue;
          }
          // 解析/核查可能耗时，每条调用前重验单调预算，绝不启动已过期的波次。
          if (System.nanoTime() >= waveEnd) break;
          unsent.remove(task.eventId());
          submitted.add(task);
          try {
            var attempt =
                Objects.requireNonNull(
                    publisher.publishAttempt(
                        event, task.leaseToken(), task.failureCount(), publishDeadline));
            attempts.add(attempt);
            synchronized (this) {
              outstanding.add(attempt);
            }
          } catch (Exception unknown) {
            // 调用已发生，即使抛错也无法证明未发送。
            invocationUncertain = true;
            attempts.add(CompletableFuture.completedFuture(PublishOutcome.UNKNOWN));
            break;
          }
        }
        var results = new ArrayList<PublishResolution>();
        for (int i = 0; i < attempts.size(); i++) {
          PublishOutcome outcome = await(attempts.get(i), waveEnd);
          if (outcome == PublishOutcome.CONFIRMED) faults.hit("relay.after-confirm");
          results.add(new PublishResolution(submitted.get(i), outcome, outcome.name()));
        }
        if (!results.isEmpty()) {
          long settleStart = System.nanoTime();
          var report =
              repo.settleBatch(
                  slot,
                  results,
                  deadline(Math.min(batchEnd, settleStart + policy.settleTimeout().toNanos())));
          // 不确定提交直接退出，不产生本地成功计数。新样本不能使保守的DB当前时间倒退。
          Instant previousUpper = sample.plusNanos(settleStart - sampleStart);
          if (!report.databaseNow().isBefore(previousUpper)) {
            sample = report.databaseNow();
            sampleStart = settleStart;
          }
        }
        // UNKNOWN仅为调用者结果，不能当作物理结束。逐attempt判断不会串行化另一通道。
        if (physicalPending()) break;
        if (Thread.currentThread().isInterrupted()) break;
      }
      return tasks.size();
    } finally {
      try {
        if (!unsent.isEmpty())
          repo.releaseUnsent(
              slot,
              List.copyOf(unsent.values()),
              new Deadline(Instant.now().plus(policy.settleTimeout())));
      } finally {
        synchronized (this) {
          active--;
          notifyAll();
        }
      }
    }
  }

  private EventEnvelope decode(ClaimedPublish task) throws Exception {
    var event = JSON.readValue(task.rawEnvelope(), EventEnvelope.class);
    // MySQL JSON读取会重排键和空白；还原writer使用的record序列化后再验证原摘要。
    String persistedEnvelope = JSON.writeValueAsString(event);
    String hash =
        task.consumerName().isEmpty()
            ? new RequestHasher().hash("EVENT", Map.of("envelope", persistedEnvelope))
            : new RequestHasher().hash("MESSAGE", Map.of("body", persistedEnvelope));
    if (!event.eventId().equals(task.eventId())
        || !event.aggregateId().equals(task.aggregateId())
        || event.aggregateVersion() != task.aggregateVersion()
        || !event.eventType().equals(task.eventType())
        || !hash.equals(task.payloadHash()))
      throw new IllegalArgumentException("Persisted event mismatch");
    return event;
  }

  private static PublishOutcome await(CompletionStage<PublishOutcome> stage, long end) {
    try {
      var future = stage.toCompletableFuture();
      if (future.isDone()) return Objects.requireNonNullElse(future.join(), PublishOutcome.UNKNOWN);
      long left = end - System.nanoTime();
      return left <= 0
          ? PublishOutcome.UNKNOWN
          : Objects.requireNonNullElse(
              future.get(left, TimeUnit.NANOSECONDS), PublishOutcome.UNKNOWN);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return PublishOutcome.UNKNOWN;
    } catch (ExecutionException
        | TimeoutException
        | CompletionException
        | CancellationException e) {
      return PublishOutcome.UNKNOWN;
    }
  }

  private static Deadline deadline(long end) {
    return new Deadline(Instant.now().plusNanos(Math.max(0, end - System.nanoTime())));
  }

  private synchronized boolean accepting() {
    return accepting;
  }

  private synchronized boolean physicalPending() {
    outstanding.removeIf(publisher::isAttemptComplete);
    return invocationUncertain || !outstanding.isEmpty();
  }

  public synchronized void stopAndDrain(Duration timeout) {
    accepting = false;
    long end = System.nanoTime() + timeout.toNanos();
    while (active > 0 || physicalPending()) {
      long left = end - System.nanoTime();
      if (left <= 0)
        throw new IllegalStateException("Relay did not drain; leases remain recoverable");
      try {
        TimeUnit.NANOSECONDS.timedWait(this, Math.min(left, TimeUnit.MILLISECONDS.toNanos(10)));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
  }
}
