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
            });
    return new BoundedDatabaseBudget(props.getInstances(), metrics);
  }

  @Bean(destroyMethod = "close")
  public BoundedVirtualExecutor v2WorkerExecutor() {
    return new BoundedVirtualExecutor("v2-worker", 16, Duration.ofSeconds(30));
  }

  @Bean(destroyMethod = "shutdown")
  public ExecutorService v2ApiExecutor() {
    return pool("v2-api");
  }

  @Bean(destroyMethod = "shutdown")
  public ExecutorService v2RelayExecutor() {
    return pool("v2-relay");
  }

  @Bean(destroyMethod = "shutdown")
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
  public SmartLifecycle v2Shutdown(
      BoundedDatabaseBudget budget,
      BoundedVirtualExecutor v2WorkerExecutor,
      ExecutorService v2ApiExecutor,
      ExecutorService v2RelayExecutor,
      ExecutorService v2RecoveryExecutor) {
    return new SmartLifecycle() {
      volatile boolean running;

      public void start() {
        running = true;
      }

      public boolean isRunning() {
        return running;
      }

      public int getPhase() {
        return Integer.MAX_VALUE - 100;
      }

      public void stop() {
        running = false;
        var workers =
            java.util.List.of(v2WorkerExecutor, v2ApiExecutor, v2RelayExecutor, v2RecoveryExecutor);
        workers.forEach(ExecutorService::shutdown);
        budget.stopAccepting();
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        try {
          for (var worker : workers)
            if (!worker.awaitTermination(
                Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS)) worker.shutdownNow();
          if (!budget.awaitIdle(Duration.ofNanos(Math.max(0, end - System.nanoTime()))))
            throw new IllegalStateException("Database transactions did not drain before shutdown");
        } catch (InterruptedException e) {
          workers.forEach(ExecutorService::shutdownNow);
          Thread.currentThread().interrupt();
        }
      }
    };
  }
}
