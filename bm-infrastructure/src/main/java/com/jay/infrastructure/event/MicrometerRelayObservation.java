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
}
