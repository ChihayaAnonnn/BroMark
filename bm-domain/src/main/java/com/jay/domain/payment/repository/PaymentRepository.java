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

  boolean saveInbox(VerifiedPaymentEvent e, String rawHash);

  java.util.List<ProviderInboxEntry> claimInbox(String token, int limit);

  boolean ownsInbox(ProviderInboxEntry e, String token);

  void finishInbox(ProviderInboxEntry e, String token, boolean success);

  java.util.Map<String, Object> lockFact(VerifiedPaymentEvent e);

  void markFact(VerifiedPaymentEvent e);

  long paidFacts(String order);

  void touch(String id);

  java.util.List<String> orderIds();

  void stage(String id, String stage);

  void failInbox(ProviderInboxEntry entry, String token, String reason, boolean permanent);

  java.util.List<PaymentFact> facts(String order);

  java.util.List<String> factOrderIds();

  java.util.List<PaymentCharge> charges();
}
