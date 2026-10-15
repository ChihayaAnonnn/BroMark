package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.service.AssetWriteGate;
import com.jay.infrastructure.admission.LegacyAssetWriteInterceptor;
import org.junit.Test;

public class LegacyAssetGateV2Test {
  @Test
  public void directOldDaoCannotBypassFence() {
    Java25RouterIntegrationTest.databaseContext()
        .withBean(
            LegacyAssetWriteInterceptor.class,
            () -> new LegacyAssetWriteInterceptor(new AssetWriteGate(true, "V2")))
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              var row = new com.jay.infrastructure.dao.po.UserCreditAccount();
              row.setUserId("bypass");
              row.setAvailableAmount(java.math.BigDecimal.ONE);
              row.setTotalAmount(java.math.BigDecimal.ONE);
              assertThatThrownBy(
                      () ->
                          c.getBean(com.jay.infrastructure.dao.IUserCreditAccountDao.class)
                              .updateAddAmount(row))
                  .hasStackTraceContaining("Asset entry is fenced");
            });
  }
}
