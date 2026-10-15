package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.infrastructure.admission.RedisRequestAdmission;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.config.Config;

public class RedisClusterAdmissionIT {
  @Test
  public void healthyClusterActuallyUsesDistributedBudgetRatherThanFallback() {
    var config = new Config();
    config
        .useClusterServers()
        .addNodeAddress(Objects.requireNonNull(System.getenv("V2_REDIS_CLUSTER_NODES")).split(","))
        .setRetryAttempts(0)
        .setTimeout(300);
    var redis = Redisson.create(config);
    try {
      var a = new RedisRequestAdmission(redis, 100, 100, 100, 1, 100, 100);
      var b = new RedisRequestAdmission(redis, 100, 100, 100, 1, 100, 100);
      String user = "cluster-" + UUID.randomUUID();
      var deadline = new Deadline(Instant.now().plusSeconds(10));
      // Healthy distributed capacity is 100; fallback would admit at most 1 per instance/window.
      int accepted = 0;
      for (int i = 0; i < 20; i++)
        try (var p = (i % 2 == 0 ? a : b).enter(user, deadline)) {
          accepted++;
        } catch (BudgetExceededException e) {
        }
      assertThat(accepted).isEqualTo(20);
    } finally {
      redis.shutdown();
    }
  }
}
