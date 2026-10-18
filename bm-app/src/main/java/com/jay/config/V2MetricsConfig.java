package com.jay.config;

import cn.bugstack.middleware.db.router.dynamic.DynamicDataSource;
import com.jay.domain.raffle.service.P0AdmissionGate;
import com.jay.domain.task.service.RelayObservation;
import com.jay.infrastructure.event.MicrometerRelayObservation;
import com.jay.infrastructure.observability.P0BacklogProbe;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
public class V2MetricsConfig {
  @Bean
  public RelayObservation relayObservation(MeterRegistry metrics) {
    return new MicrometerRelayObservation(metrics);
  }

  @Bean
  public P0AdmissionGate p0AdmissionGate() {
    return new P0AdmissionGate();
  }

  @Bean(destroyMethod = "close")
  public P0BacklogProbe p0BacklogProbe(DataSource dataSource, P0AdmissionGate gate) {
    return new P0BacklogProbe(dataSource, gate::update);
  }

  @Bean
  public SmartInitializingSingleton p0Metrics(
      MeterRegistry metrics, P0BacklogProbe probe, P0AdmissionGate gate) {
    return () -> {
      for (String result : List.of("accepted", "rejected", "ratelimited"))
        Counter.builder("p0_admission_total").tag("result", result).register(metrics);
      for (String result : List.of("delivered", "duplicate", "retry", "failed"))
        Counter.builder("p0_delivery_total").tag("result", result).register(metrics);
      for (String type : List.of("ReserveAward", "ReservationConfirmed"))
        for (String result : List.of("emitted", "delivered", "duplicate", "retry", "failed"))
          Counter.builder("p0_event_total")
              .tags("type", type, "result", result)
              .register(metrics);
      Gauge.builder("p0_backlog_orders", probe, p -> p.snapshot().pending())
          .tag("state", "PENDING_RESERVATION")
          .description("PENDING_RESERVATION orders across all draw shards")
          .register(metrics);
      Gauge.builder("p0_oldest_age_seconds", probe, p -> p.snapshot().oldestAgeSeconds())
          .tag("state", "PENDING_RESERVATION")
          .description("Age of the oldest PENDING_RESERVATION order")
          .baseUnit("seconds")
          .register(metrics);
      Gauge.builder("p0_inflight_orders", gate, P0AdmissionGate::inflight)
          .description("Admission permits currently held by in-flight draws")
          .register(metrics);
    };
  }

  @Bean
  public SmartInitializingSingleton v2ConnectionMetrics(
      DataSource dataSource,
      MeterRegistry metrics,
      com.jay.domain.asset.repository.UserTransactionRunner runner,
      V2BudgetProperties props) {
    return () -> {
      if (dataSource instanceof DynamicDataSource dynamic) {
        int index = 0;
        for (var ds : dynamic.getResolvedDataSources().values())
          if (ds instanceof HikariDataSource pool) {
            // bounded启动校验会先获取连接；已启动的Hikari名称不可再次修改。
            if (!pool.isRunning()) pool.setPoolName("v2-pool-" + index);
            index++;
            pool.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(metrics));
          }
        // InnoDB累计等待仅适用于MySQL；PostgreSQL等待由pg_stat_activity独立采集。
        if (dynamic.getResolvedDataSources().values().stream()
            .filter(HikariDataSource.class::isInstance)
            .map(HikariDataSource.class::cast)
            .anyMatch(pool -> pool.getJdbcUrl().startsWith("jdbc:postgresql:"))) return;
      }
      io.micrometer.core.instrument.Gauge.builder(
              "bm.v2.database.lock.wait",
              () -> {
                try {
                  java.util.Set<String> seen = new java.util.HashSet<>();
                  double sum = 0;
                  for (var route : props.getPhysical().entrySet())
                    if (seen.add(route.getValue())) {
                      var slot =
                          route.getKey().equals("00")
                              ? com.jay.domain.asset.model.RouteSlot.control()
                              : new com.jay.domain.asset.model.RouteSlot(route.getKey(), "000");
                      sum +=
                          runner.inSlot(
                                  slot,
                                  com.jay.domain.asset.model.WorkClass.QUERY,
                                  new com.jay.domain.asset.model.Deadline(
                                      java.time.Instant.now().plusMillis(100)),
                                  () ->
                                      new org.springframework.jdbc.core.JdbcTemplate(dataSource)
                                          .queryForObject(
                                              "select VARIABLE_VALUE from"
                                                  + " performance_schema.global_status where"
                                                  + " VARIABLE_NAME='INNODB_ROW_LOCK_TIME'",
                                              Double.class))
                              / 1000.0;
                    }
                  return sum;
                } catch (RuntimeException e) {
                  return Double.NaN;
                }
              })
          .description("Cumulative InnoDB row lock wait seconds across physical instances")
          .baseUnit("seconds")
          .register(metrics);
    };
  }
}
