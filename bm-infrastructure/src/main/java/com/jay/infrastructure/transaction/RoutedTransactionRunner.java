package com.jay.infrastructure.transaction;

import cn.bugstack.middleware.db.router.DBContextHolder;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.asset.service.RouteCodec;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

public final class RoutedTransactionRunner implements UserTransactionRunner {
  private final TransactionTemplate tx;
  private final RouteCodec codec;
  private final com.jay.domain.asset.service.DatabaseBudget budget;
  private final java.util.Map<String, String> physical;
  private final io.micrometer.core.instrument.MeterRegistry metrics;

  private record Execution(RouteSlot slot, WorkClass work) {}

  private final ThreadLocal<Execution> current = new ThreadLocal<>();

  public RoutedTransactionRunner(
      DataSource ds, PlatformTransactionManager manager, RouteCodec codec) {
    this(
        ds,
        manager,
        codec,
        new BoundedDatabaseBudget(
            defaultLimits(), new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
        java.util.Map.of("00", "mysql-it", "01", "mysql-it", "02", "mysql-it"),
        new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
  }

  private static java.util.Map<String, java.util.Map<WorkClass, Integer>> defaultLimits() {
    var m = new java.util.EnumMap<WorkClass, Integer>(WorkClass.class);
    for (var w : WorkClass.values()) m.put(w, 1);
    return java.util.Map.of("mysql-it", m);
  }

  public RoutedTransactionRunner(
      DataSource ds,
      PlatformTransactionManager manager,
      RouteCodec codec,
      com.jay.domain.asset.service.DatabaseBudget budget,
      java.util.Map<String, String> physical,
      io.micrometer.core.instrument.MeterRegistry metrics) {
    this.codec = codec;
    this.budget = budget;
    this.physical = java.util.Map.copyOf(physical);
    this.metrics = metrics;
    tx = new TransactionTemplate(manager);
    tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  public <T> T inUser(String user, WorkClass work, Deadline deadline, Supplier<T> action) {
    return inSlot(codec.routeUser(user), work, deadline, action);
  }

  public <T> T inControl(WorkClass work, Deadline deadline, Supplier<T> action) {
    return inSlot(RouteSlot.control(), work, deadline, action);
  }

  public <T> T inSlot(RouteSlot slot, WorkClass work, Deadline deadline, Supplier<T> action) {
    java.util.Objects.requireNonNull(slot);
    java.util.Objects.requireNonNull(work);
    java.util.Objects.requireNonNull(action);
    if (deadline.remaining().isNegative() || deadline.remaining().isZero())
      throw new BudgetExceededException("Deadline exceeded");
    Execution outer = current.get();
    if (outer != null) {
      if (!outer.slot().equals(slot) || outer.work() != work)
        throw new IllegalStateException("Nested route/work change is forbidden");
      return action.get();
    }
    if (TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Enter routed runner before starting a transaction");
    String previousDb = DBContextHolder.getDBKey(), previousTb = DBContextHolder.getTBKey();
    String instance = physical.get(slot.dbKey());
    if (instance == null) throw new IllegalArgumentException("Physical route mapping required");
    try (Permit permit = budget.acquire(instance, work, deadline)) {
      DBContextHolder.setRoute(slot.dbKey(), slot.tableKey());
      current.set(new Execution(slot, work));
      long start = System.nanoTime();
      String outcome = "committed";
      try {
        return tx.execute(s -> action.get());
      } catch (RuntimeException | Error e) {
        outcome = "rolled_back";
        throw e;
      } finally {
        metrics
            .timer("bm.v2.database.transaction", "work", work.name(), "result", outcome)
            .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
      }
    } finally {
      current.remove();
      DBContextHolder.setRoute(previousDb, previousTb);
    }
  }
}
