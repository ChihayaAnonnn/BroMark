package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.infrastructure.admission.RedisRequestAdmission;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.config.Config;

public class RedisAdmissionIntegrationV2Test {
  @Test
  public void distributedLuaEnforcesSharedLimitsAndShutdownFallsBackConservatively() {
    var config = new Config();
    config
        .useSingleServer()
        .setAddress(
            "redis://"
                + System.getenv().getOrDefault("V2_REDIS_HOST", "host.docker.internal")
                + ":"
                + System.getenv().getOrDefault("V2_REDIS_PORT", "16386"))
        .setTimeout(200)
        .setRetryAttempts(0);
    var redis = Redisson.create(config);
    var deadline = new Deadline(Instant.now().plusSeconds(10));
    try {
      var a = new RedisRequestAdmission(redis, 1, 1, 2, 1, 1, 1);
      var b = new RedisRequestAdmission(redis, 1, 1, 2, 1, 1, 1);
      String user = "rate-" + UUID.randomUUID();
      // Redis TIME may cross a second; use a burst, assert at most two windows accepted.
      var results =
          com.jay.test.v2.support.ConcurrentProbe.run(
              100,
              () -> {
                try (var p =
                    (Thread.currentThread().threadId() % 2 == 0 ? a : b).enter(user, deadline)) {
                  return true;
                } catch (BudgetExceededException e) {
                  return false;
                }
              });
      assertThat(results.stream().filter(Boolean::booleanValue).count()).isBetween(1L, 2L);
      redis.shutdown();
      try (var p = a.enter("fallback", deadline)) {
        assertThat(p).isNotNull();
      }
      assertThatThrownBy(() -> a.enter("fallback-again", deadline))
          .isInstanceOf(BudgetExceededException.class);
      try (var p = a.query("fallback", deadline)) {
        assertThat(p).isNotNull();
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      if (!redis.isShutdown()) redis.shutdown();
    }
  }
}
