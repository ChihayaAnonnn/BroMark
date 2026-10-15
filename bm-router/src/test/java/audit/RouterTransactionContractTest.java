package audit;

import static org.assertj.core.api.Assertions.*;

import cn.bugstack.middleware.db.router.*;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.strategy.impl.DBRouterStrategyHashCode;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RouterTransactionContractTest {
  @AfterEach
  void cleanup() {
    TransactionSynchronizationManager.setActualTransactionActive(false);
    DBContextHolder.clearDBKey();
    DBContextHolder.clearTBKey();
  }

  @Test
  void strategyRejectsTableChangeAtomically() {
    DBContextHolder.setDBKey("01");
    DBContextHolder.setTBKey("000");
    TransactionSynchronizationManager.setActualTransactionActive(true);
    var strategy = new DBRouterStrategyHashCode(new DBRouterConfig(2, 4, "userId"));
    assertThatThrownBy(() -> strategy.doRouter("user-4")).isInstanceOf(IllegalStateException.class);
    assertThat(DBContextHolder.getDBKey()).isEqualTo("01");
    assertThat(DBContextHolder.getTBKey()).isEqualTo("000");
  }

  @Test
  void strategyRejectsDatabaseChangeAtomically() {
    DBContextHolder.setDBKey("01");
    DBContextHolder.setTBKey("000");
    TransactionSynchronizationManager.setActualTransactionActive(true);
    assertThatThrownBy(
            () ->
                new DBRouterStrategyHashCode(new DBRouterConfig(2, 4, "userId")).doRouter("user-0"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(DBContextHolder.getDBKey()).isEqualTo("01");
    assertThat(DBContextHolder.getTBKey()).isEqualTo("000");
  }

  @Test
  void invalidLayoutsRejected() {
    for (int[] n : new int[][] {{0, 4}, {2, 0}, {-2, 4}, {3, 4}, {2, 3}})
      assertThatThrownBy(
              () -> new DBRouterStrategyHashCode(new DBRouterConfig(n[0], n[1], "userId")))
          .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void originalMappingPreserved() {
    String[] users = {
      "user-0", "user-1", "user-2", "user-3", "user-4", "user-5", "user-6", "user-7"
    };
    var s = new DBRouterStrategyHashCode(new DBRouterConfig(2, 4, "userId"));
    java.util.Set<String> slots = new java.util.HashSet<>();
    for (String u : users) {
      s.doRouter(u);
      int h = u.hashCode();
      int i = (h ^ (h >>> 16)) & 7;
      assertThat(DBContextHolder.getDBKey()).isEqualTo(String.format("%02d", i / 4 + 1));
      assertThat(DBContextHolder.getTBKey()).isEqualTo(String.format("%03d", i % 4));
      slots.add(DBContextHolder.getDBKey() + DBContextHolder.getTBKey());
    }
    assertThat(slots).hasSize(8);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  static class TxConfig {
    @Bean
    RoutedService routedService(DataSource ds) {
      return new RoutedService(ds);
    }
  }

  public static class RoutedService {
    private final JdbcTemplate jdbc;

    RoutedService(DataSource ds) {
      jdbc = new JdbcTemplate(ds);
    }

    @DBRouter
    @Transactional
    public String database(String userId) {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      return jdbc.queryForObject("select marker from route_marker", String.class);
    }

    @DBRouter
    public void fail(String userId) {
      throw new IllegalArgumentException("injected");
    }
  }

  @Test
  void proxyRoutesBeforeTransactionAndRestoresOnFailure() {
    new RouterAcceptanceTest()
        .configured()
        .withUserConfiguration(TxConfig.class)
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              var j = new JdbcTemplate(c.getBean(DataSource.class));
              for (String db : new String[] {"00", "01", "02"}) {
                DBContextHolder.setDBKey(db);
                j.execute("create table if not exists route_marker(marker varchar(2))");
                j.update("delete from route_marker");
                j.update("insert into route_marker values(?)", db);
              }
              DBContextHolder.clearDBKey();
              var s = c.getBean(RoutedService.class);
              assertThat(s.database("user-0")).isEqualTo("02");
              assertThat(DBContextHolder.getDBKey()).isNull();
              assertThat(DBContextHolder.getTBKey()).isNull();
              DBContextHolder.setDBKey("02");
              DBContextHolder.setTBKey("003");
              assertThatThrownBy(() -> s.fail("user-0"))
                  .isInstanceOf(IllegalArgumentException.class);
              assertThat(DBContextHolder.getDBKey()).isEqualTo("02");
              assertThat(DBContextHolder.getTBKey()).isEqualTo("003");
              DBContextHolder.setDBKey("00");
              assertThat(j.queryForObject("select marker from route_marker", String.class))
                  .isEqualTo("00");
              DBContextHolder.clearDBKey();
              assertThat(j.queryForObject("select marker from route_marker", String.class))
                  .isEqualTo("00");
            });
  }
}
