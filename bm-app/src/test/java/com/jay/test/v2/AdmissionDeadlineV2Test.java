package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.infrastructure.admission.RedisRequestAdmission;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.config.Config;

public class AdmissionDeadlineV2Test {
  @Test
  public void slowRedisReleasesInFlightWithinDeadline() throws Exception {
    String host = System.getenv().getOrDefault("V2_REDIS_HOST", "host.docker.internal");
    int port = Integer.parseInt(System.getenv().getOrDefault("V2_REDIS_PORT", "16386"));
    var config = new Config();
    config
        .useSingleServer()
        .setAddress("redis://" + host + ":" + port)
        .setTimeout(5000)
        .setRetryAttempts(0);
    var redis = Redisson.create(config);
    try {
      var admission = new RedisRequestAdmission(redis, 100, 100, 1, 1, 100, 1);
      try (var p =
          admission.enter(
              "warm-" + UUID.randomUUID(), new Deadline(Instant.now().plusSeconds(5)))) {}
      try (var socket = new Socket(host, port)) {
        socket.setSoTimeout(1000);
        socket
            .getOutputStream()
            .write(
                "*4\r\n$6\r\nCLIENT\r\n$5\r\nPAUSE\r\n$4\r\n1000\r\n$3\r\nALL\r\n"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(socket.getInputStream().readNBytes(5)).isEqualTo("+OK\r\n".getBytes());
      }
      long start = System.nanoTime();
      assertThatThrownBy(() -> admission.enter("slow", new Deadline(Instant.now().plusMillis(100))))
          .isInstanceOf(BudgetExceededException.class);
      assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500));
      try (var p = admission.enter("after", new Deadline(Instant.now().plusSeconds(3)))) {
        assertThat(p).isNotNull();
      }
    } finally {
      redis.shutdown();
    }
  }
}
