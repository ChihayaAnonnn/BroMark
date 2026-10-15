package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.RecoveryAggregatePort;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MySqlReconciliationRecoveryPort implements RecoveryAggregatePort {
  private final JdbcTemplate j;
  private final boolean pool;

  public MySqlReconciliationRecoveryPort(DataSource ds, boolean pool) {
    j = new JdbcTemplate(ds);
    this.pool = pool;
  }

  private String table() {
    return pool ? "award_stock" : "user_credit_account";
  }

  private String key() {
    return pool ? "stock_pool_id" : "user_id";
  }

  private String status() {
    return pool ? "recovery_status" : "status";
  }

  public RecoveryAggregate lock(String id) {
    return j.queryForObject(
        "select " + status() + ",version from " + table() + " where " + key() + "=? for update",
        (rs, n) ->
            new RecoveryAggregate(
                id, "RECONCILIATION", RecoveryStatus.valueOf(rs.getString(1)), rs.getLong(2)),
        id);
  }

  public boolean updateStatus(String id, long version, RecoveryStatus s) {
    return j.update(
            "update "
                + table()
                + " set "
                + status()
                + "=?,version=version+1 where "
                + key()
                + "=? and version=?",
            s.name(),
            id,
            version)
        == 1;
  }
}
