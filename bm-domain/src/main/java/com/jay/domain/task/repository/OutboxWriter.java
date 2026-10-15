package com.jay.domain.task.repository;

import com.jay.domain.task.model.EventEnvelope;

public interface OutboxWriter {
  void append(EventEnvelope event);
}
