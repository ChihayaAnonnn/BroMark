package com.jay.domain.task.repository;
import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.task.model.OutboxLease;
import java.time.*;
import java.util.List;
public interface OutboxRepository {
  List<OutboxLease> claim(RouteSlot slot,int limit,Duration lease);
  boolean complete(OutboxLease lease);
  boolean retry(OutboxLease lease,Instant next,String reason);
}
