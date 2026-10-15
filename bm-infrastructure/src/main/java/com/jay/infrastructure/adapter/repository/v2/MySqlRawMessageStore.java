package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.task.repository.RawMessageStore;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MySqlRawMessageStore implements RawMessageStore {
  private final JdbcTemplate j;

  public MySqlRawMessageStore(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  public void quarantine(String consumer, byte[] body, String reason) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Quarantine requires transaction");
    new MySqlRecoveryRepository(j.getDataSource())
        .open(
            "RAW_MESSAGE",
            new com.jay.domain.task.model.RecoveryAggregate(
                hash(body), "UNBOUND", com.jay.domain.task.model.RecoveryStatus.REVIEW, 1),
            com.jay.domain.task.model.RecoveryStatus.REVIEW,
            reason);
    j.update(
        "insert into message_quarantine values(?,?,?,?,UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE"
            + " body_hash=body_hash",
        consumer,
        hash(body),
        body,
        reason.substring(0, Math.min(1024, reason.length())));
  }

  public static String hash(byte[] body) {
    try {
      return java.util.HexFormat.of()
          .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(body));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
