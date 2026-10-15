package com.jay.domain.asset.repository;
import com.jay.domain.asset.model.*;import java.math.BigDecimal;import java.util.*;
public interface AssetRepository {
 Optional<QuotaReceipt> quotaReceipt(String user,long activity,BusinessId id,String kind);
 QuotaState quotaState(String user,long activity,BusinessTime time);
 void writeQuota(String user,long activity,BusinessId id,String kind,QuotaDelta delta,QuotaState before,QuotaState after,QuotaReceipt receipt);
 void lockAccounts(String user,List<Long> activities);
 void requireLocked(String user,Long activity);
 Optional<AssetReceipt> creditReceipt(String user,BusinessId id);
 BigDecimal balance(String user);
 void adjustCredit(String user,BigDecimal delta,AssetReceipt receipt);
}

