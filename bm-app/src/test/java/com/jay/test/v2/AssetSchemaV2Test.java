package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import javax.sql.DataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class AssetSchemaV2Test {
  @Test
  public void freshSchemaEnforcesAccountUniquenessAndNonnegativeBalance() throws Exception {
    String sql =
        Files.readString(Path.of("../docs/dev-ops/mysql/migrations/001_asset_additive.sql"));
    Java25RouterIntegrationTest.databaseContext()
        .run(
            c -> {
              JdbcTemplate j = new JdbcTemplate(c.getBean(DataSource.class));
              for (String statement : sql.replace("{{suffix}}", "000").split(";"))
                if (!statement.isBlank()) j.execute(statement);
              j.update("delete from user_credit_account where user_id='schema-probe'");
              j.update(
                  "insert into user_credit_account(user_id,balance,opening_balance,audit_source)"
                      + " values('schema-probe',100,100,'verified-fixture')");
              assertThatThrownBy(
                      () ->
                          j.update(
                              "insert into user_credit_account(user_id) values('schema-probe')"))
                  .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
              assertThatThrownBy(
                      () ->
                          j.update(
                              "update user_credit_account set balance=-1 where"
                                  + " user_id='schema-probe'"))
                  .isInstanceOf(org.springframework.dao.DataAccessException.class)
                  .hasMessageContaining("3819");
              assertThat(
                      j.queryForObject(
                          "select opening_balance from user_credit_account where"
                              + " user_id='schema-probe'",
                          java.math.BigDecimal.class))
                  .isEqualByComparingTo("100.00");
            });
  }
}
