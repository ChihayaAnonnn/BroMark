package cn.bugstack.middleware.db.router.dynamic;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class PostgresSqlTest {
  private String sql(String s) {
    return PostgresSql.translate(s).replaceAll("\\s+", " ").trim();
  }

  @SuppressWarnings("unchecked")
  private java.util.Map<String, String> cache() throws Exception {
    var field = PostgresSql.class.getDeclaredField("CACHE");
    field.setAccessible(true);
    return (java.util.Map<String, String>) field.get(null);
  }

  @org.junit.jupiter.api.Order(1)
  @Test
  void repeatedOriginalSqlReusesTranslatedTemplate() {
    String original = "select UTC_TIMESTAMP(6), 'cache reuse'";
    assertThat(PostgresSql.translate(original)).isSameAs(PostgresSql.translate(original));
  }

  @Test
  void failedTranslationsAreNeverCached() throws Exception {
    String original = "select TIMESTAMPADD(WEEK,1,UTC_TIMESTAMP(6))";
    assertThatThrownBy(() -> PostgresSql.translate(original))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(cache()).doesNotContainKey(original);
  }

  @Test
  void concurrentCacheInsertionNeverExceedsBoundAndOverflowStillTranslates() throws Exception {
    java.util.stream.IntStream.range(0, 2048)
        .parallel()
        .forEach(
            n -> {
              assertThat(PostgresSql.translate("select UTC_TIMESTAMP(6), " + n))
                  .contains("statement_timestamp");
            });
    assertThat(cache()).hasSize(1024);
    assertThat(PostgresSql.translate("select UTC_TIMESTAMP(6), 9999999"))
        .contains("statement_timestamp", "9999999");
    assertThat(cache()).hasSize(1024);
  }

  @Test
  void preservesDecimalAndScientificNumericLiterals() {
    assertThat(sql("select 12.34,1.2e-3,-0.5")).contains("12.34", "1.2e-3", "0.5");
  }

  @Test
  void protectsLiteralsAndCommentsAndUsesStatementUtcClock() {
    String translated =
        sql(
            "select 'UTC_TIMESTAMP(6) ON DUPLICATE KEY UPDATE', UTC_TIMESTAMP(6) /*"
                + " TIMESTAMPADD(SECOND,1,x) */");
    assertThat(translated)
        .contains(
            "'UTC_TIMESTAMP(6) ON DUPLICATE KEY UPDATE'",
            "statement_timestamp",
            "'UTC'",
            "/* TIMESTAMPADD(SECOND,1,x) */");
    assertThat(translated).doesNotContain("transaction_timestamp");
  }

  @Test
  void translatesNestedLeaseArithmeticAndIfWithoutChangingParameterOrder() {
    String translated =
        sql(
            "update outbox_event set lease_until=TIMESTAMPADD(MICROSECOND,?,UTC_TIMESTAMP(6)) where"
                + " event_id=?");
    assertThat(translated).contains("INTERVAL '1 microsecond'", "statement_timestamp");
    assertThat(translated.chars().filter(c -> c == '?').count()).isEqualTo(2);
    assertThat(sql("select IF(? > 0, DATE(?), DATE(?))"))
        .contains("CASE WHEN", "THEN", "ELSE", "CAST", "AS date");
  }

  @Test
  void noOpUpsertAndInsertIgnoreUseDoNothingOnEveryUniqueConstraint() {
    assertThat(
            sql(
                "insert into provider_inbox(provider,event_id) values(?,?) on duplicate key update"
                    + " event_id=event_id"))
        .contains("ON CONFLICT DO NOTHING")
        .doesNotContain("DO UPDATE");
    assertThat(sql("insert ignore into consumer_inbox values(?,?,?,UTC_TIMESTAMP(6))"))
        .contains("ON CONFLICT DO NOTHING")
        .doesNotContain("ignore");
    assertThat(
            sql(
                "insert into recovery_guard select ?,?,coalesce(max(object_version),0) from"
                    + " recovery_case where aggregate_type=? on duplicate key update"
                    + " case_version=case_version"))
        .contains("ON CONFLICT DO NOTHING");
  }

  @Test
  void retryUpsertQualifiesOldValuesAndReferencesExcludedValues() {
    String translated =
        sql(
            "insert into retry_job(event_id,failure_count,next_attempt_at) values(?,?,?) ON"
                + " DUPLICATE KEY UPDATE"
                + " next_attempt_at=IF(VALUES(failure_count)>failure_count,VALUES(next_attempt_at),next_attempt_at),failure_count=GREATEST(failure_count,VALUES(failure_count))");
    assertThat(translated)
        .contains(
            "ON CONFLICT ( event_id ) DO UPDATE SET",
            "excluded.failure_count",
            "retry_job.failure_count",
            "excluded.next_attempt_at",
            "CASE WHEN");
  }

  @Test
  void removesMysqlIndexHintButKeepsSkipLocked() {
    assertThat(
            sql(
                "select event_id from outbox FORCE INDEX(idx_outbox_due) where state=? order by"
                    + " next_attempt_at,event_id limit ? for update skip locked"))
        .doesNotContain("FORCE", "idx_outbox_due")
        .contains("for update skip locked");
  }

  @Test
  void concatenationTypesUnknownStringParametersAndPropagatesNull() {
    assertThat(sql("select CONCAT(evidence,CHAR(10),?)"))
        .contains("CAST ( ? AS text )", "||")
        .doesNotContain("CONCAT");
  }

  @Test
  void preservesFullDateTextAndNewlineEvidence() {
    assertThat(sql("select cast(a.business_date as char),CONCAT(evidence,CHAR(10),?)"))
        .contains("AS text", "CHR ( 10 )");
  }

  @Test
  void convertsKnownJsonPathsAndRejectsUnsupportedPath() {
    assertThat(sql("select json_unquote(json_extract(o.snapshot,'$.finalAward.type'))"))
        .contains("o . snapshot", "#>>", "'{finalAward,type}'");
    assertThatThrownBy(() -> sql("select json_unquote(json_extract(snapshot,'$[*]'))"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                sql(
                    "insert into unknown_table(id) values(?) on duplicate key update"
                        + " id=values(id)"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sql("select TIMESTAMPADD(WEEK,1,UTC_TIMESTAMP(6))"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
