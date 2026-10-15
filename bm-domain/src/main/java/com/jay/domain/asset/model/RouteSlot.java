package com.jay.domain.asset.model;

public record RouteSlot(String dbKey, String tableKey) {
  public RouteSlot {
    if (!java.util.Set.of("00", "01", "02").contains(dbKey)
        || ("00".equals(dbKey)
            ? tableKey != null
            : tableKey == null || !tableKey.matches("00[0-3]")))
      throw new IllegalArgumentException("Invalid route slot");
  }

  public static RouteSlot control() {
    return new RouteSlot("00", null);
  }
}
