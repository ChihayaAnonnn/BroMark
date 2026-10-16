package com.jay.domain.task.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.dao.OptimisticLockingFailureException;

public final class RecoveryService {
  private final UserTransactionRunner tx;
  private final RecoveryRepository repo;
  private final OutboxWriter outbox;
  private final Map<String, RecoveryAggregatePort> ports;
  private final Supplier<String> newId;

  public RecoveryService(
      UserTransactionRunner tx,
      RecoveryRepository repo,
      OutboxWriter outbox,
      Map<String, RecoveryAggregatePort> ports,
      Supplier<String> newId) {
    this.tx = tx;
    this.repo = repo;
    this.outbox = outbox;
    this.ports = Map.copyOf(ports);
    this.newId = newId;
  }

  public void pause(
      RouteSlot slot, String type, String id, RecoveryStatus status, String evidence) {
    tx.inSlot(
        slot,
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(10)),
        () -> {
          pauseInCurrentTransaction(type, id, status, evidence);
          return null;
        });
  }

  /** Caller owns the source short transaction (e.g. fenced task death); no route switch. */
  public void pauseInCurrentTransaction(
      String type, String id, RecoveryStatus status, String evidence) {
    if (status == RecoveryStatus.ACTIVE || evidence == null || evidence.isBlank())
      throw new IllegalArgumentException("Pause evidence required");
    var port = ports.get(type);
    var a = lockOrNull(port, id);
    if (a == null) a = new RecoveryAggregate(id, "UNBOUND", RecoveryStatus.ACTIVE, 1);
    else if (a.recoveryStatus() == RecoveryStatus.ACTIVE) {
      if (!port.updateStatus(id, a.version(), status))
        throw new OptimisticLockingFailureException("Pause version");
      a = new RecoveryAggregate(id, a.state(), status, a.version() + 1);
    }
    repo.open(type, a, status, evidence);
  }

  /** 已持有owner业务锁及guard；只defer真实持久化命令，不通过目标路由猜owner。 */
  public void pauseRedrive(String type, String id, EventEnvelope frozen, String evidence) {
    if (frozen == null || !id.equals(frozen.aggregateId()))
      throw new IllegalArgumentException("Frozen owner mismatch");
    pauseInCurrentTransaction(type, id, RecoveryStatus.RETRY_EXHAUSTED, evidence);
    repo.defer("workflow-v2", type, frozen);
  }

  private boolean dedicatedRecovery;

  public RecoveryService dedicatedWorkflows(boolean guarded) {
    dedicatedRecovery = guarded;
    return this;
  }

  /** Called inside the exact business transaction, before Inbox or handler. */
  public boolean deferIfPaused(String consumer, EventEnvelope e) {
    String type = EventRouting.aggregateType(e);
    var port = ports.get(type);
    var a = lockOrNull(port, e.aggregateId());
    var c = repo.active(type, e.aggregateId());
    if (c != null || (a != null && a.recoveryStatus() != RecoveryStatus.ACTIVE)) {
      repo.defer(consumer, type, e);
      return true;
    }
    return false;
  }

  public void resume(
      RouteSlot slot,
      String type,
      String id,
      long expectedVersion,
      String operator,
      String reason) {
    if (operator == null
        || operator.isBlank()
        || operator.length() > 128
        || reason == null
        || reason.isBlank()
        || reason.length() > 1024)
      throw new IllegalArgumentException("Audited operator and reason required");
    tx.inSlot(
        slot,
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(10)),
        () -> {
          var port = ports.get(type);
          var a = lockOrNull(port, id);
          var c = repo.active(type, id);
          if (c == null || (a == null ? c.version() : a.version()) != expectedVersion)
            throw new OptimisticLockingFailureException("Stale recovery version");
          var facts = repo.pending(type, id, 128);
          if (a != null && !port.updateStatus(id, expectedVersion, RecoveryStatus.ACTIVE))
            throw new OptimisticLockingFailureException("Resume version");
          if (a == null || !port.terminal(a)) {
            for (var fact : facts) {
              var replay = restate(fact);
              outbox.append(replay);
              repo.replayed(type, id, fact.eventId(), replay.eventId());
            }
            if (a != null)
              for (var action :
                  port.recoveryDecision(
                          new RecoveryAggregate(
                              a.id(), a.state(), RecoveryStatus.ACTIVE, a.version() + 1))
                      .actions()) outbox.append(restate(action));
          }
          if (a != null && port.terminal(a))
            for (var fact : facts) repo.replayed(type, id, fact.eventId(), "TERMINAL_NOOP");
          repo.close(c, operator, reason);
          return null;
        });
  }

  private RecoveryAggregate lockOrNull(RecoveryAggregatePort port, String id) {
    try {
      return port == null ? null : port.lock(id);
    } catch (org.springframework.dao.EmptyResultDataAccessException missing) {
      return null;
    }
  }

  private EventEnvelope restate(EventEnvelope e) {
    return new EventEnvelope(
        newId.get(),
        e.eventType(),
        e.aggregateId(),
        e.bizId(),
        e.userId(),
        e.routeSlot(),
        e.requestHash(),
        e.aggregateVersion(),
        e.schemaVersion(),
        e.payload());
  }

  public int scan(RouteSlot slot, int limit) {
    if (limit < 1 || limit > 128) throw new IllegalArgumentException("Scan bounds");
    int count = 0;
    var groups =
        tx.inSlot(
            slot,
            WorkClass.RECOVERY,
            new Deadline(Instant.now().plusSeconds(10)),
            () -> repo.readyGroups(limit));
    for (var group : groups)
      count +=
          tx.inSlot(
              slot,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(10)),
              () -> {
                String type = group[0], id = group[1];
                var port = ports.get(type);
                var a = lockOrNull(port, id);
                if (repo.active(type, id) != null
                    || (a != null && a.recoveryStatus() != RecoveryStatus.ACTIVE)) return 0;
                var facts = repo.pending(type, id, 128);
                for (var fact : facts) {
                  if (a != null && port.terminal(a))
                    repo.replayed(type, id, fact.eventId(), "TERMINAL_NOOP");
                  else {
                    var replay = restate(fact);
                    outbox.append(replay);
                    repo.replayed(type, id, fact.eventId(), replay.eventId());
                  }
                }
                return facts.size();
              });
    for (var entry : ports.entrySet()) {
      if (dedicatedRecovery && Set.of("DRAW", "RESERVATION", "PAYMENT").contains(entry.getKey()))
        continue;
      var ids =
          tx.inSlot(
              slot,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(10)),
              () -> entry.getValue().scanActive(limit));
      for (String id : ids) {
        count +=
            tx.inSlot(
                slot,
                WorkClass.RECOVERY,
                new Deadline(Instant.now().plusSeconds(10)),
                () -> {
                  var a = entry.getValue().lock(id);
                  if (a.recoveryStatus() != RecoveryStatus.ACTIVE
                      || repo.active(entry.getKey(), id) != null) return 0;
                  var actions = entry.getValue().recoveryDecision(a).actions();
                  actions.forEach(outbox::append);
                  return actions.size();
                });
      }
    }
    return count;
  }
}
