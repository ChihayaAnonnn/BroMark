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
  private final PlatformTransactionManager manager;
  private final RouteCodec codec;
  private final com.jay.domain.asset.service.DatabaseBudget budget;
  private final java.util.Map<String, String> physical;
  private final io.micrometer.core.instrument.MeterRegistry metrics;

  private record Execution(RouteSlot slot, WorkClass work, Deadline deadline) {}

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
    if (manager.getClass() != org.springframework.jdbc.datasource.DataSourceTransactionManager.class
        || ((org.springframework.jdbc.datasource.DataSourceTransactionManager) manager)
                .getDataSource()
            != ds)
      throw new IllegalArgumentException(
          "Routed runner requires the local manager for its data source");
    var original = (org.springframework.jdbc.datasource.DataSourceTransactionManager) manager;
    // 校验落在真实 JDBC commit 前，覆盖 beforeCommit / beforeCompletion 的耗时。
    var bounded =
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds) {
          @Override
          protected void doCommit(DefaultTransactionStatus status) {
            requireTime(current.get().deadline());
            super.doCommit(status);
          }
        };
    bounded.setDefaultTimeout(original.getDefaultTimeout());
    bounded.setNestedTransactionAllowed(original.isNestedTransactionAllowed());
    bounded.setEnforceReadOnly(original.isEnforceReadOnly());
    bounded.setValidateExistingTransaction(original.isValidateExistingTransaction());
    bounded.setGlobalRollbackOnParticipationFailure(
        original.isGlobalRollbackOnParticipationFailure());
    bounded.setFailEarlyOnGlobalRollbackOnly(original.isFailEarlyOnGlobalRollbackOnly());
    bounded.setRollbackOnCommitFailure(original.isRollbackOnCommitFailure());
    bounded.setTransactionSynchronization(original.getTransactionSynchronization());
    bounded.setTransactionExecutionListeners(original.getTransactionExecutionListeners());
    this.manager = bounded;
  }

  private static void requireTime(Deadline deadline) {
    if (deadline.remaining().isZero() || deadline.remaining().isNegative())
      throw new BudgetExceededException("Deadline exceeded");
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
      T value = action.get();
      requireTime(deadline);
      return value;
    }
    if (TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Enter routed runner before starting a transaction");
    String previousDb = DBContextHolder.getDBKey(), previousTb = DBContextHolder.getTBKey();
    String instance = physical.get(slot.dbKey());
    if (instance == null) throw new IllegalArgumentException("Physical route mapping required");
    try (Permit permit = budget.acquire(instance, work, deadline)) {
      requireTime(deadline);
      DBContextHolder.setRoute(slot.dbKey(), slot.tableKey());
      current.set(new Execution(slot, work, deadline));
      long start = System.nanoTime();
      String outcome = "committed";
      boolean[] actionCompleted = {false};
      long[] actionEnd = {0};
      try {
        // 每次调用独立模板；Spring 将事务余量应用到 JdbcTemplate 的 Statement。
        var tx = new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        long nanos = deadline.remaining().toNanos();
        requireTime(deadline);
        tx.setTimeout(
            (int)
                Math.min(Integer.MAX_VALUE, Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L)));
        return tx.execute(
            s -> {
              requireTime(deadline);
              try {
                T value = action.get();
                requireTime(deadline);
                actionCompleted[0] = true;
                return value;
              } finally {
                actionEnd[0] = System.nanoTime();
              }
            });
      } catch (RuntimeException | Error e) {
        outcome =
            actionCompleted[0] && !(e instanceof BudgetExceededException)
                ? "commit_unknown"
                : "rolled_back";
        throw e;
      } finally {
        if (actionEnd[0] != 0)
          metrics
              .timer("bm.v2.database.cleanup", "work", work.name(), "result", outcome)
              .record(System.nanoTime() - actionEnd[0], java.util.concurrent.TimeUnit.NANOSECONDS);
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
