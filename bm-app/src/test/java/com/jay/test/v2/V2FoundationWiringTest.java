package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.infrastructure.transaction.BoundedDatabaseBudget;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

public class V2FoundationWiringTest {
  @Configuration(proxyBeanMethods = false)
  @Import(V2TransactionConfig.class)
  static class Foundation {
    @Bean
    SimpleMeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @Test
  public void virtualWorkerUsesBudgetAndContextShutdownDrainsClosesPools() {
    var sources = new java.util.ArrayList<HikariDataSource>();
    var budgetRef = new java.util.concurrent.atomic.AtomicReference<BoundedDatabaseBudget>();
    Java25RouterIntegrationTest.databaseContext()
        .withUserConfiguration(Foundation.class)
        .withPropertyValues("bm.v2.enabled=true")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              var runner = c.getBean(UserTransactionRunner.class);
              var budget = c.getBean(BoundedDatabaseBudget.class);
              budgetRef.set(budget);
              var worker = c.getBean(BoundedVirtualExecutor.class);
              var ds = c.getBean(javax.sql.DataSource.class);
              var metrics = c.getBean(SimpleMeterRegistry.class);
              assertThat(
                      worker
                          .submit(
                              () ->
                                  runner.inUser(
                                      "user-0",
                                      WorkClass.WORKER,
                                      new Deadline(Instant.now().plusSeconds(5)),
                                      () -> {
                                        assertThat(Thread.currentThread().isVirtual()).isTrue();
                                        assertThat(budget.available("mysql-it", WorkClass.WORKER))
                                            .isZero();
                                        return new JdbcTemplate(ds)
                                            .queryForObject("select database()", String.class);
                                      }))
                          .get(5, TimeUnit.SECONDS))
                  .isEqualTo("router_two");
              assertThat(budget.available("mysql-it", WorkClass.WORKER)).isEqualTo(1);
              assertThat(metrics.find("hikaricp.connections.acquire").timer()).isNotNull();
              assertThat(metrics.get("bm.v2.database.lock.wait").gauge().value())
                  .isGreaterThanOrEqualTo(0);
              var dynamic = (cn.bugstack.middleware.db.router.dynamic.DynamicDataSource) ds;
              for (var pool : dynamic.getResolvedDataSources().values())
                sources.add((HikariDataSource) pool);
            });
    for (var pool : sources) assertThat(pool.isClosed()).isTrue();
    assertThatThrownBy(
            () ->
                budgetRef
                    .get()
                    .acquire(
                        "mysql-it", WorkClass.QUERY, new Deadline(Instant.now().plusSeconds(1))))
        .isInstanceOf(RejectedExecutionException.class);
  }

  @Test
  public void rejectsBudgetsThatCanExhaustAConcentratedLogicalPool() {
    Java25RouterIntegrationTest.databaseContext()
        .withUserConfiguration(Foundation.class)
        .withPropertyValues(
            "bm.v2.enabled=true", "mini-db-router.jdbc.datasource.global.pool.maximum-pool-size=2")
        .run(c -> assertThat(c).hasFailed());
  }
}
