package com.jay.domain.task.repository;

import com.jay.domain.task.model.EventEnvelope;
import java.util.List;

public interface ConsumerInbox {
  boolean tryInsert(String consumer, EventEnvelope event);

  boolean tryStartOperation(String consumer, EventEnvelope event);

  List<EventEnvelope> responses(String consumer, EventEnvelope event);

  void completeOperation(String consumer, EventEnvelope event, List<EventEnvelope> responses);
}
