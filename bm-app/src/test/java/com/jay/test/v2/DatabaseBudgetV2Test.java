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
  public void saturatedTradeRejectsWithoutWaitingForTheWholeRequestDeadline() throws Exception {
    var registry = new SimpleMeterRegistry();
    var budget =
        new BoundedDatabaseBudget(Map.of("mysql-it", limits()), registry, Duration.ofMillis(50));
    var held = budget.acquire("mysql-it", WorkClass.TRADE, soon());
    try (var executor = Executors.newSingleThreadExecutor()) {
      try {
        var attempt =
            executor.submit(
                () -> {
                  try (var permit =
                      budget.acquire(
                          "mysql-it",
                          WorkClass.TRADE,
                          new Deadline(Instant.now().plusSeconds(2)))) {
                    return "unexpected";
                  }
                });
        assertThatThrownBy(() -> attempt.get(500, TimeUnit.MILLISECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(BudgetExceededException.class);
        assertThat(budget.available("mysql-it", WorkClass.TRADE)).isZero();
        assertThat(registry.get("bm.v2.database.rejected").tag("work", "TRADE").counter().count())
            .isEqualTo(1);
      } finally {
        held.close();
      }
    }
    assertThat(budget.available("mysql-it", WorkClass.TRADE)).isEqualTo(1);
    assertThat(budget.isIdle()).isTrue();
  }

  @Test
  public void workerCanWaitBeyondTheForegroundTradeLimit() throws Exception {
    var budget =
        new BoundedDatabaseBudget(
            Map.of("mysql-it", limits()), new SimpleMeterRegistry(), Duration.ofMillis(50));
    var held = budget.acquire("mysql-it", WorkClass.WORKER, soon());
    var entered = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      try {
        var attempt =
            executor.submit(
                () -> {
                  entered.countDown();
                  try (var permit =
                      budget.acquire(
                          "mysql-it",
                          WorkClass.WORKER,
                          new Deadline(Instant.now().plusSeconds(2)))) {
                    return "acquired";
                  }
                });
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(150);
        assertThat(attempt.isDone()).isFalse();
        held.close();
        assertThat(attempt.get(1, TimeUnit.SECONDS)).isEqualTo("acquired");
      } finally {
        held.close();
      }
    }
    assertThat(budget.available("mysql-it", WorkClass.WORKER)).isEqualTo(1);
    assertThat(budget.isIdle()).isTrue();
  }

  @Test
  public void shortRequestDeadlineStillBoundsConfiguredTradeWait() throws Exception {
    var registry = new SimpleMeterRegistry();
    var budget =
        new BoundedDatabaseBudget(Map.of("mysql-it", limits()), registry, Duration.ofSeconds(1));
    try (var held = budget.acquire("mysql-it", WorkClass.TRADE, soon());
        var executor = Executors.newSingleThreadExecutor()) {
      var attempt = executor.submit(() -> budget.acquire("mysql-it", WorkClass.TRADE, soon()));
      assertThatThrownBy(() -> attempt.get(500, TimeUnit.MILLISECONDS))
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(BudgetExceededException.class);
      assertThat(budget.available("mysql-it", WorkClass.TRADE)).isZero();
    }
    assertThat(budget.available("mysql-it", WorkClass.TRADE)).isEqualTo(1);
  }

  @Test
  public void rejectsNonPositiveTradeWaitConfiguration() {
    for (var invalid : List.of(Duration.ZERO, Duration.ofMillis(-1))) {
      assertThatThrownBy(
              () ->
                  new BoundedDatabaseBudget(
                      Map.of("mysql-it", limits()), new SimpleMeterRegistry(), invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void configuredTradeCanWaitBeyondOnePollAndAcquire() throws Exception {
    var budget =
        new BoundedDatabaseBudget(
            Map.of("mysql-it", limits()), new SimpleMeterRegistry(), Duration.ofSeconds(1));
    var held = budget.acquire("mysql-it", WorkClass.TRADE, soon());
    var entered = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      try {
        var attempt =
            executor.submit(
                () -> {
                  entered.countDown();
                  try (var permit =
                      budget.acquire(
                          "mysql-it",
                          WorkClass.TRADE,
                          new Deadline(Instant.now().plusSeconds(3)))) {
                    return "acquired";
                  }
                });
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(150);
        assertThat(attempt.isDone()).isFalse();
        held.close();
        assertThat(attempt.get(1, TimeUnit.SECONDS)).isEqualTo("acquired");
      } finally {
        held.close();
      }
    }
    assertThat(budget.available("mysql-it", WorkClass.TRADE)).isEqualTo(1);
    assertThat(budget.isIdle()).isTrue();
  }

  @Test
  public void boundedTradeAcquiredDuringShutdownIsReleasedWithoutAdmission() throws Exception {
    assertStoppingWaiter(
        new BoundedDatabaseBudget(
            Map.of("mysql-it", limits()), new SimpleMeterRegistry(), Duration.ofSeconds(1)),
        true);
  }

  @Test
  public void legacyTradeWaitStillNoticesShutdownBeforeTheLongDeadline() throws Exception {
    assertStoppingWaiter(
        new BoundedDatabaseBudget(Map.of("mysql-it", limits()), new SimpleMeterRegistry()), false);
  }

  private void assertStoppingWaiter(BoundedDatabaseBudget budget, boolean releaseWhileWaiting)
      throws Exception {
    var held = budget.acquire("mysql-it", WorkClass.TRADE, soon());
    var entered = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      try {
        var attempt =
            executor.submit(
                () -> {
                  entered.countDown();
                  try (var permit =
                      budget.acquire(
                          "mysql-it",
                          WorkClass.TRADE,
                          new Deadline(Instant.now().plusSeconds(5)))) {
                    return "unexpected";
                  }
                });
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(100);
        budget.stopAccepting();
        if (releaseWhileWaiting) held.close();
        assertThatThrownBy(() -> attempt.get(500, TimeUnit.MILLISECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RejectedExecutionException.class);
      } finally {
        held.close();
      }
    }
    assertThat(budget.available("mysql-it", WorkClass.TRADE)).isEqualTo(1);
    assertThat(budget.isIdle()).isTrue();
  }

  @Test
  public void interruptedBoundedTradeWaitPreservesInterruptWithoutLeaking() throws Exception {
    var budget =
        new BoundedDatabaseBudget(
            Map.of("mysql-it", limits()), new SimpleMeterRegistry(), Duration.ofSeconds(10));
    var held = budget.acquire("mysql-it", WorkClass.TRADE, soon());
    var entered = new CountDownLatch(1);
    var interrupted = new java.util.concurrent.atomic.AtomicBoolean();
    var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    var waiter =
        new Thread(
            () -> {
              entered.countDown();
              try (var permit =
                  budget.acquire(
                      "mysql-it", WorkClass.TRADE, new Deadline(Instant.now().plusSeconds(30)))) {
                failure.set(new AssertionError("Unexpected admission"));
              } catch (RejectedExecutionException e) {
                interrupted.set(Thread.currentThread().isInterrupted());
                failure.set(e);
              }
            });
    try {
      waiter.start();
      assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
      Thread.sleep(100);
      waiter.interrupt();
      waiter.join(1000);
      assertThat(waiter.isAlive()).isFalse();
      assertThat(failure.get()).isInstanceOf(RejectedExecutionException.class);
      assertThat(interrupted.get()).isTrue();
    } finally {
      held.close();
      waiter.interrupt();
      waiter.join(1000);
    }
    assertThat(budget.available("mysql-it", WorkClass.TRADE)).isEqualTo(1);
    assertThat(budget.isIdle()).isTrue();
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
