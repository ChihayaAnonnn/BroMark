package com.jay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bm.v2")
public class V2FeatureProperties {
  private boolean enabled;
  private String assetWriteMode = "LEGACY";
  private Integer userRps, activityRps, maxInFlight, localFallbackRps, queryRps, queryMaxInFlight;

  public void validateAdmission() {
    if (userRps == null
        || activityRps == null
        || maxInFlight == null
        || localFallbackRps == null
        || queryRps == null
        || queryMaxInFlight == null)
      throw new IllegalArgumentException("Explicit v2 admission settings required");
  }
}
