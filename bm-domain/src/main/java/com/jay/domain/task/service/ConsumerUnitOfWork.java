package com.jay.domain.task.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.time.Instant;
import java.util.*;
import java.util.function.*;

public final class ConsumerUnitOfWork {
  private final UserTransactionRunner tx;
  private final ConsumerInbox inbox;
  private final OutboxWriter outbox;
  private final EventRouting routing;
  private final RecoveryService recovery;
  private final Supplier<String> newId;

  public ConsumerUnitOfWork(
      UserTransactionRunner tx,
      ConsumerInbox inbox,
      OutboxWriter outbox,
      EventRouting routing,
      RecoveryService recovery,
      Supplier<String> newId) {
    this.tx = tx;
    this.inbox = inbox;
    this.outbox = outbox;
    this.routing = routing;
    this.recovery = recovery;
    this.newId = newId;
  }

  public ConsumeOutcome apply(String consumer, EventEnvelope e, Runnable handler) {
    return applyWithResponses(
        consumer,
        e,
        () -> {
          handler.run();
          return List.of();
        });
  }

  public ConsumeOutcome applyWithResponses(
      String consumer, EventEnvelope e, Supplier<List<EventEnvelope>> handler) {
    return applyGuarded(consumer, e, () -> true, handler);
  }

  public ConsumeOutcome applyAttempt(
      String consumer,
      EventEnvelope e,
      int generation,
      RetryJobRepository retries,
      Supplier<List<EventEnvelope>> handler) {
    return applyGuarded(consumer, e, () -> retries.mayApply(consumer, e, generation), handler);
  }

  private ConsumeOutcome applyGuarded(
      String consumer,
      EventEnvelope e,
      BooleanSupplier eligible,
      Supplier<List<EventEnvelope>> handler) {
    if (consumer == null || !consumer.matches("[A-Za-z0-9_-]{1,64}"))
      throw new IllegalArgumentException("Consumer name");
    var slot = routing.route(e);
    return tx.inSlot(
        slot,
        WorkClass.WORKER,
        new Deadline(Instant.now().plusSeconds(10)),
        () -> {
          if (recovery != null && recovery.deferIfPaused(consumer, e))
            return ConsumeOutcome.DEFERRED;
          if (!eligible.getAsBoolean()) return ConsumeOutcome.DEFERRED;
          if (!inbox.tryInsert(consumer, e)) return ConsumeOutcome.DUPLICATE;
          if (inbox.tryStartOperation(consumer, e)) {
            var responses = List.copyOf(handler.get());
            responses.forEach(outbox::append);
            inbox.completeOperation(consumer, e, responses);
          } else
            for (var original : inbox.responses(consumer, e))
              outbox.append(
                  new EventEnvelope(
                      newId.get(),
                      original.eventType(),
                      original.aggregateId(),
                      original.bizId(),
                      original.userId(),
                      original.routeSlot(),
                      original.requestHash(),
                      original.aggregateVersion(),
                      original.schemaVersion(),
                      original.payload()));
          return ConsumeOutcome.APPLIED;
        });
  }
}
