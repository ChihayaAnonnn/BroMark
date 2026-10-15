package com.jay.domain.activity.service;

import com.jay.domain.activity.repository.AdmissionRepository;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import java.time.LocalDate;

public final class AdmissionService {
  private final AdmissionRepository repo;
  private final UserTransactionRunner tx;
  private final com.jay.domain.asset.service.RequestAdmission limiter;

  public AdmissionService(
      AdmissionRepository repo,
      UserTransactionRunner tx,
      com.jay.domain.asset.service.RequestAdmission limiter) {
    this.repo = repo;
    this.tx = tx;
    this.limiter = limiter;
  }

  public AdmissionSnapshot forExchange(String user, long sku, String version, Deadline deadline) {
    var s =
        tx.inControl(WorkClass.TRADE, deadline, () -> repo.exchange(user, sku, version, deadline));
    limiter.checkActivity(Long.toString(s.activityId()), deadline);
    return s;
  }

  public AdmissionSnapshot forSignIn(String user, LocalDate date, Deadline deadline) {
    var s = tx.inControl(WorkClass.TRADE, deadline, () -> repo.signIn(user, date, deadline));
    for (long id :
        s.rewards().stream().mapToLong(x -> x.activityId()).distinct().sorted().toArray())
      limiter.checkActivity(Long.toString(id), deadline);
    return s;
  }
}
