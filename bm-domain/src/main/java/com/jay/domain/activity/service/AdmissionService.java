package com.jay.domain.activity.service;

import com.jay.domain.activity.repository.AdmissionRepository;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.service.RelayObservation;
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

  public AdmissionSnapshot forDraw(String user, long activity, Deadline deadline) {
    var s = tx.inControl(WorkClass.TRADE, deadline, () -> repo.draw(user, activity, deadline));
    limiter.checkActivity(Long.toString(activity), deadline);
    return s;
  }

  public AdmissionSnapshot forExchange(String user, long sku, String version, Deadline deadline) {
    return forExchange(user, sku, version, deadline, RelayObservation.NOOP);
  }

  public AdmissionSnapshot forExchange(
      String user, long sku, String version, Deadline deadline, RelayObservation observation) {
    long start = System.nanoTime();
    String outcome = "error";
    AdmissionSnapshot s;
    try {
      s =
          tx.inControl(
              WorkClass.TRADE, deadline, () -> repo.exchange(user, sku, version, deadline));
      outcome = "success";
    } finally {
      observation.duration(
          "config", RouteSlot.control(), "ExchangeCompleted", outcome, System.nanoTime() - start);
    }
    start = System.nanoTime();
    outcome = "error";
    try {
      limiter.checkActivity(Long.toString(s.activityId()), deadline);
      outcome = "success";
    } finally {
      observation.duration(
          "activity_admission",
          RouteSlot.control(),
          "ExchangeCompleted",
          outcome,
          System.nanoTime() - start);
    }
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
