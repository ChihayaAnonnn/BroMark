package audit;

import static org.assertj.core.api.Assertions.*;

import cn.bugstack.middleware.db.router.*;
import cn.bugstack.middleware.db.router.annotation.*;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.zaxxer.hikari.HikariDataSource;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class RouterMySqlIntegrationTest {
  @Mapper
  @DBRouterStrategy(splitTable = true)
  public interface MySqlMapper {
    @DBRouter
    @Select("select marker from items where id=#{id}")
    String read(RouterAcceptanceTest.Query query);

    @Insert("insert into items(id,marker) values(#{id},#{marker})")
    void insert(@Param("id") int id, @Param("marker") String marker);
  }

  @Test
  void virtualThreadsCommitRollbackAndStayWithinPoolLimit() {
    String host = System.getenv().getOrDefault("ROUTER_MYSQL_HOST", "host.docker.internal:13316");
    String password =
        Objects.requireNonNull(
            System.getenv("ROUTER_MYSQL_PASSWORD"), "Set ROUTER_MYSQL_PASSWORD for isolated MySQL");
    new RouterAcceptanceTest()
        .configured()
        .withPropertyValues(
            "mini-db-router.jdbc.datasource.global.driver-class-name=com.mysql.cj.jdbc.Driver",
            "mini-db-router.jdbc.datasource.global.username=root",
            "mini-db-router.jdbc.datasource.global.password=" + password,
            "mini-db-router.jdbc.datasource.global.pool.maximum-pool-size=4",
            "mini-db-router.jdbc.datasource.global.pool.minimum-idle=0",
            "mini-db-router.jdbc.datasource.db00.url=jdbc:mysql://"
                + host
                + "/router_control?useSSL=false&allowPublicKeyRetrieval=true",
            "mini-db-router.jdbc.datasource.db01.url=jdbc:mysql://"
                + host
                + "/router_one?useSSL=false&allowPublicKeyRetrieval=true",
            "mini-db-router.jdbc.datasource.db02.url=jdbc:mysql://"
                + host
                + "/router_two?useSSL=false&allowPublicKeyRetrieval=true")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              var ds = c.getBean(DataSource.class);
              var j = new JdbcTemplate(ds);
              var tx = c.getBean(TransactionTemplate.class);
              tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
              for (int db = 1; db <= 2; db++) {
                DBContextHolder.setRoute(String.format("%02d", db), null);
                for (int t = 0; t < 4; t++) {
                  String table = "items_" + String.format("%03d", t);
                  j.execute(
                      "create table if not exists "
                          + table
                          + "(id int primary key,marker varchar(32)) ENGINE=InnoDB");
                  j.update("delete from " + table);
                  j.update(
                      "insert into " + table + " values(1,?)", String.format("%02d/%03d", db, t));
                }
              }
              DBContextHolder.setRoute(null, null);
              var strategy = c.getBean(IDBRouterStrategy.class);
              var mapper = c.getBean(MySqlMapper.class);
              try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var results = new ArrayList<Future<String>>();
                var expected = new ArrayList<String>();
                for (int i = 0; i < 1000; i++) {
                  String user = "user-" + i;
                  int h = user.hashCode(), slot = (h ^ (h >>> 16)) & 7;
                  expected.add(String.format("%02d/%03d", slot / 4 + 1, slot % 4));
                  results.add(
                      executor.submit(
                          () -> {
                            assertThat(Thread.currentThread().isVirtual()).isTrue();
                            strategy.doRouter(user);
                            try {
                              return tx.execute(
                                  s -> {
                                    assertThat(
                                            j.queryForObject(
                                                "select @@transaction_isolation", String.class))
                                        .isEqualTo("READ-COMMITTED");
                                    return mapper.read(new RouterAcceptanceTest.Query(user, 1));
                                  });
                            } finally {
                              strategy.clear();
                              assertThat(DBContextHolder.getDBKey()).isNull();
                            }
                          }));
                }
                for (int i = 0; i < 1000; i++)
                  assertThat(results.get(i).get(30, TimeUnit.SECONDS)).isEqualTo(expected.get(i));
                assertThat(new HashSet<>(expected)).hasSize(8);
              } catch (Exception e) {
                throw new AssertionError(e);
              }
              for (int db = 1; db <= 2; db++)
                for (int t = 0; t < 4; t++) {
                  DBContextHolder.setRoute(String.format("%02d", db), String.format("%03d", t));
                  tx.executeWithoutResult(s -> mapper.insert(2, "committed"));
                  assertThatThrownBy(
                          () ->
                              tx.executeWithoutResult(
                                  s -> {
                                    mapper.insert(3, "rollback");
                                    throw new IllegalStateException("fault");
                                  }))
                      .isInstanceOf(IllegalStateException.class);
                  tx.executeWithoutResult(
                      s -> {
                        String before = DBContextHolder.getDBKey();
                        assertThatThrownBy(() -> DBContextHolder.setRoute("99", "000"))
                            .isInstanceOf(IllegalStateException.class);
                        assertThat(DBContextHolder.getDBKey()).isEqualTo(before);
                      });
                  assertThat(
                          j.queryForObject(
                              "select count(*) from items_" + String.format("%03d", t),
                              Integer.class))
                      .isEqualTo(2);
                }
              DBContextHolder.setRoute(null, null);
              var dynamic = (cn.bugstack.middleware.db.router.dynamic.DynamicDataSource) ds;
              for (var pool : dynamic.getResolvedDataSources().values()) {
                var hikari = (HikariDataSource) pool;
                assertThat(hikari.getMaximumPoolSize()).isEqualTo(4);
                if (hikari.getHikariPoolMXBean() != null)
                  assertThat(hikari.getHikariPoolMXBean().getTotalConnections())
                      .isLessThanOrEqualTo(4);
              }
            });
  }
}
