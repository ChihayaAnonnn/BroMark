package com.jay.config;

import com.jay.domain.activity.service.AdmissionService;
import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.exchange.service.ExchangeService;
import com.jay.domain.rebate.service.SignInService;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.admission.RedisRequestAdmission;
import com.jay.infrastructure.id.BlockOrderIdGenerator;
import javax.sql.DataSource;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
public class V2AssetConfig {
  @Bean
  public MySqlAssetRepository assetRepository(DataSource ds) {
    return new MySqlAssetRepository(ds);
  }

  @Bean
  public AssetKernel assetKernel(MySqlAssetRepository repo, BusinessClock clock) {
    return new AssetKernel(repo, clock);
  }

  @Bean
  public QuotaKernel quotaKernel(MySqlAssetRepository repo) {
    return new QuotaKernel(repo);
  }

  @Bean
  public BlockOrderIdGenerator orderIdGenerator(
      UserTransactionRunner tx, DataSource ds, RouteCodec codec) {
    var ids = new BlockOrderIdGenerator(tx, new JdbcTemplate(ds), "app");
    codec.withGenerator(ids);
    return ids;
  }

  @Bean
  public RequestAdmission requestAdmission(
      RedissonClient redis,
      V2FeatureProperties p,
      @org.springframework.beans.factory.annotation.Value("${bm.v2.raffle.enabled:false}")
          boolean raffle) {
    p.validateAdmission();
    if (raffle && (p.getCancelRps() == null || p.getCancelMaxInFlight() == null))
      throw new IllegalArgumentException("Explicit raffle cancellation budgets required");
    return new RedisRequestAdmission(
        redis,
        p.getUserRps(),
        p.getActivityRps(),
        p.getMaxInFlight(),
        p.getLocalFallbackRps(),
        p.getQueryRps(),
        p.getQueryMaxInFlight(),
        p.getCancelRps() == null ? p.getQueryRps() : p.getCancelRps(),
        p.getCancelMaxInFlight() == null ? p.getQueryMaxInFlight() : p.getCancelMaxInFlight());
  }

  @Bean
  public AdmissionService admissionService(
      DataSource ds, UserTransactionRunner tx, RequestAdmission admission) {
    return new AdmissionService(new MySqlAdmissionRepository(ds), tx, admission);
  }

  @Bean
  public ExchangeService exchangeService(
      DataSource ds,
      UserTransactionRunner tx,
      AssetKernel assets,
      QuotaKernel quota,
      AdmissionService admission,
      RouteCodec codec,
      BlockOrderIdGenerator ids,
      FaultInjector faults) {
    return new ExchangeService(
        tx,
        assets,
        quota,
        new MySqlExchangeRepository(ds),
        admission,
        codec,
        ids,
        new MySqlOutboxWriter(ds),
        faults);
  }

  @Bean
  public SignInService signInService(
      DataSource ds,
      UserTransactionRunner tx,
      AssetKernel assets,
      QuotaKernel quota,
      AdmissionService admission,
      RouteCodec codec,
      BlockOrderIdGenerator ids,
      FaultInjector faults) {
    return new SignInService(
        tx,
        assets,
        quota,
        new MySqlSignInRepository(ds),
        admission,
        codec,
        ids,
        new MySqlOutboxWriter(ds),
        faults);
  }
}
