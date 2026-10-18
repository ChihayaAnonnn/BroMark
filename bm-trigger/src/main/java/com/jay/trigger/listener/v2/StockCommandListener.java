package com.jay.trigger.listener.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.stock.service.*;
import com.jay.domain.task.model.*;
import java.util.*;

public final class StockCommandListener {
  private final StockReservationService stock;
  private final RouteCodec codec;

  public StockCommandListener(StockReservationService s, RouteCodec c) {
    stock = s;
    codec = c;
  }

  public List<EventEnvelope> apply(EventEnvelope e) {
    if (!"ReserveAward".equals(e.eventType()))
      throw new AssetConflictException("Wrong stock command");
    var c = ReceiptJson.read(e.payload(), ReserveCommand.class);
    identity(e, c.drawOrderId(), c.userId(), c.requestHash());
    var r = stock.confirmOnReserve(c);
    return List.of(
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
            ReceiptJson.write(r)));
  }

  private void identity(EventEnvelope e, String id, String user, String hash) {
    if (!id.equals(e.aggregateId()) || !user.equals(e.userId()) || !hash.equals(e.requestHash()))
      throw new AssetConflictException("Command envelope changed");
  }
}
