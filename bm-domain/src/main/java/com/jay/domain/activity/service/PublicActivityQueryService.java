package com.jay.domain.activity.service;

import com.jay.domain.strategy.model.FrozenStrategy;
import java.util.function.LongFunction;

public final class PublicActivityQueryService {
  private final LongFunction<FrozenStrategy> load;

  public PublicActivityQueryService(LongFunction<FrozenStrategy> l) {
    load = l;
  }

  public FrozenStrategy current(long id) {
    if (id <= 0) throw new IllegalArgumentException("Activity required");
    return load.apply(id);
  }
}
