package audit;

import static org.assertj.core.api.Assertions.*;

import cn.bugstack.middleware.db.router.*;
import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RouterSqlBoundaryTest {
  @Mapper
  @DBRouterStrategy(splitTable = true)
  public interface BoundaryMapper {
    @Select("SeLeCt i.marker from `items` i where i.id=#{id}")
    String quoted(int id);

    @Select("select 'from fake update dummy' from items where id=1")
    String literal();

    @Select("select a.marker from items a join items b on a.id=b.id")
    String join();

    @Select("select marker from items where id in(select id from items)")
    String subquery();

    @Select("with x as(select marker from items) select marker from x")
    String cte();

    @Select("select marker from items; select marker from items")
    String multiple();

    @Insert("insert into `items` (id,marker) values(#{id},#{marker})")
    void insert(@Param("id") int id, @Param("marker") String marker);

    @Update("update items set marker=#{marker} where id=#{id}")
    void update(@Param("id") int id, @Param("marker") String marker);

    @Delete("delete from items where id=#{id}")
    void delete(int id);
  }

  static void assertRejected(Throwable error) {
    while (error != null) {
      if (error instanceof IllegalArgumentException) return;
      error = error.getCause();
    }
    throw new AssertionError("Expected SQL validation rejection");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "select id from items where (select max(id) from other_items) is null",
        "select id from items order by (select max(id) from other_items)",
        "select id from items group by (select max(id) from other_items)",
        "select id from items having (select max(id) from other_items) is null",
        "insert into items(id) values(1) on duplicate key update id=(select max(id) from"
            + " other_items)",
        "update items set id=1 where (select max(id) from other_items) is null",
        "delete from items where (select max(id) from other_items) is null"
      })
  void rejectsSubqueriesInEveryClause(String sql) {
    assertThatThrownBy(
            () -> cn.bugstack.middleware.db.router.dynamic.SingleTableSql.rewrite(sql, "000"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rewritesQualifiedWildcardOrderingGroupingAndNullPredicates() {
    String result =
        cn.bugstack.middleware.db.router.dynamic.SingleTableSql.rewrite(
            "select items.* from items where items.id is not null group by items.id order by"
                + " items.id",
            "003");
    assertThat(result)
        .contains(
            "items_003.*",
            "items_003.id IS NOT NULL",
            "GROUP BY items_003.id",
            "ORDER BY items_003.id");
  }

  @Test
  void completeBroMarkShardedSqlInventoryIsSupported() throws Exception {
    try (var reader =
        new java.io.BufferedReader(
            new java.io.InputStreamReader(
                getClass().getResourceAsStream("/bromark-sharded-sql.tsv")))) {
      var lines = reader.lines().toList();
      assertThat(lines).hasSize(13);
      for (String line : lines) {
        String[] parts = line.split("\\t", 2);
        assertThat(cn.bugstack.middleware.db.router.dynamic.SingleTableSql.rewrite(parts[1], "003"))
            .as(parts[0])
            .contains("_003");
      }
    }
  }

  @Test
  void aliasesQuotesLiteralsAndDmlUseOnlyPhysicalTable() {
    new RouterAcceptanceTest()
        .configured()
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              DBContextHolder.setRoute("01", "000");
              try {
                var j = new JdbcTemplate(c.getBean(DataSource.class));
                j.execute(
                    "create table if not exists items_000(id int primary key,marker varchar(100))");
                j.update("delete from items_000");
                var m = c.getBean(BoundaryMapper.class);
                m.insert(1, "initial");
                assertThat(m.quoted(1)).isEqualTo("initial");
                assertThat(m.literal()).isEqualTo("from fake update dummy");
                m.update(1, "changed");
                assertThat(m.quoted(1)).isEqualTo("changed");
                m.delete(1);
                assertThat(m.quoted(1)).isNull();
              } finally {
                DBContextHolder.setRoute(null, null);
              }
            });
  }

  @Test
  void unsupportedQueriesFailBeforeExecution() {
    new RouterAcceptanceTest()
        .configured()
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              DBContextHolder.setRoute("01", "000");
              try {
                var m = c.getBean(BoundaryMapper.class);
                for (Runnable query :
                    new Runnable[] {
                      () -> m.join(), () -> m.subquery(), () -> m.cte(), () -> m.multiple()
                    })
                  assertThatThrownBy(query::run).satisfies(RouterSqlBoundaryTest::assertRejected);
                DBContextHolder.setRoute("01", null);
                assertThatThrownBy(() -> m.literal())
                    .satisfies(RouterSqlBoundaryTest::assertRejected);
              } finally {
                DBContextHolder.setRoute(null, null);
              }
            });
  }
}
