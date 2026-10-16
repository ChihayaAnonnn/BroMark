package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.V2BudgetProperties;
import java.util.*;
import org.junit.Test;

public class CapacityEvidenceV2Test {
  @Test
  public void allReplicaPoolsAreAggregatedByPhysicalDatabaseBeforeAdmission() {
    var p = new V2BudgetProperties();
    p.setReplicaCount(2);
    p.setPhysicalConnectionLimits(Map.of("mysql-it", 40));
    p.setReservedConnections(4);
    assertThatThrownBy(() -> p.validateConnections(Map.of("00", 8, "01", 8, "02", 8)))
        .isInstanceOf(IllegalArgumentException.class);
    p.setPhysicalConnectionLimits(Map.of("mysql-it", 60));
    assertThatCode(() -> p.validateConnections(Map.of("00", 8, "01", 8, "02", 8)))
        .doesNotThrowAnyException();
    p.setReplicaCount(0);
    assertThatThrownBy(() -> p.validateConnections(Map.of("00", 8, "01", 8, "02", 8)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
