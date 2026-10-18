package com.jay.infrastructure.observability;

import cn.bugstack.middleware.db.router.DBContextHolder;
import com.jay.domain.raffle.service.P0BacklogSnapshot;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 每秒刷新 P0 订单积压快照。
 *
 * <p>查询 8 张 {@code v2_draw_order_xxx} 只统计 {@code PENDING_RESERVATION}，命中 {@code
 * draw_backlog(state,last_progress_at,order_id)} 索引。任何一次刷新失败都保留上一次成功快照。
 */
public final class P0BacklogProbe implements AutoCloseable {
  static final long REFRESH_MILLIS = 1_000;
  private static final Logger log = LoggerFactory.getLogger(P0BacklogProbe.class);
  private static final List<String> DATABASES = List.of("01", "02");
  private static final int TABLE_COUNT = 4;

  private final JdbcTemplate jdbc;
  private final Consumer<P0BacklogSnapshot> sink;
  private final ScheduledExecutorService scheduler;
  private volatile P0BacklogSnapshot snapshot = new P0BacklogSnapshot(0, 0);

  public P0BacklogProbe(DataSource dataSource) {
    this(dataSource, ignored -> {});
  }

  public P0BacklogProbe(DataSource dataSource, Consumer<P0BacklogSnapshot> sink) {
    jdbc = new JdbcTemplate(Objects.requireNonNull(dataSource));
    this.sink = Objects.requireNonNull(sink);
    refresh();
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              var thread = new Thread(runnable, "p0-backlog-probe");
              thread.setDaemon(true);
              return thread;
            });
    scheduler.scheduleWithFixedDelay(
        () -> {
          try {
            refresh();
          } catch (RuntimeException e) {
            log.debug("P0 backlog probe refresh failed", e);
          }
        },
        REFRESH_MILLIS,
        REFRESH_MILLIS,
        TimeUnit.MILLISECONDS);
  }

  public P0BacklogSnapshot snapshot() {
    return snapshot;
  }

  /** 同步刷新一次；失败时返回上一份快照。 */
  public P0BacklogSnapshot refresh() {
    String previousDb = DBContextHolder.getDBKey();
    String previousTable = DBContextHolder.getTBKey();
    try {
      long pending = 0;
      long oldestAge = 0;
      for (String database : DATABASES) {
        for (int table = 0; table < TABLE_COUNT; table++) {
          String suffix = String.format("%03d", table);
          DBContextHolder.setRoute(database, suffix);
          long[] row =
              jdbc.queryForObject(
                  "select count(*),coalesce(timestampdiff(second,min(last_progress_at),utc_timestamp(6)),0)"
                      + " from v2_draw_order_"
                      + suffix
                      + " where state='PENDING_RESERVATION'",
                  (rs, index) -> new long[] {rs.getLong(1), rs.getLong(2)});
          pending += row[0];
          oldestAge = Math.max(oldestAge, row[1]);
        }
      }
      var next = new P0BacklogSnapshot(pending, Math.max(0, oldestAge));
      snapshot = next;
      sink.accept(next);
      return next;
    } catch (RuntimeException e) {
      log.debug("P0 backlog snapshot retained after failed refresh", e);
      return snapshot;
    } finally {
      DBContextHolder.setRoute(previousDb, previousTable);
    }
  }

  @Override
  public void close() {
    scheduler.shutdownNow();
  }
}
