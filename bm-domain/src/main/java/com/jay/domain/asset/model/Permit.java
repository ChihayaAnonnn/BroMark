package com.jay.domain.asset.model;

public interface Permit extends AutoCloseable {
  @Override
  void close();
}
