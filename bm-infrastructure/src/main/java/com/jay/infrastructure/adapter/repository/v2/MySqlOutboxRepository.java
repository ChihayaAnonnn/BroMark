package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** 每库一表；过期与待发、业务类型轮转，不在等待 broker 时持有连接。 */
public class MySqlOutboxRepository implements com.jay.domain.task.repository.BatchOutboxRepository {
  protected final JdbcTemplate j;
  protected final UserTransactionRunner tx;
  protected final String table;
  private final AtomicInteger cursor = new AtomicInteger();

  public MySqlOutboxRepository(DataSource ds, UserTransactionRunner tx) {
    this(ds, tx, "outbox");
  }

  protected MySqlOutboxRepository(DataSource ds, UserTransactionRunner tx, String table) {
    if (!Set.of("outbox", "retry_job").contains(table))
      throw new IllegalArgumentException("Task table");
    this.j = new JdbcTemplate(ds);
    this.tx = tx;
    this.table = table;
  }

  protected <T> T inSlot(RouteSlot slot, Supplier<T> action) {
    return tx.inSlot(slot, WorkClass.RELAY, new Deadline(Instant.now().plusSeconds(10)), action);
  }

  public List<OutboxLease> claim(RouteSlot slot, int limit, Duration lease) {
    if (limit < 1
        || limit > 128
        || lease.isNegative()
        || lease.isZero()
        || lease.compareTo(Duration.ofMinutes(5)) > 0)
      throw new IllegalArgumentException("Claim bounds");
    return inSlot(
        slot,
        () -> {
          List<OutboxLease> result = new ArrayList<>();
          // Event-type list is bounded; unknown types still progress to return/review rather than
          // disappear.
          var types =
              j.queryForList(
                  "select distinct event_type from "
                      + table
                      + " where (state='PENDING' and next_attempt_at<=UTC_TIMESTAMP(6)) or"
                      + " (state='PROCESSING' and lease_until<=UTC_TIMESTAMP(6)) order by"
                      + " event_type limit 64",
                  String.class);
          if (types.isEmpty()) return result;
          int start = Math.floorMod(cursor.getAndIncrement(), types.size());
          boolean found = true;
          while (result.size() < limit && found) {
            found = false;
            for (int n = 0; n < types.size() && result.size() < limit; n++) {
              String type = types.get((start + n) % types.size());
              for (boolean expired : new boolean[] {true, false}) {
                if (result.size() >= limit) break;
                String due =
                    expired
                        ? "state='PROCESSING' and lease_until<=UTC_TIMESTAMP(6)"
                        : "state='PENDING' and next_attempt_at<=UTC_TIMESTAMP(6)";
                String index = expired ? "expired" : "pending";
                String order = expired ? "lease_until" : "next_attempt_at";
                var rows =
                    j.query(
                        "select payload,attempt from "
                            + table
                            + " FORCE INDEX("
                            + index
                            + ") where "
                            + due
                            + " and event_type=? order by "
                            + order
                            + ",event_id limit 1 for update skip locked",
                        (r, i) ->
                            new Object[] {
                              JsonStore.read(r.getString(1), EventEnvelope.class), r.getInt(2)
                            },
                        type);
                if (rows.isEmpty()) continue;
                found = true;
                var event = (EventEnvelope) rows.getFirst()[0];
                int attempt = Math.addExact((Integer) rows.getFirst()[1], 1);
                String token = UUID.randomUUID().toString();
                j.update(
                    "update "
                        + table
                        + " set state='PROCESSING',lease_token=?,lease_until=TIMESTAMPADD(MICROSECOND,?,UTC_TIMESTAMP(6)),attempt=?"
                        + " where event_id=?",
                    token,
                    lease.toNanos() / 1000,
                    attempt,
                    event.eventId());
                Instant until =
                    j.queryForObject(
                        "select lease_until from " + table + " where event_id=?",
                        (r, i) -> r.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        event.eventId());
                result.add(new OutboxLease(slot, event, token, until, attempt));
              }
            }
          }
          return List.copyOf(result);
        });
  }

  public boolean complete(OutboxLease lease) {
    return inSlot(
        lease.ownerSlot(),
        () ->
            j.update(
                    "update "
                        + table
                        + " set state='PUBLISHED',published_at=UTC_TIMESTAMP(6),lease_token=null,lease_until=null"
                        + " where event_id=? and state='PROCESSING' and lease_token=?",
                    lease.event().eventId(),
                    lease.leaseToken())
                == 1);
  }

