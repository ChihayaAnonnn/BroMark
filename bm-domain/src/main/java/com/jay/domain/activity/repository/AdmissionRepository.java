package com.jay.domain.activity.repository;

import com.jay.domain.asset.model.*;
import java.time.LocalDate;

public interface AdmissionRepository {
  default AdmissionSnapshot draw(String user, long activity, Deadline deadline) {
    throw new UnsupportedOperationException("Draw admission unavailable");
  }

  AdmissionSnapshot exchange(String user, long sku, String version, Deadline deadline);

  AdmissionSnapshot signIn(String user, LocalDate date, Deadline deadline);
}
