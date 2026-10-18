package com.jay.config;

import com.jay.domain.asset.model.WorkClass;
import java.time.Duration;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bm.v2.budget")
public class V2BudgetProperties {
  private Map<String, String> physical =
      new HashMap<>(Map.of("00", "mysql-it", "01", "mysql-it", "02", "mysql-it"));
  private Map<String, Map<WorkClass, Integer>> instances =
      new HashMap<>(
          Map.of(
              "mysql-it",
              new EnumMap<>(
                  Map.of(
                      WorkClass.TRADE,
                      2,
                      WorkClass.CALLBACK,
                      1,
                      WorkClass.QUERY,
                      1,
                      WorkClass.RELAY,
                      1,
                      WorkClass.WORKER,
                      1,
                      WorkClass.RECOVERY,
                      1))));

  private Duration tradePermitMaxWait = Duration.ofSeconds(1);

  public Duration getTradePermitMaxWait() {
    return tradePermitMaxWait;
  }

  public void setTradePermitMaxWait(Duration value) {
    tradePermitMaxWait = value;
  }

  private int replicaCount = 1;
  private int reservedConnections = 20;
  private Map<String, Integer> physicalConnectionLimits = new HashMap<>(Map.of("mysql-it", 100));
  private int workerConcurrency = 8;
  private int prefetch = 16;

  public int getReplicaCount() {
    return replicaCount;
  }

  public void setReplicaCount(int value) {
    replicaCount = value;
  }

  public int getReservedConnections() {
    return reservedConnections;
  }

  public void setReservedConnections(int value) {
    reservedConnections = value;
  }

  public Map<String, Integer> getPhysicalConnectionLimits() {
    return physicalConnectionLimits;
  }

  public void setPhysicalConnectionLimits(Map<String, Integer> value) {
    physicalConnectionLimits = value;
  }

  public int getWorkerConcurrency() {
    return workerConcurrency;
  }

  public void setWorkerConcurrency(int value) {
    workerConcurrency = value;
  }

  public int getPrefetch() {
    return prefetch;
  }

  public void setPrefetch(int value) {
    prefetch = value;
  }

  public void validateConnections(Map<String, Integer> logicalPoolSizes) {
    if (replicaCount < 1
        || reservedConnections < 1
        || workerConcurrency < 1
        || workerConcurrency > 256
        || prefetch < 1
        || prefetch > 256) throw new IllegalArgumentException("Invalid replica/resource budget");
    if (!logicalPoolSizes.keySet().equals(physical.keySet()))
      throw new IllegalArgumentException("Every logical pool must be measured");
    var aggregated = new HashMap<String, Long>();
    logicalPoolSizes.forEach(
        (logical, size) -> {
          if (size == null || size < 1) throw new IllegalArgumentException("Invalid pool size");
          aggregated.merge(
              physical.get(logical), Math.multiplyExact((long) size, replicaCount), Math::addExact);
        });
    instances.forEach(
        (instance, limits) -> {
          long permits = limits.values().stream().mapToLong(Integer::longValue).sum();
          logicalPoolSizes.forEach(
              (logical, size) -> {
                if (instance.equals(physical.get(logical)) && permits >= size)
                  throw new IllegalArgumentException(
                      "Budget total must leave spare capacity in every logical pool: " + logical);
              });
        });
    aggregated.forEach(
        (instance, total) -> {
          var limit = physicalConnectionLimits.get(instance);
          if (limit == null || total + reservedConnections > limit)
            throw new IllegalArgumentException(
                "All replica pools exceed physical connection budget: " + instance);
        });
  }

  public Map<String, String> getPhysical() {
    return physical;
  }

  public void setPhysical(Map<String, String> value) {
    physical = value;
  }

  public Map<String, Map<WorkClass, Integer>> getInstances() {
    return instances;
  }

  public void setInstances(Map<String, Map<WorkClass, Integer>> value) {
    instances = value;
  }
}
