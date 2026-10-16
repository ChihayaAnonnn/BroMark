package com.jay.domain.payment.repository;

import com.jay.domain.payment.model.*;
import com.jay.domain.task.repository.RecoveryAggregatePort;
import java.util.*;

public interface PaymentRepository extends RecoveryAggregatePort {
  Optional<PaymentRecord> request(String user, String request, String hash);

  PaymentProduct product(String id);

  PaymentRecord find(String id, boolean lock);

  void insert(PaymentRecord record);

  void update(PaymentRecord old, PaymentRecord next);

  void review(String id, String reason, Object evidence);

  boolean saveInbox(VerifiedPaymentEvent e);

  java.util.List<VerifiedPaymentEvent> claimInbox(String token, int limit);

  boolean ownsInbox(VerifiedPaymentEvent e, String token);

  void finishInbox(VerifiedPaymentEvent e, String token, boolean success);

  java.util.Map<String, Object> lockFact(VerifiedPaymentEvent e);

  void markFact(VerifiedPaymentEvent e);

  long paidFacts(String order);

  void touch(String id);

  java.util.List<PaymentRecord> all();

  void stage(String id, String stage);
}
