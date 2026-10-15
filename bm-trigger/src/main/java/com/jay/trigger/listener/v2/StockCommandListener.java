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
    ReservationResult r =
        switch (e.eventType()) {
          case "ReserveAward" -> {
            var c = ReceiptJson.read(e.payload(), ReserveCommand.class);
            identity(e, c.drawOrderId(), c.userId(), c.requestHash());
            yield stock.reserve(c);
          }
          case "ConfirmReservation" -> {
            var c = ReceiptJson.read(e.payload(), ConfirmCommand.class);
            identity(e, c.drawOrderId(), c.userId(), c.requestHash());
            yield stock.confirm(c);
          }
          case "CancelReservation" -> {
            var c = ReceiptJson.read(e.payload(), CancelCommand.class);
            identity(e, c.drawOrderId(), c.userId(), c.requestHash());
            yield stock.cancel(c);
          }
          default -> throw new AssetConflictException("Wrong stock command");
        };
    String type =
        switch (r.state()) {
          case "HELD" -> "ReservationHeld";
          case "CONFIRMED" -> "ReservationConfirmed";
          case "CANCELLED" -> "ReservationCancelled";
          default -> throw new AssetConflictException("Unknown reservation state");
        };
    return List.of(
        new EventEnvelope(
            codec.newEventId(),
            type,
            r.drawOrderId(),
            new BusinessId("DRAW", r.drawOrderId(), r.requestHash()),
            r.userId(),
            codec.routeUser(r.userId()),
            r.requestHash(),
            e.aggregateVersion(),
            1,
            ReceiptJson.write(r)));
  }

  private void identity(EventEnvelope e, String id, String user, String hash) {
    if (!id.equals(e.aggregateId()) || !user.equals(e.userId()) || !hash.equals(e.requestHash()))
      throw new AssetConflictException("Command envelope changed");
  }
}
