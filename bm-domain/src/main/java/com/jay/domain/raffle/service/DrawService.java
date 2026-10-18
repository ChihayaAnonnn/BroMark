package com.jay.domain.raffle.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.repository.*;
import com.jay.domain.stock.model.ReserveCommand;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.repository.StrategyVersionRepository;
import com.jay.domain.strategy.service.FrozenRaffleEngine;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import com.jay.domain.task.service.RelayObservation;
import java.time.*;
import java.util.*;

public final class DrawService {
  private final UserTransactionRunner tx;
  private final AssetKernel assets;
  private final QuotaKernel quota;
  private final AssetRepository accounts;
  private final DrawRepository repo;
  private final RouteCodec codec;
  private final OrderIdGenerator ids;
  private final OutboxWriter out;
  private final FaultInjector faults;
  private final RelayObservation observation;
  private final DrawAdmissionSource admission;
  private final FrozenRaffleEngine engine;

  public DrawService(
      UserTransactionRunner tx,
      AssetKernel assets,
      QuotaKernel quota,
      AssetRepository accounts,
      DrawRepository repo,
      RouteCodec codec,
      OrderIdGenerator ids,
      OutboxWriter out,
      FaultInjector faults,
      RelayObservation observation,
      DrawAdmissionSource admission,
      FrozenRaffleEngine engine) {
    this.tx = tx;
    this.assets = assets;
    this.quota = quota;
    this.accounts = accounts;
    this.repo = repo;
    this.codec = codec;
    this.ids = ids;
    this.out = out;
    this.faults = faults;
    this.observation = observation;
    this.admission = admission;
    this.engine = engine;
  }

  public DrawView create(DrawCommand c) {
    if (c.activityId() <= 0) throw new IllegalArgumentException("Activity required");
    var identity =
        new RequestIdentity(
            c.userId(),
            "DRAW",
            c.requestId(),
            new RequestHasher().hash("DRAW", Map.of("activity", Long.toString(c.activityId()))));
    var old = tx.inUser(c.userId(), WorkClass.TRADE, c.deadline(), () -> repo.request(identity));
    if (old.isPresent()) return get(c.userId(), old.get());
    long configStart = System.nanoTime();
    String configOutcome = "error";
    StrategyVersionRepository.DrawAdmission admitted;
    try {
      admitted = admission.admit(c.userId(), c.activityId(), c.deadline());
      configOutcome = "success";
    } finally {
      observation.duration(
          "config",
          RouteSlot.control(),
          "PrepareDraw",
          configOutcome,
          System.nanoTime() - configStart);
    }
    var f = admitted.strategy();
    ids.prefetch();
    String order = codec.newOrderId("DR", c.userId());
    String event = codec.newEventId();
    try (var observed = com.jay.domain.task.service.RelayObservationContext.asset("PrepareDraw")) {
      return tx.inUser(
          c.userId(),
          WorkClass.TRADE,
          c.deadline(),
          () -> {
            var time = assets.lockAccounts(c.userId(), List.of(c.activityId()));
            var repeat = repo.request(identity);
            if (repeat.isPresent()) return repo.find(repeat.get(), false).view();
            String hash =
                new RequestHasher()
                    .hash(
                        "DRAW_IDENTITY",
                        Map.of(
                            "user",
                            c.userId(),
                            "order",
                            order,
                            "activity",
                            Long.toString(c.activityId()),
                            "version",
                            f.version(),
                            "request",
                            identity.requestHash()));
            var biz = new BusinessId("DRAW", order, identity.requestHash());
            boolean open =
                "OPEN".equals(f.state())
                    && !time.instant().isBefore(f.beginsAt())
                    && time.instant().isBefore(f.endsAt());
            if (time.instant().isAfter(admitted.expiresAt()))
              throw new BudgetExceededException("Admission expired");
            boolean accepted =
                open && quota.consume(c.userId(), c.activityId(), biz, time).isPresent();
            faults.hit("draw.after-consume");
            var state = accounts.quotaState(c.userId(), c.activityId(), time);
            var input =
                new RuleInputSnapshot(
                    time.instant(),
                    state.effective(),
                    state.dayEffective(),
                    admitted.blacklisted(),
                    f.blacklistVersion());
            AwardSnapshot candidate = null;
            AwardSnapshot fallback = null;
            if (accepted) {
              candidate = engine.choose(f, input, new Random());
              fallback = engine.fallback(f, new Random());
            }
            var r =
                new DrawRecord(
                    order,
                    c.userId(),
                    c.activityId(),
                    identity.requestHash(),
                    hash,
                    f.version(),
                    time,
                    input,
                    f,
                    accepted ? "PENDING_RESERVATION" : "REJECTED",
                    RecoveryStatus.ACTIVE,
                    0,
                    candidate,
                    fallback,
                    null);
            repo.insert(identity, r);
            if (accepted) {
              var command =
                  new ReserveCommand(
                      order,
                      c.userId(),
                      hash,
                      identity.requestHash(),
                      f.strategyId(),
                      f.version(),
                      candidate,
                      fallback);
              out.append(
                  event(r, "ReserveAward", RouteSlot.control(), event, ReceiptJson.write(command)));
            }
            return r.view();
          });
    }
  }

  public static EventEnvelope event(
      DrawRecord r, String type, RouteSlot slot, String id, String payload) {
    return new EventEnvelope(
        id,
        type,
        r.orderId(),
        r.biz(),
        r.userId(),
        slot,
        r.requestHash(),
        r.objectVersion(),
        "ReserveAward".equals(type) ? 2 : 1,
        payload);
  }

  public DrawView get(String user, String order) {
    if (!codec.routeOrder(order).equals(codec.routeUser(user))) throw new AssetOwnershipException();
    return tx.inUser(
        user,
        WorkClass.QUERY,
        new Deadline(Instant.now().plusSeconds(5)),
        () -> repo.view(user, order));
  }

  public DrawView cancel(String user, String order) {
    if (!codec.routeOrder(order).equals(codec.routeUser(user))) throw new AssetOwnershipException();
    tx.inUser(
        user,
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(5)),
        () -> repo.view(user, order));
    ids.prefetch();
    return tx.inUser(
        user,
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(5)),
        () -> {
          var read = repo.find(order, false);
          if (!read.userId().equals(user)) throw new AssetOwnershipException();
          assets.lockAccounts(user, List.of(read.activityId()));
          var r = repo.find(order, true);
          DrawProgressService.active(r);
          if (Set.of("CANCELLED", "CANCELLING").contains(r.state())) return r.view();
          if (!Set.of("ACCEPTED", "CANDIDATE_FIXED").contains(r.state()))
            throw new AssetConflictException("Cancellation boundary passed");
          var next = r.transition("CANCELLING", r.candidate(), r.fallback(), null);
          if (!repo.update(r, next)) throw new AssetConflictException("Concurrent cancellation");
          out.append(DrawProgressService.cancelEvent(next, codec));
          return next.view();
        });
  }
}
