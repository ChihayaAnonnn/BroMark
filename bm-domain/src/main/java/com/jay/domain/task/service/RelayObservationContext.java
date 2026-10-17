package com.jay.domain.task.service;

/** 在当前线程标识资产事务，供真正的事务执行边界记录耗时。 */
public final class RelayObservationContext {
  private static final ThreadLocal<String> EVENT = new ThreadLocal<>();

  private RelayObservationContext() {}

  public static Scope asset(String eventType) {
    String previous = EVENT.get();
    EVENT.set(eventType);
    return () -> {
      if (previous == null) EVENT.remove();
      else EVENT.set(previous);
    };
  }

  public static String currentAssetEventType() {
    return EVENT.get();
  }

  public interface Scope extends AutoCloseable {
    @Override
    void close();
  }
}
