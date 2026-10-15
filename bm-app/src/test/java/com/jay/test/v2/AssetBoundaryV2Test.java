package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class AssetBoundaryV2Test {
  @Test
  public void quotaIdentityCannotChangeGrantOrOriginalRefundTime() {
    AssetTestSupport.run(
        h -> {
          String u = "boundary-" + UUID.randomUUID();
          long a = 12;
          var q = new QuotaKernel(h.repo);
          var t = BusinessTime.at(Instant.parse("2026-10-15T15:59:59Z"));
          var id = new BusinessId("GRANT", "same", "a".repeat(64));
          h.tx(
              u,
              () -> {
                h.assets.lockAccounts(u, List.of(a));
                return q.grant(u, a, id, new QuotaDelta(10, 2, 5), t);
              });
          assertThatThrownBy(
                  () ->
                      h.tx(
                          u,
                          () -> {
                            h.assets.lockAccounts(u, List.of(a));
                            return q.grant(u, a, id, new QuotaDelta(11, 2, 5), t);
                          }))
              .isInstanceOf(AssetConflictException.class);
          var consume = new BusinessId("DRAW", "original", "b".repeat(64));
          h.tx(
              u,
              () -> {
                h.assets.lockAccounts(u, List.of(a));
                q.consume(u, a, consume, t).orElseThrow();
                q.refund(u, a, consume, t);
                return null;
              });
          assertThatThrownBy(
                  () ->
                      h.tx(
                          u,
                          () -> {
                            h.assets.lockAccounts(u, List.of(a));
                            return q.refund(
                                u, a, consume, BusinessTime.at(t.instant().plusSeconds(2)));
                          }))
              .isInstanceOf(AssetConflictException.class);
        });
  }

  @Test
  public void disabledAccountCannotCommitAndHasherIsUnambiguous() {
    AssetTestSupport.run(
        h -> {
          String u = "disabled-" + UUID.randomUUID();
          h.tx(
              u,
              () -> {
                h.j.update("update user_credit_account set status='DISABLED' where user_id=?", u);
                return null;
              });
          assertThatThrownBy(
                  () ->
                      h.tx(
                          u,
                          () ->
                              h.assets.credit(
                                  u,
                                  new BusinessId("X", "x", "a".repeat(64)),
                                  java.math.BigDecimal.ONE)))
              .isInstanceOf(AssetConflictException.class);
          var hash = new RequestHasher();
          assertThat(hash.hash("X", Map.of("a", "bc", "ab", "c")))
              .isNotEqualTo(hash.hash("X", Map.of("a", "b", "ab", "cc")));
        });
  }
}