  public boolean retry(OutboxLease lease, Instant next, String reason) {
    return inSlot(
        lease.ownerSlot(),
        () -> {
          beforeRetry(lease);
          boolean dead = lease.attempt() >= 12;
          int changed =
              j.update(
                  "update "
                      + table
                      + " set state=?,next_attempt_at=?,lease_token=null,lease_until=null where"
                      + " event_id=? and state='PROCESSING' and lease_token=?",
                  dead ? "DEAD" : "PENDING",
                  LocalDateTime.ofInstant(next, ZoneOffset.UTC),
                  lease.event().eventId(),
                  lease.leaseToken());
          if (changed == 1 && dead) exhausted(lease, reason);
          return changed == 1;
        });
  }

  protected void beforeRetry(OutboxLease lease) {}

  protected void exhausted(OutboxLease lease, String reason) {
    var recovery = new MySqlRecoveryRepository(j.getDataSource());
    recovery.open(
        "OUTBOX_TASK",
        new RecoveryAggregate(
            lease.event().eventId(), "UNBOUND", RecoveryStatus.RETRY_EXHAUSTED, 1),
        RecoveryStatus.RETRY_EXHAUSTED,
        JsonStore.write(lease.event()) + " outcome=" + reason);
    recovery.deferTask("OUTBOX_TASK", lease.event().eventId(), lease.event());
  }

  private final java.util.concurrent.ConcurrentMap<String, AtomicInteger> batchCursors =
      new java.util.concurrent.ConcurrentHashMap<>();
  private static final List<String> REGISTERED_TYPES =
      java.util.stream.Stream.of(
              com.jay.domain.task.service.EventRouting.CONTROL,
              com.jay.domain.task.service.EventRouting.USER,
              com.jay.domain.task.service.EventRouting.NOTIFICATIONS)
          .flatMap(Set::stream)
          .sorted()
          .toList();

