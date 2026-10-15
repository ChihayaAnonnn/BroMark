package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jay.domain.asset.model.*;
import com.jay.trigger.http.v2.*;
import com.jay.trigger.security.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

public class V2IdentityV2Test {
  static final RSAKey KEY = key();

  static RSAKey key() {
    try {
      return new RSAKeyGenerator(2048).generate();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  @Import({
    V2SecurityConfig.class,
    CurrentUser.class,
    V2ExceptionHandler.class,
    V2IdentityProbeController.class,
    com.jay.trigger.http.v2.TestEndpoints.class
  })
  static class WebConfig {}

  static String token(
      RSAKey key, String issuer, String audience, String subject, Instant expiry, Instant notBefore)
      throws Exception {
    var claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .subject(subject)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(expiry))
            .notBeforeTime(Date.from(notBefore))
            .build();
    var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }

  WebApplicationContextRunner context() {
    return new WebApplicationContextRunner()
        .withInitializer(c -> c.getEnvironment().setActiveProfiles("v2-it"))
        .withUserConfiguration(WebConfig.class)
        .withPropertyValues(
            "bm.v2.enabled=true",
            "bm.v2.auth.issuer=https://issuer.test",
            "bm.v2.auth.audience=bromark",
            "bm.v2.auth.public-key=" + Base64.getEncoder().encodeToString(publicBytes()));
  }

  static byte[] publicBytes() {
    try {
      return KEY.toRSAPublicKey().getEncoded();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void verifiesRealSignaturesClaimsAndNeverTrustsRequestUserId() {
    context()
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              MockMvc mvc =
                  org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(c)
                      .apply(springSecurity())
                      .build();
              Instant now = Instant.now();
              mvc.perform(
                      get("/api/v2/identity")
                          .header("x-user-id", "attacker")
                          .param("userId", "attacker"))
                  .andExpect(status().isUnauthorized());
              mvc.perform(
                      post("/api/v2/identity")
                          .contentType("application/json")
                          .content("{\"userId\":\"attacker\"}"))
                  .andExpect(status().isUnauthorized());
              for (String bad :
                  List.of(
                      token(
                          key(),
                          "https://issuer.test",
                          "bromark",
                          "u1",
                          now.plusSeconds(300),
                          now.minusSeconds(1)),
                      token(
                          KEY, "wrong", "bromark", "u1", now.plusSeconds(300), now.minusSeconds(1)),
                      token(
                          KEY,
                          "https://issuer.test",
                          "wrong",
                          "u1",
                          now.plusSeconds(300),
                          now.minusSeconds(1)),
                      token(
                          KEY,
                          "https://issuer.test",
                          "bromark",
                          "u1",
                          now.minusSeconds(120),
                          now.minusSeconds(300)),
                      token(
                          KEY,
                          "https://issuer.test",
                          "bromark",
                          " ",
                          now.plusSeconds(300),
                          now.minusSeconds(1)),
                      token(
                          KEY,
                          "https://issuer.test",
                          "bromark",
                          "u1",
                          now.plusSeconds(600),
                          now.plusSeconds(300))))
                mvc.perform(get("/api/v2/identity").header("Authorization", "Bearer " + bad))
                    .andExpect(status().isUnauthorized());
              String good =
                  token(
                      KEY,
                      "https://issuer.test",
                      "bromark",
                      "u1",
                      now.plusSeconds(300),
                      now.minusSeconds(1));
              mvc.perform(
                      get("/api/v2/identity")
                          .header("Authorization", "Bearer " + good)
                          .param("userId", "attacker"))
                  .andExpect(status().isOk())
                  .andExpect(content().string("u1"));
              mvc.perform(
                      post("/api/v2/test/validate")
                          .header("Authorization", "Bearer " + good)
                          .contentType("application/json")
                          .content("{\"value\":\"\"}"))
                  .andExpect(status().isBadRequest());
              mvc.perform(get("/api/v2/activities/1")).andExpect(status().isOk());
              mvc.perform(post("/api/v2/activities/1")).andExpect(status().isUnauthorized());
              mvc.perform(get("/api/v2/orders/1")).andExpect(status().isUnauthorized());
              mvc.perform(get("/actuator/prometheus")).andExpect(status().isForbidden());
              for (int code : new int[] {400, 403, 409, 429, 503})
                mvc.perform(
                        get("/api/v2/test/error/" + code).header("Authorization", "Bearer " + good))
                    .andExpect(status().is(code))
                    .andExpect(jsonPath("$.code").value(String.valueOf(code)));
            });
  }

  @Test
  public void missingAuthConfigurationPreventsV2Startup() {
    new WebApplicationContextRunner()
        .withUserConfiguration(WebConfig.class)
        .withPropertyValues("bm.v2.enabled=true")
        .run(c -> assertThat(c).hasFailed());
  }

  @Test
  public void disabledV2DoesNotExposeIdentity() {
    new WebApplicationContextRunner()
        .withUserConfiguration(WebConfig.class)
        .withPropertyValues("bm.v2.enabled=false")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              var mvc =
                  org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(c)
                      .apply(springSecurity())
                      .build();
              mvc.perform(get("/api/v2/identity")).andExpect(status().isForbidden());
            });
  }
}
