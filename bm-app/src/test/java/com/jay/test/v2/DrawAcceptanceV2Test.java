package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.strategy.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.id.*;
import java.time.*;
import java.util.*;
import org.junit.Ignore;
import org.junit.Test;

public class DrawAcceptanceV2Test {
  static void schema(AssetTestSupport h) throws Exception {
    if (AssetTestSupport.postgres()) return;
    FrozenStrategyV2Test.schema(h);
    h.runner.inControl(
        WorkClass.RECOVERY,
        new Deadline(Instant.now().plusSeconds(30)),
        () -> {
          try {
            for (String s :
                java.nio.file.Files.readString(
                        java.nio.file.Path.of(
                            "../docs/dev-ops/mysql/migrations/002_admission_config.sql"))
                    .split(";")) if (!s.isBlank()) h.j.execute(s);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
          return null;
        });
    for (String db : List.of("01", "02"))
      for (int i = 0; i < 4; i++) {
        var slot = new RouteSlot(db, String.format("%03d", i));
        h.runner.inSlot(
            slot,
            WorkClass.RECOVERY,
            new Deadline(Instant.now().plusSeconds(30)),
            () -> {
              try {
                for (String s :
                    java.nio.file.Files.readString(
                            java.nio.file.Path.of(
                                "../docs/dev-ops/mysql/migrations/004_user_draw.sql"))
                        .replace("{{suffix}}", slot.tableKey())
                        .split(";")) if (!s.isBlank()) h.j.execute(s);
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
              return null;
            });
      }
  }

  static DrawService service(AssetTestSupport h) {
    var ids = new BlockOrderIdGenerator(h.runner, h.j, "drawtest");
    ids.prefetch();
    h.codec.withGenerator(ids);
    var strategies = new MySqlStrategyVersionRepository(h.j.getDataSource());
    return new DrawService(
        h.runner,
        h.assets,
        new QuotaKernel(h.repo),
        h.repo,
        new MySqlDrawRepository(h.j.getDataSource()),
        h.codec,
        ids,
        new MySqlOutboxWriter(h.j.getDataSource()),
        x -> {},
        com.jay.domain.task.service.RelayObservation.NOOP,
        new com.jay.infrastructure.cache.P0AdmissionCache(
            h.runner, strategies, Duration.ofSeconds(2)),
        new FrozenRaffleEngine(),
        new P0AdmissionGate());
  }

  static long publish(AssetTestSupport h, String pool) {
    long activity = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
    String stockPool = pool;
    if (stockPool == null) {
      stockPool = "draw-pool-" + UUID.randomUUID();
      String created = stockPool;
      StockProtocolV2Test.control(
          h,
          () -> {
            StockProtocolV2Test.service(h).createPool(created, "initial", 100, "default budget");
            return null;
          });
    }
    new StrategyPublicationService(
            h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()))
        .publish(FrozenStrategyV2Test.command(activity, stockPool));
    return activity;
  }

  static void grant(AssetTestSupport h, String user, long activity) {
    h.tx(
        user,
        () -> {
          var t = h.assets.lockAccounts(user, List.of(activity));
          new QuotaKernel(h.repo)
              .grant(
                  user,
                  activity,
                  new BusinessId("GRANT", UUID.randomUUID().toString(), "a".repeat(64)),
                  new QuotaDelta(10, 10, 10),
                  t);
          return null;
        });
  }

  @Ignore("legacy six-stage assertion replaced by P0; cleanup task")
  @Test
  public void sameRequestConsumesOnceAndCandidateSurvivesDateAndRuleChanges() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          long a = publish(h, null);
          String user = "draw-" + UUID.randomUUID();
          grant(h, user, a);
          var svc = service(h);
          String request = UUID.randomUUID().toString();
          var views =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  100,
                  () ->
                      svc.create(
                          new DrawCommand(
                              user, request, a, new Deadline(Instant.now().plusSeconds(60)))));
          assertThat(views.stream().map(DrawView::orderId).distinct().count()).isOne();
          String order = views.getFirst().orderId();
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          var before = h.tx(user, () -> repo.find(order, false));
          assertThat(before.ruleInput().totalEffectiveCount()).isOne();
          var worker =
              new CandidateWorker(
                  repo,
                  new com.jay.infrastructure.cache.StrategyVersionCache(
                      h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()), null, 128),
                  new FrozenRaffleEngine(),
                  h.codec,
                  new MySqlOutboxWriter(h.j.getDataSource()));
          var frozen =
              new com.jay.infrastructure.cache.StrategyVersionCache(
                      h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()), null, 128)
                  .load(a, before.version());
          var runners =
              List.of(
                  h.runner,
                  new com.jay.infrastructure.transaction.RoutedTransactionRunner(
                      h.j.getDataSource(),
                      new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                          h.j.getDataSource()),
                      h.codec));
          var workerIndex = new java.util.concurrent.atomic.AtomicInteger();
          com.jay.test.v2.support.ConcurrentProbe.run(
              2,
              () ->
                  runners
                      .get(workerIndex.getAndIncrement())
                      .inUser(
                          user,
                          WorkClass.WORKER,
                          new Deadline(Instant.now().plusSeconds(30)),
                          () -> {
                            worker.prepareFrozen(order, frozen, new Random());
                            return null;
                          }));
          var after = h.tx(user, () -> repo.find(order, false));
          assertThat(after.state()).isEqualTo("CANDIDATE_FIXED");
          assertThat(
                  h.tx(
                      user,
                      () ->
                          h.j.queryForObject(
                              "select count(*) from outbox where aggregate_id=? and"
                                  + " event_type='ReserveAward'",
                              Integer.class,
                              order)))
              .isOne();
          assertThat(after.ruleInput()).isEqualTo(before.ruleInput());
          assertThat(svc.get(user, order).finalAward()).isNull();
          assertThat(
                  h.tx(
                      user,
                      () -> {
                        h.assets.lockAccounts(user, List.of(a));
                        return h.repo.quotaState(user, a, before.time()).total();
                      }))
              .isEqualTo(9);
        });
  }

  @Test
  public void rejectionPersistsWithoutConsumption() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          long a = publish(h, null);
          String user = "no-quota-" + UUID.randomUUID();
          var svc = service(h);
          String request = UUID.randomUUID().toString();
          var cmd = new DrawCommand(user, request, a, new Deadline(Instant.now().plusSeconds(30)));
          assertThat(svc.create(cmd).state()).isEqualTo("REJECTED");
          grant(h, user, a);
          assertThat(svc.create(cmd).state()).isEqualTo("REJECTED");
        });
  }
}
