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
import com.jay.infrastructure.cache.StrategyVersionCache;
import com.jay.infrastructure.id.BlockOrderIdGenerator;
import com.jay.trigger.job.v2.DrawRecoveryJob;
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
      UserTransactionRunner tx, MySqlStrategyVersionRepository r, StrategyVersionCache cache) {
    return new StrategyPublicationService(tx, r, cache::warm);
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
  public DrawService drawService(
      UserTransactionRunner tx,
      AssetKernel assets,
      QuotaKernel quota,
      MySqlAssetRepository accounts,
      MySqlDrawRepository draws,
      MySqlStrategyVersionRepository strategies,
      RouteCodec codec,
      BlockOrderIdGenerator ids,
      DataSource ds,
      FaultInjector faults,
      MySqlOutboxWriter writer) {
    return new DrawService(
        tx, assets, quota, accounts, draws, strategies, codec, ids, writer, faults);
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
  public CandidateWorker candidateWorker(
      MySqlDrawRepository draws, RouteCodec codec, DataSource ds, MySqlOutboxWriter writer) {
    return new CandidateWorker(draws, null, new FrozenRaffleEngine(), codec, writer);
  }

  @Bean(name = "ReserveAward")
  public EventHandler reserve(StockReservationService stock, RouteCodec codec) {
    return new StockCommandListener(stock, codec)::apply;
  }

  @Bean(name = "ConfirmReservation")
  public EventHandler confirm(StockReservationService stock, RouteCodec codec) {
    return new StockCommandListener(stock, codec)::apply;
  }

  @Bean(name = "CancelReservation")
  public EventHandler cancel(StockReservationService stock, RouteCodec codec) {
    return new StockCommandListener(stock, codec)::apply;
  }

  @Bean(name = "PrepareDraw")
  public EventHandler prepare(CandidateWorker worker) {
    return e -> {
      worker.prepare(e);
      return java.util.List.of();
    };
  }

  @Bean(name = "ReservationHeld")
  public EventHandler held(DrawProgressService progress) {
    return progress::handle;
  }

  @Bean(name = "ReservationConfirmed")
  public EventHandler confirmed(DrawProgressService progress) {
    return progress::handle;
  }

  @Bean(name = "ReservationCancelled")
  public EventHandler cancelled(DrawProgressService progress) {
    return progress::handle;
  }

  @Bean(name = "AwardDelivery")
  public EventHandler deliver(AwardDeliveryService delivery) {
    return e -> {
      delivery.deliver(e);
      return java.util.List.of();
    };
  }

  @Bean
  public DrawRecoveryService drawRecoveryService(
      UserTransactionRunner tx,
      MySqlDrawRepository draws,
      MySqlStockRepository stocks,
      RouteCodec codec,
      DataSource ds,
      MySqlOutboxWriter writer,
      WorkflowRedrive guarded,
      V2RecoveryRedriveProperties policy) {
    return ("guarded".equals(policy.getMode())
            ? new DrawRecoveryService(tx, draws, stocks, codec, writer, guarded)
            : new DrawRecoveryService(tx, draws, stocks, codec, writer))
        .scanBudget(
            policy.getCandidateLimit(),
            policy.getEmissionLimit(),
            java.time.Duration.ofSeconds(policy.getRoundTimeoutSeconds()));
  }

  @Bean
  public com.jay.trigger.job.v2.StockRecoveryJob stockRecoveryJob(
      UserTransactionRunner tx,
      MySqlStockRepository stocks,
      MySqlDrawRepository draws,
      RouteCodec codec,
      BlockOrderIdGenerator ids,
      DataSource ds,
      MySqlOutboxWriter writer,
      com.jay.trigger.job.v2.CoalescingJobDispatcher dispatcher,
      DrawRecoveryService unified,
      WorkflowRedrive guarded,
      V2RecoveryRedriveProperties policy) {
    return ("guarded".equals(policy.getMode())
            ? new com.jay.trigger.job.v2.StockRecoveryJob(
                tx, stocks, draws, codec, ids, writer, unified, guarded)
            : new com.jay.trigger.job.v2.StockRecoveryJob(tx, stocks, draws, codec, ids, writer))
        .scanBudget(
            policy.getCandidateLimit(),
            policy.getEmissionLimit(),
            java.time.Duration.ofSeconds(policy.getRoundTimeoutSeconds()))
        .dispatchWith(dispatcher);
  }

  @Bean
  public DrawRecoveryJob drawRecoveryJob(
      DrawRecoveryService r,
      BlockOrderIdGenerator ids,
      com.jay.trigger.job.v2.CoalescingJobDispatcher dispatcher) {
    return new DrawRecoveryJob(r, ids).dispatchWith(dispatcher);
  }

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
