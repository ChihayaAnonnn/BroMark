package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import cn.bugstack.middleware.db.router.config.DataSourceAutoConfig;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.transaction.DatabaseBusinessClock;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

/** 真实隔离PostgreSQL三库验证；不以H2替代PG事务/JSON/唯一冲突语义。 */
public class PostgresV2IntegrationIT {
  private void run(Consumer<AssetTestSupport> test) {
    run(test, "1", "1");
  }

  private void run(Consumer<AssetTestSupport> test, String connect, String socket) {
    String host = Objects.requireNonNull(System.getenv("V2_PG_HOST"));
    String password = Objects.requireNonNull(System.getenv("V2_PG_PASSWORD"));
    var context =
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfig.class))
            .withPropertyValues(
                "mini-db-router.jdbc.datasource.dbCount=2",
                "mini-db-router.jdbc.datasource.tbCount=4",
                "mini-db-router.jdbc.datasource.routerKey=userId",
                "mini-db-router.jdbc.datasource.default=db00",
                "mini-db-router.jdbc.datasource.list=db01,db02",
                "mini-db-router.jdbc.datasource.global.username=postgres",
                "mini-db-router.jdbc.datasource.global.password=" + password,
                "mini-db-router.jdbc.datasource.global.driver-class-name=org.postgresql.Driver",
                "mini-db-router.jdbc.datasource.global.pool.maximum-pool-size=8",
                "mini-db-router.jdbc.datasource.global.pool.connection-init-sql=SET TIME ZONE"
                    + " 'UTC'",
                "mini-db-router.jdbc.datasource.global.pool.connection-timeout=1000",
                "mini-db-router.jdbc.datasource.global.pool.minimum-idle=0");
    for (int i = 0; i < 3; i++)
      context =
          context.withPropertyValues(
              "mini-db-router.jdbc.datasource.db0"
                  + i
                  + ".url=jdbc:postgresql://"
                  + host
                  + "/"
                  + Objects.requireNonNull(System.getenv("V2_DB0" + i))
                  + "?connectTimeout="
                  + connect
                  + "&socketTimeout="
                  + socket
                  + "&stringtype=unspecified&options=-c%20TimeZone%3DUTC");
    context.run(
        c -> {
          assertThat(c).hasNotFailed();
          test.accept(
              new AssetTestSupport(
                  c.getBean(DataSource.class), c.getBean(PlatformTransactionManager.class)));
        });
  }

  private String user() {
    return "pg-it-" + UUID.randomUUID();
  }

  private BusinessId id(String kind, String key) {
    return new BusinessId(kind, key, "a".repeat(64));
  }

  private EventEnvelope event(String user, RouteSlot slot) {
    String key = UUID.randomUUID().toString();
    return new EventEnvelope(
        key,
        "PgProbe",
        key,
        id("PG", key),
        user,
        slot,
        "b".repeat(64),
        0,
        1,
        "{\"text\":\"中文\",\"amount\":1.00}");
  }

  @Test
  public void boundedStartupAcceptsActualPostgresSocketAndConnectTimeouts() {
    run(
        h ->
            com.jay.config.V2TransactionConfig.validateNetworkTimeouts(
                h.j.getDataSource(), new com.jay.config.V2RelayProperties().policy()));
  }

  @Test
  public void boundedStartupRejectsUnlimitedOrOverBudgetPostgresTimeouts() {
    for (String[] pair : new String[][] {{"0", "1"}, {"1", "0"}, {"2", "1"}, {"1", "2"}})
      run(
          h ->
              assertThatThrownBy(
                      () ->
                          com.jay.config.V2TransactionConfig.validateNetworkTimeouts(
                              h.j.getDataSource(), new com.jay.config.V2RelayProperties().policy()))
                  .isInstanceOf(IllegalArgumentException.class),
          pair[0],
          pair[1]);
  }

  @Test
  public void routesToActualThreePostgresDatabasesAndAllEightUserShards() {
    run(
        h -> {
          assertThat(h.j.queryForObject("select current_database()", String.class))
              .isEqualTo(System.getenv("V2_DB00"));
          for (int db = 1; db <= 2; db++)
            for (int tb = 0; tb < 4; tb++) {
              int database = db;
              var slot = new RouteSlot("0" + db, "00" + tb);
              h.runner.inSlot(
                  slot,
                  WorkClass.QUERY,
                  new Deadline(Instant.now().plusSeconds(10)),
                  () -> {
                    assertThat(h.j.queryForObject("select current_database()", String.class))
                        .isEqualTo(System.getenv("V2_DB0" + database));
                    assertThat(
                            h.j.queryForObject(
                                "select count(*) from "
                                    + MySqlAssetRepository.table("user_quota_ledger"),
                                Long.class))
                        .isNotNull();
                    return null;
                  });
            }
        });
  }

  @Test
  public void creditIdempotencyConflictAndRollbackKeepTransactionAtomic() {
    run(
        h -> {
          String u = user();
          var biz = id("CREDIT", "same");
          h.tx(
              u,
              () -> {
                var first = h.assets.credit(u, biz, new BigDecimal("10.00"));
                assertThat(h.assets.credit(u, biz, new BigDecimal("10.00"))).isEqualTo(first);
                return null;
              });
          assertThatThrownBy(() -> h.tx(u, () -> h.assets.credit(u, biz, new BigDecimal("11.00"))))
              .isInstanceOf(AssetConflictException.class);
          assertThatThrownBy(
                  () ->
                      h.tx(
                          u,
                          () -> {
                            h.assets.debit(u, id("DEBIT", "rollback"), BigDecimal.ONE);
                            throw new IllegalStateException("rollback");
                          }))
              .isInstanceOf(IllegalStateException.class);
          h.tx(
              u,
              () -> {
                assertThat(h.repo.balance(u)).isEqualByComparingTo("10.00");
                return null;
              });
        });
  }

  @Test
  public void concurrentDebitNeverOverspendsAndDuplicateQuotaIsSingleEffect() {
    run(
        h -> {
          String u = user();
          h.tx(u, () -> h.assets.credit(u, id("OPEN", "credit"), BigDecimal.TEN));
          var executor = Executors.newFixedThreadPool(16);
          try {
            var jobs = new ArrayList<Future<Boolean>>();
            for (int n = 0; n < 32; n++) {
              String key = "debit" + n;
              jobs.add(
                  executor.submit(
                      () ->
                          h.tx(
                              u,
                              () ->
                                  h.assets
                                      .debit(u, id("DEBIT", key), BigDecimal.ONE)
                                      .isPresent())));
            }
            long success = 0;
            for (var job : jobs) if (job.get(30, TimeUnit.SECONDS)) success++;
            assertThat(success).isEqualTo(10);
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            executor.shutdownNow();
          }
          h.tx(
              u,
              () -> {
                assertThat(h.repo.balance(u)).isEqualByComparingTo("0.00");
                var time = h.assets.lockAccounts(u, List.of(777L));
                var q = new QuotaKernel(h.repo);
                var b = id("GRANT", "quota");
                var first = q.grant(u, 777L, b, new QuotaDelta(20, 20, 20), time);
                assertThat(q.grant(u, 777L, b, new QuotaDelta(20, 20, 20), time)).isEqualTo(first);
                assertThat(h.repo.quotaState(u, 777L, time).total()).isEqualTo(20);
                return null;
              });
        });
  }

  @Test
  public void duplicateInboxDoesNotAbortPostgresTransactionAndChangedPayloadRollsBack() {
    run(
        h -> {
          String u = user();
          var e = event(u, h.codec.routeUser(u));
          var inbox = new MySqlConsumerInbox(h.j.getDataSource());
          h.tx(
              u,
              () -> {
                assertThat(inbox.tryInsert("pg-it", e)).isTrue();
                assertThat(inbox.tryInsert("pg-it", e)).isFalse();
                assertThat(inbox.tryStartOperation("pg-it", e)).isTrue();
                inbox.completeOperation("pg-it", e, List.of(e));
                assertThat(inbox.tryStartOperation("pg-it", e)).isFalse();
                assertThat(inbox.responses("pg-it", e)).containsExactly(e);
                assertThat(h.j.queryForObject("select 42", Integer.class)).isEqualTo(42);
                return null;
              });
          var changed =
              new EventEnvelope(
                  e.eventId(),
                  e.eventType(),
                  e.aggregateId(),
                  e.bizId(),
                  e.userId(),
                  e.routeSlot(),
                  e.requestHash(),
                  0,
                  1,
                  "changed");
          assertThatThrownBy(() -> h.tx(u, () -> inbox.tryInsert("pg-it", changed)))
              .isInstanceOf(AssetConflictException.class);
        });
  }

  @Test
  public void outboxJsonbRoundTripClaimCompleteAndDuplicateKeepHash() {
    run(
        h -> {
          String u = user();
          var slot = h.codec.routeUser(u);
          var e = event(u, slot);
          var writer = new MySqlOutboxWriter(h.j.getDataSource());
          h.tx(
              u,
              () -> {
                writer.append(e);
                writer.append(e);
                return null;
              });
          var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
          var leases = repo.claim(slot, 128, Duration.ofSeconds(30));
          var lease =
              leases.stream()
                  .filter(l -> l.event().eventId().equals(e.eventId()))
                  .findFirst()
                  .orElseThrow();
          assertThat(lease.event()).isEqualTo(e);
          assertThat(repo.complete(lease)).isTrue();
          assertThat(repo.complete(lease)).isFalse();
        });
  }

  @Test
  public void databaseClockAdvancesInsideTransactionAndRawBytesRoundTrip() {
    run(
        h -> {
          String u = user();
          h.tx(
              u,
              () -> {
                var clock = new DatabaseBusinessClock(h.j.getDataSource());
                var before = clock.now();
                h.j.execute("select pg_sleep(0.02)");
                var after = clock.now();
                assertThat(after.instant()).isAfter(before.instant());
                byte[] body = {0, (byte) 255, (byte) 128, 92};
                h.j.update(
                    "insert into message_quarantine values(?,?,?,?,UTC_TIMESTAMP(6))",
                    "pg-it",
                    UUID.randomUUID().toString(),
                    body,
                    "raw");
                assertThat(
                        h.j.queryForObject(
                            "select body from message_quarantine where consumer_name='pg-it' order"
                                + " by created_at desc limit 1",
                            byte[].class))
                    .containsExactly(body);
                return null;
              });
        });
  }
}
