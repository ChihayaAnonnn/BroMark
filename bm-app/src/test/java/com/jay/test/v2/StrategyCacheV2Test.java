package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.repository.*;
import com.jay.domain.strategy.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.cache.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;

public class StrategyCacheV2Test {
  @Test
  public void mergesHundredColdQueriesAndCachesFailedRefresh() {
    AssetTestSupport.run(
        h -> {
          FrozenStrategyV2Test.schema(h);
          long a = DrawAcceptanceV2Test.publish(h, null);
          var mysql = new MySqlStrategyVersionRepository(h.j.getDataSource());
          var calls = new AtomicInteger();
          StrategyVersionRepository counted =
              new StrategyVersionRepository() {
                public void publish(FrozenStrategy f) {
                  mysql.publish(f);
                }

                public FrozenStrategy load(long id, String v) {
                  return mysql.load(id, v);
                }

                public FrozenStrategy current(long id) {
                  calls.incrementAndGet();
                  return mysql.current(id);
                }
              };
          var cache = new StrategyVersionCache(h.runner, counted, null, 2);
          var results = com.jay.test.v2.support.ConcurrentProbe.run(100, () -> cache.current(a));
          assertThat(results.stream().map(FrozenStrategy::version).distinct().count()).isOne();
          assertThat(calls.get()).isOne();
          for (int n = 0; n < 100; n++)
            assertThatThrownBy(() -> cache.current(-999999)).isInstanceOf(RuntimeException.class);
          assertThat(calls.get()).isEqualTo(2);
        });
  }

  @Test
  public void weightedUnlockAndFixedAmountUseFrozenInputs() {
    AssetTestSupport.run(
        h -> {
          FrozenStrategyV2Test.schema(h);
          long a = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
          var p =
              new PublishStrategyCommand.Prize(
                  1,
                  "CREDIT",
                  new java.math.BigDecimal("3.00"),
                  new java.math.BigDecimal("3.00"),
                  null,
                  java.math.BigDecimal.ONE,
                  3);
          var none =
              new PublishStrategyCommand.Prize(
                  2, "NONE", null, null, null, java.math.BigDecimal.ONE, 0);
          var c =
              new PublishStrategyCommand(
                  a,
                  a,
                  "OPEN",
                  java.time.Instant.parse("2020-01-01T00:00:00Z"),
                  java.time.Instant.parse("2099-01-01T00:00:00Z"),
                  List.of(none, p),
                  none,
                  Map.of(2L, List.of(1)),
                  2,
                  "b1");
          var repo = new MySqlStrategyVersionRepository(h.j.getDataSource());
          String v = new StrategyPublicationService(h.runner, repo).publish(c);
          var f = StockProtocolV2Test.control(h, () -> repo.load(a, v));
          var e = new FrozenRaffleEngine();
          assertThat(
                  e.choose(
                          f,
                          new RuleInputSnapshot(java.time.Instant.now(), 2, 1, false, "b1"),
                          new Random())
                      .type())
              .isEqualTo("NONE");
          assertThat(
                  e.choose(
                          f,
                          new RuleInputSnapshot(java.time.Instant.now(), 100, 1, false, "b1"),
                          new Random())
                      .type())
              .isEqualTo("NONE");
          assertThat(
                  e.choose(
                          f,
                          new RuleInputSnapshot(java.time.Instant.now(), 3, 3, false, "b1"),
                          new Random())
                      .amount())
              .isEqualByComparingTo("3.00");
        });
  }
}
