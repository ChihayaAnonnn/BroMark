package cn.bugstack.middleware.db.router.dynamic;

import cn.bugstack.middleware.db.router.DBContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @description: 动态数据源获取，每当切换数据源，都要从这个里面进行获取
 * @author: 小傅哥，微信：fustack
 * @date: 2021/9/22
 * @github: https://github.com/fuzhengwei @Copyright: 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn -
 *     沉淀、分享、成长，让自己和他人都能有所收获！
 */
public class DynamicDataSource extends AbstractRoutingDataSource implements AutoCloseable {
  private final java.util.Set<Object> owned =
      java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
  private boolean closed;
  private java.util.concurrent.CompletableFuture<Void> deferredClose;

  @Override
  public void setTargetDataSources(java.util.Map<Object, Object> targets) {
    super.setTargetDataSources(targets);
    owned.addAll(targets.values());
  }

  @Override
  public void setDefaultTargetDataSource(Object source) {
    super.setDefaultTargetDataSource(source);
    owned.add(source);
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    closeOwned();
  }

  /** V2接管清理所有权；预算未排空时不关闭仍在使用的池，默认close语义保持同步。 */
  public boolean closeBefore(long deadline, java.util.function.BooleanSupplier safeToClose) {
    java.util.concurrent.CompletableFuture<Void> completion;
    synchronized (this) {
      if (!closed) {
        closed = true;
        deferredClose = new java.util.concurrent.CompletableFuture<>();
        Thread.ofVirtual()
            .name("v2-datasource-cleanup")
            .start(
                () -> {
                  try {
                    while (!safeToClose.getAsBoolean())
                      java.util.concurrent.locks.LockSupport.parkNanos(
                          java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
                    closeOwned();
                    deferredClose.complete(null);
                  } catch (Throwable e) {
                    deferredClose.completeExceptionally(e);
                    org.slf4j.LoggerFactory.getLogger(getClass())
                        .warn("Deferred datasource cleanup failed", e);
                  }
                });
      }
      completion = deferredClose;
    }
    if (completion == null) return true;
    try {
      completion.get(
          Math.max(0, deadline - System.nanoTime()), java.util.concurrent.TimeUnit.NANOSECONDS);
      return true;
    } catch (java.util.concurrent.TimeoutException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (java.util.concurrent.ExecutionException e) {
      throw new IllegalStateException("Router cleanup failed", e.getCause());
    }
  }

  private void closeOwned() {
    RuntimeException failure = null;
    for (Object source : owned)
      if (source instanceof AutoCloseable c)
        try {
          c.close();
        } catch (Exception e) {
          if (failure == null)
            failure = new IllegalStateException("Failed closing router pools", e);
          else failure.addSuppressed(e);
        }
    if (failure != null) throw failure;
  }

  @Value("${mini-db-router.jdbc.datasource.default}")
  private String defaultDataSource;

  @Override
  protected Object determineCurrentLookupKey() {
    if (null == DBContextHolder.getDBKey()) {
      return defaultDataSource;
    } else {
      return "db" + DBContextHolder.getDBKey();
    }
  }
}
