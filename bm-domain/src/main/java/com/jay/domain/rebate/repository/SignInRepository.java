package com.jay.domain.rebate.repository;
import java.util.Optional;import java.time.LocalDate;import com.jay.domain.asset.model.*;import com.jay.domain.rebate.model.*;
public interface SignInRepository {
 Optional<SignInResult> request(RequestIdentity identity);
 Optional<SignInResult> day(String user,LocalDate date);
 void requestReceipt(RequestIdentity identity,SignInResult result);
 void save(String user,AdmissionSnapshot snapshot,BusinessTime time,SignInResult result);
}

