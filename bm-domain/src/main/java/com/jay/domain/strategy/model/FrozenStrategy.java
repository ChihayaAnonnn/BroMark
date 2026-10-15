package com.jay.domain.strategy.model;

import com.jay.domain.strategy.service.armory.algorithm.AliasTable;
import java.time.*;
import java.util.*;

public record FrozenStrategy(
    long activityId,
    long strategyId,
    String version,
    String state,
    Instant beginsAt,
    Instant endsAt,
    List<PublishStrategyCommand.Prize> prizes,
    PublishStrategyCommand.Prize fallback,
    Map<Long, List<Integer>> weightedPools,
    Integer blacklistAwardId,
    String blacklistVersion,
    Map<String, Table> tables) {
  public FrozenStrategy {
    prizes = List.copyOf(prizes);
    var copy = new TreeMap<Long, List<Integer>>();
    weightedPools.forEach((k, v) -> copy.put(k, List.copyOf(v)));
    weightedPools = Collections.unmodifiableMap(copy);
    tables = Map.copyOf(tables);
  }

  public record Table(List<Integer> ids, List<Double> probabilities, List<Integer> aliases) {
    public Table {
      ids = List.copyOf(ids);
      probabilities = List.copyOf(probabilities);
      aliases = List.copyOf(aliases);
    }

    public AliasTable alias() {
      return new AliasTable(
          ids.stream().mapToInt(x -> x).toArray(),
          probabilities.stream().mapToDouble(x -> x).toArray(),
          aliases.stream().mapToInt(x -> x).toArray());
    }
  }
}
