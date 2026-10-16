package com.jay.test.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

final class RelayOptimizationTestSupport {
  static Deadline deadline() {
    return new Deadline(Instant.now().plusSeconds(10));
  }

  static RelayPolicy policy() {
    return new com.jay.config.V2RelayProperties().policy();
  }

  static void schema(AssetTestSupport h) throws Exception {
    ConsumerInboxV2Test.schema(h);
    for (var slot : com.jay.trigger.job.v2.OutboxRelayJob.SOURCES)
      h.runner.inSlot(
          slot,
          WorkClass.RECOVERY,
          deadline(),
          () -> {
            try {
              for (String file : List.of("007_recovery_dispatch.sql", "007_outbox_indexes.sql"))
                for (String sql :
                    Files.readString(Path.of("../docs/dev-ops/mysql/migrations/" + file))
                        .split(";")) {
                  if (sql.isBlank()) continue;
                  var matcher =
                      java.util.regex.Pattern.compile(
                              "CREATE INDEX (\\w+) ON (\\w+)\\(([^)]+)\\)",
                              java.util.regex.Pattern.CASE_INSENSITIVE)
                          .matcher(sql.trim());
                  if (matcher.matches()
                      && h.j.queryForObject(
                              "select count(*) from information_schema.statistics where"
                                  + " table_schema=DATABASE() and table_name=? and index_name=?",
                              Integer.class,
                              matcher.group(2),
                              matcher.group(1))
                          > 0) continue;
                  h.j.execute(sql);
                }
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
            return null;
          });
  }

  static EventEnvelope event(String id, String type) {
    return new EventEnvelope(
        id,
        type,
        "aggregate-" + id,
        new BusinessId("EX", id, "a".repeat(64)),
        "u",
        RouteSlot.control(),
        "a".repeat(64),
        1,
        1,
        "{}");
  }
}
