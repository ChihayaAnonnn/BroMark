package com.jay.config;

import com.jay.domain.activity.service.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.service.*;
import com.jay.domain.strategy.service.*;
import com.jay.domain.task.repository.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.cache.P0AdmissionCache;
import com.jay.infrastructure.cache.StrategyVersionCache;
import com.jay.infrastructure.id.BlockOrderIdGenerator;
import com.jay.trigger.listener.v2.StockCommandListener;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "bm.v2",
    name = {"enabled", "messaging.enabled", "raffle.enabled"},
    havingValue = "true")
public class V2RaffleConfig {
  @Bean
  public MySqlStrategyVersionRepository strategyVersions(DataSource ds) {
    return new MySqlStrategyVersionRepository(ds);
  }

  @Bean
  public StrategyPublicationService strategyPublicationService(
      UserTransactionRunner tx,
      MySqlStrategyVersionRepository r,
      StrategyVersionCache cache,
      P0AdmissionCache admissions) {
    return new StrategyPublicationService(
        tx,
        r,
        f -> {
          admissions.invalidateActivity(f.activityId());
          cache.warm(f);
        });
  }

  @Bean
  public StrategyVersionCache strategyVersionCache(
      UserTransactionRunner tx,
      MySqlStrategyVersionRepository r,
      org.redisson.api.RedissonClient redis,
      @Value("${bm.v2.raffle.cache.maxVersions:128}") int max) {
    return new StrategyVersionCache(tx, r, redis, max);
  }

  @Bean
  public PublicActivityQueryService publicActivityQueryService(StrategyVersionCache cache) {
    return new PublicActivityQueryService(cache::current);
  }

  @Bean(name = "DRAW")
  public MySqlDrawRepository drawRepository(DataSource ds) {
    return new MySqlDrawRepository(ds);
  }

  @Bean(name = "RESERVATION")
  public MySqlStockRepository stockRepository(DataSource ds) {
    return new MySqlStockRepository(ds);
  }

  @Bean
  public P0AdmissionCache p0AdmissionCache(
      UserTransactionRunner tx, MySqlStrategyVersionRepository repo) {
    return new P0AdmissionCache(tx, repo, java.time.Duration.ofSeconds(2));
  }

  @Bean
  public FrozenRaffleEngine frozenRaffleEngine() {
    return new FrozenRaffleEngine();
  }

  @Bean
  public DrawService drawService(
      UserTransactionRunner tx,
      AssetKernel assets,
      QuotaKernel quota,
      MySqlAssetRepository accounts,
      MySqlDrawRepository draws,
      RouteCodec codec,
      BlockOrderIdGenerator ids,
      DataSource ds,
      FaultInjector faults,
      com.jay.domain.task.service.RelayObservation observation,
      P0AdmissionCache admission,
      FrozenRaffleEngine engine,
      P0AdmissionGate gate,
      MySqlOutboxWriter writer) {
    return new DrawService(
        tx,
        assets,
        quota,
        accounts,
        draws,
        codec,
        ids,
        writer,
        faults,
        observation,
        admission,
        engine,
        gate);
  }

  @Bean
  public StockReservationService stockReservationService(
      MySqlStockRepository stocks, MySqlStrategyVersionRepository strategies) {
    return new StockReservationService(stocks, strategies);
  }

  @Bean
  public DrawProgressService drawProgressService(
      MySqlDrawRepository draws,
      AssetKernel assets,
      QuotaKernel quota,
      RouteCodec codec,
      DataSource ds,
      MySqlOutboxWriter writer) {
    return new DrawProgressService(draws, assets, quota, codec, writer);
  }

  @Bean
  public AwardDeliveryService awardDeliveryService(
      MySqlDrawRepository draws, AssetKernel assets, FaultInjector faults) {
    return new AwardDeliveryService(draws, assets, faults);
  }

  @Bean
  public DrawDeliveryService drawDeliveryService(
      MySqlDrawRepository draws,
      AssetKernel assets,
      com.jay.domain.task.service.RelayObservation observation) {
    return new DrawDeliveryService(draws, assets, observation);
  }

  @Bean
  public CandidateWorker candidateWorker(
      MySqlDrawRepository draws, RouteCodec codec, DataSource ds, MySqlOutboxWriter writer) {
    return new CandidateWorker(draws, null, new FrozenRaffleEngine(), codec, writer);
  }

  @Bean
  public StockCommandListener stockCommandListener(
      StockReservationService stock, RouteCodec codec) {
    return new StockCommandListener(stock, codec);
  }

  @Bean(name = "ReserveAward")
  public EventHandler reserve(StockCommandListener listener) {
    return listener::apply;
  }

  @Bean(name = "ReservationConfirmed")
  public EventHandler confirmed(DrawDeliveryService delivery) {
    return e -> {
      delivery.deliver(e);
      return java.util.List.of();
    };
  }

  // P0 recovery is outbox relay + retry_job. Legacy DrawRecoveryJob/StockRecoveryJob are
  // intentionally no longer scheduled; their classes stay until the cleanup task deletes them.

  @Bean(name = "STOCK_RECONCILIATION")
  public MySqlReconciliationRecoveryPort stockReconciliationPort(DataSource ds) {
    return new MySqlReconciliationRecoveryPort(ds, true);
  }

  @Bean(name = "ASSET_RECONCILIATION")
  public MySqlReconciliationRecoveryPort assetReconciliationPort(DataSource ds) {
    return new MySqlReconciliationRecoveryPort(ds, false);
  }

  @Bean
  public RaffleReconciliationService raffleReconciliationService(
      UserTransactionRunner tx, DataSource ds) {
    return new RaffleReconciliationService(tx, new MySqlRaffleReconciliation(ds)::check);
  }
}
