package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import cn.bugstack.middleware.db.router.DBContextHolder;
import cn.bugstack.middleware.db.router.dynamic.PostgresConnection;
import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.EventEnvelope;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

/** 真实 PG 方言/事务契约；临时表遮蔽正式表，连接关闭自动回收，不污染压测数据。 */
public class PostgresSqlContractIT {
  private static final class Database implements AutoCloseable {
    final SingleConnectionDataSource ds;
    final JdbcTemplate j;
    final TransactionTemplate tx;

    Database() throws SQLException {
      String host = Objects.requireNonNull(System.getenv("V2_PG_HOST"));
      String database = Objects.requireNonNull(System.getenv("V2_DB00"));
      var raw =
          DriverManager.getConnection(
              "jdbc:postgresql://" + host + "/" + database + "?options=-c%20TimeZone%3DUTC",
              "postgres",
              Objects.requireNonNull(System.getenv("V2_PG_PASSWORD")));
      try (Statement setup = raw.createStatement()) {
        setup.execute("SET TIME ZONE 'UTC'");
      }
      ds = new SingleConnectionDataSource(PostgresConnection.wrap(raw), true);
      j = new JdbcTemplate(ds);
      tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    void isolate(String... tables) {
      for (String table : tables) {
        if (!table.matches("[a-z_0-9]+")) throw new IllegalArgumentException("table");
        j.execute("create temporary table " + table + " (like public." + table + " including all)");
      }
    }

    public void close() {
      ds.destroy();
    }
  }

  @Test
  public void timestampReadByIndexAndNamePreservesUtcInstantInNonUtcJvm() throws Exception {
    TimeZone previous = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
      try (var h = new Database()) {
        h.j.execute(
            "create temporary table pg_utc_contract(business_at timestamp(6) without time zone)");
        Instant expected = Instant.parse("2026-10-18T01:02:03.123456Z");
        assertThat(h.j.queryForObject("show TimeZone", String.class)).isEqualTo("UTC");
        // 服务端写入，避免默认时区下的写入与回读偏移相互抵消。
        h.j.execute(
            "insert into pg_utc_contract values(CAST('2026-10-18 01:02:03.123456' AS timestamp))");
        assertThat(
                h.j.queryForObject("select business_at from pg_utc_contract", LocalDateTime.class))
            .isEqualTo(LocalDateTime.ofInstant(expected, ZoneOffset.UTC));
        h.j.query(
            "select business_at from pg_utc_contract",
            rows -> {
              assertThat(rows.next()).isTrue();
              assertThat(rows.getTimestamp(1).toInstant()).isEqualTo(expected);
              assertThat(rows.getTimestamp("business_at").toInstant()).isEqualTo(expected);
              return null;
            });
      }
    } finally {
      TimeZone.setDefault(previous);
    }
  }

