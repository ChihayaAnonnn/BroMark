package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.OutboxRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** 每库一表；过期与待发、业务类型轮转，不在等待 broker 时持有连接。 */
public class MySqlOutboxRepository implements OutboxRepository {
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
}
