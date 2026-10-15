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

  private Deadline deadline() {
    return new Deadline(Instant.now().plusSeconds(5));
  }

  public RecoveryDecision inspect(String order) {
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

  private EventEnvelope receipt(DrawRecord r, ReservationResult fact, String type) {
    return DrawService.event(
        r, type, codec.routeUser(r.userId()), codec.newEventId(), ReceiptJson.write(fact));
  }

  public void tick() {
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
