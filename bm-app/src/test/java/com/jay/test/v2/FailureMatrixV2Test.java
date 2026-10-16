package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import com.jay.domain.raffle.model.ReconciliationReport;
import com.jay.domain.raffle.service.ValidationGate;
import java.util.*;
import org.junit.Test;

public class FailureMatrixV2Test {
  @Test public void uncheckedDomainAndPausedOrdersCannotPassReleaseGate() {
    var gate=new ValidationGate();
    var incomplete=new ReconciliationReport(0,0,0,0,0,0,Set.of("CREDIT","QUOTA"));
    assertThat(gate.passes(incomplete,0,0)).isFalse();
    var full=new ReconciliationReport(0,0,0,0,0,0,Set.of("CREDIT","QUOTA","STOCK","DELIVERY","PAYMENT","SIGNIN"));
    assertThat(gate.passes(full,1,0)).isFalse();
    assertThat(gate.passes(full,0,1)).isFalse();
    assertThat(gate.passes(full,0,0)).isTrue();
    assertThat(gate.passes(new ReconciliationReport(0,0,0,0,0,1,full.checkedScopes()),0,0)).isFalse();
  }
}
