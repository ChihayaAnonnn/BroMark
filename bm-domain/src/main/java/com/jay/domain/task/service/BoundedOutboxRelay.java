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
import java.util.concurrent.atomic.AtomicBoolean;

/** 领取提交后分波发布；只有确定未调用发布器的本地记录才允许回退领取次数。 */
public final class BoundedOutboxRelay {
  private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
  private final BatchOutboxRepository repo;
  private final ConfirmedEventPublisher publisher;
  private final RelayPolicy policy;
  private final FaultInjector faults;
  private final RelayObservation observation;
  private boolean accepting = true;
  private int active;
  private final List<CompletionStage<PublishOutcome>> outstanding = new ArrayList<>();
  private volatile boolean invocationUncertain;

  public BoundedOutboxRelay(
      BatchOutboxRepository repo,
      ConfirmedEventPublisher publisher,
      RelayPolicy policy,
      FaultInjector faults) {
    this(repo, publisher, policy, faults, RelayObservation.NOOP);
  }

  public BoundedOutboxRelay(
      BatchOutboxRepository repo,
      ConfirmedEventPublisher publisher,
      RelayPolicy policy,
      FaultInjector faults,
      RelayObservation observation) {
    policy.validate(2, publisher.capacity());
    this.repo = Objects.requireNonNull(repo);
    this.publisher = Objects.requireNonNull(publisher);
    this.policy = policy;
    this.faults = Objects.requireNonNull(faults);
    this.observation = Objects.requireNonNull(observation);
  }

