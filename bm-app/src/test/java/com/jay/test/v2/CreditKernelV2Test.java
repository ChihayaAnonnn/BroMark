package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.infrastructure.transaction.*;
import com.jay.test.v2.support.ConcurrentProbe;
import java.math.BigDecimal;
import java.util.*;
import org.junit.Test;

public class CreditKernelV2Test {
  @Test
  public void serializesConcurrentDebitsAndReplaysOriginalCredits() {
    AssetTestSupport.run(
        h -> {
          String u = "credit-" + UUID.randomUUID();
          h.tx(
              u,
              () ->
                  h.assets.credit(
                      u, new BusinessId("OPEN", "1", "a".repeat(64)), new BigDecimal("100.00")));
          var results =
              ConcurrentProbe.run(
                  2,
                  () ->
                      h.tx(
                          u,
                          () ->
                              h.assets.debit(
                                  u,
                                  new BusinessId(
                                      "DEBIT", UUID.randomUUID().toString(), "a".repeat(64)),
                                  new BigDecimal("80.00"))));
          assertThat(results.stream().filter(Optional::isPresent).count()).isEqualTo(1);
          assertThat(h.balance(u)).isEqualByComparingTo("20.00");
          var id = new BusinessId("TOPUP", "same", "b".repeat(64));
          var credits =
              ConcurrentProbe.run(
                  100, () -> h.tx(u, () -> h.assets.credit(u, id, new BigDecimal("3.00"))));
          assertThat(new HashSet<>(credits)).hasSize(1);
          assertThat(h.balance(u)).isEqualByComparingTo("23.00");
          assertThatThrownBy(
                  () ->
                      h.tx(
                          u,
                          () ->
                              h.assets.credit(
                                  u,
                                  new BusinessId("TOPUP", "same", "c".repeat(64)),
                                  new BigDecimal("3.00"))))
              .isInstanceOf(AssetConflictException.class);
          for (String v : List.of("1.001", "-1", "0", "10000000000000000.00"))
            assertThatThrownBy(
                    () ->
                        h.tx(
                            u,
                            () ->
                                h.assets.credit(
                                    u,
                                    new BusinessId("BAD", v, "a".repeat(64)),
                                    new BigDecimal(v))))
                .isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(() -> h.assets.credit(u, id, BigDecimal.ONE))
              .isInstanceOf(IllegalStateException.class);
        });
  }
}
