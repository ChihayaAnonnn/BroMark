package com.jay.domain.raffle.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.repository.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.stock.repository.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.time.*;
import java.util.*;

/** Observe the other domain before acquiring the local recovery transaction. No cross-db lock. */
public final class DrawRecoveryService {
  private final UserTransactionRunner tx;
  private final DrawRepository draws;
  private final StockRepository stocks;
  private final RouteCodec codec;
  private final OutboxWriter out;

  public DrawRecoveryService(
      UserTransactionRunner tx, DrawRepository d, StockRepository s, RouteCodec c, OutboxWriter o) {
    this.tx = tx;
    draws = d;
    stocks = s;
    codec = c;
    out = o;
  }

  private com.jay.domain.task.service.WorkflowRedrive guarded;
  private final com.jay.domain.task.service.WorkflowRecoveryScan scanner =
      new com.jay.domain.task.service.WorkflowRecoveryScan();

  public DrawRecoveryService(
      UserTransactionRunner tx,
      DrawRepository d,
      StockRepository s,
      RouteCodec c,
      OutboxWriter o,
      com.jay.domain.task.service.WorkflowRedrive guarded) {
    this(tx, d, s, c, o);
    this.guarded = guarded;
  }

  public DrawRecoveryService scanBudget(int candidates, int emissions, Duration timeout) {
    if (guarded != null) scanner.budget(candidates, emissions, timeout);
    return this;
  }

  private Deadline deadline() {
    return new Deadline(Instant.now().plusSeconds(5));
  }

  public RecoveryDecision inspect(String order) {
    if (guarded != null) return inspectGuarded(order);
    var slot = codec.routeOrder(order);
    var before = tx.inSlot(slot, WorkClass.RECOVERY, deadline(), () -> draws.find(order, false));
    if (before.recoveryStatus() != RecoveryStatus.ACTIVE) return new RecoveryDecision(List.of());
    // A failed authority read leaves all HELD stock untouched and propagates for alerting.
    tx.inSlot(
        slot,
        WorkClass.RECOVERY,
        deadline(),
        () -> {
          draws.touch(order, before.objectVersion());
          return null;
        });
    var fact = tx.inControl(WorkClass.RECOVERY, deadline(), () -> stocks.fact(order));
    return tx.inSlot(
        slot,
        WorkClass.RECOVERY,
        deadline(),
        () -> {
          var r = draws.find(order, true);
          if (r.objectVersion() != before.objectVersion()
              || r.recoveryStatus() != RecoveryStatus.ACTIVE)
            return new RecoveryDecision(List.of());
          var events = new ArrayList<EventEnvelope>();
          switch (r.state()) {
            case "ACCEPTED" ->
                events.add(DrawService.event(r, "PrepareDraw", slot, codec.newEventId(), "{}"));
            case "CANDIDATE_FIXED" -> {
              if (fact != null && fact.state().equals("HELD"))
                events.add(receipt(r, fact, "ReservationHeld"));
              else if (fact != null && fact.state().equals("CANCELLED"))
                throw new AssetConflictException("Stock cancelled without user decision");
              else events.add(DrawProgressService.reserveEvent(r, codec));
            }
            case "RESULT_RECORDED" -> {
              if (fact != null && fact.state().equals("CONFIRMED"))
                events.add(receipt(r, fact, "ReservationConfirmed"));
              else events.add(DrawProgressService.confirmEvent(r, codec));
            }
            case "CANCELLING" -> {
              if (fact != null && fact.state().equals("CANCELLED"))
                events.add(receipt(r, fact, "ReservationCancelled"));
              else events.add(DrawProgressService.cancelEvent(r, codec));
            }
            case "COMMITTED" -> events.add(DrawProgressService.deliveryEvent(r, codec));
            default -> {}
          }
          events.forEach(out::append);
          draws.touch(r.orderId(), r.objectVersion());
          return new RecoveryDecision(events);
        });
  }

