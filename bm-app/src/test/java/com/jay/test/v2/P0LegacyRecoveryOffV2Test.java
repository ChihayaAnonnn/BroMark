package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.V2RaffleConfig;
import com.jay.domain.raffle.service.DrawRecoveryService;
import com.jay.trigger.job.v2.DrawRecoveryJob;
import com.jay.trigger.job.v2.StockRecoveryJob;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

public class P0LegacyRecoveryOffV2Test {
  @Test
  public void legacyRecoveryBeansAreNotRegistered() {
    Set<Class<?>> legacyTypes =
        Set.of(DrawRecoveryService.class, DrawRecoveryJob.class, StockRecoveryJob.class);
    Map<String, Class<?>> legacyBeans = new LinkedHashMap<>();

    for (Method method : V2RaffleConfig.class.getDeclaredMethods()) {
      Bean bean = method.getAnnotation(Bean.class);
      if (bean == null) continue;
      if (!legacyTypes.contains(method.getReturnType())) continue;
      String name = bean.name().length == 0 ? method.getName() : bean.name()[0];
      legacyBeans.put(name, method.getReturnType());
    }

    assertThat(legacyBeans)
        .as("P0 recovery is outbox relay + retry_job; legacy recovery must not be scheduled")
        .isEmpty();
  }
}
