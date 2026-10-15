package com.jay.domain.task.repository;

public interface RawMessageStore {
  void quarantine(String consumer, byte[] body, String reason);
}
