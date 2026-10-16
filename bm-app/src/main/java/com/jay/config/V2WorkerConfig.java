package com.jay.config;

import com.jay.infrastructure.transaction.BoundedDatabaseBudget;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
@EnableConfigurationProperties(V2BudgetProperties.class)
public class V2WorkerConfig {
  @Bean
  public BoundedDatabaseBudget databaseBudget(
      V2BudgetProperties props, MeterRegistry metrics, javax.sql.DataSource dataSource) {
    if (!props.getPhysical().keySet().equals(java.util.Set.of("00", "01", "02")))
      throw new IllegalArgumentException("Map every logical database to its physical instance");
    if (!(dataSource instanceof cn.bugstack.middleware.db.router.dynamic.DynamicDataSource dynamic))
      throw new IllegalArgumentException("V2 requires routed data source");
    var measuredPools = new java.util.HashMap<String, Integer>();
    props
        .getPhysical()
        .forEach(
            (logical, instance) -> {
              var limits = props.getInstances().get(instance);
              if (limits == null) throw new IllegalArgumentException("Missing physical budget");
              long total = limits.values().stream().mapToLong(Integer::longValue).sum();
              var source = dynamic.getResolvedDataSources().get("db" + logical);
              if (!(source instanceof com.zaxxer.hikari.HikariDataSource hikari)
                  || total >= hikari.getMaximumPoolSize())
                throw new IllegalArgumentException(
                    "Budget total must leave spare capacity in every logical pool");
              measuredPools.put(
                  logical, ((com.zaxxer.hikari.HikariDataSource) source).getMaximumPoolSize());
            });
    props.validateConnections(measuredPools);
    return new BoundedDatabaseBudget(props.getInstances(), metrics);
  }

  @Bean(destroyMethod = "")
  public BoundedVirtualExecutor v2WorkerExecutor(V2BudgetProperties props) {
    return new BoundedVirtualExecutor(
        "v2-worker", props.getWorkerConcurrency(), Duration.ofSeconds(30));
  }

  @Bean(destroyMethod = "")
  public ExecutorService v2ApiExecutor() {
    return pool("v2-api");
  }

  @Bean(destroyMethod = "")
  public ExecutorService v2RelayExecutor() {
    return pool("v2-relay");
  }

  @Bean(destroyMethod = "")
  public ExecutorService v2RecoveryExecutor() {
    return pool("v2-recovery");
  }

  private ExecutorService pool(String name) {
    return new ThreadPoolExecutor(
        2,
        2,
        0,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(16),
        Thread.ofPlatform().name(name + "-", 0).factory(),
        new ThreadPoolExecutor.AbortPolicy());
  }

  @Bean
  public com.jay.trigger.job.v2.CoalescingJobDispatcher v2JobDispatcher(
      @org.springframework.beans.factory.annotation.Qualifier("v2RecoveryExecutor")
          ExecutorService executor,
      V2RecoveryRedriveProperties recovery) {
    if ("guarded".equals(recovery.getMode()))
      throw new IllegalStateException(
          "Guarded recovery requires Task6 wiring; no placeholder recovery permitted");
    return new com.jay.trigger.job.v2.CoalescingJobDispatcher(executor);
  }

  /** 在HTTP线程进入控制器之前关闭新入站，包含不经过RequestAdmission的供应商回调。 */
  public static final class InboundGate
      extends org.springframework.web.filter.OncePerRequestFilter {
    private boolean accepting = true;
    private int active;

    public synchronized void stopAccepting() {
      accepting = false;
    }

    public synchronized boolean awaitIdle(Duration timeout) throws InterruptedException {
      long end = System.nanoTime() + timeout.toNanos();
      while (active > 0) {
        long left = end - System.nanoTime();
        if (left <= 0) return false;
        TimeUnit.NANOSECONDS.timedWait(this, left);
      }
      return true;
    }

    protected void doFilterInternal(
        jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        jakarta.servlet.FilterChain chain)
        throws java.io.IOException, jakarta.servlet.ServletException {
      if (!request.getRequestURI().startsWith(request.getContextPath() + "/api/v2/")) {
        chain.doFilter(request, response);
        return;
      }
      synchronized (this) {
        if (!accepting) {
          response.setStatus(503);
          return;
        }
        active++;
      }
      try {
        chain.doFilter(request, response);
      } finally {
        synchronized (this) {
          active--;
          notifyAll();
        }
      }
    }
  }

  @Bean
  public InboundGate v2InboundGate() {
    return new InboundGate();
  }

  @Bean
  public org.springframework.boot.web.servlet.FilterRegistrationBean<InboundGate> v2InboundFilter(
      InboundGate gate) {
    var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(gate);
    registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/api/v2/*");
    return registration;
  }

  @Bean
  public SmartLifecycle v2Shutdown(
      BoundedDatabaseBudget budget,
      BoundedVirtualExecutor v2WorkerExecutor,
      @org.springframework.beans.factory.annotation.Qualifier("v2ApiExecutor")
          ExecutorService v2ApiExecutor,
      @org.springframework.beans.factory.annotation.Qualifier("v2RelayExecutor")
          ExecutorService v2RelayExecutor,
      @org.springframework.beans.factory.annotation.Qualifier("v2RecoveryExecutor")
          ExecutorService v2RecoveryExecutor,
      com.jay.trigger.job.v2.CoalescingJobDispatcher dispatcher,
      InboundGate inbound,
      org.springframework.beans.factory.ObjectProvider<V2RabbitConfig.MessagingWorkers> messaging,
      javax.sql.DataSource dataSource) {
    return new SmartLifecycle() {
      volatile boolean running;

      public void start() {
        running = true;
      }

      public boolean isRunning() {
        return running;
      }

      public int getPhase() {
        return Integer.MAX_VALUE;
      }

      public synchronized void stop() {
        if (!running) return;
        running = false;
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        inbound.stopAccepting();
        dispatcher.quiesce();
        var workers =
            java.util.List.of(v2WorkerExecutor, v2ApiExecutor, v2RelayExecutor, v2RecoveryExecutor);
        RuntimeException failure = null;
        try {
          var active = messaging.getIfAvailable();
          if (active != null) active.shutdown(end);
        } catch (RuntimeException e) {
          failure = e;
        }
        workers.forEach(ExecutorService::shutdown);
        try {
          if (!inbound.awaitIdle(remaining(end)))
            failure = new IllegalStateException("HTTP requests did not drain");
          for (var worker : workers)
            if (!worker.awaitTermination(remaining(end).toNanos(), TimeUnit.NANOSECONDS)) {
              worker.shutdownNow();
              failure =
                  new IllegalStateException(
                      "V2 workers did not drain; leased work remains recoverable");
            }
          budget.stopAccepting();
          if (!budget.awaitIdle(remaining(end)))
            failure = new IllegalStateException("Database transactions did not drain");
        } catch (InterruptedException e) {
          workers.forEach(ExecutorService::shutdownNow);
          budget.stopAccepting();
          Thread.currentThread().interrupt();
          failure = new IllegalStateException("V2 shutdown interrupted", e);
        }
        if (dataSource instanceof cn.bugstack.middleware.db.router.dynamic.DynamicDataSource routed
            && !routed.closeBefore(end, budget::isIdle))
          failure =
              new IllegalStateException(
                  "Datasource cleanup continues after deadline; active transactions retain their"
                      + " pools");
        if (failure != null) throw failure;
      }
    };
  }

  public static Duration remaining(long end) {
    return Duration.ofNanos(Math.max(0, end - System.nanoTime()));
  }
}