  @Test
  public void timestampAndObjectBindersPersistUtcFieldsInNonUtcJvm() throws Exception {
    TimeZone previous = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
      try (var h = new Database()) {
        h.j.execute(
            "create temporary table pg_utc_bind_contract(kind text,business_at timestamp(6) without"
                + " time zone)");
        Instant expected = Instant.parse("2026-10-18T01:02:03.123456Z");
        LocalDateTime utcFields = LocalDateTime.ofInstant(expected, ZoneOffset.UTC);
        h.j.update(
            "insert into pg_utc_bind_contract values('timestamp',?)",
            statement -> statement.setTimestamp(1, Timestamp.from(expected)));
        assertThat(
                h.j.queryForObject(
                    "select business_at from pg_utc_bind_contract where kind='timestamp'",
                    LocalDateTime.class))
            .isEqualTo(utcFields);
        h.j.update(
            "insert into pg_utc_bind_contract values('object',?)",
            statement -> statement.setObject(1, Timestamp.from(expected)));
        assertThat(
                h.j.queryForObject(
                    "select business_at from pg_utc_bind_contract where kind='object'",
                    LocalDateTime.class))
            .isEqualTo(utcFields);
      }
    } finally {
      TimeZone.setDefault(previous);
    }
  }

  @Test
  public void retryUpsertOnlyNewerFailureClearsLeaseAndRestartsPending() throws Exception {
    try (var h = new Database()) {
      h.isolate("retry_job");
      var repo = new MySqlRetryJobRepository(h.ds, null);
      String id = UUID.randomUUID().toString();
      var event =
          new EventEnvelope(
              id,
              "RetryProbe",
              id,
              new BusinessId("PG", id, "a".repeat(64)),
              "pg-sql",
              new RouteSlot("01", "000"),
              "b".repeat(64),
              0,
              1,
              "中文");
      Instant first = Instant.parse("2026-10-18T01:02:03Z"), next = first.plusSeconds(60);
      h.tx.execute(
          status -> {
            repo.schedule("probe", event, 1, first);
            h.j.update(
                "update retry_job set"
                    + " state='PROCESSING',lease_token='lease',lease_until=TIMESTAMPADD(SECOND,30,UTC_TIMESTAMP(6))"
                    + " where event_id=?",
                id);
            var lease =
                h.j.queryForObject(
                    "select lease_until from retry_job where event_id=?", LocalDateTime.class, id);
            repo.schedule("probe", event, 1, next);
            var unchanged = h.j.queryForMap("select * from retry_job where event_id=?", id);
            assertThat(unchanged.get("state")).isEqualTo("PROCESSING");
            assertThat(unchanged.get("lease_token")).isEqualTo("lease");
            assertThat(((Timestamp) unchanged.get("lease_until")).toLocalDateTime())
                .isEqualTo(lease);
            assertThat(
                    h.j.queryForObject(
                        "select next_attempt_at from retry_job where event_id=?",
                        LocalDateTime.class,
                        id))
                .isEqualTo(LocalDateTime.ofInstant(first, ZoneOffset.UTC));
            repo.schedule("probe", event, 2, next);
            var newer = h.j.queryForMap("select * from retry_job where event_id=?", id);
            assertThat(newer.get("state")).isEqualTo("PENDING");
            assertThat(newer.get("lease_token")).isNull();
            assertThat(newer.get("lease_until")).isNull();
            assertThat(newer.get("failure_count")).isEqualTo(2);
            assertThat(newer.get("payload")).isInstanceOf(String.class);
            assertThat(
                    h.j.queryForObject(
                        "select next_attempt_at from retry_job where event_id=?",
                        LocalDateTime.class,
                        id))
                .isEqualTo(LocalDateTime.ofInstant(next, ZoneOffset.UTC));
            return null;
          });
      assertThat(h.j.queryForObject("select count(*) from retry_job", Integer.class)).isEqualTo(1);
    }
  }

  @Test
  public void duplicatePoolInitializationCommitsOnceAndChangedCommandRollsBack() throws Exception {
    try (var h = new Database()) {
      h.isolate("award_stock", "stock_change_log");
      var repo = new MySqlStockRepository(h.ds);
      DBContextHolder.setDBKey("00");
      try {
        h.tx.execute(
            status -> {
              repo.createPool("pool", "init", 10, "probe");
              repo.createPool("pool", "init", 10, "probe");
              return null;
            });
        assertThat(
                h.j.queryForObject(
                    "select total from award_stock where stock_pool_id='pool'", Long.class))
            .isEqualTo(10);
        assertThat(h.j.queryForObject("select count(*) from stock_change_log", Integer.class))
            .isEqualTo(1);
        assertThatThrownBy(
                () ->
                    h.tx.execute(
                        status -> {
                          repo.createPool("pool", "changed", 20, "probe");
                          return null;
                        }))
            .isInstanceOf(AssetConflictException.class);
        assertThat(h.j.queryForObject("select 42", Integer.class)).isEqualTo(42);
      } finally {
        DBContextHolder.clearDBKey();
      }
    }
  }

  @Test
  public void reconciliationJsonDateAndEvidenceFunctionsKeepBusinessValues() throws Exception {
    try (var h = new Database()) {
      h.j.execute(
          "create temporary table pg_contract(snapshot jsonb,business_date date,evidence text)");
      h.j.update(
          "insert into pg_contract values(?,?,?)",
          "{\"finalAward\":{\"type\":\"CREDIT\",\"amount\":\"12.34\"}}",
          LocalDate.of(2026, 10, 18),
          "first");
      var row =
          h.j.queryForMap(
              "select json_unquote(json_extract(o.snapshot,'$.finalAward.type'))"
                  + " kind,cast(json_unquote(json_extract(o.snapshot,'$.finalAward.amount')) as"
                  + " decimal(18,2)) amount,cast(o.business_date as char) day_key from pg_contract"
                  + " o");
      assertThat(row.get("kind")).isEqualTo("CREDIT");
      assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("12.34");
      assertThat(row.get("day_key")).isEqualTo("2026-10-18");
      h.j.update("update pg_contract set evidence=CONCAT(evidence,CHAR(10),?)", "second");
      assertThat(h.j.queryForObject("select evidence from pg_contract", String.class))
          .isEqualTo("first\nsecond");
      h.isolate(
          "user_credit_account",
          "raffle_activity_account",
          "raffle_activity_account_month",
          "raffle_activity_account_day");
      for (int n = 0; n < 4; n++)
        for (String prefix :
            List.of(
                "user_credit_ledger",
                "user_quota_ledger",
                "v2_draw_order",
                "v2_award_record",
                "v2_award_delivery")) h.isolate(prefix + "_" + String.format("%03d", n));
      var report =
          h.tx.execute(
              status -> new MySqlRaffleReconciliation(h.ds).check(new RouteSlot("01", "000")));
      assertThat(report.creditMismatch()).isZero();
      assertThat(report.quotaMismatch()).isZero();
      assertThat(report.duplicateDelivery()).isZero();
    }
  }
}
