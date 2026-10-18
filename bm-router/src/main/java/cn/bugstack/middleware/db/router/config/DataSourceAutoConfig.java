package cn.bugstack.middleware.db.router.config;

import cn.bugstack.middleware.db.router.DBRouterConfig;
import cn.bugstack.middleware.db.router.DBRouterJoinPoint;
import cn.bugstack.middleware.db.router.dynamic.DynamicDataSource;
import cn.bugstack.middleware.db.router.dynamic.DynamicMybatisPlugin;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.middleware.db.router.strategy.impl.DBRouterStrategyHashCode;
import cn.bugstack.middleware.db.router.util.PropertyUtil;
import cn.bugstack.middleware.db.router.util.StringUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @description: 数据源配置解析。类的加载顺序笔记；https://t.zsxq.com/0fZELdch7 @double
 * @author: 小傅哥，微信：fustack
 * @date: 2021/9/22
 * @github: https://github.com/fuzhengwei @Copyright: 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn -
 *     沉淀、分享、成长，让自己和他人都能有所收获！
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
public class DataSourceAutoConfig implements EnvironmentAware {

  /** 分库全局属性 */
  private static final String TAG_GLOBAL = "global";

  /** 连接池属性 */
  private static final String TAG_POOL = "pool";

  /** 数据源配置组 */
  private Map<String, Map<String, Object>> dataSourceMap = new java.util.LinkedHashMap<>();

  /** 默认数据源配置 */
  private Map<String, Object> defaultDataSourceConfig;

  /** 分库数量 */
  private int dbCount;

  /** 分表数量 */
  private int tbCount;

  /** 路由字段 */
  private String routerKey;

  @Bean(name = "db-router-point")
  @ConditionalOnMissingBean
  public DBRouterJoinPoint point(
      DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
    return new DBRouterJoinPoint(dbRouterConfig, dbRouterStrategy);
  }

  @Bean
  public DBRouterConfig dbRouterConfig() {
    return new DBRouterConfig(dbCount, tbCount, routerKey);
  }

  @Bean
  public Interceptor plugin() {
    return new DynamicMybatisPlugin();
  }

  // 可参考 README
  // 配置连接池；https://gitcode.net/KnowledgePlanet/db-router-spring-boot-starter/-/blob/master/README.md
  private DataSource createDataSource(Map<String, Object> attributes) {
    try {
      DataSourceProperties dataSourceProperties = new DataSourceProperties();
      dataSourceProperties.setUrl(attributes.get("url").toString());
      dataSourceProperties.setUsername(attributes.get("username").toString());
      dataSourceProperties.setPassword(attributes.get("password").toString());

      if (attributes.get("driver-class-name") != null) {
        dataSourceProperties.setDriverClassName(attributes.get("driver-class-name").toString());
      }

      String typeClassName =
          attributes.get("type-class-name") == null
              ? "com.zaxxer.hikari.HikariDataSource"
              : attributes.get("type-class-name").toString();
      DataSource ds =
          dataSourceProperties
              .initializeDataSourceBuilder()
              .type((Class<DataSource>) Class.forName(typeClassName))
              .build();

      try {
        var wrapper = new org.springframework.beans.BeanWrapperImpl(ds);
        Map<String, Object> poolProps =
            (Map<String, Object>) attributes.getOrDefault(TAG_POOL, Collections.emptyMap());
        for (var entry : poolProps.entrySet()) {
          String key = StringUtils.middleScoreToCamelCase(entry.getKey());
          if (!wrapper.isWritableProperty(key))
            throw new IllegalArgumentException("Unknown pool property: " + key);
          wrapper.setPropertyValue(key, entry.getValue());
        }
      } catch (RuntimeException e) {
        if (ds instanceof AutoCloseable closeable)
          try {
            closeable.close();
          } catch (Exception close) {
            e.addSuppressed(close);
          }
        throw e;
      }
      return ds;
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("can not find datasource type class by class name", e);
    }
  }

  @Bean
  public DataSource createDataSource() {
    Map<Object, Object> targets = new java.util.LinkedHashMap<>();
    DynamicDataSource dynamic = new DynamicDataSource();
    boolean postgres = defaultDataSourceConfig.get("url").toString().startsWith("jdbc:postgresql:");
    if (dataSourceMap.values().stream()
        .anyMatch(c -> c.get("url").toString().startsWith("jdbc:postgresql:") != postgres))
      throw new IllegalArgumentException("All routed databases must use the same SQL dialect");
    dynamic.setPostgresCompatibility(postgres);
    try {
      DataSource control = createDataSource(defaultDataSourceConfig);
      targets.put("db00", control);
      for (var entry : dataSourceMap.entrySet()) {
        if (entry.getKey().equals("db00")) continue;
        targets.put(entry.getKey(), createDataSource(entry.getValue()));
      }
      dynamic.setTargetDataSources(targets);
      dynamic.setDefaultTargetDataSource(control);
      dynamic.setLenientFallback(false);
      return dynamic;
    } catch (RuntimeException e) {
      dynamic.setTargetDataSources(targets);
      try {
        dynamic.close();
      } catch (RuntimeException close) {
        e.addSuppressed(close);
      }
      throw e;
    }
  }

  @Bean
  public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig) {
    return new DBRouterStrategyHashCode(dbRouterConfig);
  }

  @Bean
  @ConditionalOnMissingBean(PlatformTransactionManager.class)
  public DataSourceTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean
  public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }

  @Override
  public void setEnvironment(Environment environment) {
    String prefix = "mini-db-router.jdbc.datasource.";

    dbCount = Integer.parseInt(Objects.requireNonNull(environment.getProperty(prefix + "dbCount")));
    tbCount = Integer.parseInt(Objects.requireNonNull(environment.getProperty(prefix + "tbCount")));
    routerKey = environment.getProperty(prefix + "routerKey");
    // 分库分表数据源
    String dataSources = environment.getProperty(prefix + "list");
    Map<String, Object> globalInfo = getGlobalProps(environment, prefix + TAG_GLOBAL);
    for (String dbInfo : dataSources.split(",")) {
      final String dbPrefix = prefix + dbInfo;
      Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, dbPrefix, Map.class);
      injectGlobal(dataSourceProps, globalInfo);
      dataSourceMap.put(dbInfo, dataSourceProps);
    }

    // 默认数据源
    String defaultData = environment.getProperty(prefix + "default");
    defaultDataSourceConfig = PropertyUtil.handle(environment, prefix + defaultData, Map.class);
    injectGlobal(defaultDataSourceConfig, globalInfo);
  }

  private Map<String, Object> getGlobalProps(Environment environment, String key) {
    try {
      return PropertyUtil.handle(environment, key, Map.class);
    } catch (Exception e) {
      return Collections.EMPTY_MAP;
    }
  }

  private void injectGlobal(Map<String, Object> origin, Map<String, Object> global) {
    for (String key : global.keySet()) {
      if (!origin.containsKey(key)) {
        origin.put(key, global.get(key));
      } else if (origin.get(key) instanceof Map) {
        injectGlobal((Map<String, Object>) origin.get(key), (Map<String, Object>) global.get(key));
      }
    }
  }
}
