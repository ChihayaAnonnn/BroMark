package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.repository.*;
import com.jay.domain.task.model.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MySqlDrawRepository implements DrawRepository {
  private final JdbcTemplate j;
  private final MySqlAssetRepository accounts;

  public MySqlDrawRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
    accounts = new MySqlAssetRepository(ds);
  }

  private String table() {
    return MySqlAssetRepository.table("v2_draw_order");
  }

  public DrawView view(String user, String order) {
    try {
      return j.queryForObject(
          "select user_id,state,recovery_status,snapshot from " + table() + " where order_id=?",
          (rs, n) -> {
            if (!user.equals(rs.getString(1))) throw new AssetOwnershipException();
            String state = rs.getString(2);
            com.jay.domain.strategy.model.AwardSnapshot award = null;
            if (java.util.Set.of("COMMITTED", "DELIVERED").contains(state)) {
              try {
                var node =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(rs.getString(4))
                        .path("finalAward");
                if (java.util.Set.of("CREDIT", "NONE").contains(node.path("type").asText()))
                  award =
                      JsonStore.read(
                          node.toString(), com.jay.domain.strategy.model.AwardSnapshot.class);
              } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new IllegalStateException("Invalid stored view", ex);
              }
            }
            return new DrawView(order, state, RecoveryStatus.valueOf(rs.getString(3)), award);
          },
          order);
    } catch (org.springframework.dao.EmptyResultDataAccessException missing) {
      throw new AssetNotFoundException();
    }
  }

  public Optional<String> request(RequestIdentity id) {
    return RequestReceipts.find(j, id, String.class);
  }

  public DrawRecord find(String order, boolean lock) {
    return j.queryForObject(
        "select snapshot,state,recovery_status,version from "
            + table()
            + " where order_id=?"
            + (lock ? " for update" : ""),
        (rs, n) -> {
          String json = rs.getString(1);
          try {
            var type =
                new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(json)
                    .path("finalAward")
                    .path("type")
                    .asText();
            if (!type.isEmpty() && !java.util.Set.of("CREDIT", "NONE").contains(type))
              throw new WaitingFixException("Unsupported historical award type");
          } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new WaitingFixException("Invalid historical award snapshot");
          }
          var r = JsonStore.read(json, DrawRecord.class);
          return new DrawRecord(
              r.orderId(),
              r.userId(),
              r.activityId(),
              r.requestHash(),
              r.identityHash(),
              r.version(),
              r.time(),
              r.ruleInput(),
              r.strategy(),
              rs.getString(2),
              RecoveryStatus.valueOf(rs.getString(3)),
              rs.getLong(4),
              r.candidate(),
              r.fallback(),
              r.finalAward());
        },
        order);
  }

  public void insert(RequestIdentity id, DrawRecord r) {
    j.update(
        "insert into "
            + table()
            + "(order_id,user_id,activity_id,state,snapshot) values(?,?,?,?,?)",
        r.orderId(),
        r.userId(),
        r.activityId(),
        r.state(),
        JsonStore.write(r));
    RequestReceipts.save(j, id, r.orderId(), r.state(), r.orderId());
    stage(r.orderId(), r.state());
  }

  public boolean update(DrawRecord old, DrawRecord next) {
    boolean changed =
        j.update(
                "update "
                    + table()
                    + " set state=?,snapshot=?,version=version+1,last_progress_at=UTC_TIMESTAMP(6),confirmed_at=case"
                    + " when ?='COMMITTED' then UTC_TIMESTAMP(6) else confirmed_at"
                    + " end,delivered_at=case when ?='DELIVERED' then UTC_TIMESTAMP(6) else"
                    + " delivered_at end where order_id=? and state=? and version=? and"
                    + " recovery_status='ACTIVE'",
                next.state(),
                JsonStore.write(next),
                next.state(),
                next.state(),
                old.orderId(),
                old.state(),
                old.objectVersion())
            == 1;
    if (changed) stage(next.orderId(), next.state());
    return changed;
  }

  public void award(DrawRecord r, String state) {
    j.update(
        "insert into "
            + MySqlAssetRepository.table("v2_award_record")
            + "(order_id,user_id,state,snapshot) values(?,?,?,?) on duplicate key update"
            + " state=values(state)",
        r.orderId(),
        r.userId(),
        state,
        JsonStore.write(r.finalAward()));
  }

  public void delivered(DrawRecord r) {
    j.update(
        "insert into "
            + MySqlAssetRepository.table("v2_award_delivery")
            + "(order_id,user_id,award_hash,snapshot) values(?,?,?,?)",
        r.orderId(),
        r.userId(),
        r.finalAward().configHash(),
        JsonStore.write(r.finalAward()));
    award(r, "DELIVERED");
  }

  public RecoveryAggregate lock(String id) {
    var owner =
        j.queryForMap("select user_id,activity_id from " + table() + " where order_id=?", id);
    accounts.lockAccounts(
        (String) owner.get("user_id"), List.of(((Number) owner.get("activity_id")).longValue()));
    return j.queryForObject(
        "select state,recovery_status,version from " + table() + " where order_id=? for update",
        (rs, n) ->
            new RecoveryAggregate(
                id, rs.getString(1), RecoveryStatus.valueOf(rs.getString(2)), rs.getLong(3)),
        id);
  }

  public RecoveryAggregate lockForPause(String id) {
    var owner =
        j.queryForMap("select user_id,activity_id from " + table() + " where order_id=?", id);
    String user = (String) owner.get("user_id");
    long activity = ((Number) owner.get("activity_id")).longValue();
    // 只锁元数据，顺序与业务账户→活动账户→订单一致；禁用账户不获得资产写许可。
    j.queryForList(
        "select user_id from user_credit_account where user_id=? for update", String.class, user);
    j.queryForList(
        "select activity_id from raffle_activity_account where user_id=? and activity_id=? for"
            + " update",
        Long.class,
        user,
        activity);
    return j.queryForObject(
        "select state,recovery_status,version from " + table() + " where order_id=? for update",
        (rs, n) ->
            new RecoveryAggregate(
                id, rs.getString(1), RecoveryStatus.valueOf(rs.getString(2)), rs.getLong(3)),
        id);
  }

  public boolean updateStatus(String id, long version, RecoveryStatus s) {
    return j.update(
            "update "
                + table()
                + " set recovery_status=?,version=version+1 where order_id=? and version=?",
            s.name(),
            id,
            version)
        == 1;
  }

  public boolean terminal(RecoveryAggregate r) {
    return Set.of("REJECTED", "CANCELLED", "DELIVERED").contains(r.state());
  }

  public void touch(String id, long version) {
    j.update(
        "insert into v2_recovery_scan_cursor values('DRAW',?,UTC_TIMESTAMP(6)) on duplicate key"
            + " update checked_at=values(checked_at)",
        id);
  }

  private void stage(String id, String state) {
    j.update(
        "insert into v2_raffle_stage(order_id,stage) values(?,?) on duplicate key update"
            + " order_id=order_id",
        id,
        state);
  }

  public List<String> scanActive(int limit) {
    if ("00".equals(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey())) return List.of();
    return j.query(
        "select o.order_id from "
            + table()
            + " o left join v2_recovery_scan_cursor c on c.aggregate_type='DRAW' and"
            + " c.aggregate_id=o.order_id where recovery_status='ACTIVE' and state"
            + " in('ACCEPTED','CANDIDATE_FIXED','RESULT_RECORDED','COMMITTED','CANCELLING') order"
            + " by coalesce(c.checked_at,'1970-01-01'),last_progress_at,o.order_id limit ?",
        (rs, n) -> rs.getString(1),
        limit);
  }

  public com.jay.domain.task.model.RecoveryScanPage scanStale(
      String state,
      com.jay.domain.task.model.RecoveryScanPosition cursor,
      java.time.Instant cutoff,
      int limit) {
    return RecoveryScanSql.scan(j, table(), "order_id", state, cursor, cutoff, limit);
  }

  public java.time.Instant lastProgressAt(String id) {
    return j.queryForObject(
            "select last_progress_at from " + table() + " where order_id=?",
            java.time.LocalDateTime.class,
            id)
        .toInstant(java.time.ZoneOffset.UTC);
  }
}
