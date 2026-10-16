package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.RetryJobRepository;
import java.time.*;
import javax.sql.DataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MySqlRetryJobRepository extends MySqlOutboxRepository
    implements RetryJobRepository {
  private final com.jay.domain.task.service.RecoveryService recovery;
  private final Runnable afterCommitWake;

  public MySqlRetryJobRepository(DataSource ds, UserTransactionRunner tx) {
    this(ds, tx, null);
  }

  public MySqlRetryJobRepository(
      DataSource ds,
      UserTransactionRunner tx,
      com.jay.domain.task.service.RecoveryService recovery) {
    this(ds, tx, recovery, () -> {});
  }

  public MySqlRetryJobRepository(
      DataSource ds,
      UserTransactionRunner tx,
      com.jay.domain.task.service.RecoveryService recovery,
      Runnable afterCommitWake) {
    super(ds, tx, "retry_job");
    this.afterCommitWake = java.util.Objects.requireNonNull(afterCommitWake);
    this.recovery = recovery;
  }

  protected void exhausted(OutboxLease lease, String reason) {
    var e = lease.event();
    String type = com.jay.domain.task.service.EventRouting.aggregateType(e);
    String consumer =
        j.queryForObject(
            "select consumer_name from retry_job where event_id=?", String.class, e.eventId());
    if (recovery != null) {
      recovery.pauseInCurrentTransaction(
          type,
          e.aggregateId(),
          RecoveryStatus.RETRY_EXHAUSTED,
          JsonStore.write(e) + " outcome=" + reason);
      recovery.deferIfPaused(consumer, e);
    } else {
      var repository = new MySqlRecoveryRepository(j.getDataSource());
      repository.open(
          type,
          new RecoveryAggregate(e.aggregateId(), "UNBOUND", RecoveryStatus.RETRY_EXHAUSTED, 1),
          RecoveryStatus.RETRY_EXHAUSTED,
          JsonStore.write(e) + " outcome=" + reason);
      repository.defer(consumer, type, e);
    }
  }

  public void schedule(EventEnvelope e, int attempt, Instant next) {
    schedule("workflow-v2", e, attempt, next);
  }

  public void schedule(String consumer, EventEnvelope e, int attempt, Instant next) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Retry handoff requires local transaction");
    if (attempt < 1 || attempt > 12) throw new IllegalArgumentException("Retry budget");
    String payload = JsonStore.write(e), hash = MySqlConsumerInbox.digest(payload);
    j.update(
        "insert into"
            + " retry_job(event_id,aggregate_id,aggregate_version,event_type,payload,payload_hash,state,attempt,next_attempt_at,created_at,consumer_name,failure_count)"
            + " values(?,?,?,?,?,?,'PENDING',0,?,UTC_TIMESTAMP(6),?,?) ON DUPLICATE KEY UPDATE"
            + " next_attempt_at=IF(VALUES(failure_count)>failure_count,VALUES(next_attempt_at),next_attempt_at),state=IF(VALUES(failure_count)>failure_count,'PENDING',state),lease_token=IF(VALUES(failure_count)>failure_count,NULL,lease_token),lease_until=IF(VALUES(failure_count)>failure_count,NULL,lease_until),failure_count=GREATEST(failure_count,VALUES(failure_count))",
        e.eventId(),
        e.aggregateId(),
        e.aggregateVersion(),
        e.eventType(),
        payload,
        hash,
        LocalDateTime.ofInstant(next, ZoneOffset.UTC),
        consumer,
        attempt);
    var row =
        j.queryForMap(
            "select payload_hash,consumer_name from retry_job where event_id=? for update",
            e.eventId());
    if (!hash.equals(row.get("payload_hash")) || !consumer.equals(row.get("consumer_name")))
      throw new AssetConflictException("Retry identity changed");
    MySqlOutboxWriter.registerWake(afterCommitWake);
  }

  public boolean mayApply(String consumer, EventEnvelope e, int generation) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Retry generation requires transaction");
    var rows =
        j.queryForList(
            "select failure_count,state,payload_hash,consumer_name from retry_job where event_id=?"
                + " for update",
            e.eventId());
    if (rows.isEmpty()) {
      if (generation != 0) throw new AssetConflictException("Unknown retry generation");
      return true;
    }
    var row = rows.getFirst();
    if (!consumer.equals(row.get("consumer_name"))
        || !MySqlConsumerInbox.digest(JsonStore.write(e)).equals(row.get("payload_hash")))
      throw new AssetConflictException("Retry identity changed");
    int current = ((Number) row.get("failure_count")).intValue();
    if (generation > current) throw new AssetConflictException("Future retry generation");
    return generation == current
        && java.util.Set.of("PROCESSING", "PUBLISHED").contains(row.get("state"));
  }

  protected void beforeRetry(OutboxLease lease) {
    String consumer =
        j.queryForObject(
            "select consumer_name from retry_job where event_id=?",
            String.class,
            lease.event().eventId());
    if (recovery != null) recovery.deferIfPaused(consumer, lease.event());
    else
      new MySqlRecoveryRepository(j.getDataSource())
          .active(
              com.jay.domain.task.service.EventRouting.aggregateType(lease.event()),
              lease.event().aggregateId());
  }

  public int failures(EventEnvelope e) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Retry count requires transaction");
    var rows =
        j.queryForList(
            "select failure_count from retry_job where event_id=? for update",
            Integer.class,
            e.eventId());
    return rows.isEmpty() ? 0 : rows.getFirst();
  }

  public void dead(String consumer, EventEnvelope e, int attempt) {
    schedule(consumer, e, attempt, Instant.now());
    j.update(
        "update retry_job set state='DEAD',lease_token=null,lease_until=null where event_id=?",
        e.eventId());
  }

  public int failureCount(OutboxLease lease) {
    return inSlot(
        lease.ownerSlot(),
        () ->
            j.queryForObject(
                "select failure_count from retry_job where event_id=?",
                Integer.class,
                lease.event().eventId()));
  }
}
