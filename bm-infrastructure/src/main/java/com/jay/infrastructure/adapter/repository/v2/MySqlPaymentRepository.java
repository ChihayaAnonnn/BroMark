package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.repository.*;
import com.jay.domain.task.model.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MySqlPaymentRepository implements PaymentRepository {
  private final JdbcTemplate j;
  private final MySqlRecoveryRepository recovery;

  public MySqlPaymentRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
    recovery = new MySqlRecoveryRepository(ds);
  }

  private void control() {
    if (!"00".equals(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey())
        || !org.springframework.transaction.support.TransactionSynchronizationManager
            .isActualTransactionActive())
      throw new IllegalStateException("Control transaction required");
  }

  public Optional<PaymentRecord> request(String u, String id, String hash) {
    control();
    var rows =
        j.query(
            "select order_id,request_hash from payment_order where user_id=? and request_id=?",
            (rs, n) -> new String[] {rs.getString(1), rs.getString(2)},
            u,
            id);
    if (rows.isEmpty()) return Optional.empty();
    if (!hash.equals(rows.getFirst()[1]))
      throw new AssetConflictException("Payment request changed");
    return Optional.of(find(rows.getFirst()[0], false));
  }

  public PaymentProduct product(String id) {
    control();
    try {
      return j.queryForObject(
          "select amount,currency,credit from payment_product where product_id=? and published=1",
          (rs, n) ->
              new PaymentProduct(id, rs.getBigDecimal(1), rs.getString(2), rs.getBigDecimal(3)),
          id);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw new AssetNotFoundException();
    }
  }

  public PaymentRecord find(String id, boolean lock) {
    control();
    try {
      return j.queryForObject(
          "select snapshot,state,provider_create_status,recovery_status,version from payment_order"
              + " where order_id=?"
              + (lock ? " for update" : ""),
          (rs, n) -> {
            var r = JsonStore.read(rs.getString(1), PaymentRecord.class);
            return new PaymentRecord(
                r.orderId(),
                r.userId(),
                r.requestId(),
                r.requestHash(),
                r.product(),
                r.provider(),
                r.providerAccount(),
                r.environment(),
                rs.getString(2),
                rs.getString(3),
                RecoveryStatus.valueOf(rs.getString(4)),
                rs.getLong(5),
                r.parameters(),
                r.receipt());
          },
          id);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw new AssetNotFoundException();
    }
  }

  public void insert(PaymentRecord r) {
    control();
    j.update(
        "insert into"
            + " payment_order(order_id,user_id,request_id,request_hash,state,provider_create_status,snapshot)"
            + " values(?,?,?,?,?,?,?)",
        r.orderId(),
        r.userId(),
        r.requestId(),
        r.requestHash(),
        r.state(),
        r.providerCreateStatus(),
        JsonStore.write(r));
    stage(r.orderId(), "CREATED");
  }

  public void update(PaymentRecord old, PaymentRecord next) {
    control();
    if (j.update(
            "update payment_order set"
                + " state=?,provider_create_status=?,recovery_status=?,version=version+1,snapshot=?,last_progress_at=UTC_TIMESTAMP(6),paid_at=case"
                + " when ?='PAID' then coalesce(paid_at,UTC_TIMESTAMP(6)) else paid_at"
                + " end,delivered_at=case when ?='DELIVERED' then"
                + " coalesce(delivered_at,UTC_TIMESTAMP(6)) else delivered_at end where order_id=?"
                + " and version=?",
            next.state(),
            next.providerCreateStatus(),
            next.recoveryStatus().name(),
            JsonStore.write(next),
            next.state(),
            next.state(),
            old.orderId(),
            old.version())
        != 1) throw new AssetConflictException("Payment version changed");
    if (!old.state().equals(next.state())) stage(next.orderId(), next.state());
  }

  public void stage(String id, String stage) {
    control();
    j.update(
        "insert into payment_stage(order_id,stage) values(?,?) on duplicate key update"
            + " order_id=order_id",
        id,
        stage);
  }

  public void review(String id, String reason, Object evidence) {
    control();
    String json = JsonStore.write(evidence);
    String hash = MySqlConsumerInbox.digest(id + reason + json);
    j.update(
        "insert into payment_review(evidence_hash,order_id,reason,payload) values(?,?,?,?) on"
            + " duplicate key update evidence_hash=evidence_hash",
        hash,
        id,
        reason,
        json);
    var rows =
        j.query(
            "select state,recovery_status,version from payment_order where order_id=? for update",
            (rs, n) ->
                new RecoveryAggregate(
                    id, rs.getString(1), RecoveryStatus.valueOf(rs.getString(2)), rs.getLong(3)),
            id);
    RecoveryAggregate aggregate;
    if (rows.isEmpty()) aggregate = new RecoveryAggregate(id, "UNBOUND", RecoveryStatus.REVIEW, 1);
    else {
      var r = rows.getFirst();
      if (r.recoveryStatus() == RecoveryStatus.ACTIVE) {
        if (!updateStatus(id, r.version(), RecoveryStatus.REVIEW))
          throw new AssetConflictException("Review version");
        aggregate = new RecoveryAggregate(id, r.state(), RecoveryStatus.REVIEW, r.version() + 1);
      } else aggregate = r;
    }
    recovery.open("PAYMENT", aggregate, RecoveryStatus.REVIEW, reason + ": " + json);
  }

  public boolean saveInbox(VerifiedPaymentEvent e, String rawHash) {
    control();
    j.update(
        "insert into provider_inbox(provider,account,environment,event_id,canonical_hash,payload)"
            + " values(?,?,?,?,?,?) on duplicate key update event_id=event_id",
        e.provider(),
        e.providerAccount(),
        e.environment(),
        e.eventId(),
        e.canonicalHash(),
        notificationJson(e, rawHash));
    var original =
        j.queryForObject(
            "select payload from provider_inbox where provider=? and account=? and environment=?"
                + " and event_id=? for update",
            String.class,
            e.provider(),
            e.providerAccount(),
            e.environment(),
            e.eventId());
    var old = JsonStore.read(original, VerifiedPaymentEvent.class);
    if (!old.canonicalHash().equals(e.canonicalHash())) {
      review(old.merchantOrderId(), "Provider event content changed", e);
      if (!old.merchantOrderId().equals(e.merchantOrderId()))
        review(e.merchantOrderId(), "Provider event identity reused", e);
      return false;
    }
    return true;
  }

  private String notificationJson(VerifiedPaymentEvent e, String rawHash) {
    if (rawHash == null || !rawHash.matches("[a-f0-9]{64}"))
      throw new IllegalArgumentException("Raw hash required");
    try {
      var node =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              new com.fasterxml.jackson.databind.ObjectMapper().readTree(JsonStore.write(e));
      node.put("rawSha256", rawHash);
      return node.toString();
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public List<ProviderInboxEntry> claimInbox(String token, int limit) {
    control();
    if (limit < 1 || limit > 8) throw new IllegalArgumentException("Inbox batch limit");
    var entries =
        j.query(
            "select provider,account,environment,event_id,canonical_hash,payload from"
                + " provider_inbox where (state='PENDING' and next_attempt_at<=UTC_TIMESTAMP(6)) or"
                + " (state='PROCESSING' and lease_until<UTC_TIMESTAMP(6)) order by"
                + " next_attempt_at,event_id limit ? for update skip locked",
            (rs, n) ->
                new ProviderInboxEntry(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6)),
            limit);
    for (var e : entries)
      j.update(
          "update provider_inbox set"
              + " state='PROCESSING',lease_token=?,lease_until=TIMESTAMPADD(SECOND,30,UTC_TIMESTAMP(6))"
              + " where provider=? and account=? and environment=? and event_id=?",
          token,
          e.provider(),
          e.account(),
          e.environment(),
          e.eventId());
    return entries;
  }

  public boolean ownsInbox(ProviderInboxEntry e, String token) {
    control();
    return j.queryForObject(
            "select count(*) from provider_inbox where provider=? and account=? and environment=?"
                + " and event_id=? and state='PROCESSING' and lease_token=? and"
                + " lease_until>UTC_TIMESTAMP(6) for update",
            Long.class,
            e.provider(),
            e.account(),
            e.environment(),
            e.eventId(),
            token)
        == 1;
  }

  public void finishInbox(ProviderInboxEntry e, String token, boolean success) {
    control();
    j.update(
        "update provider_inbox set"
            + " state=?,lease_token=null,lease_until=null,next_attempt_at=TIMESTAMPADD(SECOND,5,UTC_TIMESTAMP(6))"
            + " where provider=? and account=? and environment=? and event_id=? and lease_token=?",
        success ? "DONE" : "PENDING",
        e.provider(),
        e.account(),
        e.environment(),
        e.eventId(),
        token);
  }

  public void failInbox(ProviderInboxEntry e, String token, String reason, boolean permanent) {
    control();
    if (!ownsInbox(e, token)) return;
    j.update(
        "insert into"
            + " provider_inbox_failure(provider,account,environment,event_id,failures,reason,payload)"
            + " values(?,?,?,?,1,?,?) on duplicate key update"
            + " failures=failures+1,reason=values(reason),payload=values(payload),failed_at=UTC_TIMESTAMP(6)",
        e.provider(),
        e.account(),
        e.environment(),
        e.eventId(),
        reason.substring(0, Math.min(1024, reason.length())),
        e.payload());
    int n =
        j.queryForObject(
            "select failures from provider_inbox_failure where provider=? and account=? and"
                + " environment=? and event_id=?",
            Integer.class,
            e.provider(),
            e.account(),
            e.environment(),
            e.eventId());
    boolean dead = permanent || n >= com.jay.domain.task.service.RetryPolicy.MAX_ATTEMPTS;
    j.update(
        "update provider_inbox set"
            + " state=?,lease_token=null,lease_until=null,next_attempt_at=TIMESTAMPADD(SECOND,?,UTC_TIMESTAMP(6))"
            + " where provider=? and account=? and environment=? and event_id=? and lease_token=?",
        dead ? "DEAD" : "PENDING",
        Math.min(300, 1 << Math.min(n, 8)),
        e.provider(),
        e.account(),
        e.environment(),
        e.eventId(),
        token);
    if (dead) {
      String id;
      try {
        id =
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(e.payload())
                .path("merchantOrderId")
                .asText();
      } catch (Exception invalid) {
        id = "";
      }
      if (id.isBlank() || id.length() > 128)
        id =
            "INBOX-"
                + MySqlConsumerInbox.digest(
                    JsonStore.write(
                        List.of(e.provider(), e.account(), e.environment(), e.eventId())));
      review(id, "Provider inbox exhausted/corrupt: " + reason, e);
    }
  }

  public Map<String, Object> lockFact(VerifiedPaymentEvent e) {
    control();
    j.update(
        "insert into"
            + " provider_payment_fact(provider,account,environment,transaction_id,order_id,user_id,amount,currency)"
            + " values(?,?,?,?,?,?,?,?) on duplicate key update transaction_id=transaction_id",
        e.provider(),
        e.providerAccount(),
        e.environment(),
        e.transactionId(),
        e.merchantOrderId(),
        e.userBinding(),
        e.amount(),
        e.currency());
    return j.queryForMap(
        "select * from provider_payment_fact where provider=? and account=? and environment=? and"
            + " transaction_id=? for update",
        e.provider(),
        e.providerAccount(),
        e.environment(),
        e.transactionId());
  }

  public void markFact(VerifiedPaymentEvent e) {
    control();
    j.update(
        "update provider_payment_fact set "
            + (e.type().equals("REVERSED") ? "reversed" : "paid")
            + "=1 where provider=? and account=? and environment=? and transaction_id=?",
        e.provider(),
        e.providerAccount(),
        e.environment(),
        e.transactionId());
  }

  public long paidFacts(String order) {
    control();
    return j.queryForObject(
        "select count(*) from provider_payment_fact where order_id=? and paid=1",
        Long.class,
        order);
  }

  public ProviderOrder sandboxCreate(String id, java.math.BigDecimal amount, String currency) {
    control();
    j.update(
        "insert into sandbox_payment(merchant_order_id,amount,currency) values(?,?,?) on duplicate"
            + " key update merchant_order_id=merchant_order_id",
        id,
        amount,
        currency);
    var row =
        j.queryForMap("select amount,currency from sandbox_payment where merchant_order_id=?", id);
    if (((java.math.BigDecimal) row.get("amount")).compareTo(amount) != 0
        || !currency.equals(row.get("currency")))
      throw new AssetConflictException("Sandbox merchant changed");
    return new ProviderOrder(id, "READY", Map.of("sandboxOrder", id));
  }

  public ProviderPayment sandboxQuery(String id) {
    control();
    var rows =
        j.query(
            "select status,fact from sandbox_payment where merchant_order_id=?",
            (rs, n) ->
                new ProviderPayment(
                    id,
                    rs.getString(1),
                    rs.getString(2) == null
                        ? null
                        : JsonStore.read(rs.getString(2), VerifiedPaymentEvent.class)),
            id);
    return rows.isEmpty() ? new ProviderPayment(id, "NOT_FOUND", null) : rows.getFirst();
  }

  public void sandboxFact(VerifiedPaymentEvent e) {
    control();
    j.update(
        "update sandbox_payment set status=?,fact=? where merchant_order_id=?",
        e.type(),
        JsonStore.write(e),
        e.merchantOrderId());
  }

  public RecoveryAggregate lock(String id) {
    if (!"00".equals(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey()))
      throw new org.springframework.dao.EmptyResultDataAccessException(1);
    control();
    return j.queryForObject(
        "select state,recovery_status,version from payment_order where order_id=? for update",
        (rs, n) ->
            new RecoveryAggregate(
                id, rs.getString(1), RecoveryStatus.valueOf(rs.getString(2)), rs.getLong(3)),
        id);
  }

  public boolean updateStatus(String id, long v, RecoveryStatus status) {
    control();
    return j.update(
            "update payment_order set recovery_status=?,version=version+1 where order_id=? and"
                + " version=?",
            status.name(),
            id,
            v)
        == 1;
  }

  public boolean terminal(RecoveryAggregate a) {
    return "DELIVERED".equals(a.state());
  }

  public List<String> scanActive(int limit) {
    if (!"00".equals(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey())) return List.of();
    control();
    return j.query(
        "select o.order_id from payment_order o left join payment_scan_cursor c on"
            + " c.order_id=o.order_id where o.recovery_status='ACTIVE' and o.state"
            + " in('CREATED','PAID') order by"
            + " coalesce(c.checked_at,'1970-01-01'),o.last_progress_at,o.order_id limit ?",
        (rs, n) -> rs.getString(1),
        limit);
  }

  public void touch(String id) {
    control();
    j.update(
        "insert into payment_scan_cursor values(?,UTC_TIMESTAMP(6)) on duplicate key update"
            + " checked_at=values(checked_at)",
        id);
  }

  public List<String> orderIds() {
    control();
    return j.query(
        "select order_id from payment_order order by order_id", (rs, n) -> rs.getString(1));
  }

  public List<PaymentFact> facts(String id) {
    control();
    return j.query(
        "select * from provider_payment_fact where order_id=? order by"
            + " provider,account,environment,transaction_id",
        (rs, n) ->
            new PaymentFact(
                rs.getString("provider"),
                rs.getString("account"),
                rs.getString("environment"),
                rs.getString("transaction_id"),
                rs.getString("order_id"),
                rs.getString("user_id"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getBoolean("paid"),
                rs.getBoolean("reversed")),
        id);
  }

  public List<String> factOrderIds() {
    control();
    return j.query(
        "select distinct order_id from provider_payment_fact order by order_id",
        (rs, n) -> rs.getString(1));
  }

  public List<PaymentCharge> charges() {
    if ("00".equals(cn.bugstack.middleware.db.router.DBContextHolder.getDBKey())
        || !org.springframework.transaction.support.TransactionSynchronizationManager
            .isActualTransactionActive())
      throw new IllegalStateException("User transaction required");
    return j.query(
        "select user_id,biz_type,biz_id,request_hash,delta,balance_after from "
            + MySqlAssetRepository.table("user_credit_ledger")
            + " where biz_type='CHARGE'",
        (rs, n) ->
            new PaymentCharge(
                rs.getString(1),
                new AssetReceipt(
                    new BusinessId(rs.getString(2), rs.getString(3), rs.getString(4)),
                    rs.getBigDecimal(5),
                    rs.getBigDecimal(6))));
  }

  public com.jay.domain.task.model.RecoveryScanPage scanStale(
      String state,
      com.jay.domain.task.model.RecoveryScanPosition cursor,
      java.time.Instant cutoff,
      int limit) {
    return RecoveryScanSql.scan(j, "payment_order", "order_id", state, cursor, cutoff, limit);
  }

  public java.time.Instant lastProgressAt(String id) {
    return j.queryForObject(
            "select last_progress_at from " + "payment_order" + " where order_id=?",
            java.time.LocalDateTime.class,
            id)
        .toInstant(java.time.ZoneOffset.UTC);
  }
}
