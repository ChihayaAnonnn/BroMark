package com.jay.config;

import com.jay.domain.task.model.RelayPolicy;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("bm.v2.messaging.relay")
public class V2RelayProperties implements InitializingBean {
  private String mode = "legacy";
  private int claimLimit = 32;
  private int publishWindow = 8;
  private int expiredReserve = 4;
  private int fairnessSlots = 8;
  private long idlePollMs = 250;
  private long leaseSeconds = 30;
  private long claimTimeoutSeconds = 1;
  private long publishTimeoutSeconds = 3;
  private long settleTimeoutSeconds = 1;
  private long batchDeadlineSeconds = 20;
  private long safetyMarginSeconds = 5;

  public RelayPolicy policy() {
    return new RelayPolicy(
        claimLimit,
        publishWindow,
        expiredReserve,
        fairnessSlots,
        Duration.ofMillis(idlePollMs),
        Duration.ofSeconds(leaseSeconds),
        Duration.ofSeconds(claimTimeoutSeconds),
        Duration.ofSeconds(publishTimeoutSeconds),
        Duration.ofSeconds(settleTimeoutSeconds),
        Duration.ofSeconds(batchDeadlineSeconds),
        Duration.ofSeconds(safetyMarginSeconds));
  }

  public void afterPropertiesSet() {
    if (!"legacy".equals(mode) && !"bounded".equals(mode))
      throw new IllegalArgumentException("relay.mode must be legacy or bounded");
    policy().validate(2, 16);
  }
}
