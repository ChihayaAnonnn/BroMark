package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import org.junit.Test;

public class BatchIndexV2Test {
  @Test
  public void boundedQueriesUseTheirDedicatedIndexes() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                for (String table : new String[] {"outbox", "retry_job"})
                  for (String index :
                      new String[] {"pending_type", "pending", "expired", "aggregate_event"}) {
                    String where =
                        switch (index) {
                          case "pending_type" ->
                              "state='PENDING' and event_type='PrepareDraw' and"
                                  + " next_attempt_at<=UTC_TIMESTAMP(6)";
                          case "pending" -> "state='PENDING' and next_attempt_at<=UTC_TIMESTAMP(6)";
                          case "expired" -> "state='PROCESSING' and lease_until<=UTC_TIMESTAMP(6)";
                          default -> "aggregate_id='id' and event_type='PrepareDraw'";
                        };
                    assertThat(
                            h.j
                                .queryForList(
                                    "explain select event_id from "
                                        + table
                                        + " force index("
                                        + index
                                        + ") where "
                                        + where
                                        + " limit 32")
                                .getFirst()
                                .get("key"))
                        .isEqualTo(index);
                  }
                assertThat(
                        h.j
                            .queryForList(
                                "explain select event_id from retry_job force"
                                    + " index(published_recovery) where state='PUBLISHED' and"
                                    + " published_at<=UTC_TIMESTAMP(6) order by"
                                    + " published_at,event_id limit 64")
                            .getFirst()
                            .get("key"))
                    .isEqualTo("published_recovery");
                return null;
              });
        });
  }
}
