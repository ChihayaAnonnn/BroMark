package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.gateway.payment.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class PaymentIntegrationV2Test {
  @Test
  public void verifiedHttpNotificationTraversesRealMqAndReturnsOriginalCreditReceipt()
      throws Exception {
    AssetTestSupport.run(h -> new PaymentTestSupport(h));
    var s = new HashMap<String, Object>();
    s.put("spring.profiles.active", "test,v2-it");
    s.put("bm.v2.enabled", true);
    s.put("bm.v2.asset-write-mode", "V2");
    s.put("bm.v2.messaging.enabled", true);
    s.put("bm.v2.payment.enabled", true);
    s.put("V2_PAYMENT_ACCOUNT", "merchant-it");
    s.put("V2_PAYMENT_SIGNING_SECRET", "isolated-signing-key-at-least-32-bytes");
    s.put("bm.v2.messaging.prefix", "bromark.it.payment." + UUID.randomUUID());
    s.put("bm.v2.auth.issuer", "https://issuer.test");
    s.put("bm.v2.auth.audience", "bromark");
    s.put(
        "bm.v2.auth.public-key",
        Base64.getEncoder().encodeToString(V2IdentityV2Test.publicBytes()));
    for (String k :
        List.of("user-rps", "activity-rps", "max-in-flight", "query-rps", "query-max-in-flight"))
      s.put("bm.v2." + k, 100);
    s.put("bm.v2.local-fallback-rps", 1);
    try (var ctx =
        new org.springframework.boot.builder.SpringApplicationBuilder(com.jay.Application.class)
            .run(
                s.entrySet().stream()
                    .map(e -> "--" + e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new))) {
      var mvc =
          org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
                  (org.springframework.web.context.WebApplicationContext) ctx)
              .apply(
                  org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                      .springSecurity())
              .build();
      String u = "http-payment-" + UUID.randomUUID();
      String token =
          V2IdentityV2Test.token(
              V2IdentityV2Test.KEY,
              "https://issuer.test",
              "bromark",
              u,
              Instant.now().plusSeconds(300),
              Instant.now().minusSeconds(1));
      mvc.perform(
              post("/api/v2/payment/orders")
                  .contentType("application/json")
                  .content("{\"requestId\":\"r\",\"productId\":\"sku\",\"provider\":\"sandbox\"}"))
          .andExpect(status().isUnauthorized());
      var result =
          mvc.perform(
                  post("/api/v2/payment/orders")
                      .header("Authorization", "Bearer " + token)
                      .contentType("application/json")
                      .content(
                          "{\"requestId\":\"r\",\"productId\":\"sku\",\"provider\":\"sandbox\"}"))
              .andExpect(status().isAccepted())
              .andReturn()
              .getResponse()
              .getContentAsString();
      String id =
          new com.fasterxml.jackson.databind.ObjectMapper()
              .readTree(result)
              .get("orderId")
              .asText();
      var provider = ctx.getBean(SandboxPaymentProvider.class);
      var event =
          new VerifiedPaymentEvent(
              "sandbox",
              "merchant-it",
              "test",
              "http-paid-" + id,
              "http-tx-" + id,
              id,
              "PAID",
              new java.math.BigDecimal("10.00"),
              "CNY",
              u,
              null);
      provider.record(event);
      byte[] raw = PaymentFactV2Test.JsonBytes.event(event);
      mvc.perform(
              post("/api/v2/payment/notifications/sandbox")
                  .content(raw)
                  .header("x-sandbox-signature", provider.sign(raw).get("x-sandbox-signature")))
          .andExpect(status().isOk())
          .andExpect(content().string("SUCCESS"));
      var service = ctx.getBean(PaymentService.class);
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(30))
          .untilAsserted(() -> assertThat(service.get(u, id).state()).isEqualTo("DELIVERED"));
      assertThat(service.get(u, id).receipt().delta()).isEqualByComparingTo("100.00");
      mvc.perform(get("/api/v2/payment/orders/" + id).header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.state").value("DELIVERED"));
      String other =
          V2IdentityV2Test.token(
              V2IdentityV2Test.KEY,
              "https://issuer.test",
              "bromark",
              "other-user",
              Instant.now().plusSeconds(300),
              Instant.now().minusSeconds(1));
      mvc.perform(get("/api/v2/payment/orders/" + id).header("Authorization", "Bearer " + other))
          .andExpect(status().isForbidden());
      mvc.perform(
              post("/api/v2/payment/notifications/sandbox")
                  .content(raw)
                  .header("x-sandbox-signature", provider.sign(raw).get("x-sandbox-signature")))
          .andExpect(status().isOk());
      assertThat(ctx.getBean(PaymentReconciliationService.class).checkOrder(id)).isFalse();
      var tx = ctx.getBean(UserTransactionRunner.class);
      var j =
          new org.springframework.jdbc.core.JdbcTemplate(ctx.getBean(javax.sql.DataSource.class));
      var stages =
          tx.inControl(
              WorkClass.QUERY,
              new Deadline(Instant.now().plusSeconds(5)),
              () ->
                  j.queryForList(
                      "select stage,recorded_at from payment_stage where order_id=? order by"
                          + " recorded_at",
                      id));
      assertThat(stages).hasSize(3);
      System.out.println("PHASE5_STAGES " + id + " " + stages);
    }
  }
}
