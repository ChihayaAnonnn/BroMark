package com.jay.domain.raffle.repository;

import com.jay.domain.asset.model.Deadline;
import com.jay.domain.strategy.repository.StrategyVersionRepository;

/** Read-only admission source used by the draw acceptance transaction. */
public interface DrawAdmissionSource {
  StrategyVersionRepository.DrawAdmission admit(String user, long activity, Deadline deadline);
}
