package com.jay.domain.raffle.model;

import com.jay.domain.asset.model.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.task.model.*;

public record DrawRecord(
    String orderId,
    String userId,
    long activityId,
    String requestHash,
    String identityHash,
    String version,
    BusinessTime time,
    RuleInputSnapshot ruleInput,
    FrozenStrategy strategy,
    String state,
    RecoveryStatus recoveryStatus,
    long objectVersion,
    AwardSnapshot candidate,
    AwardSnapshot fallback,
    AwardSnapshot finalAward) {
  public BusinessId biz() {
    return new BusinessId("DRAW", orderId, requestHash);
  }

  public DrawView view() {
    return new DrawView(
        orderId,
        state,
        recoveryStatus,
        java.util.Set.of("COMMITTED", "DELIVERED").contains(state) ? finalAward : null);
  }

  public DrawRecord transition(
      String stage, AwardSnapshot c, AwardSnapshot f, AwardSnapshot result) {
    return new DrawRecord(
        orderId,
        userId,
        activityId,
        requestHash,
        identityHash,
        version,
        time,
        ruleInput,
        strategy,
        stage,
        recoveryStatus,
        objectVersion + 1,
        c,
        f,
        result);
  }
}
