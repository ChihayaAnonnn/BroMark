package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.AssetConflictException;
import com.jay.domain.asset.service.RequestHasher;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.DeliveryEvidenceRepository;
import com.jay.domain.task.service.EventRouting;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 只使用调用者当前库事务；不创建事务或切换路由。 */
public final class MySqlDeliveryEvidenceRepository implements DeliveryEvidenceRepository {
  private final JdbcTemplate j;
  private final MySqlRecoveryRepository recovery;

  public MySqlDeliveryEvidenceRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
    recovery = new MySqlRecoveryRepository(ds);
  }

  private void requireTx() {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Evidence requires local transaction");
  }

  public Optional<EventEnvelope> localEvent(String eventId) {
    requireTx();
    var rows =
        j.queryForList(
            "select payload,payload_hash from outbox where event_id=? for update", eventId);
    if (rows.isEmpty()) return Optional.empty();
    var row = rows.getFirst();
    var e = JsonStore.read((String) row.get("payload"), EventEnvelope.class);
    String hash = new RequestHasher().hash("EVENT", Map.of("envelope", JsonStore.write(e)));
    if (!eventId.equals(e.eventId()) || !hash.equals(row.get("payload_hash")))
      throw new AssetConflictException("Frozen redrive digest conflict");
    return Optional.of(e);
  }

  public boolean operationCompleted(String consumer, EventEnvelope event) {
    requireTx();
    var hashes =
        j.queryForList(
            "select payload_hash from consumer_receipt where consumer_name=? and operation_key=?",
            String.class,
            consumer,
            MySqlConsumerInbox.operation(event));
    if (hashes.isEmpty()) return false;
    if (!hashes.getFirst().equals(MySqlConsumerInbox.businessHash(event)))
      throw new AssetConflictException("Business receipt digest conflict");
    return true;
  }

  public DeliveryEvidence local(String type, String id, String eventType, String requestHash) {
    requireTx();
    return inspect(type, id, eventType, requestHash, null, true);
  }

  public DeliveryEvidence local(String type, String id, EventEnvelope command) {
    requireTx();
    return inspect(type, id, command.eventType(), command.requestHash(), command, true);
  }

  public DeliveryEvidence target(EventEnvelope command) {
    requireTx();
    return inspect(
        EventRouting.aggregateType(command),
        command.aggregateId(),
        command.eventType(),
        command.requestHash(),
        command,
        false);
  }

  public boolean targetPaused(EventEnvelope e) {
    if (EventRouting.USER.contains(e.eventType())) {
      if (j.queryForObject(
              "select count(*) from user_credit_account where user_id=? and status<>'ACTIVE'",
              Integer.class,
              e.userId())
          > 0) return true;
      if (j.queryForObject(
              "select count(*) from recovery_case where aggregate_type='ASSET_RECONCILIATION' and"
                  + " aggregate_id=? and active_key is not null",
              Integer.class,
              e.userId())
          > 0) return true;
      if (!e.eventType().equals("GrantCredit"))
        return j.queryForObject(
                "select count(*) from "
                    + MySqlAssetRepository.table("v2_draw_order")
                    + " where order_id=? and recovery_status<>'ACTIVE'",
                Integer.class,
                e.aggregateId())
            > 0;
    }
    if (Set.of("ReserveAward", "ConfirmReservation", "CancelReservation").contains(e.eventType()))
      return j.queryForObject(
              "select count(*) from award_reservation where draw_order_id=? and"
                  + " recovery_status<>'ACTIVE'",
              Integer.class,
              e.aggregateId())
          > 0;
    if (e.eventType().equals("CreditGranted"))
      return j.queryForObject(
              "select count(*) from payment_order where order_id=? and recovery_status<>'ACTIVE'",
              Integer.class,
              e.aggregateId())
          > 0;
    return false;
  }

  private DeliveryEvidence inspect(
      String type,
      String id,
      String eventType,
      String requestHash,
      EventEnvelope command,
      boolean local) {
    boolean inFlight = false, remote = false, completed = false, blocked = false, conflict = false;
    Instant latest = null;
    String frozenBusinessHash = null;
    try {
      blocked =
          j.queryForObject(
                  "select count(*) from recovery_case where aggregate_type=? and aggregate_id=? and"
                      + " active_key is not null",
                  Integer.class,
                  type,
                  id)
              > 0;
      if (command != null && !local) blocked = blocked || targetPaused(command);
      if (command != null && !local)
        completed =
            operationCompleted(
                EventRouting.NOTIFICATIONS.contains(command.eventType())
                    ? "notification-v2"
                    : "workflow-v2",
                command);
      for (String table : local ? List.of("outbox", "retry_job") : List.of("retry_job")) {
        var rows =
            j.query(
                "select * from "
                    + table
                    + " where aggregate_id=? and event_type=? order by event_id for update",
                (rs, index) -> {
                  var row =
                      new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs, index);
                  row.put(
                      "published_at", rs.getObject("published_at", java.time.LocalDateTime.class));
                  return row;
                },
                id,
                eventType);
        for (var row : rows) {
          String raw = (String) row.get("payload");
          var event = JsonStore.read(raw, EventEnvelope.class);
          String expected =
              table.equals("outbox")
                  ? new RequestHasher().hash("EVENT", Map.of("envelope", JsonStore.write(event)))
                  : MySqlConsumerInbox.digest(JsonStore.write(event));
          if (!expected.equals(row.get("payload_hash"))
              || !event.eventId().equals(row.get("event_id"))
              || !event.aggregateId().equals(id)
              || !event.eventType().equals(eventType)
              || !event.requestHash().equals(event.bizId().hash())
              || !event.requestHash().equals(requestHash)) {
            conflict = true;
            continue;
          }
          if (command != null
              && !MySqlConsumerInbox.businessHash(event)
                  .equals(MySqlConsumerInbox.businessHash(command))) {
            conflict = true;
            continue;
          }
          String businessHash = MySqlConsumerInbox.businessHash(event);
          if (frozenBusinessHash != null && !frozenBusinessHash.equals(businessHash)) {
            conflict = true;
            continue;
          }
          frozenBusinessHash = businessHash;
          String state = (String) row.get("state");
          if (state.equals("DEAD")) {
            if (!recovery.auditedReplay(type, id, event)) blocked = true;
          } else if (state.equals("PENDING") || state.equals("PROCESSING")) {
            if (local) inFlight = true;
            else remote = true;
          } else if (state.equals("PUBLISHED")) {
            var publishedValue = (java.time.LocalDateTime) row.get("published_at");
            Instant published =
                publishedValue == null ? null : publishedValue.toInstant(java.time.ZoneOffset.UTC);
            if (published == null)
              return new DeliveryEvidence(
                  inFlight, latest, remote, completed, blocked, true, conflict);
            if (local) {
              if (latest == null || published.isAfter(latest)) latest = published;
            } else {
              boolean done = operationCompleted((String) row.get("consumer_name"), event);
              completed = completed || done;
              remote = remote || !done;
            }
          } else
            return new DeliveryEvidence(
                inFlight, latest, remote, completed, blocked, true, conflict);
        }
      }
      return new DeliveryEvidence(inFlight, latest, remote, completed, blocked, false, conflict);
    } catch (AssetConflictException e) {
      return new DeliveryEvidence(inFlight, latest, remote, completed, blocked, false, true);
    } catch (RuntimeException e) {
      return new DeliveryEvidence(inFlight, latest, remote, completed, blocked, true, conflict);
    }
  }
}
