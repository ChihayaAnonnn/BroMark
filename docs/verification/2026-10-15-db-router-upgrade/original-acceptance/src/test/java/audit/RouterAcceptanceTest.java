package audit;

import cn.bugstack.middleware.db.router.*;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import cn.bugstack.middleware.db.router.config.DataSourceAutoConfig;
import cn.bugstack.middleware.db.router.strategy.impl.DBRouterStrategyHashCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.mybatis.spring.annotation.MapperScan;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

public class RouterAcceptanceTest {
  @Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
  @MapperScan(basePackageClasses=ProbeMapper.class,annotationClass=Mapper.class)
  static class App {}
  public record Query(String userId,int id) {}
  @Mapper @DBRouterStrategy(splitTable=true)
  public interface ProbeMapper {
    @DBRouter @Select("select marker from items where id=#{id}") String marker(Query query);
  }
  static class NullableRequest { private String userId; }
  static class RoutedDao { @DBRouter public String read(String userId){return DBContextHolder.getDBKey();} }

  ApplicationContextRunner configured() { return configured(true); }
  ApplicationContextRunner configured(boolean includeDriver) {
    var properties=new ArrayList<String>(List.of(
      "mini-db-router.jdbc.datasource.dbCount=2", "mini-db-router.jdbc.datasource.tbCount=4",
      "mini-db-router.jdbc.datasource.routerKey=userId", "mini-db-router.jdbc.datasource.list=db01,db02",
      "mini-db-router.jdbc.datasource.default=db00",
      "mini-db-router.jdbc.datasource.global.driver-class-name=org.h2.Driver",
      "mini-db-router.jdbc.datasource.global.username=sa", "mini-db-router.jdbc.datasource.global.password=",
      "mini-db-router.jdbc.datasource.db00.url=jdbc:h2:mem:acontrol;DB_CLOSE_DELAY=-1;MODE=MySQL",
      "mini-db-router.jdbc.datasource.db01.url=jdbc:h2:mem:aone;DB_CLOSE_DELAY=-1;MODE=MySQL",
      "mini-db-router.jdbc.datasource.db02.url=jdbc:h2:mem:atwo;DB_CLOSE_DELAY=-1;MODE=MySQL"));
    if(!includeDriver) properties.removeIf(p -> p.contains("driver-class-name"));
    return new ApplicationContextRunner().withUserConfiguration(App.class).withPropertyValues(properties.toArray(String[]::new));
  }
  @Test void autoConfigurationIsDiscovered() {
    configured().run(c->{assertThat(c).hasNotFailed(); assertThat(c).hasSingleBean(DBRouterConfig.class);});
  }
  @Test void nestedDaoRestoresCallerRoute() {
    var strategy=new DBRouterStrategyHashCode(new DBRouterConfig(2,4,"userId"));
    var factory=new AspectJProxyFactory(new RoutedDao());
    factory.addAspect(new DBRouterJoinPoint(new DBRouterConfig(2,4,"userId"),strategy));
    RoutedDao dao=factory.getProxy(); strategy.doRouter("outer-user");
    String beforeDb=DBContextHolder.getDBKey(),beforeTb=DBContextHolder.getTBKey();
    try {dao.read("inner-user"); assertThat(DBContextHolder.getDBKey()).isEqualTo(beforeDb); assertThat(DBContextHolder.getTBKey()).isEqualTo(beforeTb);}
    finally {strategy.clear();}
  }
  @Test void nullRouteValueIsRejected() {
    var a=new DBRouterJoinPoint(new DBRouterConfig(2,4,"userId"),new DBRouterStrategyHashCode(new DBRouterConfig(2,4,"userId")));
    assertThatThrownBy(()->a.getAttrValue("userId",new Object[]{new NullableRequest()})).isInstanceOf(IllegalArgumentException.class);
  }
  @Test void unknownDatabaseDoesNotFallback() {
    configured().withUserConfiguration(DataSourceAutoConfig.class).run(c->{
      assertThat(c).hasNotFailed(); DBContextHolder.setDBKey("99");
      try {assertThatThrownBy(()->new JdbcTemplate(c.getBean(DataSource.class)).queryForObject("select 1",Integer.class)).hasMessageContaining("Connection");}
      finally {DBContextHolder.clearDBKey();}
    });
  }
  @Test void activeTransactionRejectsChangingRoute() {
    configured().withUserConfiguration(DataSourceAutoConfig.class).run(c->{
      assertThat(c).hasNotFailed(); DBContextHolder.setDBKey("01"); DBContextHolder.setTBKey("000");
      try {c.getBean(TransactionTemplate.class).executeWithoutResult(status->{
        assertThatThrownBy(()->DBContextHolder.setDBKey("02")).isInstanceOf(IllegalStateException.class);
        assertThat(DBContextHolder.getDBKey()).isEqualTo("01");
      });} finally {DBContextHolder.clearDBKey();DBContextHolder.clearTBKey();}
    });
  }
  @Test void managerAndTemplateUseSameBean() {
    configured().withUserConfiguration(DataSourceAutoConfig.class).run(c->{ assertThat(c).hasNotFailed();
      assertThat(c.getBean(TransactionTemplate.class).getTransactionManager()).isSameAs(c.getBean(PlatformTransactionManager.class));
    });
  }
  @Test void driverMayBeInferredWhenNotConfigured() {
    configured(false).withUserConfiguration(DataSourceAutoConfig.class)
      .run(c->{assertThat(c).hasNotFailed(); assertThat(new JdbcTemplate(c.getBean(DataSource.class)).queryForObject("select 1",Integer.class)).isEqualTo(1);});
  }
  @Test void virtualThreadsExecuteRealMapperOnEveryDbAndTable() {
    configured().run(c->{
      assertThat(c).hasNotFailed(); assertThat(c).hasSingleBean(DBRouterConfig.class);
      DataSource ds=c.getBean(DataSource.class); var j=new JdbcTemplate(ds);
      for(int db=1;db<=2;db++) {
        DBContextHolder.setDBKey(String.format("%02d",db));
        try {for(int tb=0;tb<4;tb++) {
          String table="items_"+String.format("%03d",tb);
          j.execute("create table if not exists "+table+"(id int primary key,marker varchar(16))");
          j.update("merge into "+table+" key(id) values(1,?)",String.format("%02d/%03d",db,tb));
        }} finally {DBContextHolder.clearDBKey();}
      }
      var mapper=c.getBean(ProbeMapper.class);
      var strategy=c.getBean(cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy.class);
      var tx=c.getBean(TransactionTemplate.class);
      try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
        var futures=new ArrayList<Future<String>>(); var expected=new ArrayList<String>();
        for(int i=0;i<1000;i++) {
          String user="user-"+i; int h=user.hashCode();int slot=7&(h^(h>>>16));
          expected.add(String.format("%02d/%03d",slot/4+1,slot%4));
          futures.add(pool.submit(()->{assertThat(Thread.currentThread().isVirtual()).isTrue();
            strategy.doRouter(user);
            try {return tx.execute(status -> mapper.marker(new Query(user,1)));}
            finally {strategy.clear(); assertThat(DBContextHolder.getDBKey()).isNull();assertThat(DBContextHolder.getTBKey()).isNull();}
          }));
        }
        for(int i=0;i<futures.size();i++) assertThat(futures.get(i).get()).isEqualTo(expected.get(i));
        assertThat(new HashSet<>(expected)).hasSize(8);
      } catch(Exception e){throw new AssertionError(e);}
    });
  }
}
