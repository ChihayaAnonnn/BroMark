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
      String token =
          V2IdentityV2Test.token(
              V2IdentityV2Test.KEY,
              "https://issuer.test",
              "bromark",
              "new-app-user",
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
      var registry =
          context.getBean(
              org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry.class);
      assertThat(registry.getListenerContainers()).allMatch(x -> !x.isRunning());
    }
  }
}
