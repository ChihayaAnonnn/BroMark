package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.Application;
import com.jay.domain.asset.service.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class AssetApplicationV2Test {
  @Test
  public void fullApplicationWiresAssetsAndAuthenticatedApis() throws Exception {
    AssetTestSupport.run(h -> {});
    var settings = new HashMap<String, Object>();
    settings.put("spring.profiles.active", "v2-it");
    settings.put("bm.v2.enabled", true);
    settings.put("bm.v2.asset-write-mode", "V2");
    for (String key :
        List.of("user-rps", "activity-rps", "max-in-flight", "query-rps", "query-max-in-flight"))
      settings.put("bm.v2." + key, 20);
    settings.put("bm.v2.local-fallback-rps", 1);
    settings.put("bm.v2.auth.issuer", "https://issuer.test");
    settings.put("bm.v2.auth.audience", "bromark");
    settings.put(
        "bm.v2.auth.public-key",
        Base64.getEncoder().encodeToString(V2IdentityV2Test.publicBytes()));
    settings.put("server.port", 0);
    try (var context =
        new org.springframework.boot.builder.SpringApplicationBuilder(Application.class)
            .profiles("v2-it")
            .run(
                settings.entrySet().stream()
                    .map(e -> "--" + e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new))) {
      assertThat(context.getBean(AssetWriteGate.class).legacyAllowed()).isFalse();
      assertThat(context.getBean(com.jay.domain.exchange.service.ExchangeService.class))
          .isNotNull();
      int port =
          ((org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext)
                  context)
              .getWebServer()
              .getPort();
      var client = java.net.http.HttpClient.newHttpClient();
      String url = "http://localhost:" + port + "/api/v2/rebates/sign-ins/2026-10-15";
      var unauthorized =
          client.send(
              java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertThat(unauthorized.statusCode()).isEqualTo(401);
      String subject = "full-app-" + UUID.randomUUID();
      String token =
          V2IdentityV2Test.token(
              V2IdentityV2Test.KEY,
              "https://issuer.test",
              "bromark",
              subject,
              Instant.now().plusSeconds(60),
              Instant.now().minusSeconds(60));
      var result =
          client.send(
              java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                  .header("Authorization", "Bearer " + token)
                  .GET()
                  .build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertThat(result.statusCode()).isEqualTo(404);

      var runner = context.getBean(com.jay.domain.asset.repository.UserTransactionRunner.class);
      var jdbc =
          new org.springframework.jdbc.core.JdbcTemplate(
              context.getBean(javax.sql.DataSource.class));
      var deadline = new com.jay.domain.asset.model.Deadline(Instant.now().plusSeconds(30));
      var snapshot =
          new com.jay.domain.asset.model.AdmissionSnapshot(
              12,
              "OPEN",
              "v1",
              Instant.now().minusSeconds(60),
              Instant.now().plusSeconds(3600),
              Instant.now(),
              deadline.expiresAt(),
              false,
              new java.math.BigDecimal("10.00"),
              new com.jay.domain.asset.model.QuotaDelta(5, 1, 2),
              List.of(
                  new com.jay.domain.asset.model.AdmissionSnapshot.Reward(
                      "credit", 0, new java.math.BigDecimal("2.00"), null),
                  new com.jay.domain.asset.model.AdmissionSnapshot.Reward(
                      "quota", 12, null, new com.jay.domain.asset.model.QuotaDelta(5, 1, 2))));
      runner.inControl(
          com.jay.domain.asset.model.WorkClass.RECOVERY,
          deadline,
          () -> {
            try {
              for (String file : List.of("001_id_block.sql", "002_admission_config.sql"))
                for (String statement :
                    java.nio.file.Files.readString(
                            java.nio.file.Path.of("../docs/dev-ops/mysql/migrations/" + file))
                        .split(";")) if (!statement.isBlank()) jdbc.execute(statement);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
            for (String[] key :
                List.of(
                    new String[] {"EXCHANGE", "123"},
                    new String[] {"SIGNIN", "daily"},
                    new String[] {"ACTIVITY", "12"}))
              jdbc.update(
                  "insert into v2_admission_config values(?,?,?) on duplicate key update"
                      + " snapshot=values(snapshot)",
                  key[0],
                  key[1],
                  com.jay.infrastructure.adapter.repository.v2.JsonStore.write(snapshot));
            return null;
          });
      var assets = context.getBean(AssetKernel.class);
      runner.inUser(
          subject,
          com.jay.domain.asset.model.WorkClass.TRADE,
          deadline,
          () -> {
            assets.lockAccounts(subject, List.of());
            return assets.credit(
                subject,
                new com.jay.domain.asset.model.BusinessId("OPEN", "fund", "a".repeat(64)),
                new java.math.BigDecimal("100.00"));
          });
      String exchangeBody =
          "{\"requestId\":\"full-exchange\",\"sku\":123,\"configVersion\":\"v1\"}";
      var exchangeRequest =
          java.net.http.HttpRequest.newBuilder(
                  java.net.URI.create("http://localhost:" + port + "/api/v2/credit/exchanges"))
              .header("Authorization", "Bearer " + token)
              .header("Content-Type", "application/json")
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString(exchangeBody))
              .build();
      var exchange =
          client.send(exchangeRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
      assertThat(exchange.statusCode()).isEqualTo(200);
      assertThat(exchange.body()).contains("DELIVERED");
      assertThat(
              client
                  .send(exchangeRequest, java.net.http.HttpResponse.BodyHandlers.ofString())
                  .body())
          .isEqualTo(exchange.body());
      String signBody =
          "{\"requestId\":\"full-sign\",\"targetDate\":\""
              + LocalDate.now(ZoneId.of("Asia/Shanghai"))
              + "\"}";
      var signRequest =
          java.net.http.HttpRequest.newBuilder(
                  java.net.URI.create("http://localhost:" + port + "/api/v2/rebates/sign-ins"))
              .header("Authorization", "Bearer " + token)
              .header("Content-Type", "application/json")
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString(signBody))
              .build();
      var sign = client.send(signRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
      assertThat(sign.statusCode()).isEqualTo(200);
      assertThat(sign.body()).contains("DELIVERED");
      assertThat(
              client.send(signRequest, java.net.http.HttpResponse.BodyHandlers.ofString()).body())
          .isEqualTo(sign.body());
      assertThat(
              runner.inUser(
                  subject,
                  com.jay.domain.asset.model.WorkClass.QUERY,
                  deadline,
                  () ->
                      jdbc.queryForObject(
                          "select balance from user_credit_account where user_id=?",
                          java.math.BigDecimal.class,
                          subject)))
          .isEqualByComparingTo("92.00");
      var registry =
          context.getBean(
              org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry.class);
      assertThat(registry.getListenerContainers()).allMatch(x -> !x.isRunning());
    }
  }
}
