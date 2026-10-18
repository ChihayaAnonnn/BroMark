package cn.bugstack.middleware.db.router.dynamic;

import java.lang.reflect.*;
import java.sql.*;

/** 仅用于 PostgreSQL 连接；池本身保持原始 Hikari 对象以保留预算、生命周期和指标。 */
public final class PostgresConnection {
  private PostgresConnection() {}

  public static Connection wrap(Connection connection) {
    return proxy(Connection.class, connection, null);
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> contract, T delegate, Connection owner) {
    return (T)
        Proxy.newProxyInstance(
            PostgresConnection.class.getClassLoader(),
            new Class<?>[] {contract},
            (self, method, arguments) -> {
              String name = method.getName();
              if (name.equals("equals") && method.getParameterCount() == 1)
                return self == arguments[0];
              if (name.equals("hashCode") && method.getParameterCount() == 0)
                return System.identityHashCode(self);
              if (name.equals("unwrap") && ((Class<?>) arguments[0]).isInstance(self)) return self;
              if (name.equals("isWrapperFor") && ((Class<?>) arguments[0]).isInstance(self))
                return true;
              if (name.equals("getConnection") && owner != null) return owner;
              Object[] args = arguments == null ? null : arguments.clone();
              if (args != null
                  && args.length > 0
                  && args[0] instanceof String sql
                  && (name.equals("prepareStatement")
                      || name.equals("prepareCall")
                      || name.startsWith("execute")
                      || name.equals("addBatch")
                      || name.equals("nativeSQL"))) args[0] = PostgresSql.translate(sql);
              Object result;
              try {
                if (delegate instanceof ResultSet rows
                    && name.equals("getTimestamp")
                    && args.length == 1) {
                  java.util.Calendar utc =
                      java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                  return args[0] instanceof Integer index
                      ? rows.getTimestamp(index, utc)
                      : rows.getTimestamp((String) args[0], utc);
                }
                if (delegate instanceof PreparedStatement statement
                    && name.equals("setTimestamp")
                    && args.length == 2) {
                  statement.setTimestamp(
                      (Integer) args[0],
                      (Timestamp) args[1],
                      java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")));
                  return null;
                }
                if (delegate instanceof PreparedStatement statement
                    && name.equals("setObject")
                    && args[1] instanceof Timestamp timestamp
                    && (args.length == 2
                        || (args.length == 3
                            && Integer.valueOf(Types.TIMESTAMP).equals(args[2])))) {
                  statement.setTimestamp(
                      (Integer) args[0],
                      timestamp,
                      java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")));
                  return null;
                }
                if (delegate instanceof PreparedStatement statement && name.equals("setString")) {
                  // OTHER 让 PG 按目标列推断 jsonb/text；不依赖调用方是否配置 stringtype。
                  statement.setObject((Integer) args[0], args[1], Types.OTHER);
                  return null;
                }
                if (delegate instanceof PreparedStatement statement
                    && name.equals("setObject")
                    && args.length >= 2
                    && args[1] instanceof String) {
                  statement.setObject((Integer) args[0], args[1], Types.OTHER);
                  return null;
                }
                result = method.invoke(delegate, args);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
              Connection connection = delegate instanceof Connection ? (Connection) self : owner;
              if (result instanceof CallableStatement statement)
                return proxy(CallableStatement.class, statement, connection);
              if (result instanceof PreparedStatement statement)
                return proxy(PreparedStatement.class, statement, connection);
              if (result instanceof Statement statement)
                return proxy(Statement.class, statement, connection);
              if (result instanceof ResultSet rows) return proxy(ResultSet.class, rows, connection);
              if (delegate instanceof ResultSet
                  && name.equals("getObject")
                  && args.length == 1
                  && result != null
                  && result.getClass().getName().equals("org.postgresql.util.PGobject")) {
                String type = (String) result.getClass().getMethod("getType").invoke(result);
                if (type.equals("json") || type.equals("jsonb")) return result.toString();
              }
              return result;
            });
  }
}
