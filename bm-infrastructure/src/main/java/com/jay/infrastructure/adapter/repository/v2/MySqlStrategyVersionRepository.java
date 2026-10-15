package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.repository.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MySqlStrategyVersionRepository implements StrategyVersionRepository {
  private final JdbcTemplate j;

  public MySqlStrategyVersionRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  public void publish(FrozenStrategy f) {
    for (var p : f.prizes())
      if (p.stockPoolId() != null) {
        if (j.queryForObject(
                "select count(*) from award_stock where stock_pool_id=? and"
                    + " recovery_status='ACTIVE'",
                Integer.class,
                p.stockPoolId())
            != 1) throw new IllegalArgumentException("Unknown or paused pool");
      }
    j.update(
        "insert into strategy_version(strategy_id,version,snapshot) values(?,?,?)",
        f.strategyId(),
        f.version(),
        JsonStore.write(f));
    for (var p : f.prizes())
      if (p.stockPoolId() != null)
        j.update(
            "insert into strategy_stock_binding(strategy_id,version,award_id,stock_pool_id)"
                + " values(?,?,?,?)",
            f.strategyId(),
            f.version(),
            p.awardId(),
            p.stockPoolId());
    j.update(
        "insert into v2_activity_pointer(activity_id,strategy_id,version) values(?,?,?) on"
            + " duplicate key update strategy_id=values(strategy_id),version=values(version)",
        f.activityId(),
        f.strategyId(),
        f.version());
  }

  public FrozenStrategy load(long id, String v) {
    return j.queryForObject(
        "select snapshot from strategy_version where strategy_id=? and version=?",
        (rs, n) -> JsonStore.read(rs.getString(1), FrozenStrategy.class),
        id,
        v);
  }

  public DrawAdmission admit(
      String user, long activity, com.jay.domain.asset.model.Deadline deadline) {
    var f = current(activity);
    boolean blocked =
        j.queryForObject(
                "select count(*) from v2_asset_blacklist where user_id=?", Integer.class, user)
            > 0;
    var expires = java.time.Instant.now().plusSeconds(3);
    return new DrawAdmission(
        f, blocked, expires.isBefore(deadline.expiresAt()) ? expires : deadline.expiresAt());
  }

  public FrozenStrategy current(long activity) {
    return j.queryForObject(
        "select s.snapshot from v2_activity_pointer a join strategy_version s on"
            + " s.strategy_id=a.strategy_id and s.version=a.version where a.activity_id=?",
        (rs, n) -> JsonStore.read(rs.getString(1), FrozenStrategy.class),
        activity);
  }
}
