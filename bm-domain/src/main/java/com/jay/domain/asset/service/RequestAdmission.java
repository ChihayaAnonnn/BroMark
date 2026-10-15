package com.jay.domain.asset.service;

import com.jay.domain.asset.model.*;

public interface RequestAdmission {
  Permit enter(String userId, Deadline deadline);

  default Permit cancel(String userId, Deadline deadline) {
    throw new UnsupportedOperationException("Cancellation admission required");
  }

  Permit query(String userId, Deadline deadline);

  void checkActivity(String activityKey, Deadline deadline);
}
