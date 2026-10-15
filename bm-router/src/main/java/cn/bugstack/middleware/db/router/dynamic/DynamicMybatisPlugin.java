package cn.bugstack.middleware.db.router.dynamic;

import cn.bugstack.middleware.db.router.DBContextHolder;
import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import java.lang.reflect.Field;
import java.sql.Connection;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @description: Mybatis 拦截器，通过对 SQL 语句的拦截处理，修改分表信息
 * @author: 小傅哥，微信：fustack
 * @date: 2021/9/30
 * @github: https://github.com/fuzhengwei @Copyright: 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn -
 *     沉淀、分享、成长，让自己和他人都能有所收获！
 */
@Intercepts({
  @Signature(
      type = StatementHandler.class,
      method = "prepare",
      args = {Connection.class, Integer.class})
})
public class DynamicMybatisPlugin implements Interceptor {

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    // 获取StatementHandler
    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
    MetaObject metaObject =
        MetaObject.forObject(
            statementHandler,
            SystemMetaObject.DEFAULT_OBJECT_FACTORY,
            SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
            new DefaultReflectorFactory());
    MappedStatement mappedStatement =
        (MappedStatement) metaObject.getValue("delegate.mappedStatement");

    // 获取自定义注解判断是否进行分表操作
    String id = mappedStatement.getId();
    String className = id.substring(0, id.lastIndexOf("."));
    Class<?> clazz = Class.forName(className);
    DBRouterStrategy dbRouterStrategy = clazz.getAnnotation(DBRouterStrategy.class);
    if (null == dbRouterStrategy || !dbRouterStrategy.splitTable()) {
      return invocation.proceed();
    }

    // 获取SQL
    BoundSql boundSql = statementHandler.getBoundSql();
    String sql = boundSql.getSql();

    String replaceSql = SingleTableSql.rewrite(sql, DBContextHolder.getTBKey());

    // 通过反射修改SQL语句
    Field field = boundSql.getClass().getDeclaredField("sql");
    field.setAccessible(true);
    field.set(boundSql, replaceSql);
    field.setAccessible(false);

    return invocation.proceed();
  }
}
