package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.test.v2.support.ConcurrentProbe;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class QuotaConcurrencyV2Test {
  @Test
  public void independentInstancesConvergeFirstAccountsAndBucketsAndRespectPeriodLimits() {
    AssetTestSupport.run(
        h -> {
          var second =
              new AssetTestSupport(
                  h.j.getDataSource(),
                  new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                      h.j.getDataSource()));
          String u = "two-instance-" + UUID.randomUUID();
          var time = BusinessTime.at(Instant.parse("2026-10-15T00:00:00Z"));
          var counter = new AtomicInteger();
          var results =
              ConcurrentProbe.run(
                  100,
                  () -> {
                    var instance = counter.incrementAndGet() % 2 == 0 ? h : second;
                    return instance.tx(
                        u,
                        () -> {
                          instance.assets.lockAccounts(u, List.of(12L));
                          return new QuotaKernel(instance.repo)
                              .grant(
                                  u,
                                  12,
                                  new BusinessId("SAME", "first", "a".repeat(64)),
                                  new QuotaDelta(10, 2, 5),
                                  time);
                        });
                  });
          assertThat(new HashSet<>(results)).hasSize(1);
          var q = new QuotaKernel(h.repo);
          h.tx(
              u,
              () -> {
                h.assets.lockAccounts(u, List.of(12L));
                assertThat(q.consume(u, 12, new BusinessId("DRAW", "1", "a".repeat(64)), time))
                    .isPresent();
                assertThat(q.consume(u, 12, new BusinessId("DRAW", "2", "a".repeat(64)), time))
                    .isPresent();
                assertThat(q.consume(u, 12, new BusinessId("DRAW", "3", "a".repeat(64)), time))
                    .isEmpty();
                return null;
              });
          var month = BusinessTime.at(Instant.parse("2026-11-01T00:00:00Z"));
          var consumed =
              h.tx(
                  u,
                  () -> {
                    h.assets.lockAccounts(u, List.of(12L));
                    return q.consume(
                            u, 12, new BusinessId("DRAW", "newmonth", "a".repeat(64)), month)
                        .orElseThrow();
                  });
          assertThat(consumed.monthRemaining()).isEqualTo(4);
          assertThat(consumed.dayRemaining()).isEqualTo(1);
        });
  }
}
