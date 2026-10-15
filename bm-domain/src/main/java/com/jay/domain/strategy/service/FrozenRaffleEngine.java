package com.jay.domain.strategy.service;

import com.jay.domain.asset.service.RequestHasher;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.service.armory.algorithm.impl.AliasMethodAlgorithm;
import java.math.*;
import java.util.*;

public final class FrozenRaffleEngine {
  public AwardSnapshot choose(FrozenStrategy f, RuleInputSnapshot input, Random r) {
    var prize = f.fallback();
    if (input.blacklisted()) {
      if (f.blacklistAwardId() != null)
        prize =
            f.prizes().stream()
                .filter(p -> p.awardId() == f.blacklistAwardId())
                .findFirst()
                .orElseThrow();
    } else {
      String key =
          f.weightedPools().keySet().stream()
              .filter(k -> k <= input.totalEffectiveCount())
              .max(Long::compare)
              .map(Object::toString)
              .orElse("default");
      var table = f.tables().get(key);
      int column = r.nextInt(table.ids().size());
      int selected =
          r.nextDouble() < table.probabilities().get(column) ? column : table.aliases().get(column);
      int id = table.ids().get(selected);
      prize = f.prizes().stream().filter(p -> p.awardId() == id).findFirst().orElseThrow();
      if (input.dayEffectiveCount() < prize.unlockAfter()) prize = f.fallback();
    }
    return snapshot(prize, f.version(), r);
  }

  public AwardSnapshot fallback(FrozenStrategy f, Random r) {
    return snapshot(f.fallback(), f.version(), r);
  }

  public static AwardSnapshot snapshot(PublishStrategyCommand.Prize p, String v, Random r) {
    BigDecimal amount = null;
    if (p.type().equals("CREDIT")) {
      long min = p.minAmount().movePointRight(2).longValueExact(),
          max = p.maxAmount().movePointRight(2).longValueExact();
      amount = BigDecimal.valueOf(min + (min == max ? 0 : r.nextLong(max - min + 1)), 2);
    }
    String hash =
        new RequestHasher()
            .hash(
                "AWARD",
                Map.of(
                    "version",
                    v,
                    "award",
                    Integer.toString(p.awardId()),
                    "type",
                    p.type(),
                    "amount",
                    amount == null ? "" : amount.toPlainString(),
                    "pool",
                    p.stockPoolId() == null ? "" : p.stockPoolId()));
    return new AwardSnapshot(
        p.awardId(), p.type(), amount, p.stockPoolId(), p.stockPoolId() == null ? 0 : 1, hash);
  }
}
