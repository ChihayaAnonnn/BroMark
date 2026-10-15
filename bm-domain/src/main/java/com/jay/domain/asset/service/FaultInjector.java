package com.jay.domain.asset.service;

@FunctionalInterface
public interface FaultInjector {
  void hit(String point);
}
