package com.jay.domain.task.service;

import com.jay.domain.task.model.EventEnvelope;
import java.util.List;

@FunctionalInterface
public interface EventHandler {
  List<EventEnvelope> apply(EventEnvelope event);
}
