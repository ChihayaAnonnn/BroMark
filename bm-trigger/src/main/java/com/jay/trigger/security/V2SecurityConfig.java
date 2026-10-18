package com.jay.trigger.security;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class V2SecurityConfig {
  @Bean
  @ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
  public JwtDecoder v2JwtDecoder(
      @Value("${bm.v2.auth.public-key:}") String encoded,
      @Value("${bm.v2.auth.issuer:}") String issuer,
      @Value("${bm.v2.auth.audience:}") String audience)
      throws Exception {
    if (encoded.isBlank() || issuer.isBlank() || audience.isBlank())
      throw new IllegalStateException("V2 requires RSA public key, issuer and audience");
    RSAPublicKey key =
        (RSAPublicKey)
            KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    if (key.getModulus().bitLength() < 2048)
      throw new IllegalArgumentException("RSA key must have at least 2048 bits");
    var decoder =
        NimbusJwtDecoder.withPublicKey(key).signatureAlgorithm(SignatureAlgorithm.RS256).build();
    OAuth2TokenValidator<Jwt> required =
        jwt -> {
          if (jwt.getExpiresAt() == null
              || jwt.getSubject() == null
              || jwt.getSubject().isBlank()
              || !jwt.getAudience().contains(audience))
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Required identity claims are invalid", null));
          return OAuth2TokenValidatorResult.success();
        };
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), required));
    return decoder;
  }

  @Bean
  @Order(1)
  @ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
  public SecurityFilterChain v2Security(HttpSecurity http, JwtDecoder decoder) throws Exception {
    return http.securityMatcher("/api/v2/**")
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.GET, "/api/v2/activities/*", "/api/v2/activities/*/awards")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v2/payments/notify/*",
                        "/api/v2/payment/notifications/*")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> j.decoder(decoder))
                    .authenticationEntryPoint((req, res, e) -> error(res, 401, "Unauthorized")))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((req, res, ex) -> error(res, 401, "Unauthorized"))
                    .accessDeniedHandler((req, res, ex) -> error(res, 403, "Forbidden")))
        .build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain legacyAndManagement(HttpSecurity http) throws Exception {
    return http.csrf(c -> c.disable())
        .authorizeHttpRequests(
            a ->
                // health 与 prometheus 免认证；其余 actuator 端点继续拒绝。
                // 运行手册依赖 /actuator/prometheus 抓取 DELIVERED/s，改动时不得移除放行。
                a.requestMatchers("/actuator/health", "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/actuator/**", "/api/v2/**")
                    .denyAll()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((req, res, ex) -> error(res, 403, "Forbidden"))
                    .accessDeniedHandler((req, res, ex) -> error(res, 403, "Forbidden")))
        .build();
  }

  private static void error(
      jakarta.servlet.http.HttpServletResponse response, int status, String message)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write("{\"code\":\"" + status + "\",\"info\":\"" + message + "\"}");
  }
}
