package com.jay.domain.asset.service;

import com.jay.domain.asset.model.*;

public interface RequestAdmission {
  Permit enter(String userId, Deadline deadline);

  Permit query(String userId, Deadline deadline);

  void checkActivity(String activityKey, Deadline deadline);
}
