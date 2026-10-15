package com.jay.domain.raffle.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.repository.*;
import com.jay.domain.task.model.*;
import java.util.*;

public final class AwardDeliveryService {
  private final DrawRepository repo;
  private final AssetKernel assets;
  private final FaultInjector faults;

  public AwardDeliveryService(DrawRepository r, AssetKernel a, FaultInjector f) {
    repo = r;
    assets = a;
    faults = f;
  }

  public void deliver(EventEnvelope e) {
    var read = repo.find(e.aggregateId(), false);
    assets.lockAccounts(read.userId(), List.of(read.activityId()));
    var r = repo.find(e.aggregateId(), true);
    DrawProgressService.active(r);
    if (!r.userId().equals(e.userId()) || !r.requestHash().equals(e.requestHash()))
      throw new AssetConflictException("Delivery identity mismatch");
    if (r.state().equals("DELIVERED")) return;
    if (!r.state().equals("COMMITTED")) throw new AssetConflictException("Award not confirmed");
    var award = r.finalAward();
    if (award.type().equals("CREDIT"))
      assets.credit(
          r.userId(),
          new BusinessId("DRAW_AWARD", r.orderId(), award.configHash()),
          award.amount());
    else if (!award.type().equals("NONE"))
      throw new IllegalArgumentException("Unsupported award type");
    faults.hit("draw.after-award-credit");
    repo.delivered(r);
    if (!repo.update(r, r.transition("DELIVERED", r.candidate(), r.fallback(), r.finalAward())))
      throw new AssetConflictException("Delivery state changed");
  }
}
