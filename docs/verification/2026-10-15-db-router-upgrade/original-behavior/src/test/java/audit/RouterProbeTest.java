package audit;

import cn.bugstack.middleware.db.router.*;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.config.DataSourceAutoConfig;
import cn.bugstack.middleware.db.router.strategy.impl.DBRouterStrategyHashCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class RouterProbeTest {
  @Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
  static class App {}

  @Test void publishedStarterNotDiscoveredOnBoot3() {
    new ApplicationContextRunner().withUserConfiguration(App.class).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).doesNotHaveBean(DBRouterConfig.class);
      System.out.println("EVIDENCE: Boot 3 did not discover published router auto-configuration");
    });
  }

  @Test void manualImportCanCreateBeansWithModernBootApis() {
    new ApplicationContextRunner().withUserConfiguration(DataSourceAutoConfig.class)
      .withPropertyValues(
        "mini-db-router.jdbc.datasource.dbCount=2", "mini-db-router.jdbc.datasource.tbCount=4",
        "mini-db-router.jdbc.datasource.routerKey=userId", "mini-db-router.jdbc.datasource.list=db01,db02",
        "mini-db-router.jdbc.datasource.default=db00",
        "mini-db-router.jdbc.datasource.global.driver-class-name=org.h2.Driver",
        "mini-db-router.jdbc.datasource.global.username=sa", "mini-db-router.jdbc.datasource.global.password=",
        "mini-db-router.jdbc.datasource.db00.url=jdbc:h2:mem:control",
        "mini-db-router.jdbc.datasource.db01.url=jdbc:h2:mem:one",
        "mini-db-router.jdbc.datasource.db02.url=jdbc:h2:mem:two")
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(DBRouterConfig.class);
        System.out.println("EVIDENCE: manual import creates router beans on Java 25 / Boot 3.5.16");
      });
  }

  static class RoutedDao {
    @DBRouter public String read(String userId) { return DBContextHolder.getDBKey(); }
  }
  @Test void daoAspectClearsOuterManualRoute() {
    var strategy=new DBRouterStrategyHashCode(new DBRouterConfig(2,4,"userId"));
    var factory=new AspectJProxyFactory(new RoutedDao());
    factory.addAspect(new DBRouterJoinPoint(new DBRouterConfig(2,4,"userId"),strategy));
    RoutedDao dao=factory.getProxy();
    strategy.doRouter("outer-user");
    assertThat(DBContextHolder.getDBKey()).isNotNull();
    dao.read("outer-user");
    assertThat(DBContextHolder.getDBKey()).isNull();
    System.out.println("EVIDENCE: annotated DAO clears the caller's manual route");
  }

  static class NullableRequest { private String userId; }
  @Test void nullRouteAttributeBecomesLiteralNullString() {
    var aspect=new DBRouterJoinPoint(new DBRouterConfig(2,4,"userId"),
      new DBRouterStrategyHashCode(new DBRouterConfig(2,4,"userId")));
    assertThat(aspect.getAttrValue("userId",new Object[]{new NullableRequest()})).isEqualTo("null");
    System.out.println("EVIDENCE: missing route attribute is converted to literal null");
  }

  @Test void invalidDbKeySilentlyFallsBackToDefault() {
    var d=new DriverManagerDataSource("jdbc:h2:mem:fallback","sa","");
    var route=new AbstractRoutingDataSource(){
      protected Object determineCurrentLookupKey(){ return "unknown-db"; }
    };
    route.setTargetDataSources(Map.of("db01",d)); route.setDefaultTargetDataSource(d); route.afterPropertiesSet();
    assertThat(new JdbcTemplate(route).queryForObject("select 1",Integer.class)).isEqualTo(1);
    System.out.println("EVIDENCE: router inherits lenient fallback for unknown db key");
  }

  @Test void changingThreadLocalInsideTransactionDoesNotChangeBoundConnection() {
    var one=new DriverManagerDataSource("jdbc:h2:mem:txone;DB_CLOSE_DELAY=-1","sa","");
    var two=new DriverManagerDataSource("jdbc:h2:mem:txtwo;DB_CLOSE_DELAY=-1","sa","");
    for (var pair : Map.of(one,"one",two,"two").entrySet()) {
      var j=new JdbcTemplate(pair.getKey()); j.execute("create table if not exists marker(name varchar(16))");
      j.update("delete from marker"); j.update("insert into marker values(?)",pair.getValue());
    }
    var route=new AbstractRoutingDataSource(){protected Object determineCurrentLookupKey(){return DBContextHolder.getDBKey();}};
    route.setTargetDataSources(Map.of("01",one,"02",two)); route.setDefaultTargetDataSource(one); route.afterPropertiesSet();
    var tx=new TransactionTemplate(new DataSourceTransactionManager(route)); var j=new JdbcTemplate(route);
    DBContextHolder.setDBKey("01");
    try {
      tx.executeWithoutResult(status->{
        assertThat(j.queryForObject("select name from marker",String.class)).isEqualTo("one");
        DBContextHolder.setDBKey("02");
        assertThat(j.queryForObject("select name from marker",String.class)).isEqualTo("one");
      });
    } finally { DBContextHolder.clearDBKey(); }
    System.out.println("EVIDENCE: route changes after transaction start do not migrate the JDBC connection");
  }

  @Test void virtualThreadsKeepTheirOwnRouteButDoNotInheritCallerRoute() throws Exception {
    DBContextHolder.setDBKey("parent");
    try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
      var tasks=new ArrayList<Future<String>>();
      for(int i=0;i<1000;i++) {
        final String value="route-"+i;
        tasks.add(pool.submit(()->{
          assertThat(Thread.currentThread().isVirtual()).isTrue();
          assertThat(DBContextHolder.getDBKey()).isNull();
          DBContextHolder.setDBKey(value);
          try {Thread.sleep(1); return DBContextHolder.getDBKey();}
          finally {DBContextHolder.clearDBKey();}
        }));
      }
      for(int i=0;i<tasks.size();i++) assertThat(tasks.get(i).get()).isEqualTo("route-"+i);
      assertThat(DBContextHolder.getDBKey()).isEqualTo("parent");
    } finally {DBContextHolder.clearDBKey();}
    System.out.println("EVIDENCE: 1000 virtual threads retain isolated route state; caller route is not inherited");
  }
}
