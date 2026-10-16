package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.AssetConflictException;
import com.jay.domain.task.model.EventEnvelope;
import com.jay.domain.task.repository.OutboxWriter;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MySqlOutboxWriter implements OutboxWriter {
  private final JdbcTemplate j;
  private final Runnable afterCommitWake;

  public MySqlOutboxWriter(DataSource ds) {
    this(ds, () -> {});
  }

  public MySqlOutboxWriter(DataSource ds, Runnable afterCommitWake) {
    j = new JdbcTemplate(ds);
    this.afterCommitWake = java.util.Objects.requireNonNull(afterCommitWake);
  }

  public void append(EventEnvelope event) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Outbox requires active source transaction");
    String payload = JsonStore.write(event);
    String hash =
        new com.jay.domain.asset.service.RequestHasher()
            .hash("EVENT", java.util.Map.of("envelope", payload));
    j.update(
        "insert into"
            + " outbox(event_id,aggregate_id,aggregate_version,event_type,payload,payload_hash,state,attempt,next_attempt_at,created_at)"
            + " values(?,?,?,?,?,?,'PENDING',0,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)) ON DUPLICATE KEY"
            + " UPDATE event_id=event_id",
        event.eventId(),
        event.aggregateId(),
        event.aggregateVersion(),
        event.eventType(),
        payload,
        hash);
    String stored =
        j.queryForObject(
            "select payload_hash from outbox where event_id=?", String.class, event.eventId());
    if (!hash.equals(stored)) throw new AssetConflictException("Event payload changed");
    registerWake(afterCommitWake);
  }

  static void registerWake(Runnable wake) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || !TransactionSynchronizationManager.isSynchronizationActive()) return;
    TransactionSynchronizationManager.registerSynchronization(
        new org.springframework.transaction.support.TransactionSynchronization() {
          public void afterCommit() {
            try {
              wake.run();
            } catch (RuntimeException e) {
              org.slf4j.LoggerFactory.getLogger(MySqlOutboxWriter.class)
                  .warn("Relay wake failed; periodic polling remains active", e);
            }
          }
        });
  }
}
