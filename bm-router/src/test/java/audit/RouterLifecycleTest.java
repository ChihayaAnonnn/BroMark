package audit;

import static org.assertj.core.api.Assertions.*;

import cn.bugstack.middleware.db.router.dynamic.DynamicDataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.util.*;
import org.junit.jupiter.api.*;

class RouterLifecycleTest {
  public static class CountedPool extends HikariDataSource {
    static List<CountedPool> created = new ArrayList<>();
    int closes;

    public CountedPool() {
      created.add(this);
    }

    @Override
    public void close() {
      closes++;
      super.close();
    }
  }

  @BeforeEach
  void reset() {
    CountedPool.created.clear();
  }

  @Test
  void closesUniquePoolOnceEvenWhenDefaultAliasesDb00() throws Exception {
    var p = new CountedPool();
    var ds = new DynamicDataSource();
    ds.setTargetDataSources(Map.of("db00", p, "db01", p));
    ds.setDefaultTargetDataSource(p);
    ds.afterPropertiesSet();
    assertThat(ds).isInstanceOf(AutoCloseable.class);
    ((AutoCloseable) ds).close();
    ((AutoCloseable) ds).close();
    assertThat(p.closes).isEqualTo(1);
  }

  @Test
  void partialInitializationClosesEarlierAndFailingPool() {
    new RouterAcceptanceTest()
        .configured()
        .withPropertyValues(
            "mini-db-router.jdbc.datasource.global.type-class-name=" + CountedPool.class.getName(),
            "mini-db-router.jdbc.datasource.db02.pool.no-such-property=1")
        .run(
            c -> {
              assertThat(c).hasFailed();
              assertThat(CountedPool.created).isNotEmpty();
              for (var p : CountedPool.created) assertThat(p.closes).isEqualTo(1);
            });
  }

  @Test
  void poolPropertiesConvertAndContextCloses() {
    new RouterAcceptanceTest()
        .configured()
        .withPropertyValues(
            "mini-db-router.jdbc.datasource.global.type-class-name=" + CountedPool.class.getName(),
            "mini-db-router.jdbc.datasource.global.pool.maximum-pool-size=3")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              for (var p : CountedPool.created) assertThat(p.getMaximumPoolSize()).isEqualTo(3);
            });
    for (var p : CountedPool.created) assertThat(p.closes).isEqualTo(1);
  }
}
