package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.AssetConflictException;
import com.jay.domain.asset.service.RequestHasher;
import com.jay.domain.task.model.EventEnvelope;
import com.jay.domain.task.repository.ConsumerInbox;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MySqlConsumerInbox implements ConsumerInbox {
  private final JdbcTemplate j;

  public MySqlConsumerInbox(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  public static String digest(String value) {
    return new RequestHasher().hash("MESSAGE", Map.of("body", value));
  }

  static String operation(EventEnvelope e) {
    return digest(e.eventType() + ":" + e.bizId().type() + ":" + e.bizId().id());
  }

  static String businessHash(EventEnvelope e) {
    return digest(
        JsonStore.write(
            List.of(
                e.eventType(),
                e.aggregateId(),
                e.bizId(),
                e.userId() == null ? "" : e.userId(),
                e.routeSlot(),
                e.requestHash(),
                e.payload())));
  }

  private void requireTx() {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Inbox requires transaction");
  }

  public boolean tryInsert(String consumer, EventEnvelope e) {
    requireTx();
    String hash = digest(JsonStore.write(e));
    try {
      if (j.update(
              "insert into consumer_inbox values(?,?,?,UTC_TIMESTAMP(6))",
              consumer,
              e.eventId(),
              hash)
          == 1) return true;
    } catch (DuplicateKeyException ex) {
      // MySQL 重复键不使事务失效；PG 兼容层通过 ON CONFLICT 返回 0。
    }
    String saved =
        j.queryForObject(
            "select payload_hash from consumer_inbox where consumer_name=? and event_id=? for"
                + " update",
            String.class,
            consumer,
            e.eventId());
    if (!hash.equals(saved)) throw new AssetConflictException("Transport event changed");
    return false;
  }

  public boolean tryStartOperation(String consumer, EventEnvelope e) {
    requireTx();
    String hash = businessHash(e);
    try {
      if (j.update("insert into consumer_receipt values(?,?,?,'[]')", consumer, operation(e), hash)
          == 1) return true;
    } catch (DuplicateKeyException ex) {
      // 同 tryInsert：重复请求继续锁定并校验原始摘要。
    }
    String saved =
        j.queryForObject(
            "select payload_hash from consumer_receipt where consumer_name=? and operation_key=?"
                + " for update",
            String.class,
            consumer,
            operation(e));
    if (!hash.equals(saved)) throw new AssetConflictException("Business event changed");
    return false;
  }

  public List<EventEnvelope> responses(String consumer, EventEnvelope e) {
    requireTx();
    String json =
        j.queryForObject(
            "select responses from consumer_receipt where consumer_name=? and operation_key=?",
            String.class,
            consumer,
            operation(e));
    return List.of(JsonStore.read(json, EventEnvelope[].class));
  }

  public void completeOperation(String consumer, EventEnvelope e, List<EventEnvelope> responses) {
    requireTx();
    j.update(
        "update consumer_receipt set responses=? where consumer_name=? and operation_key=?",
        JsonStore.write(responses),
        consumer,
        operation(e));
  }
}
