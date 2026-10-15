package cn.bugstack.middleware.db.router;

import java.util.Objects;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @description: 数据源上下文
 * @author: 小傅哥，微信：fustack
 * @date: 2021/9/22
 * @github: https://github.com/fuzhengwei @Copyright: 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn -
 *     沉淀、分享、成长，让自己和他人都能有所收获！
 */

/** 路由绑定到执行线程；原子校验库表组合后修改，禁止活动事务改变槽。 */
public final class DBContextHolder {
  private record Route(String db, String table) {}

  private static final ThreadLocal<Route> ROUTE = new ThreadLocal<>();

  private DBContextHolder() {}

  public static String getDBKey() {
    Route r = ROUTE.get();
    return r == null ? null : r.db();
  }

  public static String getTBKey() {
    Route r = ROUTE.get();
    return r == null ? null : r.table();
  }

  public static void setRoute(String db, String tb) {
    if (db != null && !db.matches("[0-9]{2}"))
      throw new IllegalArgumentException("Invalid database route");
    if (tb != null && !tb.matches("[0-9]{3}"))
      throw new IllegalArgumentException("Invalid table route");
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && (!Objects.equals(db, getDBKey()) || !Objects.equals(tb, getTBKey())))
      throw new IllegalStateException("Cannot change route during an active transaction");
    if (db == null && tb == null) ROUTE.remove();
    else ROUTE.set(new Route(db, tb));
  }

  public static void setDBKey(String db) {
    setRoute(db, getTBKey());
  }

  public static void setTBKey(String tb) {
    setRoute(getDBKey(), tb);
  }

  public static void clearDBKey() {
    setRoute(null, getTBKey());
  }

  public static void clearTBKey() {
    setRoute(getDBKey(), null);
  }
}
