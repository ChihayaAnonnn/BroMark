package com.jay.domain.stock.repository;

import com.jay.domain.stock.model.*;
import com.jay.domain.task.repository.RecoveryAggregatePort;

public interface StockRepository extends RecoveryAggregatePort {

  default com.jay.domain.task.model.RecoveryScanPage scanStale(
      String state,
      com.jay.domain.task.model.RecoveryScanPosition cursor,
      java.time.Instant cutoff,
      int limit) {
    return new com.jay.domain.task.model.RecoveryScanPage(java.util.List.of(), cursor, true);
  }

  /** 不存在预留时允许原命令；状态未知必须抛出，不能当作ACTIVE。 */
  default com.jay.domain.task.model.RecoveryStatus recoveryStatus(String id) {
    throw new UnsupportedOperationException("Stock authority unavailable");
  }

  void createPool(String pool, String biz, long total, String reason);

  java.util.List<String> scanRecovery(int limit);

  void touchRecovery(String id);

  ReservationResult fact(String id);

  void bindRequest(String id, String hash);

  ReservationResult lockOrCreate(CancelCommand c);

  void save(ReservationResult r);

  boolean hold(String pool, String order);

  void move(String pool, String order, String kind);

  void addStock(String pool, String biz, long delta, String reason);
}
