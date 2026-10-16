package com.jay.domain.task.repository;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import java.util.*;

public interface BatchOutboxRepository extends OutboxRepository {
  List<ClaimedPublish> claimBatch(RouteSlot slot, RelayPolicy policy, Deadline deadline);

  BatchSettleReport settleBatch(RouteSlot slot, List<PublishResolution> results, Deadline deadline);

  Set<String> releaseUnsent(RouteSlot slot, List<ClaimedPublish> unsent, Deadline deadline);

  boolean quarantineMalformed(ClaimedPublish task, String reason, Deadline deadline);
}
