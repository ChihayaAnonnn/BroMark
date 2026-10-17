package com.jay.config;

import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.asset.service.*;
import com.jay.infrastructure.transaction.*;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

@Import({V2WorkerConfig.class, V2MetricsConfig.class})
@org.springframework.boot.context.properties.EnableConfigurationProperties({
  V2RelayProperties.class,
  V2RecoveryRedriveProperties.class
})
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
public class V2TransactionConfig {
  @Bean
  public RouteCodec routeCodec() {
    return new RouteCodec(2, 4);
  }

  @Bean
  public UserTransactionRunner userTransactionRunner(
      DataSource ds,
      PlatformTransactionManager tm,
      RouteCodec codec,
      BoundedDatabaseBudget budget,
      V2BudgetProperties properties,
      V2RelayProperties relay,
      io.micrometer.core.instrument.MeterRegistry metrics,
      com.jay.domain.task.service.RelayObservation observation) {
    if ("bounded".equals(relay.getMode())) validateNetworkTimeouts(ds, relay.policy());
    return new RoutedTransactionRunner(
        ds, tm, codec, budget, properties.getPhysical(), metrics, observation);
  }

  /** 读取驱动实际属性，拒绝仅有 future 超时但 JDBC 网络无限等待的配置。 */
  public static void validateNetworkTimeouts(
      DataSource ds, com.jay.domain.task.model.RelayPolicy policy) {
    if (!(ds instanceof cn.bugstack.middleware.db.router.dynamic.DynamicDataSource routed))
      throw new IllegalArgumentException("Bounded relay requires routed MySQL data sources");
    long limit = Math.min(policy.claimTimeout().toMillis(), policy.settleTimeout().toMillis());
    for (var source : routed.getResolvedDataSources().values()) {
      if (!(source instanceof com.zaxxer.hikari.HikariDataSource pool)
          || pool.getConnectionTimeout() > limit)
        throw new IllegalArgumentException(
            "Bounded relay requires Hikari acquisition timeout within DB budget");
      try (var connection = source.getConnection()) {
        var properties = connection.unwrap(com.mysql.cj.jdbc.JdbcConnection.class).getPropertySet();
        int socket = properties.getIntegerProperty("socketTimeout").getValue();
        int connect = properties.getIntegerProperty("connectTimeout").getValue();
        if (socket < 1 || connect < 1 || socket > limit || connect > limit)
          throw new IllegalArgumentException(
              "Bounded relay requires finite JDBC network timeouts within DB budget");
      } catch (java.sql.SQLException e) {
        throw new IllegalStateException("Cannot verify bounded relay network timeout", e);
      }
    }
  }

  @Bean
  public BusinessClock businessClock(DataSource ds) {
    return new DatabaseBusinessClock(ds);
  }

  @Bean
  public FaultInjector faultInjector() {
    return point -> {};
  }
}
