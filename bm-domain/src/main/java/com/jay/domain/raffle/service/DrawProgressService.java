package com.jay.domain.raffle.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.repository.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import java.util.*;

public final class DrawProgressService {
  private final DrawRepository repo;
  private final AssetKernel assets;
  private final QuotaKernel quota;
  private final RouteCodec codec;
  private final OutboxWriter out;

  public DrawProgressService(
      DrawRepository r, AssetKernel a, QuotaKernel q, RouteCodec c, OutboxWriter o) {
    repo = r;
    assets = a;
    quota = q;
    codec = c;
    out = o;
  }

  public static ReservationResult parse(EventEnvelope e) {
    try {
      return new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .readValue(e.payload(), ReservationResult.class);
    } catch (Exception x) {
      throw new AssetConflictException("Invalid reservation receipt");
    }
  }

  public void onHeld(EventEnvelope e) {
    handle(e).forEach(out::append);
  }

  public void onConfirmed(EventEnvelope e) {
    handle(e).forEach(out::append);
  }

  public void onCancelled(EventEnvelope e) {
    handle(e).forEach(out::append);
  }

  public List<EventEnvelope> handle(EventEnvelope e) {
    var read = repo.find(e.aggregateId(), false);
    assets.lockAccounts(read.userId(), List.of(read.activityId()));
    var r = repo.find(e.aggregateId(), true);
    active(r);
    var result = parse(e);
    identity(r, result, e);
    String expected =
        switch (e.eventType()) {
          case "ReservationHeld" -> "HELD";
          case "ReservationConfirmed" -> "CONFIRMED";
          case "ReservationCancelled" -> "CANCELLED";
          default -> throw new AssetConflictException("Wrong receipt type");
        };
    if (!expected.equals(result.state()))
      throw new AssetConflictException("Receipt state mismatch");
    if (expected.equals("HELD")) {
      validateFinal(r, result.finalSnapshot());
      if (r.state().equals("CANCELLING")) return List.of(cancelEvent(r, codec));
      if (Set.of("RESULT_RECORDED", "COMMITTED", "DELIVERED").contains(r.state())) {
        if (!Objects.equals(r.finalAward(), result.finalSnapshot()))
          throw new AssetConflictException("Final changed");
        return r.state().equals("RESULT_RECORDED") ? List.of(confirmEvent(r, codec)) : List.of();
      }
      if (!r.state().equals("CANDIDATE_FIXED"))
        throw new AssetConflictException("Held at wrong stage");
      var next =
          r.transition("RESULT_RECORDED", r.candidate(), r.fallback(), result.finalSnapshot());
      advance(r, next);
      repo.award(next, "PENDING_CONFIRM");
      return List.of(confirmEvent(next, codec));
    }
    if (expected.equals("CONFIRMED")) {
      if (!Objects.equals(r.finalAward(), result.finalSnapshot()))
        throw new AssetConflictException("Confirmation changed");
      if (Set.of("COMMITTED", "DELIVERED").contains(r.state()))
        return r.state().equals("COMMITTED") ? List.of(deliveryEvent(r, codec)) : List.of();
      if (!r.state().equals("RESULT_RECORDED"))
        throw new AssetConflictException("Confirmation at wrong stage");
      var next = r.transition("COMMITTED", r.candidate(), r.fallback(), r.finalAward());
      advance(r, next);
      repo.award(next, "PENDING_DELIVERY");
      return List.of(deliveryEvent(next, codec));
    }
    if (r.state().equals("CANCELLED")) return List.of();
    if (!r.state().equals("CANCELLING")) throw new AssetConflictException("Cancel at wrong stage");
    quota.refund(r.userId(), r.activityId(), r.biz(), r.time());
    advance(r, r.transition("CANCELLED", r.candidate(), r.fallback(), null));
    return List.of();
  }

  private void advance(DrawRecord old, DrawRecord next) {
    if (!repo.update(old, next)) throw new AssetConflictException("Concurrent stage change");
  }

  public static void active(DrawRecord r) {
    if (r.recoveryStatus() != RecoveryStatus.ACTIVE)
      throw new AssetConflictException("Draw paused");
  }

  public static void identity(DrawRecord r, ReservationResult s, EventEnvelope e) {
    if (!r.orderId().equals(s.drawOrderId())
        || !r.userId().equals(s.userId())
        || !r.identityHash().equals(s.identityHash())
        || !r.requestHash().equals(s.requestHash())
        || !r.userId().equals(e.userId())
        || !r.requestHash().equals(e.requestHash())
        || s.quantity() != (s.finalSnapshot() == null ? 0 : s.finalSnapshot().quantity()))
      throw new AssetConflictException("Receipt identity mismatch");
  }

  private static void validateFinal(DrawRecord r, AwardSnapshot a) {
    if (!Objects.equals(a, r.candidate()) && !Objects.equals(a, r.fallback()))
      throw new AssetConflictException("Receipt award mismatch");
  }

  public static EventEnvelope cancelEvent(DrawRecord r, RouteCodec codec) {
    return DrawService.event(
        r,
        "CancelReservation",
        RouteSlot.control(),
        codec.newEventId(),
        ReceiptJson.write(
            new CancelCommand(r.orderId(), r.userId(), r.identityHash(), r.requestHash())));
  }

  public static EventEnvelope confirmEvent(DrawRecord r, RouteCodec codec) {
    return DrawService.event(
        r,
        "ConfirmReservation",
        RouteSlot.control(),
        codec.newEventId(),
        ReceiptJson.write(
            new ConfirmCommand(
                r.orderId(),
                r.userId(),
                r.identityHash(),
                r.requestHash(),
                r.finalAward().configHash())));
  }

  public static EventEnvelope reserveEvent(DrawRecord r, RouteCodec codec) {
    return DrawService.event(
        r,
        "ReserveAward",
        RouteSlot.control(),
        codec.newEventId(),
        ReceiptJson.write(
            new ReserveCommand(
                r.orderId(),
                r.userId(),
                r.identityHash(),
                r.requestHash(),
                r.strategy().strategyId(),
                r.version(),
                r.candidate(),
                r.fallback())));
  }

  public static EventEnvelope deliveryEvent(DrawRecord r, RouteCodec codec) {
    return DrawService.event(
        r, "AwardDelivery", codec.routeUser(r.userId()), codec.newEventId(), "{}");
  }
}
