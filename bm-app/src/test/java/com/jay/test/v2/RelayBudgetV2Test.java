package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.V2RelayProperties;
import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.RelayPolicy;
import java.time.*;
import org.junit.Test;

public class RelayBudgetV2Test {
  private RelayPolicy policy(int limit, int window, int lease) {
    return new RelayPolicy(
        limit,
        window,
        4,
        8,
        Duration.ofMillis(250),
        Duration.ofSeconds(lease),
        Duration.ofSeconds(1),
        Duration.ofSeconds(3),
        Duration.ofSeconds(1),
        Duration.ofSeconds(20),
        Duration.ofSeconds(5));
  }

  @Test
  public void rejectsOversizedWindowAndUnsafeLease() {
    new V2RelayProperties().policy().validate(2, 16);
    assertThatThrownBy(() -> policy(32, 9, 30).validate(2, 16))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> policy(128, 8, 30).validate(2, 16))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> policy(32, 8, 22).validate(2, 16))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void deadlineRollsBackRealSlowStatementAndRouteCanBeReused() {
    AssetTestSupport.run(
        h -> {
          var slot = new RouteSlot("01", "000");
          long start = System.nanoTime();
          assertThatThrownBy(
                  () ->
                      h.runner.inSlot(
                          slot,
                          WorkClass.RELAY,
                          new Deadline(Instant.now().plusSeconds(1)),
                          () -> {
                            h.j.queryForObject("select sleep(3)", Integer.class);
                            return null;
                          }))
              .isInstanceOf(RuntimeException.class);
          assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
          assertThat(
                  h.runner.inSlot(
                      slot,
                      WorkClass.RELAY,
                      new Deadline(Instant.now().plusSeconds(5)),
                      () -> h.j.queryForObject("select 1", Integer.class)))
              .isEqualTo(1);
          assertThat(
                  org.springframework.transaction.support.TransactionSynchronizationManager
                      .isActualTransactionActive())
              .isFalse();
        });
  }

  @Test
  public void expiryBeforeCommitRollsBackWrites() {
    AssetTestSupport.run(
        h -> {
          var slot = new RouteSlot("01", "000");
          h.runner.inSlot(
              slot,
              WorkClass.RELAY,
              new Deadline(Instant.now().plusSeconds(5)),
              () -> {
                h.j.execute(
                    "create table if not exists relay_deadline_probe (id varchar(64) primary key)"
                        + " engine=InnoDB");
                return null;
              });
          String id = java.util.UUID.randomUUID().toString();
          assertThatThrownBy(
                  () ->
                      h.runner.inSlot(
                          slot,
                          WorkClass.RELAY,
                          new Deadline(Instant.now().plusMillis(100)),
                          () -> {
                            h.j.update("insert into relay_deadline_probe values (?)", id);
                            try {
                              Thread.sleep(150);
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                            }
                            return null;
                          }))
              .isInstanceOf(BudgetExceededException.class);
          assertThat(
                  h.runner.inSlot(
                      slot,
                      WorkClass.RELAY,
                      new Deadline(Instant.now().plusSeconds(5)),
                      () ->
                          h.j.queryForObject(
                              "select count(*) from relay_deadline_probe where id=?",
                              Integer.class,
                              id)))
              .isZero();
        });
  }

  @Test
  public void boundedStartupRejectsUnboundedNetworkReadTimeout() {
    Java25RouterIntegrationTest.databaseContext()
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          com.jay.config.V2TransactionConfig.validateNetworkTimeouts(
                              c.getBean(javax.sql.DataSource.class), policy(32, 8, 30)))
                  .isInstanceOf(IllegalArgumentException.class);
            });
  }

  @Test
  public void parallelDeadlinesDoNotChangeEachOthersStatementTimeout() {
    AssetTestSupport.run(
        h -> {
          try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var started = new java.util.concurrent.CountDownLatch(1);
            var slow =
                executor.submit(
                    () ->
                        h.runner.inSlot(
                            new RouteSlot("01", "000"),
                            WorkClass.TRADE,
                            new Deadline(Instant.now().plusSeconds(5)),
                            () -> {
                              started.countDown();
                              return h.j.queryForObject("select sleep(2)", Integer.class);
                            }));
            assertThat(started.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var shortCall =
                executor.submit(
                    () ->
                        h.runner.inSlot(
                            new RouteSlot("02", "000"),
                            WorkClass.TRADE,
                            new Deadline(Instant.now().plusSeconds(1)),
                            () -> h.j.queryForObject("select sleep(3)", Integer.class)));
            assertThatThrownBy(() -> shortCall.get(4, java.util.concurrent.TimeUnit.SECONDS))
                .isInstanceOf(java.util.concurrent.ExecutionException.class);
            assertThat(slow.get(6, java.util.concurrent.TimeUnit.SECONDS)).isZero();
          }
        });
  }

  @Test
  public void expiryDuringCommitHooksCannotCommitWrites() {
    AssetTestSupport.run(
        h -> {
          var slot = new RouteSlot("01", "000");
          h.runner.inSlot(
              slot,
              WorkClass.RELAY,
              new Deadline(Instant.now().plusSeconds(5)),
              () -> {
                h.j.execute(
                    "create table if not exists relay_commit_probe (id varchar(64) primary key)"
                        + " engine=InnoDB");
                return null;
              });
          for (boolean beforeCompletion : new boolean[] {false, true}) {
            String id = java.util.UUID.randomUUID().toString();
            assertThatThrownBy(
                    () ->
                        h.runner.inSlot(
                            slot,
                            WorkClass.RELAY,
                            new Deadline(Instant.now().plusMillis(200)),
                            () -> {
                              h.j.update("insert into relay_commit_probe values (?)", id);
                              org.springframework.transaction.support
                                  .TransactionSynchronizationManager.registerSynchronization(
                                  new org.springframework.transaction.support
                                      .TransactionSynchronization() {
                                    public void beforeCommit(boolean readOnly) {
                                      if (!beforeCompletion) delay();
                                    }

                                    public void beforeCompletion() {
                                      if (beforeCompletion) delay();
                                    }

                                    private void delay() {
                                      try {
                                        Thread.sleep(300);
                                      } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                      }
                                    }
                                  });
                              return null;
                            }))
                .isInstanceOf(BudgetExceededException.class);
            assertThat(
                    h.runner.inSlot(
                        slot,
                        WorkClass.RELAY,
                        new Deadline(Instant.now().plusSeconds(5)),
                        () ->
                            h.j.queryForObject(
                                "select count(*) from relay_commit_probe where id=?",
                                Integer.class,
                                id)))
                .isZero();
          }
        });
  }

  @Test
  public void boundedStartupRejectsPoolWaitBeyondDatabaseBudget() {
    boundedNetworkContext()
        .withPropertyValues("mini-db-router.jdbc.datasource.global.pool.connection-timeout=30000")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          com.jay.config.V2TransactionConfig.validateNetworkTimeouts(
                              c.getBean(javax.sql.DataSource.class), policy(32, 8, 30)))
                  .isInstanceOf(IllegalArgumentException.class);
            });
  }

  @Test
  public void boundedStartupAcceptsFiniteDriverAndPoolWait() {
    boundedNetworkContext()
        .withPropertyValues("mini-db-router.jdbc.datasource.global.pool.connection-timeout=1000")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              com.jay.config.V2TransactionConfig.validateNetworkTimeouts(
                  c.getBean(javax.sql.DataSource.class), policy(32, 8, 30));
            });
  }

  private org.springframework.boot.test.context.runner.ApplicationContextRunner
      boundedNetworkContext() {
    String host = System.getenv().getOrDefault("V2_MYSQL_HOST", "host.docker.internal:13316");
    return Java25RouterIntegrationTest.databaseContext()
        .withPropertyValues(
            "mini-db-router.jdbc.datasource.db00.url=jdbc:mysql://"
                + host
                + "/router_control?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=1000&socketTimeout=1000",
            "mini-db-router.jdbc.datasource.db01.url=jdbc:mysql://"
                + host
                + "/router_one?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=1000&socketTimeout=1000",
            "mini-db-router.jdbc.datasource.db02.url=jdbc:mysql://"
                + host
                + "/router_two?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=1000&socketTimeout=1000");
  }

  @Test
  public void exhaustedPoolTimesOutWithoutEnteringActionAndRemainsReusable() {
    boundedNetworkContext()
        .withPropertyValues("mini-db-router.jdbc.datasource.global.pool.connection-timeout=1000")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              var ds = c.getBean(javax.sql.DataSource.class);
              var pool =
                  (com.zaxxer.hikari.HikariDataSource)
                      ((cn.bugstack.middleware.db.router.dynamic.DynamicDataSource) ds)
                          .getResolvedDataSources()
                          .get("db01");
              var connections = new java.util.ArrayList<java.sql.Connection>();
              var entered = new java.util.concurrent.atomic.AtomicBoolean();
              var runner =
                  new com.jay.infrastructure.transaction.RoutedTransactionRunner(
                      ds,
                      c.getBean(org.springframework.transaction.PlatformTransactionManager.class),
                      new com.jay.domain.asset.service.RouteCodec(2, 4));
              try {
                for (int i = 0; i < pool.getMaximumPoolSize(); i++)
                  connections.add(pool.getConnection());
                long start = System.nanoTime();
                assertThatThrownBy(
                        () ->
                            runner.inSlot(
                                new RouteSlot("01", "000"),
                                WorkClass.RELAY,
                                new Deadline(Instant.now().plusMillis(100)),
                                () -> {
                                  entered.set(true);
                                  return null;
                                }))
                    .isInstanceOf(
                        org.springframework.transaction.CannotCreateTransactionException.class);
                assertThat(Duration.ofNanos(System.nanoTime() - start))
                    .isLessThan(Duration.ofSeconds(2));
                assertThat(entered).isFalse();
                assertThat(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey()).isNull();
              } catch (java.sql.SQLException e) {
                throw new AssertionError(e);
              } finally {
                for (var connection : connections) {
                  try {
                    connection.close();
                  } catch (java.sql.SQLException e) {
                    throw new AssertionError(e);
                  }
                }
              }
              assertThat(
                      runner.inSlot(
                          new RouteSlot("01", "000"),
                          WorkClass.RELAY,
                          new Deadline(Instant.now().plusSeconds(3)),
                          () ->
                              new org.springframework.jdbc.core.JdbcTemplate(ds)
                                  .queryForObject("select 1", Integer.class)))
                  .isEqualTo(1);
            });
  }
}
