package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.infrastructure.admission.RedisRequestAdmission;
import java.time.*;
import org.junit.Test;

public class RequestAdmissionV2Test {
  @Test
  public void separatesQueryCapacityAndReleasesPermitsOnFailure() {
    var admission = new RedisRequestAdmission(null, 1, 1, 1, 1, 1, 1);
    var deadline = new Deadline(Instant.now().plusSeconds(5));
    try (var a = admission.enter("user", deadline)) {
      assertThatThrownBy(() -> admission.enter("another", deadline))
          .isInstanceOf(BudgetExceededException.class);
      try (var q = admission.query("user", deadline)) {
        assertThat(q).isNotNull();
      }
    }
    assertThatThrownBy(
            () -> admission.enter("another", new Deadline(Instant.now().minusSeconds(1))))
        .isInstanceOf(BudgetExceededException.class);
    assertThatThrownBy(() -> new RedisRequestAdmission(null, 0, 1, 1, 1, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
