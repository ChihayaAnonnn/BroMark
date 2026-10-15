package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.trigger.http.v2.*;
import com.jay.trigger.security.CurrentUser;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class AssetApiV2Test {
  @Test
  public void endpointsUseVerifiedSubjectAndFenceAllLegacyConsumers() {
    AssetTestSupport.run(
        h -> {
          var f = new BusinessTestSupport(h);
          var gate = new AssetWriteGate(true, "V2");
          RequestAdmission admission =
              new RequestAdmission() {
                public Permit enter(String u, Deadline d) {
                  return () -> {};
                }

                public Permit query(String u, Deadline d) {
                  return () -> {};
                }

                public void checkActivity(String key, Deadline d) {}
              };
          var controller =
              new CreditExchangeController(new CurrentUser(), f.exchange, admission, gate);
          var mvc =
              org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                      controller,
                      new SignInController(new CurrentUser(), f.signin, admission, gate))
                  .setControllerAdvice(new V2ExceptionHandler())
                  .build();
          var jwt =
              org.springframework.security.oauth2.jwt.Jwt.withTokenValue("unit")
                  .header("alg", "RS256")
                  .subject("api-" + UUID.randomUUID())
                  .build();
          org.springframework.security.core.context.SecurityContextHolder.getContext()
              .setAuthentication(
                  new org.springframework.security.oauth2.server.resource.authentication
                      .JwtAuthenticationToken(jwt, java.util.List.of()));
          try {
            var result =
                mvc.perform(
                        post("/api/v2/credit/exchanges")
                            .contentType("application/json")
                            .content(
                                "{\"requestId\":\"r1\",\"sku\":123,\"configVersion\":\"v1\",\"userId\":\"forged\"}"))
                    .andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(
                    mvc.perform(
                            post("/api/v2/credit/exchanges")
                                .contentType("application/json")
                                .content(
                                    "{\"requestId\":\"r1\",\"sku\":123,\"configVersion\":\"v1\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .isEqualTo(result.getResponse().getContentAsString());
            String body = result.getResponse().getContentAsString();
            assertThat(body).contains("REJECTED");
            var order =
                new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(body)
                    .get("orderId")
                    .asText();
            var other =
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("other")
                    .header("alg", "RS256")
                    .subject("other-user")
                    .build();
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(
                    new org.springframework.security.oauth2.server.resource.authentication
                        .JwtAuthenticationToken(other, java.util.List.of()));
            assertThat(
                    mvc.perform(get("/api/v2/credit/exchanges/" + order))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(403);
            for (Object legacy :
                List.of(
                    new com.jay.trigger.listener.RebateMessageCustomer(),
                    new com.jay.trigger.listener.CreditAdjustSuccessCustomer(),
                    new com.jay.trigger.listener.SendAwardCustomer())) {
              org.springframework.test.util.ReflectionTestUtils.setField(
                  legacy, "assetWriteGate", gate);
              assertThatThrownBy(
                      () -> {
                        try {
                          legacy
                              .getClass()
                              .getMethod("listener", String.class)
                              .invoke(legacy, "{}");
                        } catch (java.lang.reflect.InvocationTargetException e) {
                          throw (RuntimeException) e.getCause();
                        }
                      })
                  .isInstanceOf(com.jay.domain.asset.model.LegacyWriteBlockedException.class);
            }
          } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
          }
        });
  }

  @Test
  public void entryRejectionsCreateNoReceiptAndFinallyReleasesCapacity() {
    AssetTestSupport.run(
        h -> {
          var f = new BusinessTestSupport(h);
          var closed = new java.util.concurrent.atomic.AtomicInteger();
          var reject = new java.util.concurrent.atomic.AtomicBoolean(true);
          RequestAdmission limiter =
              new RequestAdmission() {
                public Permit enter(String u, Deadline d) {
                  if (reject.get()) throw new BudgetExceededException("limited");
                  return closed::incrementAndGet;
                }

                public Permit query(String u, Deadline d) {
                  return () -> {};
                }

                public void checkActivity(String key, Deadline d) {}
              };
          var mvc =
              org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                      new CreditExchangeController(
                          new CurrentUser(), f.exchange, limiter, new AssetWriteGate(true, "V2")))
                  .setControllerAdvice(new V2ExceptionHandler())
                  .build();
          String user = "api-limit-" + UUID.randomUUID();
          var jwt =
              org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test")
                  .header("alg", "RS256")
                  .subject(user)
                  .build();
          org.springframework.security.core.context.SecurityContextHolder.getContext()
              .setAuthentication(
                  new org.springframework.security.oauth2.server.resource.authentication
                      .JwtAuthenticationToken(jwt, List.of()));
          try {
            String body = "{\"requestId\":\"limited\",\"sku\":123,\"configVersion\":\"v1\"}";
            assertThat(
                    mvc.perform(
                            post("/api/v2/credit/exchanges")
                                .contentType("application/json")
                                .content(body))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(429);
            assertThat(
                    h.tx(
                        user,
                        () ->
                            h.j.queryForObject(
                                "select count(*) from "
                                    + com.jay.infrastructure.adapter.repository.v2
                                        .MySqlAssetRepository.table("user_request_receipt")
                                    + " where user_id=?",
                                Integer.class,
                                user)))
                .isZero();
            reject.set(false);
            f.configAvailable = false;
            assertThat(
                    mvc.perform(
                            post("/api/v2/credit/exchanges")
                                .contentType("application/json")
                                .content(body))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(503);
            assertThat(closed.get()).isEqualTo(1);
          } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
          }
        });
  }
}
