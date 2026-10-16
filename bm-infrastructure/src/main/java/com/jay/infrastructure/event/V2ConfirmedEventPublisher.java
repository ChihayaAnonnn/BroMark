package com.jay.infrastructure.event;

import com.jay.domain.asset.model.Deadline;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.ConfirmedEventPublisher;
import com.jay.infrastructure.adapter.repository.v2.JsonStore;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class V2ConfirmedEventPublisher implements ConfirmedEventPublisher, AutoCloseable {
  private final RabbitTemplate template;
  private final String exchange;
  private final Duration timeout;
  private final int capacity;
  private final Semaphore permits;
  private final ThreadPoolExecutor sends;
  private final ScheduledThreadPoolExecutor timer =
      new ScheduledThreadPoolExecutor(1, Thread.ofPlatform().name("v2-confirm-timeout").factory());
  private final ConcurrentMap<CompletionStage<PublishOutcome>, Attempt> pending =
      new ConcurrentHashMap<>();
  private volatile boolean closed;

  public V2ConfirmedEventPublisher(
      RabbitTemplate template, String exchange, int max, Duration timeout) {
    if (max < 1 || max > 128 || timeout.isZero() || timeout.isNegative())
      throw new IllegalArgumentException("Publisher bounds");
    this.template = template;
    this.exchange = exchange;
    this.timeout = timeout;
    this.capacity = max;
    this.permits = new Semaphore(max);
    template.setMandatory(true);
    timer.setRemoveOnCancelPolicy(true);
    sends =
        new ThreadPoolExecutor(
            2,
            2,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(max),
            Thread.ofPlatform().name("v2-send-", 0).factory(),
            new ThreadPoolExecutor.AbortPolicy());
  }

  public int capacity() {
    return capacity;
  }

  public boolean isAttemptComplete(CompletionStage<PublishOutcome> attempt) {
    return !pending.containsKey(attempt);
  }

  public CompletionStage<PublishOutcome> publish(EventEnvelope e, Deadline d) {
    return publishAttempt(e, UUID.randomUUID().toString(), 0, d);
  }

  public synchronized CompletionStage<PublishOutcome> publishAttempt(
      EventEnvelope e, String token, int failureCount, Deadline d) {
    if (TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Broker I/O outside database transaction");
    long nanos = Math.min(timeout.toNanos(), d.remaining().toNanos());
    if (closed || nanos <= 0 || !permits.tryAcquire())
      return CompletableFuture.completedFuture(PublishOutcome.UNKNOWN);
    var attempt = new Attempt();
    pending.put(attempt.result, attempt);
    var expiry =
        timer.schedule(
            () -> attempt.result.complete(PublishOutcome.UNKNOWN), nanos, TimeUnit.NANOSECONDS);
    attempt.result.whenComplete((v, t) -> expiry.cancel(false));
    try {
      sends.execute(
          () -> {
            if (attempt.result.isDone()) {
              attempt.noSend();
              return;
            }
            var cd = new CorrelationData(e.eventId() + ":" + token);
            cd.getFuture()
                .whenComplete(
                    (confirm, error) ->
                        attempt.transportFinished(
                            error != null
                                ? PublishOutcome.UNKNOWN
                                : cd.getReturned() != null
                                    ? PublishOutcome.RETURNED
                                    : confirm.isAck()
                                        ? PublishOutcome.CONFIRMED
                                        : confirm.getReason() != null
                                            ? PublishOutcome.UNKNOWN
                                            : PublishOutcome.NACKED));
            try {
              var props = new MessageProperties();
              props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
              props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
              props.setMessageId(e.eventId());
              props.setHeader("x-v2-failure-count", failureCount);
              template.send(
                  exchange,
                  e.eventType(),
                  new Message(
                      JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8), props),
                  cd);
            } catch (Exception ex) {
              // send抛错后本地已没有迟到发送路径；仍按UNKNOWN保留远端迟到确认/重复的可能。
              attempt.result.complete(PublishOutcome.UNKNOWN);
              attempt.transportFinished(PublishOutcome.UNKNOWN);
            } finally {
              attempt.sendExited();
            }
          });
    } catch (RejectedExecutionException ex) {
      attempt.noSend();
    }
    return attempt.result;
  }

  /** 调用方超时/取消只完成result；必须send退出且confirm终结或本地send异常终结后才释放物理槽位。 */
  private final class Attempt {
    final CompletableFuture<PublishOutcome> result = new CompletableFuture<>();
    boolean sendExited, transportFinished, released;
    PublishOutcome outcome;

    synchronized void noSend() {
      sendExited = true;
      transportFinished(PublishOutcome.UNKNOWN);
    }

    synchronized void sendExited() {
      sendExited = true;
      finish();
    }

    synchronized void transportFinished(PublishOutcome value) {
      transportFinished = true;
      outcome = value;
      finish();
    }

    private void finish() {
      if (!released && sendExited && transportFinished) {
        released = true;
        permits.release();
        pending.remove(result);
        // 正常确认也等send退出，避免confirm先到时本通道过早开启新波。
        result.complete(outcome);
      }
    }
  }

  public void close() {
    synchronized (this) {
      closed = true;
      sends.shutdown();
    }
    long end = System.nanoTime() + timeout.plusSeconds(1).toNanos();
    try {
      long left = end - System.nanoTime();
      if (left > 0) sends.awaitTermination(left, TimeUnit.NANOSECONDS);
      while (!pending.isEmpty() && (left = end - System.nanoTime()) > 0)
        TimeUnit.NANOSECONDS.sleep(Math.min(left, TimeUnit.MILLISECONDS.toNanos(10)));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      // 强制关闭只能通知UNKNOWN，不能伪称迟到send已结束或释放它的槽位。
      pending.keySet().forEach(f -> f.toCompletableFuture().complete(PublishOutcome.UNKNOWN));
      sends.shutdownNow();
      timer.shutdownNow();
    }
  }
}
