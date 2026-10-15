package com.jay.config;

import com.jay.domain.asset.service.AssetWriteGate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(V2FeatureProperties.class)
public class AssetWriteGateConfig {
  @Bean
  public com.jay.infrastructure.admission.LegacyAssetWriteInterceptor legacyAssetWriteInterceptor(
      AssetWriteGate gate) {
    return new com.jay.infrastructure.admission.LegacyAssetWriteInterceptor(gate);
  }

  @Bean
  public AssetWriteGate assetWriteGate(V2FeatureProperties properties) {
    return new AssetWriteGate(properties.isEnabled(), properties.getAssetWriteMode());
  }
}
