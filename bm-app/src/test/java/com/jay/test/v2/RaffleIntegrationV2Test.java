package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class RaffleIntegrationV2Test {
  @Test
  public void fullApplicationTransportsSevenTransactionsAndProtectsHttpIdentity() throws Exception {
    AssetTestSupport.run(h -> DrawAcceptanceV2Test.schema(h));
    var settings = new HashMap<String, Object>();
    settings.put("spring.profiles.active", "v2-it");
    settings.put("bm.v2.enabled", true);
    settings.put("bm.v2.asset-write-mode", "V2");
    settings.put("bm.v2.messaging.enabled", true);
    settings.put("bm.v2.raffle.enabled", true);
    settings.put("bm.v2.cancel-rps", 100);
    settings.put("bm.v2.cancel-max-in-flight", 4);
    settings.put("bm.v2.messaging.prefix", "bromark.it.raffle." + UUID.randomUUID());
    settings.put("bm.v2.auth.issuer", "https://issuer.test");
    settings.put("bm.v2.auth.audience", "bromark");
    settings.put(
        "bm.v2.auth.public-key",
        Base64.getEncoder().encodeToString(V2IdentityV2Test.publicBytes()));
    for (String k :
        List.of("user-rps", "activity-rps", "max-in-flight", "query-rps", "query-max-in-flight"))
      settings.put("bm.v2." + k, 100);
    settings.put("bm.v2.local-fallback-rps", 1);
    try (var ctx =
        new org.springframework.boot.builder.SpringApplicationBuilder(com.jay.Application.class)
            .profiles("v2-it")
            .run(
                settings.entrySet().stream()
                    .map(e -> "--" + e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new))) {
      var tx = ctx.getBean(com.jay.domain.asset.repository.UserTransactionRunner.class);
      var ds = ctx.getBean(javax.sql.DataSource.class);
      var j = new org.springframework.jdbc.core.JdbcTemplate(ds);
      var pub = ctx.getBean(com.jay.domain.strategy.service.StrategyPublicationService.class);
      long a = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
      String pool = "http-pool-" + UUID.randomUUID();
      tx.inControl(
          WorkClass.TRADE,
          new Deadline(Instant.now().plusSeconds(10)),
          () -> {
            ctx.getBean(com.jay.domain.stock.service.StockReservationService.class)
                .createPool(pool, "initial", 1, "isolated budget");
            return null;
          });
      pub.publish(FrozenStrategyV2Test.command(a, pool));
      String user = "http-raffle-" + UUID.randomUUID();
      var assets = ctx.getBean(AssetKernel.class);
      var quota = ctx.getBean(QuotaKernel.class);
      tx.inUser(
          user,
          WorkClass.TRADE,
          new Deadline(Instant.now().plusSeconds(10)),
          () -> {
            var t = assets.lockAccounts(user, List.of(a));
            quota.grant(
                user, a, new BusinessId("GRANT", user, "a".repeat(64)), new QuotaDelta(2, 2, 2), t);
            return null;
          });
      var mvc =
          org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
                  (org.springframework.web.context.WebApplicationContext) ctx)
              .apply(
                  org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                      .springSecurity())
              .build();
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                  "/api/v2/activities/" + a))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                      "$.version")
                  .exists())
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.tables")
                  .doesNotExist());
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                      "/api/v2/draws")
                  .contentType("application/json")
                  .content("{\"requestId\":\"r1\",\"activityId\":" + a + "}"))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                  .isUnauthorized());
      String token =
          V2IdentityV2Test.token(
              V2IdentityV2Test.KEY,
              "https://issuer.test",
              "bromark",
              user,
              Instant.now().plusSeconds(300),
              Instant.now().minusSeconds(1));
      String body =
          mvc.perform(
                  org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                          "/api/v2/draws")
                      .header("Authorization", "Bearer " + token)
                      .contentType("application/json")
                      .content("{\"requestId\":\"r1\",\"activityId\":" + a + "}"))
              .andExpect(
                  org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                      .isAccepted())
              .andReturn()
              .getResponse()
              .getContentAsString();
      String order =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("orderId").asText();
      var draws = ctx.getBean(DrawService.class);
      try {
        org.awaitility.Awaitility.await()
            .atMost(Duration.ofSeconds(25))
            .untilAsserted(() -> assertThat(draws.get(user, order).state()).isEqualTo("DELIVERED"));
      } catch (org.awaitility.core.ConditionTimeoutException failure) {
        System.err.println(
            "RAFFLE_INTEGRATION_DIAGNOSTIC order="
                + order
                + " route="
                + ctx.getBean(RouteCodec.class).routeOrder(order));
        try {
          System.err.println("drawState=" + draws.get(user, order).state());
          tx.inUser(
              user,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(5)),
              () -> {
                System.err.println(
                    "outbox="
                        + j.queryForList(
                            "select"
                                + " event_id,event_type,state,attempt,lease_until,next_attempt_at,created_at,published_at"
                                + " from outbox where aggregate_id=? order by created_at",
                            order));
                for (var event :
                    j.query(
                        "select payload from outbox where aggregate_id=?",
                        (rs, n) -> JsonStore.read(rs.getString(1), EventEnvelope.class),
                        order)) {
                  String key =
                      MySqlConsumerInbox.digest(
                          event.eventType()
                              + ":"
                              + event.bizId().type()
                              + ":"
                              + event.bizId().id());
                  System.err.println(
                      "receipt "
                          + event.eventType()
                          + "="
                          + j.queryForList(
                              "select consumer_name,operation_key from consumer_receipt where"
                                  + " operation_key=?",
                              key));
                }
                System.err.println(
                    "retry="
                        + j.queryForList(
                            "select event_id,event_type,state,attempt,lease_until,next_attempt_at"
                                + " from retry_job where aggregate_id=?",
                            order));
                System.err.println(
                    "inbox="
                        + j.queryForList(
                            "select consumer_name,event_id,processed_at from consumer_inbox where"
                                + " event_id in (select event_id from outbox where aggregate_id=?)",
                            order));
                System.err.println(
                    "dispatch="
                        + j.queryForList(
                            "select"
                                + " aggregate_type,observed_state,last_event_type,last_event_id,redrive_count"
                                + " from v2_recovery_dispatch where aggregate_id=?",
                            order));
                System.err.println(
                    "cases="
                        + j.queryForList(
                            "select"
                                + " aggregate_type,business_state,recovery_status,created_at,closed_at"
                                + " from recovery_case where aggregate_id=?",
                            order));
                return null;
              });
          tx.inControl(
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(5)),
              () -> {
                System.err.println(
                    "stock="
                        + j.queryForList(
                            "select draw_order_id,state,recovery_status,last_progress_at from"
                                + " award_reservation where draw_order_id=?",
                            order));
                System.err.println(
                    "stockOutbox="
                        + j.queryForList(
                            "select event_id,event_type,state,attempt,lease_until,next_attempt_at"
                                + " from outbox where aggregate_id=?",
                            order));
                return null;
              });
        } catch (RuntimeException diagnosticError) {
          failure.addSuppressed(diagnosticError);
        }
        throw failure;
      }
      assertThat(draws.get(user, order).finalAward().amount())
          .isBetween(new java.math.BigDecimal("1.00"), new java.math.BigDecimal("2.00"));
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                      "/api/v2/draws/" + order + "/cancel")
                  .header("Authorization", "Bearer " + token))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                  .isConflict());
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                      "/api/v2/draws")
                  .header("Authorization", "Bearer " + token)
                  .contentType("application/json")
                  .content("{\"requestId\":\"r1\",\"activityId\":" + a + "}"))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
      var second =
          draws.create(
              new DrawCommand(user, "second", a, new Deadline(Instant.now().plusSeconds(10))));
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(25))
          .untilAsserted(
              () -> assertThat(draws.get(user, second.orderId()).state()).isEqualTo("DELIVERED"));
      assertThat(draws.get(user, second.orderId()).finalAward().type()).isEqualTo("NONE");
      var report =
          ctx.getBean(RaffleReconciliationService.class)
              .check(ctx.getBean(RouteCodec.class).routeUser(user));
      assertThat(report.checkedScopes()).contains("credit", "quota", "draw", "delivery");
      assertThat(report.creditMismatch() + report.quotaMismatch() + report.duplicateDelivery())
          .isZero();
      assertThat(
              ctx.getBean(RaffleReconciliationService.class)
                  .check(RouteSlot.control())
                  .stockMismatch())
          .isZero();
      System.out.println(
          "PHASE4_CACHE heapUsedBytes="
              + java.lang.management.ManagementFactory.getMemoryMXBean()
                  .getHeapMemoryUsage()
                  .getUsed()
              + " versionEntries="
              + ctx.getBean(com.jay.infrastructure.cache.StrategyVersionCache.class)
                  .estimatedVersions());
      String other =
          V2IdentityV2Test.token(
              V2IdentityV2Test.KEY,
              "https://issuer.test",
              "bromark",
              "another",
              Instant.now().plusSeconds(300),
              Instant.now().minusSeconds(1));
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/v2/draws/" + order)
                  .header("Authorization", "Bearer " + other))
          .andExpect(
              org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                  .isForbidden());
    }
  }

  @Test
  public void freezesBlacklistAndBusinessDayAndRollsBackTechnicalAcceptance() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long a = DrawAcceptanceV2Test.publish(h, null);
          String u = "blocked-" + UUID.randomUUID();
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update("insert into v2_asset_blacklist values(?)", u);
                return null;
              });
          DrawAcceptanceV2Test.grant(h, u, a);
          var svc = DrawAcceptanceV2Test.service(h);
          var view =
              svc.create(
                  new DrawCommand(u, "frozen", a, new Deadline(Instant.now().plusSeconds(30))));
          var repo = new MySqlDrawRepository(h.j.getDataSource());
          var r = h.tx(u, () -> repo.find(view.orderId(), false));
          assertThat(r.ruleInput().blacklisted()).isTrue();
          StockProtocolV2Test.control(
              h,
              () -> {
                h.j.update("delete from v2_asset_blacklist where user_id=?", u);
                return null;
              });
          h.runner.inUser(
              u,
              WorkClass.WORKER,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                new CandidateWorker(
                        repo,
                        null,
                        new com.jay.domain.strategy.service.FrozenRaffleEngine(),
                        h.codec,
                        new MySqlOutboxWriter(h.j.getDataSource()))
                    .prepareFrozen(r.orderId(), r.strategy(), new Random());
                assertThat(repo.find(r.orderId(), false).candidate().type()).isEqualTo("NONE");
                return null;
              });
        });
  }
}
