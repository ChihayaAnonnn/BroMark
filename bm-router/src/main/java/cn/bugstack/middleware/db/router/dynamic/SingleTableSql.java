package cn.bugstack.middleware.db.router.dynamic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.*;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;

/** 分表仅接受单物理表 DML；先检查完整语义 AST，再改写表及其限定引用。 */
public final class SingleTableSql {
  private SingleTableSql() {}

  public static String rewrite(String sql, String suffix) {
    if (suffix == null || !suffix.matches("[0-9]{3}"))
      throw new IllegalArgumentException("Table route is required");
    try {
      Statement statement = CCJSqlParserUtil.parse(sql);
      Table table;
      Values values = null;
      if (statement instanceof PlainSelect select && select.getFromItem() instanceof Table target) {
        table = target;
      } else if (statement instanceof Insert insert && insert.getSelect() instanceof Values rows) {
        table = insert.getTable();
        values = rows;
      } else if (statement instanceof Update update) {
        table = update.getTable();
      } else if (statement instanceof Delete delete) {
        table = delete.getTable();
      } else throw unsupported();
      if (table == null || table.getName() == null || table.getName().isBlank())
        throw unsupported();
      var ast = new AstBoundary(statement, table, values);
      ast.walk(statement, null);
      String old = table.getName();
      String name =
          old.startsWith("`") && old.endsWith("`")
              ? "`" + old.substring(1, old.length() - 1) + "_" + suffix + "`"
              : old + "_" + suffix;
      for (Table qualifier : ast.qualifiers)
        if (qualifier != table && sameName(qualifier.getName(), old)) qualifier.setName(name);
      table.setName(name);
      return statement.toString();
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Unsupported sharded SQL", e);
    }
  }

  /**
   * 4.9 的 TablesNamesFinder 并非穷尽遍历，不能用作安全边界。 遍历固定版本的所有语义 AST 字段，包括继承字段、排序、分组及 duplicate-update； 不调用
   * AST getter，也不进入 parser 的 token/parent 元数据图或字符串字面量。 未能访问的字段直接拒绝，禁止静默略过。
   */
  private static final class AstBoundary {
    private final Statement root;
    private final Table target;
    private final Values insertValues;
    private final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Table> qualifiers = Collections.newSetFromMap(new IdentityHashMap<>());

    AstBoundary(Statement root, Table target, Values insertValues) {
      this.root = root;
      this.target = target;
      this.insertValues = insertValues;
    }

    void walk(Object value, Object parent) throws IllegalAccessException {
      if (value == null || !seen.add(value)) return;
      if (value instanceof Iterable<?> list) {
        for (Object child : list) walk(child, parent);
        return;
      }
      if (value instanceof Map<?, ?> map) {
        for (Object child : map.values()) walk(child, parent);
        return;
      }
      if (value.getClass().isArray()) {
        for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++)
          walk(java.lang.reflect.Array.get(value, i), parent);
        return;
      }
      if (!semantic(value.getClass())) return;
      if (value instanceof Select && value != root && value != insertValues) throw unsupported();
      if (value instanceof Join || value instanceof WithItem) throw unsupported();
      if (value instanceof FromItem && !(value instanceof Table)) throw unsupported();
      if (value instanceof Table table) {
        if (table.getSchemaName() != null
            || table.getDatabase() != null && table.getDatabase().getDatabaseName() != null)
          throw unsupported();
        if (table != target) {
          if (!(parent instanceof Column || parent instanceof AllTableColumns)) throw unsupported();
          String name = table.getName();
          if (name != null
              && !name.isBlank()
              && !sameName(name, target.getName())
              && (target.getAlias() == null || !sameName(name, target.getAlias().getName())))
            throw unsupported();
          qualifiers.add(table);
        }
      }
      for (Class<?> type = value.getClass(); semantic(type); type = type.getSuperclass()) {
        for (Field field : type.getDeclaredFields()) {
          if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
          if (!field.trySetAccessible()) throw unsupported();
          walk(field.get(value), value);
        }
      }
    }

    private static boolean semantic(Class<?> type) {
      if (type == null) return false;
      String name = type.getName();
      return name.startsWith("net.sf.jsqlparser.")
          && !name.startsWith("net.sf.jsqlparser.parser.")
          && !name.equals("net.sf.jsqlparser.ASTNodeAccessImpl");
    }
  }

  private static boolean sameName(String first, String second) {
    return first != null
        && second != null
        && first.replace("`", "").equalsIgnoreCase(second.replace("`", ""));
  }

  private static IllegalArgumentException unsupported() {
    return new IllegalArgumentException(
        "Only single-table SELECT/INSERT VALUES/UPDATE/DELETE are supported");
  }
}
