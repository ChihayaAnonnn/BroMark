package com.jay.config;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.asset.service.*;
import com.jay.infrastructure.transaction.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
@Import({V2WorkerConfig.class,V2MetricsConfig.class})
@Configuration(proxyBeanMethods=false) @ConditionalOnProperty(name="bm.v2.enabled",havingValue="true")
public class V2TransactionConfig {
 @Bean public RouteCodec routeCodec(){return new RouteCodec(2,4);}
 @Bean public UserTransactionRunner userTransactionRunner(DataSource ds,PlatformTransactionManager tm,RouteCodec codec,BoundedDatabaseBudget budget,V2BudgetProperties properties,io.micrometer.core.instrument.MeterRegistry metrics){return new RoutedTransactionRunner(ds,tm,codec,budget,properties.getPhysical(),metrics);}
 @Bean public BusinessClock businessClock(DataSource ds){return new DatabaseBusinessClock(ds);}
 @Bean public FaultInjector faultInjector(){return point->{};}
}
