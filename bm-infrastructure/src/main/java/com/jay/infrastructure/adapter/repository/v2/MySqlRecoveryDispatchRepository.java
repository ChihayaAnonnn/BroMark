package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.RecoveryDispatchRepository;
import java.time.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MySqlRecoveryDispatchRepository implements RecoveryDispatchRepository {
  private final JdbcTemplate j;

  public MySqlRecoveryDispatchRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  private void requireTx() {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Dispatch requires local transaction");
  }

  public RedriveState lock(String type, String id) {
    requireTx();
    j.update(
        "insert into v2_recovery_dispatch(aggregate_type,aggregate_id) values(?,?) ON DUPLICATE KEY"
            + " UPDATE aggregate_id=aggregate_id",
        type,
        id);
    var saved =
        j.queryForObject(
            "select * from v2_recovery_dispatch where aggregate_type=? and aggregate_id=? for"
                + " update",
            (r, i) ->
                new RedriveState(
                    type,
                    id,
                    r.getString("observed_state"),
                    r.getLong("observed_version"),
                    r.getString("last_event_type"),
                    r.getString("last_event_id"),
                    r.getObject("last_redrive_at", LocalDateTime.class) == null
                        ? null
                        : r.getObject("last_redrive_at", LocalDateTime.class)
                            .toInstant(ZoneOffset.UTC),
                    r.getObject("next_redrive_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                    r.getInt("redrive_count")),
            type,
            id);
    if (saved.count() > 0
        && new MySqlRecoveryRepository(j.getDataSource())
            .auditedReplayAfter(type, id, saved.lastEventId(), saved.lastRedriveAt()))
      return new RedriveState(
          type,
          id,
          saved.observedState(),
          saved.observedVersion(),
          saved.lastEventType(),
          saved.lastEventId(),
          saved.lastRedriveAt(),
          saved.nextRedriveAt(),
          0);
    return saved;
  }

  public void record(
      String type,
      String id,
      String state,
      long version,
      EventEnvelope event,
      Instant now,
      Duration interval) {
    record(type, id, state, version, event, now, interval, null);
  }

  public void record(
      String type,
      String id,
      String state,
      long version,
      EventEnvelope event,
      Instant now,
      Duration interval,
      Instant lastProgressAt) {
    requireTx();
    // 旧签名保守累加；显式进展时间必须来自最终锁定的业务行。
    var saved = lock(type, id);
    boolean progressed =
        !state.equals(saved.observedState())
            && saved.lastRedriveAt() != null
            && lastProgressAt != null
            && lastProgressAt.isAfter(saved.lastRedriveAt())
            && !lastProgressAt.isAfter(now);
    int count = progressed ? 1 : Math.addExact(saved.count(), 1);
    if (j.update(
            "update v2_recovery_dispatch set"
                + " redrive_count=?,observed_state=?,observed_version=?,last_event_type=?,last_event_id=?,last_redrive_at=?,next_redrive_at=?"
                + " where aggregate_type=? and aggregate_id=?",
            count,
            state,
            version,
            event.eventType(),
            event.eventId(),
            LocalDateTime.ofInstant(now, ZoneOffset.UTC),
            LocalDateTime.ofInstant(now.plus(interval), ZoneOffset.UTC),
            type,
            id)
        != 1) throw new IllegalStateException("Dispatch row missing");
  }
}
