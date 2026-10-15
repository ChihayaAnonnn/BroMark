package com.jay.domain.stock.repository;

import com.jay.domain.stock.model.*;
import com.jay.domain.task.repository.RecoveryAggregatePort;

public interface StockRepository extends RecoveryAggregatePort {
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
