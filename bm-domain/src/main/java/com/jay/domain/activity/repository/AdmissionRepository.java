package com.jay.domain.activity.repository;

import com.jay.domain.asset.model.*;
import java.time.LocalDate;

public interface AdmissionRepository {
  AdmissionSnapshot exchange(String user, long sku, String version, Deadline deadline);

  AdmissionSnapshot signIn(String user, LocalDate date, Deadline deadline);
}
