package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.stock.repository.*;
import com.jay.domain.task.model.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MySqlStockRepository implements StockRepository {
  private final JdbcTemplate j;

  public MySqlStockRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  public java.util.List<String> scanRecovery(int limit) {
    return j.query(
        "select r.draw_order_id from award_reservation r left join v2_recovery_scan_cursor c on"
            + " c.aggregate_type='RESERVATION' and c.aggregate_id=r.draw_order_id where"
            + " r.recovery_status='ACTIVE' and r.state in('HELD','CONFIRMED','CANCELLED') order by"
            + " coalesce(c.checked_at,'1970-01-01'),r.last_progress_at,r.draw_order_id limit ?",
        (rs, n) -> rs.getString(1),
        limit);
  }

  public void touchRecovery(String id) {
    j.update(
        "insert into v2_recovery_scan_cursor values('RESERVATION',?,UTC_TIMESTAMP(6)) on duplicate"
            + " key update checked_at=values(checked_at)",
        id);
  }

  private void requireControlTransaction() {
    if (!org.springframework.transaction.support.TransactionSynchronizationManager
            .isActualTransactionActive()
        || !"00".equals(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey()))
      throw new IllegalStateException("Stock requires owned control transaction");
  }

  public ReservationResult fact(String id) {
    return j
        .query(
            "select snapshot from award_reservation where draw_order_id=?",
            (rs, n) -> JsonStore.read(rs.getString(1), ReservationResult.class),
            id)
        .stream()
        .findFirst()
        .orElse(null);
  }

  public void bindRequest(String id, String hash) {
    requireControlTransaction();
    j.update(
        "insert into award_reservation_request values(?,?) on duplicate key update"
            + " draw_order_id=draw_order_id",
        id,
        hash);
    if (!hash.equals(
        j.queryForObject(
            "select command_hash from award_reservation_request where draw_order_id=?",
            String.class,
            id))) throw new AssetConflictException("Reserve command changed");
  }

  public ReservationResult lockOrCreate(CancelCommand c) {
    requireControlTransaction();
    if (c.drawOrderId() == null
        || c.userId() == null
        || c.identityHash() == null
        || !c.identityHash().matches("[a-f0-9]{64}")
        || c.requestHash() == null
        || !c.requestHash().matches("[a-f0-9]{64}"))
      throw new IllegalArgumentException("Reservation identity");
    var empty =
        new ReservationResult(
            c.drawOrderId(), c.userId(), c.identityHash(), c.requestHash(), "NEW", null, 0);
    j.update(
        "insert into"
            + " award_reservation(draw_order_id,user_id,identity_hash,request_hash,state,snapshot)"
            + " values(?,?,?,?,?,?) on duplicate key update draw_order_id=draw_order_id",
        c.drawOrderId(),
        c.userId(),
        c.identityHash(),
        c.requestHash(),
        "NEW",
        JsonStore.write(empty));
    var row =
        j.queryForMap(
            "select * from award_reservation where draw_order_id=? for update", c.drawOrderId());
    if (!row.get("user_id").equals(c.userId())
        || !row.get("identity_hash").equals(c.identityHash())
        || !row.get("request_hash").equals(c.requestHash()))
      throw new AssetConflictException("Reservation identity changed");
    if (!row.get("recovery_status").equals("ACTIVE"))
      throw new AssetConflictException("Reservation paused");
    return JsonStore.read((String) row.get("snapshot"), ReservationResult.class);
  }

  public void save(ReservationResult r) {
    requireControlTransaction();
    j.update(
        "update award_reservation set"
            + " state=?,snapshot=?,stock_pool_id=?,quantity=?,version=version+1,last_progress_at=UTC_TIMESTAMP(6)"
            + " where draw_order_id=?",
        r.state(),
        JsonStore.write(r),
        r.finalSnapshot() == null ? null : r.finalSnapshot().stockPoolId(),
        r.quantity(),
        r.drawOrderId());
    j.update(
        "insert into v2_raffle_stage(order_id,stage) values(?,?) on duplicate key update"
            + " order_id=order_id",
        r.drawOrderId(),
        r.state());
  }

  private Map<String, Object> pool(String p) {
    var n = j.queryForMap("select * from award_stock where stock_pool_id=? for update", p);
    if (!n.get("recovery_status").equals("ACTIVE"))
      throw new AssetConflictException("Stock pool paused");
    return n;
  }

  private void log(
      String p,
      String biz,
      String kind,
      long total,
      long available,
      long reserved,
      long allocated,
      String reason) {
    j.update(
        "insert into"
            + " stock_change_log(stock_pool_id,biz_id,kind,total_delta,available_delta,reserved_delta,allocated_delta,reason)"
            + " values(?,?,?,?,?,?,?,?)",
        p,
        biz,
        kind,
        total,
        available,
        reserved,
        allocated,
        reason);
  }

  public boolean hold(String p, String order) {
    requireControlTransaction();
    var n = pool(p);
    if (((Number) n.get("available")).longValue() == 0) return false;
    j.update(
        "update award_stock set available=available-1,reserved=reserved+1,version=version+1 where"
            + " stock_pool_id=?",
        p);
    log(p, order, "RESERVE", 0, -1, 1, 0, "reserve");
    return true;
  }

  public boolean commit(String p, String order) {
    requireControlTransaction();
    var rows =
        j.queryForList(
            "select available,recovery_status from award_stock where stock_pool_id=? for update",
            p);
    if (rows.isEmpty()) throw new AssetConflictException("Stock pool missing");
    var row = rows.getFirst();
    if (!"ACTIVE".equals(row.get("recovery_status")))
      throw new AssetConflictException("Stock pool paused");
    if (((Number) row.get("available")).longValue() == 0) return false;
    if (j.update(
            "update award_stock set available=available-1,allocated=allocated+1,version=version+1"
                + " where stock_pool_id=? and available>0",
            p)
        != 1) throw new AssetConflictException("Stock counters disagree");
    log(p, order, "COMMIT", 0, -1, 0, 1, "commit");
    return true;
  }

  public void move(String p, String order, String kind) {
    requireControlTransaction();
    pool(p);
    int n;
    if (kind.equals("CONFIRM")) {
      n =
          j.update(
              "update award_stock set reserved=reserved-1,allocated=allocated+1,version=version+1"
                  + " where stock_pool_id=? and reserved>0",
              p);
      log(p, order, kind, 0, 0, -1, 1, kind);
    } else {
      n =
          j.update(
              "update award_stock set reserved=reserved-1,available=available+1,version=version+1"
                  + " where stock_pool_id=? and reserved>0",
              p);
      log(p, order, kind, 0, 1, -1, 0, kind);
    }
    if (n != 1) throw new AssetConflictException("Stock counters disagree");
  }

  public void createPool(String p, String biz, long total, String reason) {
    requireControlTransaction();
    if (p == null
        || !p.matches("[A-Za-z0-9_.:-]{1,64}")
        || total < 0
        || biz == null
        || biz.isBlank()
        || reason == null
        || reason.isBlank()) throw new IllegalArgumentException("Pool budget required");
    boolean created;
    try {
      created =
          j.update(
                  "insert into award_stock(stock_pool_id,total,available,reserved,allocated)"
                      + " values(?,?,?,0,0)",
                  p,
                  total,
                  total)
              == 1;
    } catch (org.springframework.dao.DuplicateKeyException duplicate) {
      created = false;
    }
    pool(p);
    var old =
        j.queryForList(
            "select biz_id,total_delta,reason from stock_change_log where stock_pool_id=? and"
                + " kind='INIT'",
            p);
    if (!old.isEmpty()) {
      if (!old.getFirst().get("biz_id").equals(biz)
          || ((Number) old.getFirst().get("total_delta")).longValue() != total
          || !old.getFirst().get("reason").equals(reason))
        throw new AssetConflictException("Pool initialization changed");
      return;
    }
    if (!created) throw new AssetConflictException("Existing pool must not reset");
    log(p, biz, "INIT", total, total, 0, 0, reason);
  }

  public void addStock(String p, String biz, long delta, String reason) {
    requireControlTransaction();
    if (delta == 0 || reason == null || reason.isBlank())
      throw new IllegalArgumentException("Stock adjustment");
    pool(p);
    var old =
        j.queryForList(
            "select total_delta,reason from stock_change_log where stock_pool_id=? and biz_id=? and"
                + " kind='ADJUST'",
            p,
            biz);
    if (!old.isEmpty()) {
      if (((Number) old.getFirst().get("total_delta")).longValue() != delta
          || !old.getFirst().get("reason").equals(reason))
        throw new AssetConflictException("Stock business changed");
      return;
    }
    if (j.update(
            "update award_stock set total=total+?,available=available+?,version=version+1 where"
                + " stock_pool_id=? and available+?>=0",
            delta,
            delta,
            p,
            delta)
        != 1) throw new AssetConflictException("Cannot consume reserved stock");
    log(p, biz, "ADJUST", delta, delta, 0, 0, reason);
  }

  public RecoveryAggregate lock(String id) {
    return j.queryForObject(
        "select state,recovery_status,version from award_reservation where draw_order_id=? for"
            + " update",
        (rs, n) ->
            new RecoveryAggregate(
                id, rs.getString(1), RecoveryStatus.valueOf(rs.getString(2)), rs.getLong(3)),
        id);
  }

  public boolean updateStatus(String id, long version, RecoveryStatus s) {
    requireControlTransaction();
    return j.update(
            "update award_reservation set recovery_status=?,version=version+1 where draw_order_id=?"
                + " and version=?",
            s.name(),
            id,
            version)
        == 1;
  }

  public boolean terminal(RecoveryAggregate a) {
    return Set.of("CONFIRMED", "CANCELLED").contains(a.state());
  }

  public List<String> scanActive(int limit) {
    if (!"00".equals(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey())) return List.of();
    return j.query(
        "select draw_order_id from award_reservation where recovery_status='ACTIVE' and"
            + " state='HELD' order by last_progress_at,draw_order_id limit ?",
        (rs, n) -> rs.getString(1),
        limit);
  }

  public com.jay.domain.task.model.RecoveryScanPage scanStale(
      String state,
      com.jay.domain.task.model.RecoveryScanPosition cursor,
      java.time.Instant cutoff,
      int limit) {
    return RecoveryScanSql.scan(
        j, "award_reservation", "draw_order_id", state, cursor, cutoff, limit);
  }

  public RecoveryStatus recoveryStatus(String id) {
    requireControlTransaction();
    if (j.queryForObject(
            "select count(*) from recovery_case where aggregate_type='RESERVATION' and"
                + " aggregate_id=? and active_key is not null",
            Integer.class,
            id)
        > 0) return RecoveryStatus.REVIEW;
    var rows =
        j.queryForList(
            "select recovery_status from award_reservation where draw_order_id=?",
            String.class,
            id);
    return rows.isEmpty() ? RecoveryStatus.ACTIVE : RecoveryStatus.valueOf(rows.getFirst());
  }
}
