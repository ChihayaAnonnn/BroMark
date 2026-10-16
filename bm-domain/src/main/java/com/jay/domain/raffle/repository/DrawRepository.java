package com.jay.domain.raffle.repository;

import com.jay.domain.asset.model.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.task.repository.*;
import java.util.*;

public interface DrawRepository extends RecoveryAggregatePort {

  default com.jay.domain.task.model.RecoveryScanPage scanStale(
      String state,
      com.jay.domain.task.model.RecoveryScanPosition cursor,
      java.time.Instant cutoff,
      int limit) {
    return new com.jay.domain.task.model.RecoveryScanPage(java.util.List.of(), cursor, true);
  }

  /** 旧fake不提供进展证据时，guarded保守不补发。 */
  default java.time.Instant lastProgressAt(String id) {
    return null;
  }

  DrawView view(String user, String order);

  Optional<String> request(RequestIdentity id);

  DrawRecord find(String order, boolean lock);

  void insert(RequestIdentity id, DrawRecord order);

  boolean update(DrawRecord before, DrawRecord after);

  void award(DrawRecord order, String status);

  void delivered(DrawRecord order);

  void touch(String order, long version);
}