  public int runOnce(RouteSlot slot) {
    faults.beforeClaim();
    synchronized (this) {
      if (!accepting || active > 0 || physicalPending()) return 0;
      active++;
    }
    long start = System.nanoTime(), batchEnd = start + policy.batchTimeout().toNanos();
    var unsent = new LinkedHashMap<String, ClaimedPublish>();
    boolean batchSucceeded = false;
    try {
      long claimStart = System.nanoTime();
      List<ClaimedPublish> tasks;
      String claimOutcome = "error";
      try {
        tasks = repo.claimBatch(slot, policy, deadline(Math.min(batchEnd, start + policy.claimTimeout().toNanos())));
        claimOutcome = "success";
      } finally {
        observation.duration("claim", slot, "OTHER", claimOutcome, System.nanoTime() - claimStart);
      }
      for (var task : tasks)
        observation.count("claimed", slot, knownType(task.eventType()), "success", 1);
      tasks.forEach(t -> unsent.put(t.eventId(), t));
      if (tasks.isEmpty()) { batchSucceeded = true; return 0; }
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
        long waveStarted = System.nanoTime();
        String waveOutcome = "error";
        observation.count("started", slot, "OTHER", "success", 1);
        try {
          Deadline publishDeadline = deadline(waveEnd);
          var attempts = new ArrayList<CompletionStage<PublishOutcome>>();
          var submitted = new ArrayList<ClaimedPublish>();
          var invocationStarts = new ArrayList<Long>();
          var observedCompletions = new ArrayList<AtomicBoolean>();
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
            long invocationStart = System.nanoTime();
            var observedCompletion = new AtomicBoolean();
            invocationStarts.add(invocationStart);
            observedCompletions.add(observedCompletion);
            try {
              var attempt =
                  Objects.requireNonNull(
                      publisher.publishAttempt(
                          event, task.leaseToken(), task.failureCount(), publishDeadline));
              attempts.add(attempt);
              attempt.whenComplete(
                  (value, failure) ->
                      recordConfirm(
                          slot,
                          task,
                          invocationStart,
                          observedCompletion,
                          failure == null ? value : PublishOutcome.UNKNOWN));
              synchronized (this) {
                outstanding.add(attempt);
              }
            } catch (Exception unknown) {
              // 调用已发生，即使抛错也无法证明未发送。
              invocationUncertain = true;
              attempts.add(CompletableFuture.completedFuture(PublishOutcome.UNKNOWN));
              recordConfirm(slot, task, invocationStart, observedCompletion, PublishOutcome.UNKNOWN);
              break;
            }
          }
          var results = new ArrayList<PublishResolution>();
          for (int i = 0; i < attempts.size(); i++) {
            PublishOutcome outcome = await(attempts.get(i), waveEnd);
            recordConfirm(
                slot, submitted.get(i), invocationStarts.get(i), observedCompletions.get(i), outcome);
            if (outcome == PublishOutcome.CONFIRMED) {
              var task = submitted.get(i);
              faults.hit(
                  "relay.after-confirm",
                  task.eventId(),
                  task.aggregateId(),
                  task.leaseToken(),
                  task.attempt());
            }
            results.add(new PublishResolution(submitted.get(i), outcome, outcome.name()));
          }
          if (!results.isEmpty()) {
            long settleStart = System.nanoTime();
            BatchSettleReport report;
            String settleOutcome = "error";
            try {
              report = repo.settleBatch(slot, results,
                  deadline(Math.min(batchEnd, settleStart + policy.settleTimeout().toNanos())));
              settleOutcome = "success";
            } finally {
              observation.duration("settle", slot, "OTHER", settleOutcome, System.nanoTime() - settleStart);
            }
            for (var result : results) {
              var eventType = knownType(result.task().eventType());
              var id = result.task().eventId();
              if (report.completed().contains(id))
                observation.count("completed", slot, eventType, "confirmed", 1);
              if (report.lost().contains(id))
                observation.count("lost_lease", slot, eventType, "lease_lost", 1);
            }
            // 不确定提交直接退出，不产生本地成功计数。新样本不能使保守的DB当前时间倒退。
            Instant previousUpper = sample.plusNanos(settleStart - sampleStart);
            if (!report.databaseNow().isBefore(previousUpper)) {
              sample = report.databaseNow();
              sampleStart = settleStart;
            }
          }
          waveOutcome = results.stream().anyMatch(r -> r.outcome() == PublishOutcome.UNKNOWN)
              ? "UNKNOWN" : results.stream().anyMatch(r -> r.outcome() == PublishOutcome.RETURNED)
              ? "returned" : results.stream().anyMatch(r -> r.outcome() == PublishOutcome.NACKED)
              ? "nacked" : results.isEmpty() ? "empty" : "confirmed";
          // UNKNOWN仅为调用者结果，不能当作物理结束。逐attempt判断不会串行化另一通道。
          if (physicalPending()) break;
          if (Thread.currentThread().isInterrupted()) break;
        } finally {
          observation.duration("wave", slot, "OTHER", waveOutcome, System.nanoTime() - waveStarted);
        }
      }
      batchSucceeded = true;
      return tasks.size();
    } finally {
      boolean cleanupSucceeded = false;
      try {
        if (!unsent.isEmpty()) {
          var released =
              repo.releaseUnsent(
                  slot,
                  List.copyOf(unsent.values()),
                  new Deadline(Instant.now().plus(policy.settleTimeout())));
          for (var task : unsent.values())
            if (released.contains(task.eventId()))
              observation.count(
                  "released_unsent", slot, knownType(task.eventType()), "not_sent", 1);
        }
        cleanupSucceeded = true;
      } finally {
        synchronized (this) {
          active--;
          notifyAll();
        }
        long elapsed = System.nanoTime() - start;
        boolean timedOut = elapsed >= policy.batchTimeout().toNanos();
        // finished仅表示loop及release正常返回；运输结果由wave/confirm/settle分别说明。
        observation.duration("batch", slot, "OTHER",
            !batchSucceeded || !cleanupSucceeded ? "error" : timedOut ? "deadline" : "finished", elapsed);
        if (timedOut) observation.count("timeout", slot, "OTHER", "deadline", 1);
      }
    }
  }

  private static String knownType(String type) {
    return EventRouting.CONTROL.contains(type)
            || EventRouting.USER.contains(type)
            || EventRouting.NOTIFICATIONS.contains(type)
        ? type
        : "OTHER";
  }

  private void recordConfirm(
      RouteSlot slot,
      ClaimedPublish task,
      long started,
      AtomicBoolean recorded,
      PublishOutcome outcome) {
    if (!recorded.compareAndSet(false, true)) return;
    observation.duration(
        "confirm",
        slot,
        knownType(task.eventType()),
        outcome == PublishOutcome.CONFIRMED
            ? "confirmed"
            : outcome == PublishOutcome.RETURNED ? "returned"
            : outcome == PublishOutcome.NACKED ? "nacked" : "UNKNOWN",
        System.nanoTime() - started);
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

  /** 先对所有通道停止领取，再分别消费共享关闭截止的剩余时间。 */
  public synchronized void quiesce() {
    accepting = false;
  }

  public synchronized void stopAndDrain(Duration timeout) {
    quiesce();
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
