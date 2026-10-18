package com.jay.domain.raffle.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.repository.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.task.model.*;
import java.util.*;

/** Caller owns the user transaction; one confirmation delivers the award exactly once. */
public final class DrawDeliveryService {
  private final DrawRepository repo;
  private final AssetKernel assets;

  public DrawDeliveryService(DrawRepository r, AssetKernel a) {
    repo = r;
    assets = a;
  }

  public void deliver(EventEnvelope e) {
    var result = DrawProgressService.parse(e);
    if (!"CONFIRMED".equals(result.state()))
      throw new AssetConflictException("Not a confirmed receipt");
    var read = repo.find(e.aggregateId(), false);
    assets.lockAccounts(read.userId(), List.of(read.activityId()));
    var r = repo.find(e.aggregateId(), true);
    DrawProgressService.active(r);
    DrawProgressService.identity(r, result, e);
    if (r.state().equals("DELIVERED")) return;
    if (!r.state().equals("PENDING_RESERVATION"))
      throw new AssetConflictException("Delivery at wrong stage");
    var award = result.finalSnapshot();
    if (award == null) throw new AssetConflictException("Missing delivery award");
    if (!Objects.equals(award, r.candidate()) && !Objects.equals(award, r.fallback()))
      throw new AssetConflictException("Delivery award mismatch");
    if (award.type().equals("CREDIT"))
      assets.credit(
          r.userId(),
          new BusinessId("DRAW_AWARD", r.orderId(), award.configHash()),
          award.amount());
    else if (!award.type().equals("NONE"))
      throw new IllegalArgumentException("Unsupported award type");
    var delivered = r.transition("DELIVERED", r.candidate(), r.fallback(), award);
    repo.delivered(delivered);
    if (!repo.update(r, delivered)) throw new AssetConflictException("Delivery state changed");
  }
}
