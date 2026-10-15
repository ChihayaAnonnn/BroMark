package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.asset.model.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class RequestReceipts {
  static <T> Optional<T> find(JdbcTemplate j, RequestIdentity id, Class<T> type) {
    return j
        .query(
            "select request_hash,receipt from "
                + MySqlAssetRepository.table("user_request_receipt")
                + " where user_id=? and operation=? and request_id=?",
            (rs, n) -> {
              if (!id.requestHash().equals(rs.getString(1)))
                throw new AssetConflictException("Request content changed");
              return JsonStore.read(rs.getString(2), type);
            },
            id.userId(),
            id.operation(),
            id.requestId())
        .stream()
        .findFirst();
  }

  static void save(JdbcTemplate j, RequestIdentity id, String order, String state, Object result) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Receipt requires transaction");
    j.update(
        "insert into "
            + MySqlAssetRepository.table("user_request_receipt")
            + "(user_id,operation,request_id,request_hash,order_id,result_state,receipt)"
            + " values(?,?,?,?,?,?,?)",
        id.userId(),
        id.operation(),
        id.requestId(),
        id.requestHash(),
        order,
        state,
        JsonStore.write(result));
  }
}
