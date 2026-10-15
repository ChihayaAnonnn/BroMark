package com.jay.domain.asset.service;

import com.jay.domain.asset.model.RouteSlot;

public final class RouteCodec {
  private final int dbCount, tbCount;

  public RouteCodec(int dbCount, int tbCount) {
    if (dbCount != 2 || tbCount != 4)
      throw new IllegalArgumentException("Only existing 2x4 layout is supported");
    this.dbCount = dbCount;
    this.tbCount = tbCount;
  }

  public RouteSlot routeUser(String user) {
    if (user == null || user.isBlank()) throw new IllegalArgumentException("User is required");
    int hash = user.hashCode(), slot = (dbCount * tbCount - 1) & (hash ^ (hash >>> 16));
    return new RouteSlot(
        String.format("%02d", slot / tbCount + 1), String.format("%03d", slot % tbCount));
  }

  public RouteSlot routeOrder(String order) {
    if (order == null || !order.matches("[A-Z][A-Z0-9_]*-0[12]\\.00[0-3]-[A-Za-z0-9]+-[1-9][0-9]*"))
      throw new IllegalArgumentException("Invalid v2 order identity");
    String slot = order.split("-")[1];
    return new RouteSlot(slot.substring(0, 2), slot.substring(3));
  }
}
