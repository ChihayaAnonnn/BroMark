package com.jay.infrastructure.gateway.payment;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.payment.adapter.*;
import com.jay.domain.payment.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.*;

public final class SandboxPaymentProvider implements PaymentProvider {
  private final UserTransactionRunner tx;
  private final MySqlPaymentRepository repo;
  private final String environment, account, secret;
  private volatile String mode = "NORMAL";
  private volatile Consumer<String> beforeResponse = id -> {};

  public SandboxPaymentProvider(
      UserTransactionRunner tx,
      MySqlPaymentRepository repo,
      String environment,
      String account,
      String secret) {
    if (!Set.of("test", "local-sandbox").contains(environment)
        || account == null
        || account.isBlank()
        || secret == null
        || secret.length() < 32)
      throw new IllegalArgumentException(
          "Sandbox requires explicit test environment/account/signing secret");
    this.tx = tx;
    this.repo = repo;
    this.environment = environment;
    this.account = account;
    this.secret = secret;
  }

  public String name() {
    return "sandbox";
  }

  public String account() {
    return account;
  }

  public String environment() {
    return environment;
  }

  public void mode(String m) {
    if (!Set.of("NORMAL", "TIMEOUT_CREATED", "QUERY_UNKNOWN").contains(m))
      throw new IllegalArgumentException("Sandbox mode");
    mode = m;
  }

  public void beforeResponse(Consumer<String> c) {
    beforeResponse = Objects.requireNonNull(c);
  }

  private void outside() {
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive())
      throw new IllegalStateException("Channel call inside transaction");
  }

  private <T> T channel(Supplier<T> s) {
    return tx.inControl(WorkClass.CALLBACK, new Deadline(Instant.now().plusSeconds(5)), s);
  }

  public ProviderOrder create(String id, BigDecimal amount, String currency) {
    outside();
    var r = channel(() -> repo.sandboxCreate(id, amount, currency));
    beforeResponse.accept(id);
    if (mode.equals("TIMEOUT_CREATED"))
      throw new IllegalStateException("Sandbox timeout after creation");
    return r;
  }

  public ProviderPayment query(String id) {
    outside();
    if (mode.equals("QUERY_UNKNOWN")) return new ProviderPayment(id, "UNKNOWN", null);
    return channel(() -> repo.sandboxQuery(id));
  }

  public void record(VerifiedPaymentEvent e) {
    outside();
    channel(
        () -> {
          repo.sandboxFact(e);
          return null;
        });
  }

  public Map<String, String> sign(byte[] raw) {
    try {
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(
          new javax.crypto.spec.SecretKeySpec(
              secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Map.of("x-sandbox-signature", HexFormat.of().formatHex(mac.doFinal(raw)));
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public VerifiedPaymentEvent verify(byte[] raw, Map<String, String> headers) {
    outside();
    if (raw == null || raw.length > 65536)
      throw new IllegalArgumentException("Notification exceeds 64KiB");
    String signature =
        headers.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase("x-sandbox-signature"))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("");
    if (!java.security.MessageDigest.isEqual(
        sign(raw).get("x-sandbox-signature").getBytes(StandardCharsets.UTF_8),
        signature.getBytes(StandardCharsets.UTF_8)))
      throw new IllegalArgumentException("Invalid signature");
    var e = JsonStore.read(new String(raw, StandardCharsets.UTF_8), VerifiedPaymentEvent.class);
    if (!name().equals(e.provider())
        || !account.equals(e.providerAccount())
        || !environment.equals(e.environment()))
      throw new IllegalArgumentException("Provider account/environment mismatch");
    return e;
  }
}
