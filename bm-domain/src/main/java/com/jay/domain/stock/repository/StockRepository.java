package com.jay.domain.stock.repository;
import com.jay.domain.stock.model.*;import com.jay.domain.task.repository.RecoveryAggregatePort;
public interface StockRepository extends RecoveryAggregatePort {ReservationResult lockOrCreate(CancelCommand c);void save(ReservationResult r);boolean hold(String pool,String order);void move(String pool,String order,String kind);void addStock(String pool,String biz,long delta,String reason);}
