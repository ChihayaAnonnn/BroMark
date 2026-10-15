package cn.bugstack.middleware.db.router;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: 数据路由切面，通过自定义注解的方式，拦截被切面的方法，进行数据库路由
 * @author: 小傅哥，微信：fustack
 * @date: 2021/9/22
 * @github: https://github.com/fuzhengwei @Copyright: 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn -
 *     沉淀、分享、成长，让自己和他人都能有所收获！
 */
@Aspect
@org.springframework.core.annotation.Order(
    org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 100)
public class DBRouterJoinPoint {

  private Logger logger = LoggerFactory.getLogger(DBRouterJoinPoint.class);

  private DBRouterConfig dbRouterConfig;

  private IDBRouterStrategy dbRouterStrategy;

  public DBRouterJoinPoint(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
    this.dbRouterConfig = dbRouterConfig;
    this.dbRouterStrategy = dbRouterStrategy;
  }

  @Pointcut("@annotation(cn.bugstack.middleware.db.router.annotation.DBRouter)")
  public void aopPoint() {}

  /**
   * 所有需要分库分表的操作，都需要使用自定义注解进行拦截，拦截后读取方法中的入参字段，根据字段进行路由操作。 1. dbRouter.key() 确定根据哪个字段进行路由 2.
   * getAttrValue 根据数据库路由字段，从入参中读取出对应的值。比如路由 key 是 uId，那么就从入参对象 Obj 中获取到 uId 的值。 3.
   * dbRouterStrategy.doRouter(dbKeyAttr) 路由策略根据具体的路由值进行处理 4. 路由处理完成比，就是放行。 jp.proceed(); 5. 最后
   * dbRouterStrategy 需要执行 clear 因为这里用到了 ThreadLocal 需要手动清空。关于 ThreadLocal 内存泄漏介绍
   * https://t.zsxq.com/027QF2fae
   */
  @Around("aopPoint() && @annotation(dbRouter)")
  public Object doRouter(ProceedingJoinPoint jp, DBRouter dbRouter) throws Throwable {
    String dbKey = dbRouter.key();
    if (StringUtils.isBlank(dbKey) && StringUtils.isBlank(dbRouterConfig.getRouterKey())) {
      throw new RuntimeException("annotation DBRouter key is null！");
    }
    dbKey = StringUtils.isNotBlank(dbKey) ? dbKey : dbRouterConfig.getRouterKey();
    // 路由属性
    String dbKeyAttr = getAttrValue(dbKey, jp.getArgs());
    String previousDb = DBContextHolder.getDBKey(), previousTb = DBContextHolder.getTBKey();
    try {
      dbRouterStrategy.doRouter(dbKeyAttr);
      return jp.proceed();
    } finally {
      DBContextHolder.setRoute(previousDb, previousTb);
    }
  }

  private Method getMethod(JoinPoint jp) throws NoSuchMethodException {
    Signature sig = jp.getSignature();
    MethodSignature methodSignature = (MethodSignature) sig;
    return jp.getTarget()
        .getClass()
        .getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
  }

  public String getAttrValue(String attr, Object[] args) {
    if (args.length == 1 && args[0] instanceof String value) {
      if (value.isBlank()) throw new IllegalArgumentException("Route value is required");
      return value;
    }
    for (Object arg : args) {
      if (arg == null) continue;
      Object value = getValueByName(arg, attr);
      if (value != null && !value.toString().isBlank()) return value.toString();
    }
    throw new IllegalArgumentException("Route attribute is required: " + attr);
  }

  /**
   * 获取对象的特定属性值
   *
   * @author tang
   * @param item 对象
   * @param name 属性名
   * @return 属性值
   */
  private Object getValueByName(Object item, String name) {
    try {
      Field field = getFieldByName(item, name);
      if (field == null) {
        return null;
      }
      field.setAccessible(true);
      Object o = field.get(item);
      field.setAccessible(false);
      return o;
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  /**
   * 根据名称获取方法，该方法同时兼顾继承类获取父类的属性
   *
   * @author tang
   * @param item 对象
   * @param name 属性名
   * @return 该属性对应方法
   */
  private Field getFieldByName(Object item, String name) {
    try {
      Field field;
      try {
        field = item.getClass().getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        field = item.getClass().getSuperclass().getDeclaredField(name);
      }
      return field;
    } catch (NoSuchFieldException e) {
      return null;
    }
  }
}
