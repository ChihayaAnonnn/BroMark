package com.jay.trigger.job.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.repository.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.repository.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;

/** HELD age is diagnostic, never an authority to release stock. */
public final class StockRecoveryJob {
  private final UserTransactionRunner tx;
  private final StockRepository stocks;
  private final DrawRepository draws;
  private final RouteCodec codec;
  private final OrderIdGenerator ids;
  private final OutboxWriter out;

  public StockRecoveryJob(
      UserTransactionRunner t,
      StockRepository s,
      DrawRepository d,
      RouteCodec c,
      OrderIdGenerator i,
      OutboxWriter o) {
    tx = t;
    stocks = s;
    draws = d;
    codec = c;
    ids = i;
    out = o;
  }

  private Deadline deadline() {
    return new Deadline(Instant.now().plusSeconds(5));
  }

  private CoalescingJobDispatcher dispatcher;

  public StockRecoveryJob dispatchWith(CoalescingJobDispatcher dispatcher) {
    this.dispatcher = dispatcher;
    return this;
  }

  @Scheduled(fixedDelayString = "${bm.v2.raffle.recovery-delay-ms:10000}")
  public void tick() {
    if (dispatcher == null) runWork();
    else dispatcher.submit("stock", this::runWork);
  }

  private void runWork() {
    ids.prefetch();
    var work = tx.inControl(WorkClass.RECOVERY, deadline(), () -> stocks.scanRecovery(8));
    for (String order : work)
      try {
        inspect(order);
      } catch (RuntimeException e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Stock authority unavailable; reservation retained", e);
      }
  }

  public void inspect(String order) {
    tx.inControl(
        WorkClass.RECOVERY,
        deadline(),
        () -> {
          stocks.touchRecovery(order);
          return null;
        });
    var slot = codec.routeOrder(order);
    var user = tx.inSlot(slot, WorkClass.RECOVERY, deadline(), () -> draws.find(order, false));
    if (user.recoveryStatus() != RecoveryStatus.ACTIVE) return;
    tx.inControl(
        WorkClass.RECOVERY,
        deadline(),
        () -> {
          var a = stocks.lock(order);
          if (a.recoveryStatus() != RecoveryStatus.ACTIVE) return null;
          var fact = stocks.fact(order);
          String type = null;
          if (fact.state().equals("HELD") && user.state().equals("CANDIDATE_FIXED"))
            type = "ReservationHeld";
          else if (fact.state().equals("CONFIRMED") && user.state().equals("RESULT_RECORDED"))
            type = "ReservationConfirmed";
          else if (fact.state().equals("CANCELLED") && user.state().equals("CANCELLING"))
            type = "ReservationCancelled";
          if (type != null)
            out.append(
                DrawService.event(user, type, slot, codec.newEventId(), ReceiptJson.write(fact)));
          else if (fact.state().equals("HELD") && user.state().equals("CANCELLING"))
            out.append(DrawProgressService.cancelEvent(user, codec));
          else if (fact.state().equals("HELD") && user.state().equals("RESULT_RECORDED"))
            out.append(DrawProgressService.confirmEvent(user, codec));
          return null;
        });
  }
}
