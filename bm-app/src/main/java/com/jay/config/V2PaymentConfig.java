package com.jay.config;

import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.payment.service.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.gateway.payment.*;
import com.jay.infrastructure.id.*;
import com.jay.trigger.job.v2.*;
import com.jay.trigger.listener.v2.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "bm.v2",
    name = {"enabled", "messaging.enabled", "payment.enabled"},
    havingValue = "true")
public class V2PaymentConfig {
  public static void requireSandboxProfiles(Set<String> profiles) {
    if (profiles.stream().anyMatch(p -> p.toLowerCase(java.util.Locale.ROOT).contains("prod"))
        || Collections.disjoint(profiles, Set.of("test", "local-sandbox")))
      throw new IllegalStateException(
          "Sandbox adapter requires test/local-sandbox and forbids production profiles");
  }

  @Bean(name = "PAYMENT")
  public MySqlPaymentRepository paymentRepository(DataSource ds) {
    return new MySqlPaymentRepository(ds);
  }

  @Bean
  public SandboxPaymentProvider paymentProvider(
      UserTransactionRunner tx,
      MySqlPaymentRepository repo,
      Environment env,
      @Value("${V2_PAYMENT_ACCOUNT:}") String account,
      @Value("${V2_PAYMENT_SIGNING_SECRET:}") String secret) {
    var profiles = Set.of(env.getActiveProfiles());
    requireSandboxProfiles(profiles);
    return new SandboxPaymentProvider(
        tx, repo, profiles.contains("test") ? "test" : "local-sandbox", account, secret);
  }

  @Bean
  public PaymentService paymentService(
      UserTransactionRunner tx,
      MySqlPaymentRepository repo,
      SandboxPaymentProvider provider,
      RouteCodec codec,
      BlockOrderIdGenerator ids,
      DataSource ds,
      MySqlOutboxWriter writer) {
    return new PaymentService(tx, repo, provider, codec, ids, writer);
  }

  @Bean
  public PaymentFactService paymentFactService(
      UserTransactionRunner tx,
      MySqlPaymentRepository repo,
      RouteCodec codec,
      DataSource ds,
      MySqlOutboxWriter writer) {
    return new PaymentFactService(tx, repo, codec, writer);
  }

  @Bean
  public PaymentNotificationService paymentNotificationService(
      UserTransactionRunner tx, MySqlPaymentRepository repo, SandboxPaymentProvider provider) {
    return new PaymentNotificationService(tx, repo, provider);
  }

  @Bean
  public PaymentCreditService paymentCreditService(
      AssetKernel assets, RouteCodec codec, FaultInjector faults) {
    return new PaymentCreditService(assets, codec, faults);
  }

  @Bean(name = "GrantCredit")
  public EventHandler grantCredit(PaymentCreditService s) {
    return new GrantCreditListener(s);
  }

  @Bean(name = "CreditGranted")
  public EventHandler creditGranted(PaymentFactService s) {
    return new CreditGrantedListener(s);
  }

  @Bean
  public PaymentInboxJob paymentInboxJob(
      UserTransactionRunner tx,
      MySqlPaymentRepository repo,
      PaymentFactService facts,
      BlockOrderIdGenerator ids,
      CoalescingJobDispatcher dispatcher) {
    return new PaymentInboxJob(tx, repo, facts, ids).dispatchWith(dispatcher);
  }

  @Bean
  public PaymentRecoveryService paymentRecoveryService(
      UserTransactionRunner tx,
      MySqlPaymentRepository repo,
      SandboxPaymentProvider provider,
      PaymentFactService facts,
      DataSource ds,
      MySqlOutboxWriter writer,
      com.jay.domain.task.service.WorkflowRedrive guarded,
      V2RecoveryRedriveProperties policy) {
    return ("guarded".equals(policy.getMode())
            ? new PaymentRecoveryService(tx, repo, provider, facts, writer, guarded)
            : new PaymentRecoveryService(tx, repo, provider, facts, writer))
        .scanBudget(
            policy.getCandidateLimit(),
            policy.getEmissionLimit(),
            java.time.Duration.ofSeconds(policy.getRoundTimeoutSeconds()));
  }

  @Bean
  public PaymentRecoveryJob paymentRecoveryJob(
      PaymentRecoveryService s, BlockOrderIdGenerator ids, CoalescingJobDispatcher dispatcher) {
    return new PaymentRecoveryJob(s, ids).dispatchWith(dispatcher);
  }

  @Bean
  public PaymentReconciliationService paymentReconciliationService(
      UserTransactionRunner tx,
      MySqlPaymentRepository repo,
      MySqlAssetRepository assets,
      PaymentFactService facts) {
    return new PaymentReconciliationService(tx, repo, assets, facts);
  }
}
