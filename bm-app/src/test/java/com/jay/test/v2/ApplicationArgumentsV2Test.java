package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.Test;

public class ApplicationArgumentsV2Test {
  @Test
  public void executableMainHonorsCutoverCommandLineOverrides() throws Exception {
    int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    var log = Files.createTempFile("bromark-main-arguments-", ".log");
    var p =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx256m",
                "-cp",
                System.getProperty("java.class.path"),
                "com.jay.Application",
                "--spring.profiles.active=v2-it",
                "--bm.v2.enabled=false",
                "--bm.v2.asset-write-mode=LEGACY",
                "--server.port=" + port)
            .redirectErrorStream(true)
            .redirectOutput(log.toFile());
    // This invalid default is deliberately corrected only by argv.
    p.environment()
        .put(
            "SPRING_APPLICATION_JSON",
            "{\"spring.profiles.active\":\"v2-it\",\"bm.v2.enabled\":false,\"bm.v2.asset-write-mode\":\"V2\"}");
    var process = p.start();
    try {
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(20))
          .ignoreExceptionsMatching(e -> e instanceof java.net.ConnectException)
          .untilAsserted(
              () -> {
                assertThat(process.isAlive()).withFailMessage(Files.readString(log)).isTrue();
                var conn =
                    (HttpURLConnection)
                        new URL("http://127.0.0.1:" + port + "/actuator/health").openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                try {
                  assertThat(conn.getResponseCode()).isEqualTo(200);
                } finally {
                  conn.disconnect();
                }
              });
    } finally {
      process.destroy();
      if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly();
    }
  }
}
