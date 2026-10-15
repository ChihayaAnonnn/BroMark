package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.test.v2.support.ConcurrentProbe;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class QuotaKernelV2Test {
  @Test
  public void grantsPeriodsAndRefundsOriginalBucketsExactlyOnce() {
    AssetTestSupport.run(
        h -> {
          String u = "quota-" + UUID.randomUUID();
          long a = 12;
          var q = new QuotaKernel(h.repo);
          var t = BusinessTime.at(Instant.parse("2026-10-15T12:00:00Z"));
          java.util.function.Supplier<BusinessTime> lock =
              () -> h.assets.lockAccounts(u, List.of(a));
          h.tx(
              u,
              () -> {
                lock.get();
                q.grant(
                    u,
                    a,
                    new BusinessId("GRANT", "initial", "a".repeat(64)),
                    new QuotaDelta(10, 2, 5),
                    t);
                q.consume(u, a, new BusinessId("CONSUME", "first", "a".repeat(64)), t);
                q.consume(
                    u,
                    a,
                    new BusinessId("CONSUME", "second", "a".repeat(64)),
                    BusinessTime.at(t.instant().minusSeconds(86400)));
                return null;
              });
          var grant =
              h.tx(
                  u,
                  () -> {
                    lock.get();
                    return q.grant(
                        u,
                        a,
                        new BusinessId("GRANT", "next", "a".repeat(64)),
                        new QuotaDelta(5, 1, 2),
                        t);
                  });
          assertThat(grant.totalRemaining()).isEqualTo(13);
          assertThat(grant.dayRemaining()).isEqualTo(2);
          assertThat(grant.monthRemaining()).isEqualTo(5);
          var nextDay = BusinessTime.at(t.instant().plusSeconds(86400));
          var consumed =
              h.tx(
                  u,
                  () -> {
                    lock.get();
                    return q.consume(
                            u, a, new BusinessId("CONSUME", "future", "b".repeat(64)), nextDay)
                        .orElseThrow();
                  });
          assertThat(consumed.dayRemaining()).isEqualTo(2);
          var refunds =
              ConcurrentProbe.run(
                  100,
                  () ->
                      h.tx(
                          u,
                          () -> {
                            lock.get();
                            return q.refund(
                                u, a, new BusinessId("CONSUME", "first", "a".repeat(64)), t);
                          }));
          assertThat(new HashSet<>(refunds)).hasSize(1);
          assertThat(refunds.getFirst().businessTime()).isEqualTo(t);
          assertThatThrownBy(() -> new QuotaDelta(-1, 0, 0))
              .isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(
                  () ->
                      h.tx(
                          u,
                          () -> {
                            lock.get();
                            return q.grant(
                                u,
                                a,
                                new BusinessId("GRANT", "overflow", "a".repeat(64)),
                                new QuotaDelta(Long.MAX_VALUE, 0, 0),
                                t);
                          }))
              .isInstanceOf(ArithmeticException.class);
        });
  }
}
