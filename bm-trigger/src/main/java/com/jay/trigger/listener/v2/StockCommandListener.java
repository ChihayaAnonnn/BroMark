package com.jay.trigger.listener.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.stock.service.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.RelayObservation;
import java.util.*;

public final class StockCommandListener {
  private final StockReservationService stock;
  private final RouteCodec codec;
  private final RelayObservation observation;

  public StockCommandListener(StockReservationService s, RouteCodec c) {
    this(s, c, RelayObservation.NOOP);
  }

  public StockCommandListener(StockReservationService s, RouteCodec c, RelayObservation observation) {
    stock = s;
    codec = c;
    this.observation = Objects.requireNonNull(observation);
  }

  public List<EventEnvelope> apply(EventEnvelope e) {
    long start = System.nanoTime();
    String outcome = "error";
    try {
      if (!"ReserveAward".equals(e.eventType()))
        throw new AssetConflictException("Wrong stock command");
      var c = ReceiptJson.read(e.payload(), ReserveCommand.class);
      identity(e, c.drawOrderId(), c.userId(), c.requestHash());
      var r = stock.confirmOnReserve(c);
      var confirmed =
          new EventEnvelope(
              codec.newEventId(),
              "ReservationConfirmed",
              r.drawOrderId(),
              new BusinessId("DRAW", r.drawOrderId(), r.requestHash()),
              r.userId(),
              codec.routeUser(r.userId()),
              r.requestHash(),
              e.aggregateVersion(),
              2,
              ReceiptJson.write(r));
      // 事务内的 P0 指标由 MicrometerRelayObservation 延迟到 afterCommit 记录。
      observation.p0Event("ReservationConfirmed", "emitted");
      outcome = "success";
      return List.of(confirmed);
    } finally {
      observation.p0Stage("reserve", outcome, System.nanoTime() - start);
    }
  }

  private void identity(EventEnvelope e, String id, String user, String hash) {
    if (!id.equals(e.aggregateId()) || !user.equals(e.userId()) || !hash.equals(e.requestHash()))
      throw new AssetConflictException("Command envelope changed");
  }
}
