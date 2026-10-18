package com.jay.domain.task.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.RouteCodec;
import com.jay.domain.task.model.EventEnvelope;
import java.util.Set;

public final class EventRouting {
  public static final Set<String> CONTROL = Set.of("ReserveAward");
  public static final Set<String> USER = Set.of("ReservationConfirmed");
  public static final Set<String> NOTIFICATIONS = Set.of("ExchangeCompleted", "SignInCompleted");
  private final RouteCodec codec;

  public EventRouting(RouteCodec codec) {
    this.codec = codec;
  }

  public static String aggregateType(EventEnvelope e) {
    return switch (e.eventType()) {
      case "ReserveAward", "ConfirmReservation", "CancelReservation" -> "RESERVATION";
      case "CreditGranted" -> "PAYMENT";
      case "PrepareDraw",
          "ReservationHeld",
          "ReservationConfirmed",
          "ReservationCancelled",
          "AwardDelivery" ->
          "DRAW";
      case "GrantCredit" -> "CREDIT_DELIVERY";
      case "ExchangeCompleted", "SignInCompleted" -> "NOTIFICATION";
      default -> "UNKNOWN_EVENT";
    };
  }

  public RouteSlot route(EventEnvelope e) {
    if (e.schemaVersion() != 1 && e.schemaVersion() != 2)
      throw new IllegalArgumentException("Unknown schema");
    RouteSlot expected;
    if (CONTROL.contains(e.eventType())) expected = RouteSlot.control();
    else if (USER.contains(e.eventType()) || NOTIFICATIONS.contains(e.eventType()))
      expected = codec.routeUser(e.userId());
    else throw new IllegalArgumentException("Unknown event type");
    if (!expected.equals(e.routeSlot()))
      throw new IllegalArgumentException("Destination slot does not match event type/user");
    if (!e.requestHash().equals(e.bizId().hash()))
      throw new IllegalArgumentException("Business digest mismatch");
    return expected;
  }
}
