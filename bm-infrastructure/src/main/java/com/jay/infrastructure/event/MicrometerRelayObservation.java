package com.jay.infrastructure.event;

import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.task.service.RelayObservation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 将输入约束为有限基数后注册 Micrometer 指标。 */
public final class MicrometerRelayObservation implements RelayObservation {
  private static final Set<String> STAGES =
      Set.of(
          "batch",
          "claim",
          "wave",
          "confirm",
          "settle",
          "permit",
          "http_admission",
          "activity_admission",
          "config",
          "asset_tx",
          "recovery_check",
          "recovery_redrive");
  private static final Set<String> ACTIONS =
      Set.of(
          "claimed",
          "started",
          "completed",
          "lost_lease",
          "released_unsent",
          "timeout",
          "checked",
          "redriven",
          "suppressed",
          "exhausted",
          "sql",
          "pending",
          "processing");
  private static final Set<String> REASONS =
      Set.of(
          "success",
          "error",
          "UNKNOWN",
          "confirmed",
          "returned",
          "nacked",
          "finished",
          "expired",
          "no_progress",
          "active_task",
          "remote_retry",
          "cooldown",
          "paused",
          "uncertain_fact",
          "lease_lost",
          "not_sent",
          "deadline",
          "review",
          "empty",
          "other");
  private static final Set<String> EVENTS =
      Set.of(
          "ReserveAward",
          "ConfirmReservation",
          "CancelReservation",
          "CreditGranted",
          "PrepareDraw",
          "ReservationHeld",
          "ReservationConfirmed",
          "ReservationCancelled",
          "AwardDelivery",
          "GrantCredit",
          "ExchangeCompleted",
          "SignInCompleted",
          "OTHER");
  private static final Set<String> P0_ADMISSION_RESULTS =
      Set.of("accepted", "rejected", "ratelimited");
  private static final Set<String> P0_DELIVERY_RESULTS =
      Set.of("delivered", "duplicate", "retry", "failed");
  private static final Set<String> P0_EVENT_RESULTS =
      Set.of("emitted", "accepted", "rejected", "delivered", "duplicate", "retry", "failed");
  private static final Set<String> P0_STAGES = Set.of("admission", "reserve", "deliver");
  private static final Set<String> P0_STAGE_RESULTS =
      Set.of("success", "error", "ratelimited", "delivered", "duplicate", "retry", "failed");
  private final MeterRegistry registry;

  public MicrometerRelayObservation(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry);
  }

  private static String oneOf(String value, Set<String> allowed) {
    if (!allowed.contains(value))
      throw new IllegalArgumentException("Unbounded observation label: " + value);
    return value;
  }

  private static List<Tag> tags(RouteSlot slot, String eventType, String name, String value) {
    Objects.requireNonNull(slot);
    var tags = new ArrayList<Tag>();
    tags.add(Tag.of("database", slot.dbKey()));
    tags.add(Tag.of("event_type", oneOf(eventType, EVENTS)));
    tags.add(Tag.of(name, value));
    return tags;
  }

  @Override
  public void duration(String stage, RouteSlot slot, String eventType, String outcome, long nanos) {
    oneOf(stage, STAGES);
    oneOf(outcome, REASONS);
    if (nanos < 0) throw new IllegalArgumentException("Negative duration");
    var labels = tags(slot, eventType, "stage", stage);
    labels.add(Tag.of("outcome", outcome));
    Timer.builder("bm.v2.relay.duration")
        .tags(labels)
        .register(registry)
        .record(nanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public void count(String action, RouteSlot slot, String eventType, String reason, long n) {
    oneOf(action, ACTIONS);
    oneOf(reason, REASONS);
    if (n < 0) throw new IllegalArgumentException("Negative count");
    if (action.equals("completed") && reason.equals("UNKNOWN")) return;
    var labels = tags(slot, eventType, "action", action);
    labels.add(Tag.of("reason", reason));
    var counter = Counter.builder("bm.v2.relay.count").tags(labels).register(registry);
    if ((action.equals("redriven") || action.equals("exhausted"))
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              counter.increment(n);
            }
          });
    } else counter.increment(n);
  }

  /**
   * P0 指标只在事务真正提交后生效；回滚/提交失败不能被计成 delivered/emitted。
   *
   * <p>没有事务同步（例如直接调用 service、读取路径）时保持立即记录。
   */
  private static void afterCommit(Runnable record) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              record.run();
            }
          });
    } else record.run();
  }

  @Override
  public void p0Admission(String result) {
    var counter =
        Counter.builder("p0_admission_total")
            .tag("result", oneOf(result, P0_ADMISSION_RESULTS))
            .register(registry);
    afterCommit(counter::increment);
  }

  @Override
  public void p0Delivery(String result) {
    var counter =
        Counter.builder("p0_delivery_total")
            .tag("result", oneOf(result, P0_DELIVERY_RESULTS))
            .register(registry);
    afterCommit(counter::increment);
  }

  @Override
  public void p0Event(String type, String result) {
    String boundedType = EVENTS.contains(type) ? type : "OTHER";
    var counter =
        Counter.builder("p0_event_total")
            .tags("type", boundedType, "result", oneOf(result, P0_EVENT_RESULTS))
            .register(registry);
    afterCommit(counter::increment);
  }

  @Override
  public void p0Stage(String stage, String result, long nanos) {
    oneOf(stage, P0_STAGES);
    oneOf(result, P0_STAGE_RESULTS);
    if (nanos < 0) throw new IllegalArgumentException("Negative duration");
    var timer =
        Timer.builder("p0_stage_seconds")
            .tags("stage", stage, "result", result)
            .register(registry);
    afterCommit(() -> timer.record(nanos, TimeUnit.NANOSECONDS));
  }
}
