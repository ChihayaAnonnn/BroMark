package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.AssetConflictException;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.RecoveryRepository;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MySqlRecoveryRepository implements RecoveryRepository {
  private final JdbcTemplate j;

  public MySqlRecoveryRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  private void requireTx() {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Recovery requires local transaction");
  }

  private String key(String type, String id) {
    return MySqlConsumerInbox.digest(JsonStore.write(List.of(type, id)));
  }

  public Case active(String type, String id) {
    requireTx();
    lockIdentity(type, id);
    return j
        .query(
            "select * from recovery_case where active_key=? for update",
            (r, i) ->
                new Case(
                    r.getString("case_id"),
                    r.getString("aggregate_type"),
                    r.getString("aggregate_id"),
                    r.getString("business_state"),
                    RecoveryStatus.valueOf(r.getString("recovery_status")),
                    r.getLong("object_version")),
            key(type, id))
        .stream()
        .findFirst()
        .orElse(null);
  }

  private long lockIdentity(String type, String id) {
    requireTx();
    j.update(
        "insert into recovery_guard(aggregate_type,aggregate_id,case_version)"
            + " select ?,?,COALESCE(MAX(object_version),0) from recovery_case"
            + " where aggregate_type=? and aggregate_id=?"
            + " ON DUPLICATE KEY UPDATE case_version=case_version",
        type,
        id,
        type,
        id);
    return j.queryForObject(
        "select case_version from recovery_guard"
            + " where aggregate_type=? and aggregate_id=? for update",
        Long.class,
        type,
        id);
  }

  public void open(String type, RecoveryAggregate a, RecoveryStatus status, String evidence) {
    var current = active(type, a.id());
    if (current != null) {
      j.update(
          "update recovery_case set evidence=CONCAT(evidence,CHAR(10),?) where case_id=?",
          evidence,
          current.caseId());
      return;
    }
    long previous = lockIdentity(type, a.id());
    long version = a.state().equals("UNBOUND") ? Math.addExact(previous, 1) : a.version();
    j.update(
        "update recovery_guard set case_version=GREATEST(case_version,?)"
            + " where aggregate_type=? and aggregate_id=?",
        version,
        type,
        a.id());
    j.update(
        "insert into recovery_case(case_id,aggregate_type,aggregate_id,business_state,"
            + "recovery_status,object_version,active_key,evidence,created_at)"
            + " values(?,?,?,?,?,?,?,?,UTC_TIMESTAMP(6))",
        UUID.randomUUID().toString(),
        type,
        a.id(),
        a.state(),
        status.name(),
        version,
        key(type, a.id()),
        evidence);
  }

  public void close(Case c, String operator, String reason) {
    requireTx();
    if (j.update(
            "update recovery_case set"
                + " active_key=null,operator_id=?,resolution_reason=?,closed_at=UTC_TIMESTAMP(6)"
                + " where case_id=? and active_key is not null",
            operator,
            reason,
            c.caseId())
        != 1)
      throw new org.springframework.dao.OptimisticLockingFailureException("Case already closed");
  }

  public void defer(EventEnvelope e) {
    defer("workflow-v2", com.jay.domain.task.service.EventRouting.aggregateType(e), e);
  }

  public void defer(String consumer, String type, EventEnvelope e) {
    deferFor(consumer, type, e.aggregateId(), e);
  }

  public void deferTask(String type, String taskId, EventEnvelope e) {
    deferFor("task-relay", type, taskId, e);
  }

  private void deferFor(String consumer, String type, String aggregateId, EventEnvelope e) {
    requireTx();
    String payload = JsonStore.write(e), hash = MySqlConsumerInbox.digest(payload);
    j.update(
        "insert into"
            + " deferred_fact(consumer_name,event_id,aggregate_type,aggregate_id,payload,payload_hash,created_at)"
            + " values(?,?,?,?,?,?,UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE event_id=event_id",
        consumer,
        e.eventId(),
        type,
        aggregateId,
        payload,
        hash);
    String stored =
        j.queryForObject(
            "select payload_hash from deferred_fact where consumer_name=? and event_id=? for"
                + " update",
            String.class,
            consumer,
            e.eventId());
    if (!hash.equals(stored)) throw new AssetConflictException("Deferred event changed");
  }

  public List<EventEnvelope> pending(String type, String id, int limit) {
    requireTx();
    return j.query(
        "select payload from deferred_fact where aggregate_type=? and aggregate_id=? and"
            + " replayed_event_id is null order by event_id limit ? for update",
        (r, i) -> JsonStore.read(r.getString(1), EventEnvelope.class),
        type,
        id,
        limit);
  }

  public List<String[]> readyGroups(int limit) {
    requireTx();
    return j.query(
        "select distinct f.aggregate_type,f.aggregate_id from deferred_fact f where"
            + " f.replayed_event_id is null and exists(select 1 from recovery_case c where"
            + " c.aggregate_type=f.aggregate_type and c.aggregate_id=f.aggregate_id and c.closed_at"
            + " is not null) and not exists(select 1 from recovery_case a where"
            + " a.aggregate_type=f.aggregate_type and a.aggregate_id=f.aggregate_id and"
            + " a.active_key is not null) order by f.aggregate_type,f.aggregate_id limit ?",
        (r, i) -> new String[] {r.getString(1), r.getString(2)},
        limit);
  }

  public void replayed(String type, String id, String eventId, String replayId) {
    requireTx();
    j.update(
        "update deferred_fact set replayed_event_id=? where aggregate_type=? and aggregate_id=? and"
            + " event_id=? and replayed_event_id is null",
        replayId,
        type,
        id,
        eventId);
  }

  /** 历史 DEAD 必须有审计关闭、deferred 映射和真实持久新运输身份。 */
  boolean auditedReplay(String type, String id, EventEnvelope original) {
    return auditedReplay(type, id, original, null);
  }

  private boolean auditedReplay(
      String type, String id, EventEnvelope original, java.time.Instant after) {
    requireTx();
    var mappings =
        j.queryForList(
            "select f.payload,f.payload_hash,f.replayed_event_id from deferred_fact f where"
                + " f.event_id=? and f.replayed_event_id is not null and"
                + " f.replayed_event_id<>f.event_id and exists(select 1 from recovery_case c where"
                + " c.aggregate_type=f.aggregate_type and c.aggregate_id=f.aggregate_id and"
                + " c.closed_at is not null and c.operator_id is not null and c.operator_id<>'' and"
                + " c.resolution_reason is not null and c.resolution_reason<>'' and"
                + " c.created_at<=f.created_at and c.closed_at>=f.created_at and (? is null or"
                + " c.closed_at>?)) and ((f.aggregate_type=? and f.aggregate_id=?) or"
                + " (f.aggregate_type in ('OUTBOX_TASK','RETRY_TASK') and f.aggregate_id=?))",
            original.eventId(),
            after == null
                ? null
                : java.time.LocalDateTime.ofInstant(after, java.time.ZoneOffset.UTC),
            after == null
                ? null
                : java.time.LocalDateTime.ofInstant(after, java.time.ZoneOffset.UTC),
            type,
            id,
            original.eventId());
    for (var mapping : mappings) {
      String raw = (String) mapping.get("payload");
      var deferred = JsonStore.read(raw, EventEnvelope.class);
      if (!MySqlConsumerInbox.digest(JsonStore.write(deferred)).equals(mapping.get("payload_hash")))
        continue;
      if (!deferred.eventId().equals(original.eventId())
          || !MySqlConsumerInbox.businessHash(deferred)
              .equals(MySqlConsumerInbox.businessHash(original))) continue;
      for (String table : List.of("outbox", "retry_job")) {
        var replays =
            j.queryForList(
                "select payload,payload_hash from " + table + " where event_id=?",
                mapping.get("replayed_event_id"));
        for (var stored : replays) {
          var replay = JsonStore.read((String) stored.get("payload"), EventEnvelope.class);
          String canonical = JsonStore.write(replay);
          String expected =
              table.equals("outbox")
                  ? new com.jay.domain.asset.service.RequestHasher()
                      .hash("EVENT", Map.of("envelope", canonical))
                  : MySqlConsumerInbox.digest(canonical);
          if (!expected.equals(stored.get("payload_hash"))) continue;
          if (replay.eventId().equals(mapping.get("replayed_event_id"))
              && MySqlConsumerInbox.businessHash(replay)
                  .equals(MySqlConsumerInbox.businessHash(original))) return true;
        }
      }
    }
    return false;
  }

  boolean auditedReplayAfter(String type, String id, String eventId, java.time.Instant after) {
    requireTx();
    if (eventId == null || after == null) return false;
    for (String table : List.of("outbox", "retry_job")) {
      var events =
          j.queryForList(
              "select payload from " + table + " where event_id=?", String.class, eventId);
      for (String raw : events)
        if (auditedReplay(type, id, JsonStore.read(raw, EventEnvelope.class), after)) return true;
    }
    return false;
  }
}
