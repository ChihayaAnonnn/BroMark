package com.jay.domain.raffle.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * P0 入口背压闸门。
 *
 * <p>只有全局积压（pending）超过保护线时拒绝新的 draw；最老积压年龄只作为指标/告警，不参与全局
 * 拒绝——单笔卡住不能把全部 draw 打成 429。同一 requestId 的重试共享一次在途许可，不会被当成
 * 新的在途请求拒绝。许可只覆盖受理事务，交付是异步的，不能在交付完成前一直占用。
 *
 * <p>在全局背压和单用户 1 单在途之外，再按活动限制受理在途许可（{@link #MAX_ACTIVITY_INFLIGHT}），
 * 避免单个热点活动占满受理资源。活动许可与用户许可同生命周期：受理成功时各占一个，离场时各还一个。
 */
public final class P0AdmissionGate {
  public static final long MAX_PENDING = 20_000;
  /** 单个活动同时持有的受理在途许可上限，规格初始值。 */
  public static final int MAX_ACTIVITY_INFLIGHT = 5_000;
  /** 仅用于 p0_oldest_age_seconds 告警与人工重放判断，不参与入口拒绝。 */
  public static final long OLDEST_AGE_ALERT_SECONDS = 120;

  private record Entry(String requestId, long activity, AtomicInteger permits) {}

  private final AtomicReference<P0BacklogSnapshot> backlog =
      new AtomicReference<>(new P0BacklogSnapshot(0, 0));
  private final ConcurrentHashMap<String, Entry> inflight = new ConcurrentHashMap<>();
  /** 活动 -> 受理在途许可数；归零即移除，避免热点活动长期残留空 entry。 */
  private final ConcurrentHashMap<Long, AtomicInteger> activityInflight = new ConcurrentHashMap<>();

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
   * 单用户 1 单在途限制。只有 pending 超过保护线或活动许可已满会拒绝；oldestAge 只观测/告警。
   */
  public boolean tryEnter(String user, long activity, String requestId) {
    Objects.requireNonNull(user);
    if (activity <= 0) throw new IllegalArgumentException("Activity required");
    var snapshot = backlog.get();
    if (snapshot.pending() > MAX_PENDING) return false;

    // 先占活动许可：活动已满时直接拒绝，不留下任何用户许可。
    if (!acquireActivity(activity)) return false;

    var entered = tryAcquireUser(user, activity, requestId);
    if (!entered) releaseActivity(activity);
    return entered;
  }

  private boolean tryAcquireUser(String user, long activity, String requestId) {
    var entered = new AtomicBoolean();
    inflight.compute(
        user,
        (key, entry) -> {
          if (entry == null) {
            entered.set(true);
            return new Entry(requestId, activity, new AtomicInteger(1));
          }
          if (requestId != null
              && requestId.equals(entry.requestId())
              && activity == entry.activity()) {
            entry.permits().incrementAndGet();
            entered.set(true);
          }
          return entry;
        });
    return entered.get();
  }

  /** 兼容旧调用：同时释放该用户持有的活动许可。 */
  public void leave(String user) {
    Objects.requireNonNull(user);
    var activity = new AtomicLong();
    var released = new AtomicBoolean();
    inflight.computeIfPresent(
        user,
        (key, entry) -> {
          released.set(true);
          activity.set(entry.activity());
          return entry.permits().decrementAndGet() <= 0 ? null : entry;
        });
    if (released.get()) releaseActivity(activity.get());
  }

  /** 释放用户许可及指定活动的许可；用户不存在时不做任何扣减。 */
  public void leave(String user, long activity) {
    Objects.requireNonNull(user);
    if (activity <= 0) throw new IllegalArgumentException("Activity required");
    var released = new AtomicBoolean();
    inflight.computeIfPresent(
        user,
        (key, entry) -> {
          released.set(true);
          return entry.permits().decrementAndGet() <= 0 ? null : entry;
        });
    if (released.get()) releaseActivity(activity);
  }

  private boolean acquireActivity(long activity) {
    var acquired = new AtomicBoolean();
    activityInflight.compute(
        activity,
        (key, permits) -> {
          if (permits == null) {
            acquired.set(true);
            return new AtomicInteger(1);
          }
          if (permits.get() >= MAX_ACTIVITY_INFLIGHT) return permits;
          permits.incrementAndGet();
          acquired.set(true);
          return permits;
        });
    return acquired.get();
  }

  private void releaseActivity(long activity) {
    activityInflight.computeIfPresent(
        activity,
        (key, permits) -> permits.decrementAndGet() <= 0 ? null : permits);
  }

  /** 当前在途许可数，供 p0_inflight_orders gauge 使用。 */
  public int inflight() {
    return inflight.values().stream().mapToInt(entry -> entry.permits().get()).sum();
  }
}
