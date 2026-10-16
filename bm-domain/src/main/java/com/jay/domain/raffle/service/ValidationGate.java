package com.jay.domain.raffle.service;

import com.jay.domain.raffle.model.ReconciliationReport;
import java.util.Set;

/** 发布门禁只接受完整且已执行的六域检查，不能把缺项补成零。 */
public final class ValidationGate {
  public static final Set<String> SCOPES=Set.of("CREDIT","QUOTA","STOCK","DELIVERY","PAYMENT","SIGNIN");
  public boolean passes(ReconciliationReport r,long unfinishedWithoutReview,long unresolvedReviews) {
    return r.checkedScopes().containsAll(SCOPES)
        && r.creditMismatch()==0 && r.quotaMismatch()==0 && r.stockMismatch()==0
        && r.duplicateDelivery()==0 && r.paymentMismatch()==0 && r.signinMismatch()==0
        && unfinishedWithoutReview==0 && unresolvedReviews==0;
  }
}
