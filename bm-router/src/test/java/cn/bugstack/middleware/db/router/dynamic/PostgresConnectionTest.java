package cn.bugstack.middleware.db.router.dynamic;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PostgresConnectionTest {
  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  @Test
  void translatesPreparedAndStatementBatchSqlAndUnwrapsSqlException() throws Exception {
    var sql = new ArrayList<String>();
    var failure = new SQLException("real failure", "23505");
    Statement st =
        proxy(
            Statement.class,
            (p, m, a) -> {
              if (a != null && a.length > 0 && a[0] instanceof String s) sql.add(s);
              if (m.getName().equals("execute")) throw failure;
              return null;
            });
    PreparedStatement ps = proxy(PreparedStatement.class, (p, m, a) -> null);
    Connection raw =
        proxy(
            Connection.class,
            (p, m, a) -> {
              if (m.getName().equals("prepareStatement")) {
                sql.add((String) a[0]);
                return ps;
              }
              if (m.getName().equals("createStatement")) return st;
              return null;
            });
    Connection wrapped = PostgresConnection.wrap(raw);
    wrapped.prepareStatement("select UTC_TIMESTAMP(6)").addBatch();
    Statement statement = wrapped.createStatement();
    statement.addBatch("select UTC_TIMESTAMP(6)");
    assertThatThrownBy(() -> statement.execute("select UTC_TIMESTAMP(6)")).isSameAs(failure);
    assertThat(sql).hasSize(3).allMatch(s -> s.contains("statement_timestamp"));
    assertThat(statement.getConnection()).isSameAs(wrapped);
    assertThat(wrapped.unwrap(Connection.class)).isSameAs(wrapped);
  }

  @Test
  void timestampReadingUsesUtcUnlessCallerProvidesCalendar() throws Exception {
    var calls = new ArrayList<Object[]>();
    ResultSet rows =
        proxy(
            ResultSet.class,
            (p, m, a) -> {
              if (m.getName().equals("getTimestamp")) {
                calls.add(a);
                return Timestamp.from(java.time.Instant.EPOCH);
              }
              return null;
            });
    Statement statement = proxy(Statement.class, (p, m, a) -> rows);
    Connection raw = proxy(Connection.class, (p, m, a) -> statement);
    ResultSet wrapped =
        PostgresConnection.wrap(raw)
            .createStatement()
            .executeQuery("select business_at from probe");
    wrapped.getTimestamp(1);
    wrapped.getTimestamp("business_at");
    Calendar explicit = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
    wrapped.getTimestamp(2, explicit);
    wrapped.getTimestamp("explicit_at", explicit);
    assertThat(calls.get(0)).hasSize(2);
    assertThat(calls.get(0)[0]).isEqualTo(1);
    assertThat(((Calendar) calls.get(0)[1]).getTimeZone().getID()).isEqualTo("UTC");
    assertThat(calls.get(1)).hasSize(2);
    assertThat(calls.get(1)[0]).isEqualTo("business_at");
    assertThat(((Calendar) calls.get(1)[1]).getTimeZone().getID()).isEqualTo("UTC");
    assertThat(calls.get(2)[1]).isSameAs(explicit);
    assertThat(calls.get(3)[1]).isSameAs(explicit);
  }

  @Test
  void timestampBindingUsesUtcEvenWhenJvmTimezoneDiffers() throws Exception {
    var calls = new ArrayList<Object[]>();
    PreparedStatement ps =
        proxy(
            PreparedStatement.class,
            (p, m, a) -> {
              if (m.getName().equals("setTimestamp")) calls.add(a);
              return null;
            });
    Connection raw = proxy(Connection.class, (p, m, a) -> ps);
    PostgresConnection.wrap(raw)
        .prepareStatement("select ?")
        .setTimestamp(1, Timestamp.from(java.time.Instant.parse("2026-10-18T01:02:03Z")));
    PostgresConnection.wrap(raw)
        .prepareStatement("select ?")
        .setObject(1, Timestamp.from(java.time.Instant.parse("2026-10-18T01:02:03Z")));
    assertThat(calls).hasSize(2);
    assertThat(calls.getFirst()).hasSize(3);
    assertThat(((Calendar) calls.getFirst()[2]).getTimeZone().getID()).isEqualTo("UTC");
    assertThat(((Calendar) calls.get(1)[2]).getTimeZone().getID()).isEqualTo("UTC");
  }

  @Test
  void bindsStringAsUnspecifiedAndPreservesBytea() throws Exception {
    var calls = new ArrayList<List<Object>>();
    PreparedStatement ps =
        proxy(
            PreparedStatement.class,
            (p, m, a) -> {
              if (m.getName().startsWith("set"))
                calls.add(Arrays.asList(m.getName(), a[0], a[1], a.length > 2 ? a[2] : ""));
              return null;
            });
    Connection raw = proxy(Connection.class, (p, m, a) -> ps);
    PreparedStatement wrapped =
        PostgresConnection.wrap(raw)
            .prepareStatement("insert into outbox_event(payload) values(?)");
    wrapped.setString(1, "{\"x\":1}");
    byte[] bytes = {0, 1, -1};
    wrapped.setBytes(2, bytes);
    assertThat(calls.getFirst()).containsExactly("setObject", 1, "{\"x\":1}", Types.OTHER);
    assertThat(calls.get(1).get(2)).isSameAs(bytes);
  }
}
