package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.RouteCodec;
import com.jay.infrastructure.transaction.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;

public class DatabaseBudgetV2Test {
  static Deadline soon() {
    return new Deadline(Instant.now().plusMillis(40));
  }

  static Map<WorkClass, Integer> limits() {
    var result = new EnumMap<WorkClass, Integer>(WorkClass.class);
    for (var w : WorkClass.values()) result.put(w, 1);
    return result;
  }

  @Test
  public void independentClassesTimeoutReleaseAndShutdown() throws Exception {
    var registry = new SimpleMeterRegistry();
    var budget = new BoundedDatabaseBudget(Map.of("mysql-it", limits()), registry);
    var held = budget.acquire("mysql-it", WorkClass.TRADE, soon());
    assertThatThrownBy(() -> budget.acquire("mysql-it", WorkClass.TRADE, soon()))
        .isInstanceOf(BudgetExceededException.class);
    for (var work : new WorkClass[] {WorkClass.CALLBACK, WorkClass.QUERY, WorkClass.RECOVERY}) {
      try (var permit = budget.acquire("mysql-it", work, soon())) {
        assertThat(budget.available("mysql-it", work)).isZero();
      }
      assertThat(budget.available("mysql-it", work)).isEqualTo(1);
    }
    held.close();
    held.close();
    assertThat(budget.available("mysql-it", WorkClass.TRADE)).isEqualTo(1);
    assertThatThrownBy(
            () -> budget.acquire("mysql-it", WorkClass.TRADE, new Deadline(Instant.EPOCH)))
        .isInstanceOf(BudgetExceededException.class);
    var active = budget.acquire("mysql-it", WorkClass.RECOVERY, soon());
    budget.stopAccepting();
    assertThatThrownBy(() -> budget.acquire("mysql-it", WorkClass.QUERY, soon()))
        .isInstanceOf(RejectedExecutionException.class);
    assertThat(budget.awaitIdle(Duration.ZERO)).isFalse();
    active.close();
    assertThat(budget.awaitIdle(Duration.ofSeconds(1))).isTrue();
    assertThat(registry.get("bm.v2.database.rejected").tag("work", "TRADE").counter().count())
        .isGreaterThanOrEqualTo(2);
  }

  @Test
  public void runnerAcquiresBeforeConnectionAndReleasesAfterRollback() {
    Java25RouterIntegrationTest.databaseContext()
        .run(
            c -> {
              var registry = new SimpleMeterRegistry();
              var budget = new BoundedDatabaseBudget(Map.of("mysql-it", limits()), registry);
              var ds = c.getBean(javax.sql.DataSource.class);
              var runner =
                  new RoutedTransactionRunner(
                      ds,
                      c.getBean(org.springframework.transaction.PlatformTransactionManager.class),
                      new RouteCodec(2, 4),
                      budget,
                      Map.of("00", "mysql-it", "01", "mysql-it", "02", "mysql-it"),
                      registry);
              var hold = budget.acquire("mysql-it", WorkClass.TRADE, soon());
              assertThatThrownBy(
                      () -> runner.inUser("user-0", WorkClass.TRADE, soon(), () -> "unexpected"))
                  .isInstanceOf(BudgetExceededException.class);
              hold.close();
              assertThatThrownBy(
                      () ->
                          runner.inUser(
                              "user-0",
                              WorkClass.TRADE,
                              soon(),
                              () -> {
                                throw new IllegalStateException("fault");
                              }))
                  .isInstanceOf(IllegalStateException.class);
              assertThat(budget.available("mysql-it", WorkClass.TRADE)).isEqualTo(1);
              assertThat(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey()).isNull();
            });
  }

  @Test
  public void controlledVirtualWorkerRejectsOverflowAndStops() throws Exception {
    try (var worker = new BoundedVirtualExecutor("it-worker", 1, Duration.ofSeconds(5))) {
      var start = new CountDownLatch(1);
      var release = new CountDownLatch(1);
      var result =
          worker.submit(
              () -> {
                assertThat(Thread.currentThread().isVirtual()).isTrue();
                start.countDown();
                release.await();
                return "ok";
              });
      assertThat(start.await(2, TimeUnit.SECONDS)).isTrue();
      assertThatThrownBy(() -> worker.submit(() -> "overflow"))
          .isInstanceOf(RejectedExecutionException.class);
      release.countDown();
      assertThat(result.get(2, TimeUnit.SECONDS)).isEqualTo("ok");
    }
    var stopped = new BoundedVirtualExecutor("stopped", 1, Duration.ofSeconds(1));
    stopped.close();
    assertThatThrownBy(() -> stopped.submit(() -> null))
        .isInstanceOf(RejectedExecutionException.class);
  }
}
