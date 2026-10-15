package com.jay.test.v2.support;

import java.util.*;
import java.util.concurrent.*;

public final class ConcurrentProbe {
  private ConcurrentProbe() {}

  public static <T> List<T> run(int n, Callable<T> action) throws Exception {
    if (n < 1) throw new IllegalArgumentException("Positive concurrency required");
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var ready = new CountDownLatch(n);
      var start = new CountDownLatch(1);
      var futures = new ArrayList<Future<T>>();
      for (int i = 0; i < n; i++)
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException("start");
                  return action.call();
                }));
      if (!ready.await(10, TimeUnit.SECONDS)) throw new TimeoutException("ready");
      start.countDown();
      var result = new ArrayList<T>();
      Exception error = null;
      for (var f : futures)
        try {
          result.add(f.get(30, TimeUnit.SECONDS));
        } catch (Exception e) {
          if (error == null) error = e;
          else error.addSuppressed(e);
        }
      if (error != null) throw error;
      return result;
    }
  }
}
