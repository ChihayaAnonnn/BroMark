package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.RetryJobRepository;
import java.time.*;
import java.util.*;
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

  private void requireTransaction() {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Retry transport recovery requires target transaction");
  }

  public Instant databaseNow() {
    requireTransaction();
    return j.queryForObject("select UTC_TIMESTAMP(6)", LocalDateTime.class)
        .toInstant(ZoneOffset.UTC);
  }

  private record PublishedKey(Instant time, String id) {}

  public RecoveryScanPage scanPublished(RecoveryScanPosition cursor, Instant cutoff, int limit) {
    requireTransaction();
    if (cutoff == null || limit < 1 || limit > 64)
      throw new IllegalArgumentException("Published scan bounds");
    var c = cursor == null ? new RecoveryScanPosition(null, "", null, "") : cursor;
    Instant upper = c.upper();
    String upperId = c.upperId();
    if (upper == null) {
      var last =
          j.query(
              "select published_at,event_id from retry_job FORCE INDEX(published_recovery)"
                  + " where state='PUBLISHED' and published_at<=? order by published_at desc,"
                  + " event_id desc limit 1",
              (r, n) ->
                  new PublishedKey(
                      r.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                      r.getString(2)),
              RecoveryScanSql.utc(cutoff));
      if (last.isEmpty()) return new RecoveryScanPage(List.of(), c, true);
      upper = last.getFirst().time();
      upperId = last.getFirst().id();
    }
    var args =
        new ArrayList<Object>(
            List.of(
                RecoveryScanSql.utc(cutoff),
                RecoveryScanSql.utc(upper),
                RecoveryScanSql.utc(upper),
                upperId));
    String after = "";
    if (c.after() != null) {
      after = " and (published_at>? or (published_at=? and event_id>?))";
      args.add(RecoveryScanSql.utc(c.after()));
      args.add(RecoveryScanSql.utc(c.after()));
      args.add(c.afterId());
    }
    args.add(limit);
    var keys =
        j.query(
            "select published_at,event_id from retry_job FORCE INDEX(published_recovery)"
                + " where state='PUBLISHED' and published_at<=? and (published_at<? or"
                + " (published_at=? and event_id<=?))"
                + after
                + " order by published_at,event_id limit ?",
            (r, n) ->
                new PublishedKey(
                    r.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                    r.getString(2)),
            args.toArray());
    var last = keys.isEmpty() ? null : keys.getLast();
    var next =
        new RecoveryScanPosition(
            last == null ? c.after() : last.time(),
            last == null ? c.afterId() : last.id(),
            upper,
            upperId);
    return new RecoveryScanPage(
        keys.stream().map(PublishedKey::id).toList(),
        next,
        keys.size() < limit
            || (last != null && last.time().equals(upper) && last.id().equals(upperId)));
  }

  public Optional<PublishedRetrySnapshot> findPublished(String eventId) {
    return readPublished(eventId, false);
  }

  public Optional<PublishedRetrySnapshot> findPublishedForReview(String eventId) {
    return readPublished(eventId, true);
  }

  private Optional<PublishedRetrySnapshot> readPublished(String eventId, boolean lock) {
    requireTransaction();
    var rows =
        j.query(
            "select payload,payload_hash,aggregate_id,aggregate_version,event_type,consumer_name,attempt,"
                + " failure_count,published_at from retry_job where event_id=? and state='PUBLISHED'"
                + (lock ? " for update" : ""),
            (r, n) -> {
              EventEnvelope event;
              try {
                event = JsonStore.read(r.getString("payload"), EventEnvelope.class);
              } catch (RuntimeException malformed) {
                throw new AssetConflictException("Published Retry payload invalid");
              }
              if (event == null || event.bizId() == null || event.routeSlot() == null)
                throw new AssetConflictException("Published Retry payload invalid");
              if (!com.jay.domain.task.service.EventRouting.CONTROL.contains(event.eventType())
                  && !com.jay.domain.task.service.EventRouting.USER.contains(event.eventType())
                  && !com.jay.domain.task.service.EventRouting.NOTIFICATIONS.contains(
                      event.eventType()))
                throw new AssetConflictException("Published Retry type invalid");
              String hash = MySqlConsumerInbox.digest(JsonStore.write(event));
              String expectedConsumer =
                  com.jay.domain.task.service.EventRouting.NOTIFICATIONS.contains(event.eventType())
                      ? "notification-v2"
                      : "workflow-v2";
              if (!hash.equals(r.getString("payload_hash"))
                  || !eventId.equals(event.eventId())
                  || !Objects.equals(event.aggregateId(), r.getString("aggregate_id"))
                  || event.aggregateVersion() != r.getLong("aggregate_version")
                  || !Objects.equals(event.eventType(), r.getString("event_type"))
                  || !Objects.equals(event.requestHash(), event.bizId().hash())
                  || !expectedConsumer.equals(r.getString("consumer_name")))
                throw new AssetConflictException("Published Retry identity changed");
              int attempt = r.getInt("attempt"), failureCount = r.getInt("failure_count");
              var published = r.getObject("published_at", LocalDateTime.class);
              if (attempt < 1 || attempt > 12 || failureCount < 1 || failureCount > 12 || published == null)
                throw new AssetConflictException("Published Retry generation changed");
              return new PublishedRetrySnapshot(
                  event,
                  r.getString("consumer_name"),
                  attempt,
                  failureCount,
                  published.toInstant(ZoneOffset.UTC));
            },
            eventId);
    return rows.stream().findFirst();
  }

  private boolean transitionPublished(
      String eventId,
      int expectedAttempt,
      int expectedFailureCount,
      Instant expectedPublishedAt,
      boolean exhausted) {
    requireTransaction();
    if (expectedPublishedAt == null || expectedAttempt < 1 || expectedAttempt > 12)
      throw new IllegalArgumentException("Published generation");
    if (exhausted != (expectedAttempt == 12))
      throw new IllegalArgumentException("Published retry budget");
    return j.update(
            "update retry_job set state=?,next_attempt_at=UTC_TIMESTAMP(6),lease_token=null,"
                + " lease_until=null where event_id=? and state='PUBLISHED' and attempt=?"
                + " and failure_count=? and published_at=?",
            exhausted ? "DEAD" : "PENDING",
            eventId,
            expectedAttempt,
            expectedFailureCount,
            RecoveryScanSql.utc(expectedPublishedAt))
        == 1;
  }

  public boolean rearmPublished(
      String eventId,
      int expectedAttempt,
      int expectedFailureCount,
      Instant expectedPublishedAt) {
    return transitionPublished(
        eventId, expectedAttempt, expectedFailureCount, expectedPublishedAt, false);
  }

  public boolean deadPublished(
      String eventId,
      int expectedAttempt,
      int expectedFailureCount,
      Instant expectedPublishedAt) {
    return transitionPublished(
        eventId, expectedAttempt, expectedFailureCount, expectedPublishedAt, true);
  }

  public boolean quarantinePublished(String eventId) {
    requireTransaction();
    return j.update(
            "update retry_job set state='DEAD',lease_token=null,lease_until=null"
                + " where event_id=? and state='PUBLISHED'",
            eventId)
        == 1;
  }
}
