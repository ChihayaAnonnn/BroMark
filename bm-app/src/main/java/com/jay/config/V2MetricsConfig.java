package com.jay.config;

import cn.bugstack.middleware.db.router.dynamic.DynamicDataSource;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
public class V2MetricsConfig {
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
