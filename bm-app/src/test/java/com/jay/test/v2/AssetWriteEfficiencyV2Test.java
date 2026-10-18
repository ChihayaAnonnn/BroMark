package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import java.lang.reflect.*;
import java.sql.Connection;
import java.util.*;
import javax.sql.DataSource;
import org.junit.Test;
import org.springframework.jdbc.datasource.*;

public class AssetWriteEfficiencyV2Test {
  @Test
  public void existingAccountsLockWithoutInitializationWritesAndKeepQuotaAtomic() {
    AssetTestSupport.run(
        original -> {
          var sql = new ArrayList<String>();
          DataSource source =
              new DelegatingDataSource(original.j.getDataSource()) {
                @Override
                public Connection getConnection() throws java.sql.SQLException {
                  Connection connection = super.getConnection();
                  return (Connection)
                      Proxy.newProxyInstance(
                          Connection.class.getClassLoader(),
                          new Class<?>[] {Connection.class},
                          (proxy, method, args) -> {
                            if (method.getName().equals("prepareStatement"))
                              sql.add(((String) args[0]).toLowerCase());
                            try {
                              return method.invoke(connection, args);
                            } catch (InvocationTargetException failure) {
                              throw failure.getCause();
                            }
                          });
                }
              };
          var h = new AssetTestSupport(source, new DataSourceTransactionManager(source));
          var quota = new QuotaKernel(h.repo);
          String user = "write-efficiency-" + UUID.randomUUID();
          long activity = 910017;
          h.tx(
              user,
              () -> {
                var time = h.assets.lockAccounts(user, List.of(activity));
                return quota.grant(
                    user,
                    activity,
                    new BusinessId("OPEN", "1", "a".repeat(64)),
                    new QuotaDelta(10, 10, 10),
                    time);
              });
          sql.clear();
          h.tx(
              user,
              () -> {
                var time = h.assets.lockAccounts(user, List.of(activity));
                assertThat(
                        quota.consume(
                            user, activity, new BusinessId("DRAW", "1", "b".repeat(64)), time))
                    .isPresent();
                return null;
              });
          assertThat(
                  sql.stream()
                      .filter(
                          s ->
                              s.startsWith("insert into user_credit_account")
                                  || s.startsWith("insert into raffle_activity_account(")))
              .isEmpty();
          assertThat(sql.stream().filter(s -> s.startsWith("insert into user_quota_ledger")))
              .hasSize(1);
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from "
                                + com.jay.infrastructure.adapter.repository.v2.MySqlAssetRepository
                                    .table("user_quota_ledger")
                                + " where user_id=?",
                            Integer.class,
                            user))
                    .isEqualTo(6);
                return null;
              });
          assertThatThrownBy(
                  () ->
                      h.tx(
                          user,
                          () -> {
                            var time = h.assets.lockAccounts(user, List.of(activity));
                            quota.consume(
                                user,
                                activity,
                                new BusinessId("DRAW", "rollback", "c".repeat(64)),
                                time);
                            throw new IllegalStateException("rollback");
                          }))
              .isInstanceOf(IllegalStateException.class);
          h.tx(
              user,
              () -> {
                var time = h.assets.lockAccounts(user, List.of(activity));
                var state = h.repo.quotaState(user, activity, time);
                String ledger =
                    com.jay.infrastructure.adapter.repository.v2.MySqlAssetRepository.table(
                        "user_quota_ledger");
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from " + ledger + " where user_id=?",
                            Integer.class,
                            user))
                    .isEqualTo(6);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from "
                                + ledger
                                + " where user_id=? and biz_id='rollback'",
                            Integer.class,
                            user))
                    .isZero();
                assertThat(state.total()).isEqualTo(9);
                assertThat(state.dayRemaining()).isEqualTo(9);
                assertThat(state.monthRemaining()).isEqualTo(9);
                return null;
              });
        });
  }
}
