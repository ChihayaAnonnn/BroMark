package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.task.model.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 表名及列名只来自生产仓储常量；所有DATETIME按UTC读取。 */
final class RecoveryScanSql {
  private record Key(Instant time, String id) {}

  static RecoveryScanPage scan(
      JdbcTemplate j,
      String table,
      String id,
      String state,
      RecoveryScanPosition cursor,
      Instant cutoff,
      int limit) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Scan requires owned transaction");
    if (limit < 1 || limit > 64 || cutoff == null)
      throw new IllegalArgumentException("Scan bounds");
    var c = cursor == null ? new RecoveryScanPosition(null, "", null, "") : cursor;
    Instant upper = c.upper();
    String upperId = c.upperId();
    if (upper == null) {
      var max =
          j.query(
              "select last_progress_at,"
                  + id
                  + " from "
                  + table
                  + " where recovery_status='ACTIVE' and state=? and last_progress_at<=? order by"
                  + " last_progress_at desc,"
                  + id
                  + " desc limit 1",
              (r, n) ->
                  new Key(
                      r.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                      r.getString(2)),
              state,
              utc(cutoff));
      if (max.isEmpty()) return new RecoveryScanPage(List.of(), c, true);
      upper = max.getFirst().time();
      upperId = max.getFirst().id();
    }
    var args = new ArrayList<Object>(List.of(state, utc(cutoff), utc(upper), utc(upper), upperId));
    String after = "";
    if (c.after() != null) {
      after = " and (last_progress_at>? or (last_progress_at=? and " + id + ">?))";
      args.add(utc(c.after()));
      args.add(utc(c.after()));
      args.add(c.afterId());
    }
    args.add(limit);
    var keys =
        j.query(
            "select last_progress_at,"
                + id
                + " from "
                + table
                + " where recovery_status='ACTIVE' and state=? and last_progress_at<=? and"
                + " (last_progress_at<? or (last_progress_at=? and "
                + id
                + "<=?))"
                + after
                + " order by last_progress_at,"
                + id
                + " limit ?",
            (r, n) ->
                new Key(
                    r.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC), r.getString(2)),
            args.toArray());
    var last = keys.isEmpty() ? null : keys.getLast();
    var next =
        new RecoveryScanPosition(
            last == null ? c.after() : last.time(),
            last == null ? c.afterId() : last.id(),
            upper,
            upperId);
    return new RecoveryScanPage(
        keys.stream().map(Key::id).toList(),
        next,
        keys.size() < limit
            || (last != null && last.time().equals(upper) && last.id().equals(upperId)));
  }

  static LocalDateTime utc(Instant t) {
    return LocalDateTime.ofInstant(t, ZoneOffset.UTC);
  }
}
