package cn.bugstack.middleware.db.router.dynamic;

import cn.bugstack.middleware.db.router.DBContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @description: 动态数据源获取，每当切换数据源，都要从这个里面进行获取
 * @author: 小傅哥，微信：fustack
 * @date: 2021/9/22
 * @github: https://github.com/fuzhengwei @Copyright: 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn -
 *     沉淀、分享、成长，让自己和他人都能有所收获！
 */
public class DynamicDataSource extends AbstractRoutingDataSource implements AutoCloseable {
  private final java.util.Set<Object> owned =
      java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
  private boolean closed;

  @Override
  public void setTargetDataSources(java.util.Map<Object, Object> targets) {
    super.setTargetDataSources(targets);
    owned.addAll(targets.values());
  }

  @Override
  public void setDefaultTargetDataSource(Object source) {
    super.setDefaultTargetDataSource(source);
    owned.add(source);
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    RuntimeException failure = null;
    for (Object source : owned)
      if (source instanceof AutoCloseable c)
        try {
          c.close();
        } catch (Exception e) {
          if (failure == null)
            failure = new IllegalStateException("Failed closing router pools", e);
          else failure.addSuppressed(e);
        }
    if (failure != null) throw failure;
  }

  @Value("${mini-db-router.jdbc.datasource.default}")
  private String defaultDataSource;

  @Override
  protected Object determineCurrentLookupKey() {
    if (null == DBContextHolder.getDBKey()) {
      return defaultDataSource;
    } else {
      return "db" + DBContextHolder.getDBKey();
    }
  }
}
