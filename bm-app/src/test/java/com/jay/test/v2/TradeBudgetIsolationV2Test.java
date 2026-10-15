package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.exchange.model.ExchangeCommand;
import java.time.*;
import java.util.concurrent.*;
import org.junit.Test;

public class TradeBudgetIsolationV2Test {
  @Test
  public void postPreflightAndConfigCannotBorrowReservedQueryCapacity() {
    AssetTestSupport.run(
        h -> {
          var f = new BusinessTestSupport(h);
          var entered = new CountDownLatch(1);
          var release = new CountDownLatch(1);
          try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var held =
                pool.submit(
                    () ->
                        h.runner.inControl(
                            WorkClass.QUERY,
                            new Deadline(Instant.now().plusSeconds(10)),
                            () -> {
                              entered.countDown();
                              try {
                                if (!release.await(5, TimeUnit.SECONDS))
                                  throw new IllegalStateException("test latch");
                              } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                              }
                              return true;
                            }));
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            try {
              assertThat(
                      f.exchange
                          .exchange(
                              new ExchangeCommand(
                                  "budget-user",
                                  "new-" + java.util.UUID.randomUUID(),
                                  123,
                                  "v1",
                                  new Deadline(Instant.now().plusMillis(500))))
                          .state())
                  .isEqualTo("REJECTED");
            } finally {
              release.countDown();
            }
            assertThat(held.get()).isTrue();
          }
        });
  }
}
