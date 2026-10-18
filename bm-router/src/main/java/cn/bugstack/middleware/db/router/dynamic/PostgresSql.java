package cn.bugstack.middleware.db.router.dynamic;

import java.util.*;

/** 当前 V2 JDBC SQL 的显式 PostgreSQL 方言边界；字面量与注释始终作为不可改写的 token。 */
public final class PostgresSql {
  private PostgresSql() {}

  private static final java.util.concurrent.ConcurrentHashMap<String, String> CACHE =
      new java.util.concurrent.ConcurrentHashMap<>();

  private static final java.util.regex.Pattern NUMBER =
      java.util.regex.Pattern.compile("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

  private static final Map<String, String> UPDATE_KEYS =
      Map.of(
          "outbox", "event_id",
          "retry_job", "event_id",
          "provider_inbox_failure", "provider,account,environment,event_id",
          "payment_scan_cursor", "order_id",
          "v2_recovery_scan_cursor", "aggregate_type,aggregate_id",
          "v2_activity_pointer", "activity_id",
          "message_quarantine", "consumer_name,body_hash");
  // 这些普通 INSERT 的仓储明确捕获重复后继续同一事务；PG 必须在语句内消解冲突。
  private static final Set<String> IDEMPOTENT_INSERTS =
      Set.of("consumer_inbox", "consumer_receipt", "award_stock");

  public static String translate(String sql) {
    Objects.requireNonNull(sql, "sql");
    String cached = CACHE.get(sql);
    if (cached != null) return cached;
    String translated = translateUncached(sql);
    synchronized (CACHE) {
      if (CACHE.size() < 1024) {
        String previous = CACHE.putIfAbsent(sql, translated);
        if (previous != null) return previous;
      }
    }
    return translated;
  }

  private static String translateUncached(String sql) {
    List<String> tokens = lex(sql);
    boolean ignore = starts(tokens, 0, "insert", "ignore", "into");
    if (ignore) tokens.remove(1);
    String table =
        starts(tokens, 0, "insert", "into") && tokens.size() > 2
            ? tokens.get(2).toLowerCase(Locale.ROOT)
            : "";
    int duplicate = sequence(tokens, "on", "duplicate", "key", "update");
    if (duplicate >= 0) {
      List<String> tail = new ArrayList<>(tokens.subList(duplicate + 4, tokens.size()));
      tokens = new ArrayList<>(tokens.subList(0, duplicate));
      boolean noop =
          split(tail, ",").stream()
              .allMatch(
                  a ->
                      a.size() == 3 && a.get(1).equals("=") && a.get(0).equalsIgnoreCase(a.get(2)));
      if (noop) return render(functions(tokens)) + " ON CONFLICT DO NOTHING";
      String keys = table.matches("v2_award_record_00[0-3]") ? "order_id" : UPDATE_KEYS.get(table);
      if (keys == null)
        throw new IllegalArgumentException("Unsupported V2 PostgreSQL upsert: " + table);
      List<String> updates = new ArrayList<>();
      for (List<String> assignment : split(tail, ",")) {
        if (assignment.size() < 3 || !assignment.get(1).equals("="))
          throw new IllegalArgumentException("Unsupported upsert assignment");
        if (!updates.isEmpty()) updates.add(",");
        updates.add(assignment.getFirst());
        updates.add("=");
        List<String> rhs = functions(assignment.subList(2, assignment.size()));
        for (int i = 0; i < rhs.size(); i++) {
          String token = rhs.get(i);
          // 未限定的旧行列与 excluded 列在 PG DO UPDATE 中有歧义。
          if (identifier(token)
              && !keyword(token)
              && !(i + 1 < rhs.size() && rhs.get(i + 1).equals("("))
              && !(i > 0 && rhs.get(i - 1).equals("."))
              && !(i + 1 < rhs.size() && rhs.get(i + 1).equals(".")))
            updates.add(table + "." + token);
          else updates.add(token);
        }
      }
      return render(functions(tokens))
          + " ON CONFLICT ( "
          + keys.replace(",", " , ")
          + " ) DO UPDATE SET "
          + render(updates);
    }
    String translated = render(functions(tokens));
    if (ignore || IDEMPOTENT_INSERTS.contains(table)) translated += " ON CONFLICT DO NOTHING";
    return translated;
  }

  private static boolean keyword(String s) {
    return Set.of(
                "CASE",
                "WHEN",
                "THEN",
                "ELSE",
                "END",
                "NULL",
                "TRUE",
                "FALSE",
                "AND",
                "OR",
                "NOT",
                "IS",
                "AS",
                "date",
                "text",
                "INTERVAL",
                "AT",
                "TIME",
                "ZONE")
            .contains(s)
        || s.startsWith("excluded.");
  }

  private static List<String> functions(List<String> input) {
    List<String> out = new ArrayList<>();
    for (int i = 0; i < input.size(); i++) {
      String token = input.get(i);
      if (starts(input, i, "force", "index") || starts(input, i, "use", "index")) {
        if (i + 2 >= input.size() || !input.get(i + 2).equals("("))
          throw new IllegalArgumentException("Invalid index hint");
        i = close(input, i + 2);
        continue;
      }
      if (identifier(token) && i + 1 < input.size() && input.get(i + 1).equals("(")) {
        int end = close(input, i + 1);
        List<String> body = input.subList(i + 2, end);
        String name = token.toUpperCase(Locale.ROOT);
        List<List<String>> args = split(body, ",");
        switch (name) {
          case "UTC_TIMESTAMP" -> {
            if (!(body.isEmpty() || body.equals(List.of("6"))))
              throw new IllegalArgumentException("UTC precision");
            out.add("(statement_timestamp() AT TIME ZONE 'UTC')");
          }
          case "TIMESTAMPADD" -> {
            arity(args, 3);
            String unit = render(args.get(0)).toUpperCase(Locale.ROOT);
            if (!Set.of("SECOND", "MICROSECOND", "MINUTE", "HOUR", "DAY").contains(unit))
              throw new IllegalArgumentException("Unsupported interval " + unit);
            out.add(
                "("
                    + render(functions(args.get(2)))
                    + " + ("
                    + render(functions(args.get(1)))
                    + ") * INTERVAL '1 "
                    + unit.toLowerCase(Locale.ROOT)
                    + "')");
          }
          case "IF" -> {
            arity(args, 3);
            out.add("CASE");
            out.add("WHEN");
            out.addAll(functions(args.get(0)));
            out.add("THEN");
            out.addAll(functions(args.get(1)));
            out.add("ELSE");
            out.addAll(functions(args.get(2)));
            out.add("END");
          }
          case "VALUES" -> {
            // INSERT VALUES(...) 是构造器，只有单标识符形式才是 MySQL upsert 引用。
            if (body.size() == 1 && identifier(body.getFirst()))
              out.add("excluded." + body.getFirst());
            else {
              out.add(token);
              out.add("(");
              out.addAll(functions(body));
              out.add(")");
            }
          }
          case "DATE" -> {
            arity(args, 1);
            out.add("CAST ( " + render(functions(body)) + " AS date )");
          }
          case "CONCAT" -> {
            out.add("(");
            for (int arg = 0; arg < args.size(); arg++) {
              if (arg > 0) out.add("||");
              out.add("CAST ( " + render(functions(args.get(arg))) + " AS text )");
            }
            out.add(")");
          }
          case "CHAR" -> {
            arity(args, 1);
            out.add("CHR");
            out.add("(");
            out.addAll(functions(body));
            out.add(")");
          }
          case "CAST" -> {
            List<String> cast = new ArrayList<>(functions(body));
            int as = sequence(cast, "as");
            if (as >= 0 && as + 2 == cast.size() && cast.get(as + 1).equalsIgnoreCase("char")) {
              cast.set(as, "AS");
              cast.set(as + 1, "text");
            }
            out.add(token);
            out.add("(");
            out.addAll(cast);
            out.add(")");
          }
          case "JSON_UNQUOTE" -> {
            arity(args, 1);
            List<String> inner = args.getFirst();
            if (!starts(inner, 0, "json_extract", "(") || !inner.getLast().equals(")"))
              throw new IllegalArgumentException("Unsupported JSON_UNQUOTE");
            var jsonArgs = split(inner.subList(2, inner.size() - 1), ",");
            arity(jsonArgs, 2);
            String path = render(jsonArgs.get(1));
            if (!path.matches("'\\$\\.[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*'"))
              throw new IllegalArgumentException("Unsupported JSON path " + path);
            out.add("(");
            out.addAll(functions(jsonArgs.getFirst()));
            out.add("#>>");
            out.add("'{" + path.substring(3, path.length() - 1).replace('.', ',') + "}'");
            out.add(")");
          }
          case "JSON_EXTRACT" ->
              throw new IllegalArgumentException(
                  "JSON_EXTRACT requires supported JSON_UNQUOTE wrapper");
          default -> {
            out.add(token);
            out.add("(");
            out.addAll(functions(body));
            out.add(")");
          }
        }
        i = end;
      } else out.add(token);
    }
    return out;
  }

  private static void arity(List<List<String>> args, int expected) {
    if (args.size() != expected)
      throw new IllegalArgumentException("Unsupported SQL function arity");
  }

  private static boolean identifier(String s) {
    return s.matches("[A-Za-z_][A-Za-z_0-9]*");
  }

  private static String render(List<String> tokens) {
    return String.join(" ", tokens);
  }

  private static boolean starts(List<String> tokens, int pos, String... words) {
    if (pos + words.length > tokens.size()) return false;
    for (int n = 0; n < words.length; n++)
      if (!tokens.get(pos + n).equalsIgnoreCase(words[n])) return false;
    return true;
  }

  private static int sequence(List<String> tokens, String... words) {
    for (int i = 0; i < tokens.size(); i++) if (starts(tokens, i, words)) return i;
    return -1;
  }

  private static int close(List<String> tokens, int open) {
    int depth = 0;
    for (int i = open; i < tokens.size(); i++) {
      if (tokens.get(i).equals("(")) depth++;
      if (tokens.get(i).equals(")") && --depth == 0) return i;
    }
    throw new IllegalArgumentException("Unbalanced SQL parentheses");
  }

  private static List<List<String>> split(List<String> tokens, String separator) {
    List<List<String>> result = new ArrayList<>();
    int start = 0, depth = 0;
    for (int i = 0; i < tokens.size(); i++) {
      String t = tokens.get(i);
      if (t.equals("(")) depth++;
      else if (t.equals(")")) depth--;
      else if (depth == 0 && t.equals(separator)) {
        result.add(tokens.subList(start, i));
        start = i + 1;
      }
    }
    result.add(tokens.subList(start, tokens.size()));
    return result;
  }

  private static List<String> lex(String sql) {
    List<String> tokens = new ArrayList<>();
    for (int i = 0; i < sql.length(); ) {
      char c = sql.charAt(i);
      if (Character.isWhitespace(c)) {
        i++;
        continue;
      }
      int start = i++;
      if (c == '\'' || c == '"' || c == '`') {
        boolean ended = false;
        while (i < sql.length()) {
          if (sql.charAt(i++) == c) {
            if (i < sql.length() && sql.charAt(i) == c) {
              i++;
              continue;
            }
            ended = true;
            break;
          }
        }
        if (!ended) throw new IllegalArgumentException("Unterminated SQL quote");
        String quoted = sql.substring(start, i);
        tokens.add(
            c == '`'
                ? "\""
                    + quoted
                        .substring(1, quoted.length() - 1)
                        .replace("``", "`")
                        .replace("\"", "\"\"")
                    + "\""
                : quoted);
      } else if (c == '-' && i < sql.length() && sql.charAt(i) == '-') {
        while (i < sql.length() && sql.charAt(i) != '\n') i++;
        tokens.add(sql.substring(start, i) + "\n");
      } else if (c == '/' && i < sql.length() && sql.charAt(i) == '*') {
        int end = sql.indexOf("*/", i + 1);
        if (end < 0) throw new IllegalArgumentException("Unterminated SQL comment");
        i = end + 2;
        tokens.add(sql.substring(start, i));
      } else if (Character.isDigit(c)
          || (c == '.' && i < sql.length() && Character.isDigit(sql.charAt(i)))) {
        java.util.regex.Matcher number = NUMBER.matcher(sql);
        number.region(start, sql.length());
        if (!number.lookingAt()) throw new IllegalArgumentException("Invalid SQL number");
        i = number.end();
        tokens.add(number.group());
      } else if (Character.isLetter(c) || c == '_') {
        while (i < sql.length()
            && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) i++;
        tokens.add(sql.substring(start, i));
      } else {
        if (i < sql.length()
            && Set.of("<=", ">=", "<>", "!=", "||", "::", "->")
                .contains(sql.substring(start, i + 1))) i++;
        tokens.add(sql.substring(start, i));
      }
    }
    return tokens;
  }
}
