package com.jay.domain.activity.repository;
import com.jay.domain.asset.model.*;import java.time.LocalDate;
public interface AdmissionRepository {AdmissionSnapshot exchange(long sku,String version,Deadline deadline);AdmissionSnapshot signIn(LocalDate date,Deadline deadline);}

