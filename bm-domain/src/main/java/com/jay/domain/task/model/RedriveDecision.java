package com.jay.domain.task.model;

public enum RedriveDecision {
  EMIT,
  FRESH,
  LOCAL_IN_FLIGHT,
  REMOTE_RETRY,
  OBSERVATION_WINDOW,
  COOLDOWN,
  PAUSED,
  UNKNOWN,
  EXHAUSTED,
  CONFLICT
}
