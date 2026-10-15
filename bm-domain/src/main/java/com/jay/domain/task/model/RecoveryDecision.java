package com.jay.domain.task.model;

import java.util.List;

public record RecoveryDecision(List<EventEnvelope> actions) {
  public RecoveryDecision {
    actions = List.copyOf(actions);
  }
}
