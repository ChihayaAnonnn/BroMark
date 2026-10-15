package com.jay.domain.asset.service;

import com.jay.domain.asset.model.RouteSlot;

public interface OrderIdGenerator {
  String next(String type, RouteSlot slot);

  void prefetch();
}
