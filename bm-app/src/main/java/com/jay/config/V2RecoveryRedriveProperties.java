package com.jay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("bm.v2.recovery.redrive")
public class V2RecoveryRedriveProperties implements InitializingBean {
  private String mode = "legacy";
  private int staleSeconds = 30;
  private int intervalSeconds = 30;
  private int maxPerStage = 12;
  private int candidateLimit = 64;
  private int emissionLimit = 8;
  private int roundTimeoutSeconds = 2;

  public void afterPropertiesSet() {
    if ((!"legacy".equals(mode) && !"guarded".equals(mode))
        || staleSeconds < 1
        || intervalSeconds < 1
        || maxPerStage < 1
        || maxPerStage > 12
        || candidateLimit < 1
        || candidateLimit > 64
        || emissionLimit < 1
        || emissionLimit > 8
        || emissionLimit > candidateLimit
        || roundTimeoutSeconds < 1)
      throw new IllegalArgumentException("Invalid recovery redrive budget or mode");
  }
}
