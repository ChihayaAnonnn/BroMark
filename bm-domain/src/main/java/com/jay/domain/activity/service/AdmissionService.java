package com.jay.domain.activity.service;
import com.jay.domain.activity.repository.AdmissionRepository;import com.jay.domain.asset.model.*;import com.jay.domain.asset.repository.UserTransactionRunner;import java.time.LocalDate;
public final class AdmissionService {
 private final AdmissionRepository repo;private final UserTransactionRunner tx;
 public AdmissionService(AdmissionRepository repo,UserTransactionRunner tx){this.repo=repo;this.tx=tx;}
 public AdmissionSnapshot forExchange(String user,long sku,String version,Deadline deadline){return tx.inControl(WorkClass.QUERY,deadline,()->repo.exchange(sku,version,deadline));}
 public AdmissionSnapshot forSignIn(String user,LocalDate date,Deadline deadline){return tx.inControl(WorkClass.QUERY,deadline,()->repo.signIn(date,deadline));}
}