  private RecoveryDecision inspectGuarded(String order) {
    var slot = codec.routeOrder(order);
    var before = tx.inSlot(slot, WorkClass.RECOVERY, deadline(), () -> draws.find(order, false));
    if (before.recoveryStatus() != RecoveryStatus.ACTIVE) return new RecoveryDecision(List.of());
    // 无锁跨库读取；authority异常不触发资产或库存副作用。
    var fact =
        tx.inControl(
            WorkClass.RECOVERY,
            deadline(),
            () -> {
              if (stocks.recoveryStatus(order) != RecoveryStatus.ACTIVE)
                throw new IllegalStateException("Stock paused");
              return stocks.fact(order);
            });
    EventEnvelope command;
    try {
      command = allowed(before, fact, slot);
    } catch (AssetConflictException mismatch) {
      return tx.inSlot(
          slot,
          WorkClass.RECOVERY,
          deadline(),
          () -> {
            draws.lock(order);
            var current = draws.find(order, false);
            if (current.objectVersion() == before.objectVersion()
                && current.recoveryStatus() == RecoveryStatus.ACTIVE)
              guarded.conflict("DRAW", order, mismatch.getMessage());
            return new RecoveryDecision(List.of());
          });
    }
    if (command == null) return new RecoveryDecision(List.of());
    var remote = guarded.observe(command);
    return tx.inSlot(
        slot,
        WorkClass.RECOVERY,
        deadline(),
        () -> {
          draws.lock(order); // 账户/活动账户→订单，不反向取账户锁。
          var current = draws.find(order, false);
          if (current.objectVersion() != before.objectVersion()
              || current.recoveryStatus() != RecoveryStatus.ACTIVE
              || !current.state().equals(before.state())) return new RecoveryDecision(List.of());
          return guarded.commit(
              "DRAW",
              order,
              current.state(),
              current.objectVersion(),
              draws.lastProgressAt(order),
              command,
              remote);
        });
  }

  private EventEnvelope allowed(DrawRecord r, ReservationResult fact, RouteSlot slot) {
    if (fact != null) {
      if (!r.orderId().equals(fact.drawOrderId())
          || !r.userId().equals(fact.userId())
          || !r.identityHash().equals(fact.identityHash())
          || !r.requestHash().equals(fact.requestHash())
          || fact.quantity()
              != (fact.finalSnapshot() == null ? 0 : fact.finalSnapshot().quantity()))
        throw new AssetConflictException("Recovery stock identity mismatch");
      if (Set.of("HELD", "CONFIRMED").contains(fact.state())
          && !Objects.equals(fact.finalSnapshot(), r.candidate())
          && !Objects.equals(fact.finalSnapshot(), r.fallback()))
        throw new AssetConflictException("Recovery frozen award mismatch");
      if (Set.of("RESULT_RECORDED", "COMMITTED").contains(r.state())
          && !Objects.equals(r.finalAward(), fact.finalSnapshot()))
        throw new AssetConflictException("Recovery final award mismatch");
    }
    return switch (r.state()) {
      case "ACCEPTED" -> DrawService.event(r, "PrepareDraw", slot, codec.newEventId(), "{}");
      case "CANDIDATE_FIXED" -> {
        if (fact == null) yield DrawProgressService.reserveEvent(r, codec);
        if (fact.state().equals("HELD")) yield receipt(r, fact, "ReservationHeld");
        throw new AssetConflictException("Candidate authority contradicts user state");
      }
      case "RESULT_RECORDED" -> {
        if (fact != null && fact.state().equals("CONFIRMED"))
          yield receipt(r, fact, "ReservationConfirmed");
        if (fact != null && fact.state().equals("HELD"))
          yield DrawProgressService.confirmEvent(r, codec);
        throw new AssetConflictException("Recorded result lacks held stock authority");
      }
      case "CANCELLING" -> {
        if (fact != null && fact.state().equals("CANCELLED"))
          yield receipt(r, fact, "ReservationCancelled");
        if (fact == null || fact.state().equals("HELD"))
          yield DrawProgressService.cancelEvent(r, codec);
        throw new AssetConflictException("Confirmed stock cannot be cancelled");
      }
      case "COMMITTED" -> {
        if (fact != null && fact.state().equals("CONFIRMED"))
          yield DrawProgressService.deliveryEvent(r, codec);
        throw new AssetConflictException("Committed draw requires confirmed stock authority");
      }
      default -> null;
    };
  }

  private EventEnvelope receipt(DrawRecord r, ReservationResult fact, String type) {
    return DrawService.event(
        r, type, codec.routeUser(r.userId()), codec.newEventId(), ReceiptJson.write(fact));
  }

  public void tick() {
    if (guarded != null) {
      scanner.run(
          com.jay.domain.task.service.WorkflowRecoveryScan.userPartitions(
              List.of("ACCEPTED", "CANDIDATE_FIXED", "RESULT_RECORDED", "CANCELLING", "COMMITTED")),
          (p, c) ->
              tx.inSlot(
                  p.slot(),
                  WorkClass.RECOVERY,
                  deadline(),
                  () -> draws.scanStale(p.state(), c, guarded.cutoff(), 1)),
          id -> inspect(id).actions().size());
      return;
    }
    for (String db : List.of("01", "02"))
      for (int tb = 0; tb < 4; tb++) {
        var slot = new RouteSlot(db, String.format("%03d", tb));
        var ids = tx.inSlot(slot, WorkClass.RECOVERY, deadline(), () -> draws.scanActive(8));
        for (String id : ids)
          try {
            inspect(id);
          } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                .warn("Draw recovery unavailable for route {}", slot, e);
          }
      }
  }
}
