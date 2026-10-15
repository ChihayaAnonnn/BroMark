package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.model.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MySqlRaffleReconciliation {
  private final JdbcTemplate j;

  public MySqlRaffleReconciliation(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  private String union(String table, String fields) {
    return java.util.stream.IntStream.range(0, 4)
        .mapToObj(n -> "select " + fields + " from " + table + "_" + String.format("%03d", n))
        .collect(java.util.stream.Collectors.joining(" union all "));
  }

  private Set<String> users(String sql) {
    return new TreeSet<>(j.query(sql, (rs, n) -> rs.getString(1)));
  }

  public ReconciliationReport check(RouteSlot slot) {
    if (slot.dbKey().equals("00")) return stock();
    // Maintenance check: bounded by the selected physical database, account locks serialize asset
    // writers.
    j.queryForList("select user_id from user_credit_account order by user_id for update");
    j.queryForList(
        "select user_id,activity_id from raffle_activity_account order by user_id,activity_id for"
            + " update");
    String credit = union("user_credit_ledger", "user_id,delta"),
        quota =
            union(
                "user_quota_ledger",
                "user_id,activity_id,biz_type,biz_id,bucket_type,bucket_key,delta,limit_delta");
    var badCredit =
        users(
            "select a.user_id from user_credit_account a left join (select user_id,sum(delta) delta"
                + " from ("
                + credit
                + ") e group by user_id) l on l.user_id=a.user_id where"
                + " a.balance<>a.opening_balance+coalesce(l.delta,0)");
    var badQuota = new TreeSet<String>();
    String prefix = "with entries as (" + quota + ") ";
    badQuota.addAll(
        users(
            prefix
                + "select a.user_id from raffle_activity_account a left join (select"
                + " user_id,activity_id,sum(delta) delta from entries where bucket_type='TOTAL'"
                + " group by user_id,activity_id) l on l.user_id=a.user_id and"
                + " l.activity_id=a.activity_id where"
                + " a.total_remaining<>a.opening_total+coalesce(l.delta,0)"));
    for (String bucket : List.of("day", "month")) {
      String key = bucket.equals("day") ? "cast(a.business_date as char)" : "a.business_month";
      badQuota.addAll(
          users(
              prefix
                  + "select a.user_id from raffle_activity_account_"
                  + bucket
                  + " a left join (select user_id,activity_id,bucket_key,sum(delta) delta from"
                  + " entries where bucket_type='"
                  + bucket.toUpperCase()
                  + "' group by user_id,activity_id,bucket_key) l on l.user_id=a.user_id and"
                  + " l.activity_id=a.activity_id and l.bucket_key="
                  + key
                  + " where a.remaining<>a.opening_quota+coalesce(l.delta,0)"));
    }
    badQuota.addAll(
        users(
            prefix
                + "select a.user_id from raffle_activity_account a left join(select"
                + " user_id,activity_id,sum(case when bucket_type='DAY' then limit_delta else 0"
                + " end) d,sum(case when bucket_type='MONTH' then limit_delta else 0 end)"
                + " m,-sum(case when bucket_type='TOTAL' and (biz_type like 'CONSUME:%' or biz_type"
                + " like 'REFUND:%') then delta else 0 end) effective from entries group by"
                + " user_id,activity_id) l on l.user_id=a.user_id and l.activity_id=a.activity_id"
                + " where a.day_limit<>a.opening_day_limit+coalesce(l.d,0) or"
                + " a.month_limit<>a.opening_month_limit+coalesce(l.m,0) or"
                + " a.effective_draw_count<>a.opening_effective_count+coalesce(l.effective,0)"));
    var badDelivery = new TreeSet<String>();
    for (int n = 0; n < 4; n++) {
      String suffix = String.format("%03d", n);
      String orders = "v2_draw_order_" + suffix, ledger = "user_quota_ledger_" + suffix;
      badQuota.addAll(
          users(
              "select o.user_id from "
                  + orders
                  + " o left join "
                  + ledger
                  + " c on c.user_id=o.user_id and c.biz_id=o.order_id and"
                  + " c.biz_type='CONSUME:DRAW' and c.bucket_type='TOTAL' left join "
                  + ledger
                  + " r on r.user_id=o.user_id and r.biz_id=o.order_id and r.biz_type='REFUND:DRAW'"
                  + " and r.bucket_type='TOTAL' where (o.state='REJECTED' and c.biz_id is not null)"
                  + " or (o.state<>'REJECTED' and c.biz_id is null) or (o.state='CANCELLED' and"
                  + " r.biz_id is null) or (o.state<>'CANCELLED' and r.biz_id is not null)"));
      badDelivery.addAll(
          users(
              "select o.user_id from "
                  + orders
                  + " o left join v2_award_delivery_"
                  + suffix
                  + " d on d.order_id=o.order_id left join v2_award_record_"
                  + suffix
                  + " a on a.order_id=o.order_id left join user_credit_ledger_"
                  + suffix
                  + " c on c.user_id=o.user_id and c.biz_type='DRAW_AWARD' and c.biz_id=o.order_id"
                  + " where (o.state='DELIVERED' and (d.order_id is null or a.order_id is null or"
                  + " a.state<>'DELIVERED' or"
                  + " (json_unquote(json_extract(o.snapshot,'$.finalAward.type'))='CREDIT' and"
                  + " (c.biz_id is null or"
                  + " c.delta<>cast(json_unquote(json_extract(o.snapshot,'$.finalAward.amount')) as"
                  + " decimal(18,2)))))) or (o.state<>'DELIVERED' and (d.order_id is not null or"
                  + " c.biz_id is not null))"));
    }
    var affected = new TreeSet<String>();
    affected.addAll(badCredit);
    affected.addAll(badQuota);
    affected.addAll(badDelivery);
    for (String u : affected) {
      j.update(
          "update user_credit_account set status='REVIEW',version=version+1 where user_id=? and"
              + " status='ACTIVE'",
          u);
      audit("ASSET_RECONCILIATION", u, "credit/quota/delivery drift");
    }
    return new ReconciliationReport(
        badCredit.size(),
        badQuota.size(),
        0,
        badDelivery.size(),
        0,
        0,
        Set.of("credit", "quota", "quota_limits", "effective_count", "draw", "delivery"));
  }

  private ReconciliationReport stock() {
    j.queryForList("select stock_pool_id from award_stock order by stock_pool_id for update");
    String bad =
        "select s.stock_pool_id from award_stock s left join (select stock_pool_id,sum(total_delta)"
            + " total,sum(available_delta) available,sum(reserved_delta)"
            + " reserved,sum(allocated_delta) allocated from stock_change_log group by"
            + " stock_pool_id) l on l.stock_pool_id=s.stock_pool_id where"
            + " s.total<>s.available+s.reserved+s.allocated or s.total<>coalesce(l.total,0) or"
            + " s.available<>coalesce(l.available,0) or s.reserved<>coalesce(l.reserved,0) or"
            + " s.allocated<>coalesce(l.allocated,0) or s.reserved<>(select"
            + " coalesce(sum(quantity),0) from award_reservation r where"
            + " r.stock_pool_id=s.stock_pool_id and r.state='HELD') or s.allocated<>(select"
            + " coalesce(sum(quantity),0) from award_reservation r where"
            + " r.stock_pool_id=s.stock_pool_id and r.state='CONFIRMED')";
    var pools = users(bad);
    for (String pool : pools) {
      j.update(
          "update award_stock set recovery_status='REVIEW',version=version+1 where stock_pool_id=?"
              + " and recovery_status='ACTIVE'",
          pool);
      audit("STOCK_RECONCILIATION", pool, "stock/log/reservation drift");
    }
    return new ReconciliationReport(0, 0, pools.size(), 0, 0, 0, Set.of("stock"));
  }

  private void audit(String type, String id, String evidence) {
    var aggregate =
        new MySqlReconciliationRecoveryPort(j.getDataSource(), type.equals("STOCK_RECONCILIATION"))
            .lock(id);
    new MySqlRecoveryRepository(j.getDataSource())
        .open(type, aggregate, com.jay.domain.task.model.RecoveryStatus.REVIEW, evidence);
  }
}
