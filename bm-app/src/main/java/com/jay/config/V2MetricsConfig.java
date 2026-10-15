package com.jay.config;
import io.micrometer.core.instrument.MeterRegistry;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import cn.bugstack.middleware.db.router.dynamic.DynamicDataSource;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.SmartInitializingSingleton;
import javax.sql.DataSource;
@Configuration(proxyBeanMethods=false) @ConditionalOnProperty(name="bm.v2.enabled",havingValue="true")
public class V2MetricsConfig {
 @Bean public SmartInitializingSingleton v2ConnectionMetrics(DataSource dataSource,MeterRegistry metrics){return ()->{if(dataSource instanceof DynamicDataSource dynamic){int index=0;for(var ds:dynamic.getResolvedDataSources().values())if(ds instanceof HikariDataSource pool){pool.setPoolName("v2-pool-"+index++);pool.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(metrics));}}};}
}
