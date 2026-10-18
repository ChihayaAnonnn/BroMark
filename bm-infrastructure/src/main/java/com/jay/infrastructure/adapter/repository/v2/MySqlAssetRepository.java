package com.jay.infrastructure.adapter.repository.v2;

import cn.bugstack.middleware.db.router.DBContextHolder;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.AssetRepository;
import com.jay.domain.asset.service.RouteCodec;
import java.math.BigDecimal;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.*;

public final class MySqlAssetRepository implements AssetRepository {
  final JdbcTemplate j;
  private final RouteCodec codec = new RouteCodec(2, 4);

  private static final class Locks {
    String user;
    final SortedSet<Long> activities = new TreeSet<>();
  }

  private final Object resourceKey = new Object();

  public MySqlAssetRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  public static String table(String name) {
    String tb = DBContextHolder.getTBKey();
    if (tb == null || !tb.matches("00[0-3]"))
      throw new IllegalStateException("User table route required");
    return name + "_" + tb;
  }

  public void requireLocked(String user, Long activity) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Asset requires user transaction");
    var route = codec.routeUser(user);
    if (!route.dbKey().equals(DBContextHolder.getDBKey())
        || !route.tableKey().equals(DBContextHolder.getTBKey()))
      throw new IllegalStateException("Asset route mismatch");
    Locks locks = (Locks) TransactionSynchronizationManager.getResource(resourceKey);
    if (locks == null
        || !user.equals(locks.user)
        || (activity != null && !locks.activities.contains(activity)))
      throw new IllegalStateException("Lock accounts first");
  }

  public void lockAccounts(String user, List<Long> activities) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Asset requires transaction");
    var route = codec.routeUser(user);
    if (!route.dbKey().equals(DBContextHolder.getDBKey())
        || !route.tableKey().equals(DBContextHolder.getTBKey()))
      throw new IllegalStateException("Asset route mismatch");
    Locks locks = (Locks) TransactionSynchronizationManager.getResource(resourceKey);
    if (locks == null) {
      locks = new Locks();
      locks.user = user;
      TransactionSynchronizationManager.bindResource(resourceKey, locks);
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            public void afterCompletion(int status) {
              TransactionSynchronizationManager.unbindResource(resourceKey);
            }
          });
      var statuses =
          j.queryForList(
              "select status from user_credit_account where user_id=? for update",
              String.class,
              user);
      if (statuses.isEmpty()) {
        j.update(
            "insert into user_credit_account(user_id) values(?) on duplicate key update"
                + " user_id=user_id",
            user);
        statuses =
            j.queryForList(
                "select status from user_credit_account where user_id=? for update",
                String.class,
                user);
      }
      String status = statuses.getFirst();
      if (!"ACTIVE".equals(status))
        throw new AssetConflictException("Account disabled or unaudited");
    } else if (!user.equals(locks.user)) throw new IllegalStateException("Cross user locks");
    for (Long activity : new TreeSet<>(activities)) {
      if (activity == null || activity <= 0) throw new IllegalArgumentException("Invalid activity");
      if (locks.activities.contains(activity)) continue;
      if (!locks.activities.isEmpty() && activity < locks.activities.last())
        throw new IllegalStateException("Activity lock order");
      var rows =
          j.queryForList(
              "select activity_id from raffle_activity_account where user_id=? and activity_id=?"
                  + " for update",
              Long.class,
              user,
              activity);
      if (rows.isEmpty()) {
        j.update(
            "insert into raffle_activity_account(user_id,activity_id) values(?,?) on duplicate key"
                + " update user_id=user_id",
            user,
            activity);
        j.queryForObject(
            "select activity_id from raffle_activity_account where user_id=? and activity_id=? for"
                + " update",
            Long.class,
            user,
            activity);
      }
      locks.activities.add(activity);
    }
  }

  public BigDecimal balance(String user) {
    requireLocked(user, null);
    return j.queryForObject(
        "select balance from user_credit_account where user_id=?", BigDecimal.class, user);
  }

  public Optional<AssetReceipt> creditReceipt(String user, BusinessId id) {
    return j
        .query(
            "select request_hash,delta,balance_after from "
                + table("user_credit_ledger")
                + " where user_id=? and biz_type=? and biz_id=?",
            (rs, n) ->
                new AssetReceipt(
                    new BusinessId(id.type(), id.id(), rs.getString(1)),
                    rs.getBigDecimal(2),
                    rs.getBigDecimal(3)),
            user,
            id.type(),
            id.id())
        .stream()
        .findFirst();
  }

  public void adjustCredit(String user, BigDecimal delta, AssetReceipt receipt) {
    requireLocked(user, null);
    if (j.update(
            "update user_credit_account set balance=balance+?,version=version+1 where user_id=? and"
                + " status='ACTIVE' and balance+?>=0",
            delta,
            user,
            delta)
        != 1) throw new IllegalStateException("Credit conditional update failed");
    var id = receipt.bizId();
    j.update(
        "insert into "
            + table("user_credit_ledger")
            + "(user_id,biz_type,biz_id,request_hash,delta,balance_after,receipt)"
            + " values(?,?,?,?,?,?,?)",
        user,
        id.type(),
        id.id(),
        id.hash(),
        delta,
        receipt.balanceAfter(),
        JsonStore.write(receipt));
  }

  public Optional<QuotaReceipt> quotaReceipt(
      String user, long activity, BusinessId id, String kind) {
    requireLocked(user, activity);
    return j
        .query(
            "select receipt from "
                + table("user_quota_ledger")
                + " where user_id=? and activity_id=? and biz_type=? and biz_id=? and"
                + " bucket_type='TOTAL'",
            (rs, n) -> JsonStore.read(rs.getString(1), QuotaReceipt.class),
            user,
            activity,
            kind + ":" + id.type(),
            id.id())
        .stream()
        .findFirst();
  }

  public QuotaState quotaState(String user, long activity, BusinessTime time) {
    requireLocked(user, activity);
    long[] account =
        j.queryForObject(
            "select total_remaining,day_limit,month_limit,effective_draw_count from"
                + " raffle_activity_account where user_id=? and activity_id=?",
            (rs, n) -> new long[] {rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)},
            user,
            activity);
    j.update(
        "insert into"
            + " raffle_activity_account_month(user_id,activity_id,business_month,quota_limit,remaining,opening_quota)"
            + " values(?,?,?,?,?,?) on duplicate key update user_id=user_id",
        user,
        activity,
        time.businessMonth().toString(),
        account[2],
        account[2],
        account[2]);
    j.update(
        "insert into"
            + " raffle_activity_account_day(user_id,activity_id,business_date,quota_limit,remaining,opening_quota)"
            + " values(?,?,?,?,?,?) on duplicate key update user_id=user_id",
        user,
        activity,
        time.businessDate(),
        account[1],
        account[1],
        account[1]);
    long month =
        j.queryForObject(
            "select remaining from raffle_activity_account_month where user_id=? and activity_id=?"
                + " and business_month=? for update",
            Long.class,
            user,
            activity,
            time.businessMonth().toString());
    long[] day =
        j.queryForObject(
            "select remaining,effective_draw_count from raffle_activity_account_day where user_id=?"
                + " and activity_id=? and business_date=? for update",
            (rs, n) -> new long[] {rs.getLong(1), rs.getLong(2)},
            user,
            activity,
            time.businessDate());
    return new QuotaState(account[0], account[1], account[2], account[3], day[0], month, day[1]);
  }

  public void writeQuota(
      String user,
      long activity,
      BusinessId id,
      String kind,
      QuotaDelta delta,
      QuotaState before,
      QuotaState after,
      QuotaReceipt receipt) {
    requireLocked(user, activity);
    var t = receipt.businessTime();
    int count =
        j.update(
            "update raffle_activity_account set"
                + " total_remaining=?,day_limit=?,month_limit=?,effective_draw_count=?,version=version+1"
                + " where user_id=? and activity_id=? and total_remaining=?",
            after.total(),
            after.dayLimit(),
            after.monthLimit(),
            after.effective(),
            user,
            activity,
            before.total());
    count +=
        j.update(
            "update raffle_activity_account_month set remaining=?,quota_limit=quota_limit+? where"
                + " user_id=? and activity_id=? and business_month=? and remaining=?",
            after.monthRemaining(),
            delta.monthLimitDelta(),
            user,
            activity,
            t.businessMonth().toString(),
            before.monthRemaining());
    count +=
        j.update(
            "update raffle_activity_account_day set"
                + " remaining=?,quota_limit=quota_limit+?,effective_draw_count=? where user_id=?"
                + " and activity_id=? and business_date=? and remaining=?",
            after.dayRemaining(),
            delta.dayLimitDelta(),
            after.dayEffective(),
            user,
            activity,
            t.businessDate(),
            before.dayRemaining());
    if (count != 3) throw new IllegalStateException("Quota conditional update failed");
    String[] types = {"TOTAL", "MONTH", "DAY"};
    String[] keys = {"ALL", t.businessMonth().toString(), t.businessDate().toString()};
    long[] changes = {
      after.total() - before.total(),
      after.monthRemaining() - before.monthRemaining(),
      after.dayRemaining() - before.dayRemaining()
    };
    long[] limits = {0, delta.monthLimitDelta(), delta.dayLimitDelta()};
    long[] remaining = {after.total(), after.monthRemaining(), after.dayRemaining()};
    String receiptJson = JsonStore.write(receipt);
    var parameters = new ArrayList<Object>(33);
    for (int i = 0; i < 3; i++) {
      Collections.addAll(
          parameters,
          user,
          activity,
          kind + ":" + id.type(),
          id.id(),
          id.hash(),
          types[i],
          keys[i],
          changes[i],
          limits[i],
          remaining[i],
          receiptJson);
    }
    if (j.update(
            "insert into "
                + table("user_quota_ledger")
                + "(user_id,activity_id,biz_type,biz_id,request_hash,bucket_type,bucket_key,delta,limit_delta,remaining_after,receipt)"
                + " values(?,?,?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?,?,?)",
            parameters.toArray())
        != 3) throw new IllegalStateException("Quota ledger insert failed");
  }
}
