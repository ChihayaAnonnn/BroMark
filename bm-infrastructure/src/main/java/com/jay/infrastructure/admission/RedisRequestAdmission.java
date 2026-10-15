package com.jay.infrastructure.admission;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;

public final class RedisRequestAdmission implements RequestAdmission {
  private final RedissonClient redis;
  private final int userRps, activityRps, localRps, queryRps, cancelRps;
  private final Semaphore trades, queries, cancellations;
  private final String script;
  private final Map<String, long[]> local = new HashMap<>();

  public RedisRequestAdmission(
      RedissonClient redis,
      int userRps,
      int activityRps,
      int maxInFlight,
      int localRps,
      int queryRps,
      int queryInFlight) {
    this(
        redis,
        userRps,
        activityRps,
        maxInFlight,
        localRps,
        queryRps,
        queryInFlight,
        queryRps,
        queryInFlight);
  }

  public RedisRequestAdmission(
      RedissonClient redis,
      int userRps,
      int activityRps,
      int maxInFlight,
      int localRps,
      int queryRps,
      int queryInFlight,
      int cancelRps,
      int cancelInFlight) {
    if (userRps <= 0
        || activityRps <= 0
        || maxInFlight <= 0
        || localRps <= 0
        || queryRps <= 0
        || queryInFlight <= 0
        || cancelRps <= 0
        || cancelInFlight <= 0
        || localRps > cancelRps
        || localRps > Math.min(userRps, Math.min(activityRps, queryRps)))
      throw new IllegalArgumentException("Explicit conservative admission budgets required");
    this.redis = redis;
    this.userRps = userRps;
    this.activityRps = activityRps;
    this.localRps = localRps;
    this.queryRps = queryRps;
    this.cancelRps = cancelRps;
    cancellations = new Semaphore(cancelInFlight);
    trades = new Semaphore(maxInFlight);
    queries = new Semaphore(queryInFlight);
    try (var in = getClass().getResourceAsStream("/redis/v2_rate_admission.lua")) {
      if (in == null) throw new IllegalStateException("Admission Lua missing");
      script = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void deadline(Deadline d) {
    if (d.remaining().isZero() || d.remaining().isNegative())
      throw new BudgetExceededException("Admission deadline expired");
  }

  private synchronized boolean fallback(String category) {
    long second = System.nanoTime() / 1_000_000_000L;
    var bucket = local.computeIfAbsent(category, k -> new long[] {second, 0});
    if (bucket[0] != second) {
      bucket[0] = second;
      bucket[1] = 0;
    }
    return ++bucket[1] <= localRps;
  }

  private void rate(String category, String identity, int limit, Deadline d) {
    deadline(d);
    boolean allowed;
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive())
      throw new IllegalStateException("Redis admission must precede database transaction");
    org.redisson.api.RFuture<Number> pending = null;
    try {
      if (redis == null) throw new IllegalStateException("Redis unavailable");
      String hash = new RequestHasher().hash("ADMISSION", Map.of("identity", identity));
      pending =
          redis
              .getScript(StringCodec.INSTANCE)
              .evalAsync(
                  RScript.Mode.READ_WRITE,
                  script,
                  RScript.ReturnType.INTEGER,
                  List.of("bm:v2:admission:" + category + ":" + hash),
                  Integer.toString(limit));
      Number n =
          pending.get(
              Math.min(
                  d.remaining().toNanos(), java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(200)),
              java.util.concurrent.TimeUnit.NANOSECONDS);
      allowed = n.longValue() == 1;
    } catch (InterruptedException e) {
      if (pending != null) pending.cancel(true);
      Thread.currentThread().interrupt();
      throw new BudgetExceededException("Admission interrupted");
    } catch (java.util.concurrent.TimeoutException
        | java.util.concurrent.ExecutionException
        | RuntimeException e) {
      if (pending != null) pending.cancel(true);
      deadline(d);
      allowed = fallback(category);
    }
    deadline(d);
    if (!allowed) throw new BudgetExceededException("Request rate exceeded");
  }

  private Permit enter(String user, Deadline d, String category, Semaphore semaphore, int limit) {
    deadline(d);
    if (!semaphore.tryAcquire()) throw new BudgetExceededException("In-flight capacity exceeded");
    try {
      rate(category, user, limit, d);
    } catch (RuntimeException e) {
      semaphore.release();
      throw e;
    }
    var released = new AtomicBoolean();
    return () -> {
      if (released.compareAndSet(false, true)) semaphore.release();
    };
  }

  public Permit enter(String user, Deadline d) {
    return enter(user, d, "user", trades, userRps);
  }

  public Permit cancel(String user, Deadline d) {
    return enter(user, d, "cancel", cancellations, cancelRps);
  }

  public Permit query(String user, Deadline d) {
    return enter(user, d, "query", queries, queryRps);
  }

  public void checkActivity(String key, Deadline d) {
    rate("activity", key, activityRps, d);
  }
}
