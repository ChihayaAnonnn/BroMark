package com.jay.test.v2.support;

public final class FaultInjector implements com.jay.domain.asset.service.FaultInjector {
  private volatile String point;

  public void failAt(String point) {
    this.point = point;
  }

  public void hit(String point) {
    if (point.equals(this.point)) throw new IllegalStateException("Injected failure: " + point);
  }
}
