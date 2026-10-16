package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.Test;

public class MigrationRehearsalV2Test {
  @Test public void issuedLegacyMessageReplaysOriginalReceiptAndUnknownIdentityRequiresReview() {
    AssetTestSupport.run(h -> {
      String user = "cutover-" + UUID.randomUUID();
      var id = new BusinessId("LEGACY_AWARD", "award-17", "a".repeat(64));
      var original = h.tx(user, () -> h.assets.credit(user, id, new BigDecimal("12.00")));
      List<String> reviews = new ArrayList<>();
      var adapter = new LegacyIdentityAdapter((entry, legacy) ->
          entry.equals("award") && legacy.equals("17") ? Optional.of(id) : Optional.empty(),
          (entry, legacy) -> reviews.add(entry + ":" + legacy));
      var replay = h.tx(user, () -> h.assets.credit(user,
          adapter.resolve("award", "17").orElseThrow(), new BigDecimal("12.00")));
      assertThat(replay).isEqualTo(original);
      assertThat(h.balance(user)).isEqualByComparingTo("12.00");
      assertThat(adapter.resolve("signin", "unknown")).isEmpty();
      assertThat(reviews).containsExactly("signin:unknown");
      assertThatThrownBy(() -> new AssetWriteGate(true,"V2").check("legacy.dao"))
          .isInstanceOf(LegacyWriteBlockedException.class);
    });
  }
}
