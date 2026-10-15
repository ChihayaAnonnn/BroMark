package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.stock.service.*;
import com.jay.domain.strategy.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class StockProtocolV2Test {
  static StockReservationService service(AssetTestSupport h) {
    return new StockReservationService(
        new MySqlStockRepository(h.j.getDataSource()),
        new MySqlStrategyVersionRepository(h.j.getDataSource()));
  }

  static <T> T control(AssetTestSupport h, java.util.function.Supplier<T> f) {
    return h.runner.inControl(WorkClass.WORKER, new Deadline(Instant.now().plusSeconds(60)), f);
  }

  @Test
  public void sharedVersionsConserveStockAndDuplicatesDoNotAllocateTwice() {
    AssetTestSupport.run(
        h -> {
          FrozenStrategyV2Test.schema(h);
          String pool = "pool-" + UUID.randomUUID();
          control(
              h,
              () -> {
                service(h).createPool(pool, "initial", 100, "new budget");
                return null;
              });
          long id = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
          var pub =
              new StrategyPublicationService(
                  h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()));
          var c = FrozenStrategyV2Test.command(id, pool);
          String v1 = pub.publish(c), v2 = pub.publish(c);
          var repo = new MySqlStrategyVersionRepository(h.j.getDataSource());
          var svc = service(h);
          var engine = new FrozenRaffleEngine();
          var ids = new com.jay.infrastructure.id.BlockOrderIdGenerator(h.runner, h.j, "stocktest");
          ids.prefetch();
          h.codec.withGenerator(ids);
          control(
              h,
              () -> {
                var f = repo.load(id, v1);
                for (int n = 0; n < 40; n++) {
                  var cmd =
                      new ReserveCommand(
                          h.codec.newOrderId("DR", "stock-user"),
                          "stock-user",
                          "a".repeat(64),
                          "b".repeat(64),
                          id,
                          v1,
                          engine.choose(
                              f,
                              new com.jay.domain.strategy.model.RuleInputSnapshot(
                                  Instant.now(), 1, 1, false, "v"),
                              new Random()),
                          engine.fallback(f, new Random()));
                  var r = svc.reserve(cmd);
                  if (n < 30)
                    svc.confirm(
                        new ConfirmCommand(
                            cmd.drawOrderId(),
                            cmd.userId(),
                            cmd.identityHash(),
                            cmd.requestHash(),
                            r.finalSnapshot().configHash()));
                }
                return null;
              });
          var runners =
              List.of(
                  h.runner,
                  new com.jay.infrastructure.transaction.RoutedTransactionRunner(
                      h.j.getDataSource(),
                      new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                          h.j.getDataSource()),
                      h.codec));
          var outcomes =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  80,
                  () ->
                      runners
                          .get((int) (Thread.currentThread().threadId() % 2))
                          .inControl(
                              WorkClass.WORKER,
                              new Deadline(Instant.now().plusSeconds(60)),
                              () -> {
                                String v = Thread.currentThread().threadId() % 2 == 0 ? v1 : v2;
                                var f = repo.load(id, v);
                                String order = h.codec.newOrderId("DR", "stock-user");
                                var candidate =
                                    engine.choose(
                                        f,
                                        new com.jay.domain.strategy.model.RuleInputSnapshot(
                                            Instant.now(), 1, 1, false, "v"),
                                        new Random());
                                var cmd =
                                    new ReserveCommand(
                                        order,
                                        "stock-user",
                                        "a".repeat(64),
                                        "b".repeat(64),
                                        id,
                                        v,
                                        candidate,
                                        engine.fallback(f, new Random()));
                                var result = svc.reserve(cmd);
                                assertThat(svc.reserve(cmd)).isEqualTo(result);
                                return result;
                              }));
          assertThat(outcomes.stream().filter(x -> x.quantity() == 1).count()).isEqualTo(60);
          control(
              h,
              () -> {
                var n = h.j.queryForMap("select * from award_stock where stock_pool_id=?", pool);
                assertThat(((Number) n.get("available")).longValue()).isZero();
                assertThat(((Number) n.get("reserved")).longValue()).isEqualTo(70);
                assertThat(((Number) n.get("allocated")).longValue()).isEqualTo(30);
                return null;
              });
        });
  }

  @Test
  public void cancellationTombstoneAndZeroQuantityAreFinal() {
    AssetTestSupport.run(
        h -> {
          FrozenStrategyV2Test.schema(h);
          var svc = service(h);
          String id = "tomb-" + UUID.randomUUID();
          control(
              h,
              () -> {
                var cancel = new CancelCommand(id, "u", "a".repeat(64), "b".repeat(64));
                assertThat(svc.cancel(cancel).state()).isEqualTo("CANCELLED");
                var f = new MySqlStrategyVersionRepository(h.j.getDataSource());
                var none =
                    new com.jay.domain.strategy.model.AwardSnapshot(
                        0, "NONE", null, null, 0, "c".repeat(64));
                assertThat(
                        svc.reserve(
                                new ReserveCommand(
                                    id,
                                    "u",
                                    cancel.identityHash(),
                                    cancel.requestHash(),
                                    1,
                                    "missing",
                                    none,
                                    none))
                            .state())
                    .isEqualTo("CANCELLED");
                return null;
              });
          long strategy = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
          String v =
              new StrategyPublicationService(
                      h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()))
                  .publish(FrozenStrategyV2Test.command(strategy, null));
          control(
              h,
              () -> {
                var f = new MySqlStrategyVersionRepository(h.j.getDataSource()).load(strategy, v);
                var e = new FrozenRaffleEngine();
                var candidate =
                    e.choose(
                        f,
                        new com.jay.domain.strategy.model.RuleInputSnapshot(
                            Instant.now(), 1, 1, false, "v"),
                        new Random());
                var cmd =
                    new ReserveCommand(
                        "zero-" + UUID.randomUUID(),
                        "u",
                        "d".repeat(64),
                        "e".repeat(64),
                        strategy,
                        v,
                        candidate,
                        e.fallback(f, new Random()));
                var r = svc.reserve(cmd);
                assertThat(r.quantity()).isZero();
                var confirmed =
                    svc.confirm(
                        new ConfirmCommand(
                            cmd.drawOrderId(),
                            cmd.userId(),
                            cmd.identityHash(),
                            cmd.requestHash(),
                            r.finalSnapshot().configHash()));
                assertThat(confirmed.state()).isEqualTo("CONFIRMED");
                assertThatThrownBy(
                        () ->
                            svc.cancel(
                                new CancelCommand(
                                    cmd.drawOrderId(),
                                    cmd.userId(),
                                    cmd.identityHash(),
                                    cmd.requestHash())))
                    .isInstanceOf(AssetConflictException.class);
                return null;
              });
        });
  }
 @Test public void refusesStockWritesOutsideOwnedTransaction(){AssetTestSupport.run(h->{FrozenStrategyV2Test.schema(h);assertThatThrownBy(()->service(h).createPool("unowned-"+UUID.randomUUID(),"initial",1,"budget")).isInstanceOf(IllegalStateException.class);});}

 @Test public void refusesUnloggedExistingBudgetAsNewPool(){AssetTestSupport.run(h->{FrozenStrategyV2Test.schema(h);control(h,()->{String pool="unlogged-"+UUID.randomUUID();h.j.update("insert into award_stock values(?,2,2,0,0,0,'ACTIVE')",pool);try{assertThatThrownBy(()->service(h).createPool(pool,"initial",2,"budget")).isInstanceOf(AssetConflictException.class);}finally{h.j.update("delete from stock_change_log where stock_pool_id=?",pool);h.j.update("delete from award_stock where stock_pool_id=?",pool);}return null;});});}

}
