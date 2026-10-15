package com.jay.domain.strategy.repository;
import com.jay.domain.strategy.model.*;
public interface StrategyVersionRepository {void publish(FrozenStrategy strategy);FrozenStrategy load(long strategy,String version);FrozenStrategy current(long activity);}