  protected Instant databaseNow() {
    return j.queryForObject(
        "select UTC_TIMESTAMP(6)",
        (r, i) -> r.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC));
  }

  private static LocalDateTime utc(Instant at) {
    return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
  }

  private static String placeholders(int n) {
    return String.join(",", Collections.nCopies(n, "?"));
  }

  private void checkBatch(RouteSlot slot, List<ClaimedPublish> rows) {
    if (rows.size() > 128
        || rows.stream().anyMatch(t -> !t.ownerSlot().equals(slot))
        || rows.stream().map(ClaimedPublish::eventId).distinct().count() != rows.size())
      throw new IllegalArgumentException("Batch route, identity or size");
  }

  private void selectDue(
      RouteSlot slot,
      List<ClaimedPublish> selected,
      boolean expired,
      String type,
      int limit,
      Instant now,
      Instant until) {
    if (limit <= 0) return;
    String index = expired ? "expired" : type == null ? "pending" : "pending_type";
    String due =
        expired
            ? "state='PROCESSING' and lease_until<=?"
            : "state='PENDING' and next_attempt_at<=?";
    List<Object> args = new ArrayList<>();
    args.add(utc(now));
    if (type != null) {
      due += " and event_type=?";
      args.add(type);
    }
    if (!selected.isEmpty()) {
      due += " and event_id not in (" + placeholders(selected.size()) + ")";
      selected.forEach(t -> args.add(t.eventId()));
    }
    args.add(limit);
    selected.addAll(
        j.query(
            "select event_id,aggregate_id,aggregate_version,event_type,payload,payload_hash,attempt"
                + (table.equals("retry_job") ? ",consumer_name,failure_count" : "")
                + " from "
                + table
                + " FORCE INDEX("
                + index
                + ") where "
                + due
                + " order by "
                + (expired ? "lease_until" : "next_attempt_at")
                + ",event_id limit ? for update skip locked",
            (r, i) ->
                new ClaimedPublish(
                    slot,
                    r.getString("event_id"),
                    r.getString("aggregate_id"),
                    r.getLong("aggregate_version"),
                    r.getString("event_type"),
                    r.getString("payload"),
                    r.getString("payload_hash"),
                    UUID.randomUUID().toString(),
                    until,
                    Math.addExact(r.getInt("attempt"), 1),
                    table.equals("retry_job") ? r.getInt("failure_count") : 0,
                    table.equals("retry_job") ? r.getString("consumer_name") : "",
                    now),
            args.toArray()));
  }

  public List<ClaimedPublish> claimBatch(RouteSlot slot, RelayPolicy policy, Deadline deadline) {
    policy.validate(2, 16);
    return tx.inSlot(
        slot,
        WorkClass.RELAY,
        deadline,
        () -> {
          Instant now = databaseNow(), until = now.plus(policy.lease());
          List<ClaimedPublish> rows = new ArrayList<>();
          selectDue(slot, rows, true, null, policy.expiredReserve(), now, until);
          int start =
              Math.floorMod(
                  batchCursors
                      .computeIfAbsent(slot.dbKey(), k -> new AtomicInteger())
                      .getAndIncrement(),
                  REGISTERED_TYPES.size());
          int found = 0;
          for (int n = 0;
              n < REGISTERED_TYPES.size()
                  && found < policy.fairnessSlots()
                  && rows.size() < policy.claimLimit();
              n++) {
            int previous = rows.size();
            selectDue(
                slot,
                rows,
                false,
                REGISTERED_TYPES.get((start + n) % REGISTERED_TYPES.size()),
                1,
                now,
                until);
            if (rows.size() > previous) found++;
          }
          selectDue(slot, rows, false, null, policy.claimLimit() - rows.size(), now, until);
          selectDue(slot, rows, true, null, policy.claimLimit() - rows.size(), now, until);
          if (rows.isEmpty()) return List.of();
          StringBuilder tokens = new StringBuilder("case event_id ");
          List<Object> args = new ArrayList<>();
          for (var row : rows) {
            tokens.append("when ? then ? ");
            args.add(row.eventId());
            args.add(row.leaseToken());
          }
          tokens.append("end");
          args.add(utc(until));
          rows.forEach(row -> args.add(row.eventId()));
          int changed =
              j.update(
                  "update "
                      + table
                      + " set state='PROCESSING',lease_token="
                      + tokens
                      + ",lease_until=?,attempt=attempt+1 where event_id in ("
                      + placeholders(rows.size())
                      + ")",
                  args.toArray());
          if (changed != rows.size())
            throw new IllegalStateException("Claim affected rows are uncertain");
          return List.copyOf(rows);
        });
  }

  private List<ClaimedPublish> lockOwned(List<ClaimedPublish> rows) {
    if (rows.isEmpty()) return List.of();
    var owned = new HashMap<String, String>();
    j.query(
        "select event_id,lease_token,attempt from "
            + table
            + " where state='PROCESSING' and event_id in ("
            + placeholders(rows.size())
            + ") order by event_id for update",
        r -> {
          owned.put(r.getString(1), r.getString(2) + ":" + r.getInt(3));
        },
        rows.stream().map(ClaimedPublish::eventId).toArray());
    return rows.stream()
        .filter(t -> (t.leaseToken() + ":" + t.attempt()).equals(owned.get(t.eventId())))
        .toList();
  }

  private int fencedUpdate(List<ClaimedPublish> rows, String assignments, List<Object> values) {
    if (rows.isEmpty()) return 0;
    List<Object> args = new ArrayList<>(values);
    List<String> clauses = new ArrayList<>();
    for (var t : rows) {
      clauses.add("(event_id=? and lease_token=? and attempt=?)");
      args.add(t.eventId());
      args.add(t.leaseToken());
      args.add(t.attempt());
    }
    int changed =
        j.update(
            "update "
                + table
                + " set "
                + assignments
                + " where state='PROCESSING' and ("
                + String.join(" or ", clauses)
                + ")",
            args.toArray());
    if (changed != rows.size())
      throw new IllegalStateException("Fenced affected rows are uncertain");
    return changed;
  }

  private OutboxLease parsedLease(ClaimedPublish t) {
    return new OutboxLease(
        t.ownerSlot(),
        JsonStore.read(t.rawEnvelope(), EventEnvelope.class),
        t.leaseToken(),
        t.leaseUntil(),
        t.attempt());
  }

  private static final class LostBatchLease extends RuntimeException {}

  public BatchSettleReport settleBatch(
      RouteSlot slot, List<PublishResolution> results, Deadline deadline) {
    var tasks = results.stream().map(PublishResolution::task).toList();
    checkBatch(slot, tasks);
    // Retry 失败先取得业务锁，再锁单条任务；不反转账户→订单→任务的锁序。
    var normal =
        table.equals("retry_job")
            ? results.stream().filter(r -> r.outcome() == PublishOutcome.CONFIRMED).toList()
            : results;
    BatchSettleReport base =
        tx.inSlot(
            slot,
            WorkClass.RELAY,
            deadline,
            () -> {
              Instant now = databaseNow();
              var owned = lockOwned(normal.stream().map(PublishResolution::task).toList());
              var ids =
                  owned.stream()
                      .map(ClaimedPublish::eventId)
                      .collect(java.util.stream.Collectors.toSet());
              var lost = new HashSet<String>();
              normal.stream()
                  .map(r -> r.task().eventId())
                  .filter(id -> !ids.contains(id))
                  .forEach(lost::add);
              var success =
                  normal.stream()
                      .filter(
                          r ->
                              ids.contains(r.task().eventId())
                                  && r.outcome() == PublishOutcome.CONFIRMED)
                      .map(PublishResolution::task)
                      .toList();
              fencedUpdate(
                  success,
                  "state='PUBLISHED',published_at=?,lease_token=null,lease_until=null",
                  List.of(utc(now)));
              var failed =
                  normal.stream()
                      .filter(
                          r ->
                              ids.contains(r.task().eventId())
                                  && r.outcome() != PublishOutcome.CONFIRMED)
                      .toList();
              var retried = new HashSet<String>();
              var dead = new HashSet<String>();
              if (!failed.isEmpty()) {
                var next = new StringBuilder("case event_id ");
                var args = new ArrayList<Object>();
                for (var r : failed) {
                  next.append("when ? then ? ");
                  args.add(r.task().eventId());
                  args.add(
                      utc(
                          new com.jay.domain.task.service.RetryPolicy()
                              .next(r.task().attempt(), now)));
                }
                next.append("end");
                fencedUpdate(
                    failed.stream().map(PublishResolution::task).toList(),
                    "state=case when attempt>=12 then 'DEAD' else 'PENDING' end,next_attempt_at="
                        + next
                        + ",lease_token=null,lease_until=null",
                    args);
                for (var r : failed)
                  if (r.task().attempt() >= 12) {
                    exhausted(parsedLease(r.task()), r.reason());
                    dead.add(r.task().eventId());
                  } else retried.add(r.task().eventId());
              }
              return new BatchSettleReport(
                  success.stream()
                      .map(ClaimedPublish::eventId)
                      .collect(java.util.stream.Collectors.toSet()),
                  retried,
                  dead,
                  lost,
                  now);
            });
    if (!table.equals("retry_job")) return base;
    var retried = new HashSet<>(base.retried());
    var dead = new HashSet<>(base.dead());
    var lost = new HashSet<>(base.lost());
    for (var r : results)
      if (r.outcome() != PublishOutcome.CONFIRMED) {
        try {
          tx.inSlot(
              slot,
              WorkClass.RELAY,
              deadline,
              () -> {
                var l = parsedLease(r.task());
                beforeRetry(l);
                if (lockOwned(List.of(r.task())).isEmpty()) throw new LostBatchLease();
                Instant now = databaseNow();
                fencedUpdate(
                    List.of(r.task()),
                    "state=?,next_attempt_at=?,lease_token=null,lease_until=null",
                    List.of(
                        r.task().attempt() >= 12 ? "DEAD" : "PENDING",
                        utc(
                            new com.jay.domain.task.service.RetryPolicy()
                                .next(r.task().attempt(), now))));
                if (r.task().attempt() >= 12) exhausted(l, r.reason());
                return null;
              });
          (r.task().attempt() >= 12 ? dead : retried).add(r.task().eventId());
        } catch (LostBatchLease e) {
          lost.add(r.task().eventId());
        }
      }
    return new BatchSettleReport(base.completed(), retried, dead, lost, base.databaseNow());
  }

  public Set<String> releaseUnsent(RouteSlot slot, List<ClaimedPublish> unsent, Deadline deadline) {
    checkBatch(slot, unsent);
    return tx.inSlot(
        slot,
        WorkClass.RELAY,
        deadline,
        () -> {
          var owned = lockOwned(unsent);
          fencedUpdate(
              owned,
              "state='PENDING',attempt=attempt-1,lease_token=null,lease_until=null",
              List.of());
          return owned.stream()
              .map(ClaimedPublish::eventId)
              .collect(java.util.stream.Collectors.toUnmodifiableSet());
        });
  }

  public boolean quarantineMalformed(ClaimedPublish task, String reason, Deadline deadline) {
    return tx.inSlot(
        task.ownerSlot(),
        WorkClass.RELAY,
        deadline,
        () -> {
          if (lockOwned(List.of(task)).isEmpty()) return false;
          String boundedReason =
              reason == null ? "malformed" : reason.substring(0, Math.min(1024, reason.length()));
          j.update(
              "insert into message_quarantine values(?,?,?,?,UTC_TIMESTAMP(6)) on duplicate key"
                  + " update reason=VALUES(reason)",
              "task-relay",
              MySqlConsumerInbox.digest(task.rawEnvelope()),
              task.rawEnvelope().getBytes(java.nio.charset.StandardCharsets.UTF_8),
              boundedReason);
          new MySqlRecoveryRepository(j.getDataSource())
              .open(
                  "OUTBOX_TASK",
                  new RecoveryAggregate(task.eventId(), "UNBOUND", RecoveryStatus.WAITING_FIX, 1),
                  RecoveryStatus.WAITING_FIX,
                  boundedReason);
          fencedUpdate(List.of(task), "state='DEAD',lease_token=null,lease_until=null", List.of());
          return true;
        });
  }
}
