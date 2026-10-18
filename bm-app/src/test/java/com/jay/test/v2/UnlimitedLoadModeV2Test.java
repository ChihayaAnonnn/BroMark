package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.infrastructure.admission.RedisRequestAdmission;
import com.jay.infrastructure.transaction.BoundedDatabaseBudget;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class UnlimitedLoadModeV2Test {
  private Map<String, Map<WorkClass, Integer>> limits() {
    var work = new EnumMap<WorkClass, Integer>(WorkClass.class);
    for (var w : WorkClass.values()) work.put(w, 1);
    return Map.of("mysql-it", work);
  }

  private Deadline deadline() {
    return new Deadline(Instant.now().plusSeconds(2));
  }

  @Test
  public void allAdmissionCategoriesAcceptBeyondOriginalLimitWithoutRedis() {
    var registry = new SimpleMeterRegistry();
    var original = new RedisRequestAdmission(null, 1, 1, 1, 1, 1, 1, 1, 1);
    var admission =
        (RequestAdmission)
            UnlimitedLoadMode.processor(limits(), registry)
                .postProcessAfterInitialization(original, "requestAdmission");
    var held = new ArrayList<Permit>();
    try {
      assertThatCode(
              () -> {
                for (int i = 0; i < 101; i++) {
                  held.add(admission.enter("same", deadline()));
                  held.add(admission.query("public-activity", deadline()));
                  held.add(admission.cancel("same", deadline()));
                  admission.checkActivity("same", deadline());
                }
              })
          .doesNotThrowAnyException();
    } finally {
      held.forEach(Permit::close);
    }
    assertThatThrownBy(() -> admission.query("same", new Deadline(Instant.now().minusSeconds(1))))
        .isInstanceOf(BudgetExceededException.class);
  }

  @Test
  public void modeRequiresTestProfileAndMatchingIsolatedSchemas() {
    var e = new org.springframework.mock.env.MockEnvironment();
    e.setActiveProfiles("dev");
    assertThatThrownBy(() -> UnlimitedLoadMode.validate(e))
        .isInstanceOf(IllegalArgumentException.class);
    e.setActiveProfiles("v2-it");
    e.setProperty("V2_DB00", "router_control");
    assertThatThrownBy(() -> UnlimitedLoadMode.validate(e))
        .isInstanceOf(IllegalArgumentException.class);
    e.setProperty("V2_DB00", "router_perf_it_unlimited1_control");
    e.setProperty("V2_DB01", "router_perf_it_other_one");
    e.setProperty("V2_DB02", "router_perf_it_unlimited1_two");
    assertThatThrownBy(() -> UnlimitedLoadMode.validate(e))
        .isInstanceOf(IllegalArgumentException.class);
    e.setProperty("V2_DB01", "router_perf_it_unlimited1_one");
    assertThatCode(() -> UnlimitedLoadMode.validate(e)).doesNotThrowAnyException();
  }

  @Test
  public void databaseClassesDoNotWaitForOriginalPermitAndStillTrackShutdown() throws Exception {
    var registry = new SimpleMeterRegistry();
    var original = new BoundedDatabaseBudget(limits(), registry, Duration.ofMillis(20));
    var budget =
        (BoundedDatabaseBudget)
            UnlimitedLoadMode.processor(limits(), registry)
                .postProcessAfterInitialization(original, "databaseBudget");
    var held = new ArrayList<Permit>();
    try {
      assertThatCode(
              () -> {
                for (var work : WorkClass.values())
                  for (int i = 0; i < 32; i++)
                    held.add(budget.acquire("mysql-it", work, deadline()));
              })
          .doesNotThrowAnyException();
      assertThat(budget.isIdle()).isFalse();
      budget.stopAccepting();
      assertThatThrownBy(() -> budget.acquire("mysql-it", WorkClass.TRADE, deadline()))
          .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
    } finally {
      held.forEach(Permit::close);
    }
    assertThat(budget.awaitIdle(Duration.ofSeconds(1))).isTrue();
  }
}
