package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.infrastructure.adapter.repository.v2.MySqlAssetRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.Test;

public class MigrationRehearsalV2Test {
  @Test
  public void issuedLegacyMessageReplaysImportedOriginalReceiptAndUnknownIdentityRequiresReview() {
    AssetTestSupport.run(
        h -> {
          String user = "cutover-" + UUID.randomUUID();
          var id = new BusinessId("LEGACY_AWARD", "award-17", "a".repeat(64));
          // 导入已发生的资产事实；不能先调用新内核发一次奖励来替代真实存量导入。
          var original = new AssetReceipt(id, new BigDecimal("12.00"), new BigDecimal("12.00"));
          String entry = "award:" + user;
          h.tx(
              user,
              () -> {
                h.j.update(
                    "update user_credit_account set"
                        + " balance=12.00,opening_balance=0.00,audit_source='legacy audit' where"
                        + " user_id=?",
                    user);
                h.j.update(
                    "insert into "
                        + MySqlAssetRepository.table("user_credit_ledger")
                        + "(user_id,biz_type,biz_id,request_hash,delta,balance_after,receipt)"
                        + " values(?,?,?,?,12.00,12.00,?)",
                    user,
                    id.type(),
                    id.id(),
                    id.hash(),
                    ReceiptJson.write(original));
                h.j.update(
                    "insert into cutover_identity values(?,?,?,?,?,?)",
                    entry,
                    "17",
                    id.type(),
                    id.id(),
                    id.hash(),
                    "original issued award");
                return null;
              });
          List<String> reviews = new ArrayList<>();
          var adapter =
              new LegacyIdentityAdapter(
                  (e, legacy) ->
                      h.tx(
                          user,
                          () ->
                              h
                                  .j
                                  .query(
                                      "select biz_type,biz_id,request_hash from cutover_identity"
                                          + " where entry=? and legacy_id=?",
                                      (row, n) ->
                                          new BusinessId(
                                              row.getString(1), row.getString(2), row.getString(3)),
                                      e,
                                      legacy)
                                  .stream()
                                  .findFirst()),
                  (e, legacy) -> reviews.add(e + ":" + legacy));
          var replay =
              h.tx(
                  user,
                  () ->
                      h.assets.credit(
                          user,
                          adapter.resolve(entry, "17").orElseThrow(),
                          new BigDecimal("12.00")));
          assertThat(replay).isEqualTo(original);
          assertThat(h.balance(user)).isEqualByComparingTo("12.00");
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from "
                                + MySqlAssetRepository.table("user_credit_ledger")
                                + " where user_id=?",
                            Long.class,
                            user))
                    .isEqualTo(1);
                return null;
              });
          assertThat(adapter.resolve("signin", "unknown")).isEmpty();
          assertThat(reviews).containsExactly("signin:unknown");
          assertThatThrownBy(() -> new AssetWriteGate(true, "V2").check("legacy.dao"))
              .isInstanceOf(LegacyWriteBlockedException.class);
        });
  }

  @Test
  public void importedSigninDatePreventsNewKeyFromIssuingAnotherReward() {
    AssetTestSupport.run(
        h -> {
          var business = new BusinessTestSupport(h);
          h.overrideTime = BusinessTime.at(java.time.Instant.parse("2026-10-15T15:59:59Z"));
          String user = "legacy-sign-" + UUID.randomUUID();
          String order = "legacy-sign-order-" + UUID.randomUUID();
          var biz = new BusinessId("SIGN_REWARD", order + ":credit", "a".repeat(64));
          var credit = new AssetReceipt(biz, new BigDecimal("12.00"), new BigDecimal("12.00"));
          var original =
              new com.jay.domain.rebate.model.SignInResult(
                  order,
                  h.overrideTime.businessDate(),
                  "DELIVERED",
                  List.of(
                      new com.jay.domain.rebate.model.SignInResult.RewardReceipt(
                          "credit", credit, null)),
                  null);
          var deadline = new Deadline(java.time.Instant.now().plusSeconds(30));
          h.tx(
              user,
              () -> {
                h.j.update(
                    "update user_credit_account set"
                        + " balance=12.00,opening_balance=0.00,audit_source='legacy signin' where"
                        + " user_id=?",
                    user);
                h.j.update(
                    "insert into "
                        + MySqlAssetRepository.table("user_credit_ledger")
                        + "(user_id,biz_type,biz_id,request_hash,delta,balance_after,receipt)"
                        + " values(?,?,?,?,12.00,12.00,?)",
                    user,
                    biz.type(),
                    biz.id(),
                    biz.hash(),
                    ReceiptJson.write(credit));
                var configured = business.snapshot(deadline);
                var frozen =
                    new AdmissionSnapshot(
                        configured.activityId(),
                        configured.activityState(),
                        configured.version(),
                        configured.beginsAt(),
                        configured.endsAt(),
                        configured.readAt(),
                        configured.expiresAt(),
                        false,
                        null,
                        null,
                        List.of(
                            new AdmissionSnapshot.Reward(
                                "credit", 0, new BigDecimal("12.00"), null)));
                new com.jay.infrastructure.adapter.repository.v2.MySqlSignInRepository(
                        h.j.getDataSource())
                    .save(user, frozen, h.overrideTime, original);
                return null;
              });
          business.configAvailable = false;
          h.overrideTime = BusinessTime.at(java.time.Instant.parse("2026-10-16T01:00:00Z"));
          var replay =
              business.signin.signIn(
                  new com.jay.domain.rebate.model.SignInCommand(
                      user, "new-request", original.businessDate(), deadline));
          assertThat(replay).isEqualTo(original);
          assertThat(h.balance(user)).isEqualByComparingTo("12.00");
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from "
                                + MySqlAssetRepository.table("user_credit_ledger")
                                + " where user_id=?",
                            Long.class,
                            user))
                    .isEqualTo(1);
                return null;
              });
        });
  }

  @Test
  public void migratedAlreadyDebitedExchangeUsesOriginalQuotaGrantAndNeverDebitsAgain() {
    AssetTestSupport.run(
        h -> {
          var business = new BusinessTestSupport(h);
          String user = "legacy-ex-" + UUID.randomUUID();
          String order = "legacy-ex-order-" + UUID.randomUUID();
          String hash = new RequestHasher().hash("EXCHANGE", Map.of("sku", "12", "version", "v1"));
          var id = new BusinessId("EXCHANGE", order, hash);
          var debit = new AssetReceipt(id, new BigDecimal("-10.00"), new BigDecimal("90.00"));
          var time = BusinessTime.at(java.time.Instant.parse("2026-10-15T15:59:59Z"));
          var deadline = new Deadline(java.time.Instant.now().plusSeconds(30));
          h.tx(
              user,
              () -> {
                h.j.update(
                    "update user_credit_account set"
                        + " balance=90.00,opening_balance=100.00,audit_source='legacy debited"
                        + " order' where user_id=?",
                    user);
                h.j.update(
                    "insert into "
                        + MySqlAssetRepository.table("user_credit_ledger")
                        + "(user_id,biz_type,biz_id,request_hash,delta,balance_after,receipt)"
                        + " values(?,?,?,?,-10.00,90.00,?)",
                    user,
                    id.type(),
                    id.id(),
                    id.hash(),
                    ReceiptJson.write(debit));
                return null;
              });
          var original =
              h.runner.inUser(
                  user,
                  WorkClass.RECOVERY,
                  deadline,
                  () -> {
                    h.assets.lockAccounts(user, List.of(12L));
                    var quota = new QuotaKernel(h.repo);
                    var grant = quota.grant(user, 12, id, new QuotaDelta(5, 1, 2), time);
                    assertThat(quota.grant(user, 12, id, new QuotaDelta(5, 1, 2), time))
                        .isEqualTo(grant);
                    var result =
                        new com.jay.domain.exchange.model.ExchangeResult(
                            order, "DELIVERED", debit, grant, null);
                    var repo =
                        new com.jay.infrastructure.adapter.repository.v2.MySqlExchangeRepository(
                            h.j.getDataSource());
                    repo.save(
                        new RequestIdentity(user, "EXCHANGE", "original-request", hash),
                        business.snapshot(deadline),
                        time,
                        result);
                    return result;
                  });
          business.configAvailable = false;
          var replay =
              business.exchange.exchange(
                  new com.jay.domain.exchange.model.ExchangeCommand(
                      user, "original-request", 12, "v1", deadline));
          assertThat(replay).isEqualTo(original);
          assertThat(replay.quotaReceipt().businessTime()).isEqualTo(time);
          assertThat(replay.quotaReceipt().totalRemaining()).isEqualTo(5);
          assertThat(h.balance(user)).isEqualByComparingTo("90.00");
          h.tx(
              user,
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from "
                                + MySqlAssetRepository.table("user_credit_ledger")
                                + " where user_id=?",
                            Long.class,
                            user))
                    .isEqualTo(1);
                return null;
              });
        });
  }
}
