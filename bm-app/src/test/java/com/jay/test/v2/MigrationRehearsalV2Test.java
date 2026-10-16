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
}
