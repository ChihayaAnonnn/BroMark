package com.jay.domain.raffle.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * P0 入口背压闸门。
 *
 * <p>只有全局积压（pending）超过保护线时拒绝新的 draw；最老积压年龄只作为指标/告警，不参与全局
 * 拒绝——单笔卡住不能把全部 draw 打成 429。同一 requestId 的重试共享一次在途许可，不会被当成
 * 新的在途请求拒绝。许可只覆盖受理事务，交付是异步的，不能在交付完成前一直占用。
 */
public final class P0AdmissionGate {
  public static final long MAX_PENDING = 20_000;
  /** 仅用于 p0_oldest_age_seconds 告警与人工重放判断，不参与入口拒绝。 */
  public static final long OLDEST_AGE_ALERT_SECONDS = 120;

  private record Entry(String requestId, AtomicInteger permits) {}

  private final AtomicReference<P0BacklogSnapshot> backlog =
      new AtomicReference<>(new P0BacklogSnapshot(0, 0));
  private final ConcurrentHashMap<String, Entry> inflight = new ConcurrentHashMap<>();

  public void update(P0BacklogSnapshot snapshot) {
    backlog.set(Objects.requireNonNull(snapshot));
  }

  public P0BacklogSnapshot snapshot() {
    return backlog.get();
  }

  /** 新请求入口；同一用户已有在途请求时返回 false。 */
  public boolean tryEnter(String user, long activity) {
    return tryEnter(user, activity, null);
  }

  /**
   * 受理入口。requestId 非空时，同一 requestId 的并发重试共享在途许可；不同 requestId 仍受
   * 单用户 1 单在途限制。只有 pending 超过保护线会拒绝；oldestAge 只观测/告警。
   */
  public boolean tryEnter(String user, long activity, String requestId) {
    Objects.requireNonNull(user);
    if (activity <= 0) throw new IllegalArgumentException("Activity required");
    var snapshot = backlog.get();
    if (snapshot.pending() > MAX_PENDING) return false;

    var entered = new AtomicBoolean();
    inflight.compute(
        user,
        (key, entry) -> {
          if (entry == null) {
            entered.set(true);
            return new Entry(requestId, new AtomicInteger(1));
          }
          if (requestId != null && requestId.equals(entry.requestId())) {
            entry.permits().incrementAndGet();
            entered.set(true);
          }
          return entry;
        });
    return entered.get();
  }

  public void leave(String user) {
    Objects.requireNonNull(user);
    inflight.computeIfPresent(
        user, (key, entry) -> entry.permits().decrementAndGet() <= 0 ? null : entry);
  }

  /** 当前在途许可数，供 p0_inflight_orders gauge 使用。 */
  public int inflight() {
    return inflight.values().stream().mapToInt(entry -> entry.permits().get()).sum();
  }
}
