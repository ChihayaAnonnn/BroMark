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
    lockIdentity(type,id);
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
        type, id, type, id);
    return j.queryForObject(
        "select case_version from recovery_guard"
            + " where aggregate_type=? and aggregate_id=? for update",
        Long.class, type, id);
  }

  public void open(String type, RecoveryAggregate a, RecoveryStatus status, String evidence) {
    var current = active(type, a.id());
    if (current != null) {
      j.update(
          "update recovery_case set evidence=CONCAT(evidence,CHAR(10),?) where case_id=?",
          evidence, current.caseId());
      return;
    }
    long previous = lockIdentity(type, a.id());
    long version = a.state().equals("UNBOUND") ? Math.addExact(previous, 1) : a.version();
    j.update(
        "update recovery_guard set case_version=GREATEST(case_version,?)"
            + " where aggregate_type=? and aggregate_id=?",
        version, type, a.id());
    j.update(
        "insert into recovery_case(case_id,aggregate_type,aggregate_id,business_state,"
            + "recovery_status,object_version,active_key,evidence,created_at)"
            + " values(?,?,?,?,?,?,?,?,UTC_TIMESTAMP(6))",
        UUID.randomUUID().toString(), type, a.id(), a.state(), status.name(), version,
        key(type, a.id()), evidence);
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
}
