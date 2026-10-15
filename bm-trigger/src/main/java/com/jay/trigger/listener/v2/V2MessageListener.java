package com.jay.trigger.listener.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.asset.service.FaultInjector;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import com.jay.domain.task.service.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.slf4j.LoggerFactory;

/**
 * Transport Ack is exclusively after a confirmed local commit, including retry/quarantine handoffs.
 */
public final class V2MessageListener {
  public interface Delivery {
    void ack() throws Exception;

    void pause();
  }

  private final UserTransactionRunner tx;
  private final ConsumerUnitOfWork uow;
  private final RetryJobRepository retries;
  private final RecoveryService recovery;
  private final EventRouting routing;
  private final Function<byte[], EventEnvelope> decoder;
  private final RawMessageStore raw;
  private final Map<String, EventHandler> handlers;
  private final FaultInjector faults;
  private final RetryPolicy policy = new RetryPolicy();

  public V2MessageListener(
      UserTransactionRunner tx,
      ConsumerUnitOfWork uow,
      RetryJobRepository retries,
      RecoveryService recovery,
      EventRouting routing,
      Function<byte[], EventEnvelope> decoder,
      RawMessageStore raw,
      Map<String, EventHandler> handlers,
      FaultInjector faults) {
    this.tx = tx;
    this.uow = uow;
    this.retries = retries;
    this.recovery = recovery;
    this.routing = routing;
    this.decoder = decoder;
    this.raw = raw;
    this.handlers = Map.copyOf(handlers);
    this.faults = faults;
  }

  public void handle(String consumer, byte[] body, int headerFailures, Delivery delivery) {
    EventEnvelope event;
    RouteSlot slot;
    try {
      if (body.length > 65536) throw new IllegalArgumentException("Message larger than 64KiB");
      if (headerFailures < 0 || headerFailures > 12)
        throw new IllegalArgumentException("Invalid retry header");
      event = decoder.apply(body);
      slot = routing.route(event);
      if (EventRouting.NOTIFICATIONS.contains(event.eventType())
          != consumer.equals("notification-v2"))
        throw new IllegalArgumentException("Wrong consumer binding");
    } catch (RuntimeException invalid) {
      try {
        tx.inControl(
            WorkClass.RECOVERY,
            new Deadline(Instant.now().plusSeconds(10)),
            () -> {
              faults.hit("consumer.before-quarantine");
              raw.quarantine(consumer, body, "Invalid schema/type/route/header");
              return null;
            });
        ack(delivery);
      } catch (RuntimeException failed) {
        delivery.pause();
      }
      return;
    }
    try {
      var handler = handlers.get(event.eventType());
      if (handler == null) {
        defer(consumer, event, slot, RecoveryStatus.WAITING_FIX, "Missing event handler");
      } else uow.applyAttempt(consumer, event, headerFailures, retries, () -> handler.apply(event));
    } catch (WaitingFixException unsupported) {
      try {
        defer(
            consumer,
            event,
            slot,
            RecoveryStatus.WAITING_FIX,
            "Unsupported historical award or configuration");
      } catch (RuntimeException persistence) {
        delivery.pause();
        return;
      }
    } catch (AssetConflictException conflict) {
      try {
        defer(
            consumer,
            event,
            slot,
            RecoveryStatus.REVIEW,
            "Message or business digest/state conflict");
      } catch (AssetConflictException deferredConflict) {
        if (!quarantine(
            consumer,
            body,
            "Conflicting deferred identity: "
                + event.eventId()
                + " aggregate="
                + event.aggregateId(),
            delivery)) return;
      } catch (RuntimeException persistence) {
        delivery.pause();
        return;
      }
    } catch (RuntimeException transientFailure) {
      try {
        tx.inSlot(
            slot,
            WorkClass.RECOVERY,
            new Deadline(Instant.now().plusSeconds(10)),
            () -> {
              if (recovery.deferIfPaused(consumer, event)) return null;
              faults.hit("consumer.before-retry-persist");
              int failures = Math.max(headerFailures + 1, retries.failures(event));
              if (failures >= RetryPolicy.MAX_ATTEMPTS) {
                recovery.pause(
                    slot,
                    EventRouting.aggregateType(event),
                    event.aggregateId(),
                    RecoveryStatus.RETRY_EXHAUSTED,
                    "Retry budget exhausted: " + event.eventId());
                retries.dead(consumer, event, RetryPolicy.MAX_ATTEMPTS);
                recovery.deferIfPaused(consumer, event);
              } else
                retries.schedule(consumer, event, failures, policy.next(failures, Instant.now()));
              return null;
            });
      } catch (RuntimeException persistence) {
        LoggerFactory.getLogger(getClass())
            .warn("V2 retry handoff unavailable; consumer paused, delivery unacked");
        delivery.pause();
        return;
      }
    }
    ack(delivery);
  }

  private boolean quarantine(String consumer, byte[] body, String reason, Delivery delivery) {
    try {
      tx.inControl(
          WorkClass.RECOVERY,
          new Deadline(Instant.now().plusSeconds(10)),
          () -> {
            faults.hit("consumer.before-quarantine");
            raw.quarantine(consumer, body, reason);
            return null;
          });
      return true;
    } catch (RuntimeException unavailable) {
      delivery.pause();
      return false;
    }
  }

  private void defer(
      String consumer, EventEnvelope e, RouteSlot slot, RecoveryStatus status, String reason) {
    tx.inSlot(
        slot,
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(10)),
        () -> {
          recovery.pause(
              slot,
              EventRouting.aggregateType(e),
              e.aggregateId(),
              status,
              reason + " " + e.eventId());
          recovery.deferIfPaused(consumer, e);
          return null;
        });
  }

  private void ack(Delivery delivery) {
    try {
      faults.hit("consumer.before-ack");
      delivery.ack();
    } catch (Exception failed) {
      delivery.pause();
    }
  }
}
