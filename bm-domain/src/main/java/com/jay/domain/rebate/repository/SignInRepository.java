package com.jay.domain.rebate.repository;

import com.jay.domain.asset.model.*;
import com.jay.domain.rebate.model.*;
import java.time.LocalDate;
import java.util.Optional;

public interface SignInRepository {
  Optional<SignInResult> request(RequestIdentity identity);

  Optional<SignInResult> day(String user, LocalDate date);

  void requestReceipt(RequestIdentity identity, SignInResult result);

  void save(String user, AdmissionSnapshot snapshot, BusinessTime time, SignInResult result);
}
