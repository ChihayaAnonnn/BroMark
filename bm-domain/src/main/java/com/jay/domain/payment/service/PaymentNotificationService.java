package com.jay.domain.payment.service;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.payment.adapter.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.repository.*;
import java.time.*;
import java.util.*;

public final class PaymentNotificationService {
  private final UserTransactionRunner tx;
  private final PaymentRepository repo;
  private final PaymentProvider provider;
  private final com.jay.domain.asset.service.FaultInjector faults;

  public PaymentNotificationService(
      UserTransactionRunner tx, PaymentRepository repo, PaymentProvider provider) {
    this(tx, repo, provider, x -> {});
  }

  public PaymentNotificationService(
      UserTransactionRunner tx,
      PaymentRepository repo,
      PaymentProvider provider,
      com.jay.domain.asset.service.FaultInjector faults) {
    this.faults = faults;
    this.tx = tx;
    this.repo = repo;
    this.provider = provider;
  }

  public NotificationAck receive(String name, byte[] raw, Map<String, String> headers) {
    if (raw == null || raw.length > 65536 || !provider.name().equals(name))
      throw new IllegalArgumentException("Invalid notification");
    var e = provider.verify(raw, headers);
    tx.inControl(
        WorkClass.CALLBACK,
        new Deadline(Instant.now().plusSeconds(5)),
        () -> {
          repo.saveInbox(e, rawHash(raw));
          faults.hit("payment.after-inbox");
          return null;
        });
    return provider.accepted();
  }

  private static String rawHash(byte[] raw) {
    try {
      return java.util.HexFormat.of()
          .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
