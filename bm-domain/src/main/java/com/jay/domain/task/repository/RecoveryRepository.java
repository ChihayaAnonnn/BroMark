package com.jay.domain.task.repository;

import com.jay.domain.task.model.*;
import java.util.List;

public interface RecoveryRepository {
  record Case(
      String caseId, String type, String id, String state, RecoveryStatus status, long version) {}

  Case active(String type, String id);

  void open(String type, RecoveryAggregate aggregate, RecoveryStatus status, String evidence);

  void close(Case c, String operator, String reason);

  void defer(EventEnvelope event);

  void defer(String consumer, String type, EventEnvelope event);

  List<EventEnvelope> pending(String type, String id, int limit);

  void deferTask(String type, String taskId, EventEnvelope event);

  List<String[]> readyGroups(int limit);

  void replayed(String type, String id, String eventId, String replayId);
}
