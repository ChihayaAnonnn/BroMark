package com.jay.test.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.jay.infrastructure.event.MicrometerRelayObservation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** P0 指标的成功/失败记录边界：成功等提交，失败必须立即可见。 */
public class P0MetricsV2Test {
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final MicrometerRelayObservation observation = new MicrometerRelayObservation(registry);

  @After
  public void clearTransactionSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.clear();
  }

  @Test
  public void failureResultsIncrementWithoutAnyTransaction() {
    observation.p0Delivery("retry");
    observation.p0Delivery("failed");
    observation.p0Stage("deliver", "failed", 1_000_000);
    observation.p0Stage("reserve", "error", 1_000_000);
    observation.p0Admission("ratelimited");

    assertThat(delivery("retry")).isOne();
    assertThat(delivery("failed")).isOne();
    assertThat(stage("deliver", "failed")).isOne();
    assertThat(stage("reserve", "error")).isOne();
    assertThat(admission("ratelimited")).isOne();
  }

  @Test
  public void failureResultsSurviveRollbackWhileDeliveredWaitsForCommit() {
    TransactionSynchronizationManager.initSynchronization();

    observation.p0Delivery("retry");
    observation.p0Delivery("failed");
    observation.p0Stage("deliver", "failed", 1_000_000);
    observation.p0Stage("reserve", "error", 1_000_000);
    observation.p0Event("ReserveAward", "retry");
    observation.p0Admission("ratelimited");
    observation.p0Delivery("delivered");
    observation.p0Event("ReserveAward", "emitted");
    observation.p0Stage("deliver", "delivered", 1_000_000);
    observation.p0Stage("reserve", "success", 1_000_000);
    observation.p0Admission("accepted");

    // 失败类在回滚真正发生前已经记录。
    assertThat(delivery("retry")).isOne();
    assertThat(delivery("failed")).isOne();
    assertThat(stage("deliver", "failed")).isOne();
    assertThat(stage("reserve", "error")).isOne();
    assertThat(event("ReserveAward", "retry")).isOne();
    assertThat(admission("ratelimited")).isOne();
    // 正向结果仍在等待提交。
    assertThat(delivery("delivered")).isZero();
    assertThat(event("ReserveAward", "emitted")).isZero();
    assertThat(stage("deliver", "delivered")).isZero();
    assertThat(stage("reserve", "success")).isZero();
    assertThat(admission("accepted")).isZero();

    // 只清除同步、不触发 afterCommit，等价于事务回滚。
    TransactionSynchronizationManager.clearSynchronization();

    assertThat(delivery("retry")).isOne();
    assertThat(delivery("failed")).isOne();
    assertThat(stage("deliver", "failed")).isOne();
    assertThat(stage("reserve", "error")).isOne();
    assertThat(delivery("delivered")).isZero();
    assertThat(event("ReserveAward", "emitted")).isZero();
    assertThat(stage("deliver", "delivered")).isZero();
    assertThat(stage("reserve", "success")).isZero();
    assertThat(admission("accepted")).isZero();
  }

  @Test
  public void successResultsAreRecordedOnlyAfterCommit() {
    TransactionSynchronizationManager.initSynchronization();

    observation.p0Delivery("delivered");
    observation.p0Delivery("duplicate");
    observation.p0Event("ReserveAward", "emitted");
    observation.p0Event("ReservationConfirmed", "delivered");
    observation.p0Stage("deliver", "delivered", 1_000_000);
    observation.p0Stage("reserve", "success", 1_000_000);
    observation.p0Admission("accepted");

    assertThat(delivery("delivered")).isZero();
    assertThat(delivery("duplicate")).isZero();
    assertThat(event("ReserveAward", "emitted")).isZero();
    assertThat(event("ReservationConfirmed", "delivered")).isZero();
    assertThat(stage("deliver", "delivered")).isZero();
    assertThat(stage("reserve", "success")).isZero();
    assertThat(admission("accepted")).isZero();

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);

    assertThat(delivery("delivered")).isOne();
    assertThat(delivery("duplicate")).isOne();
    assertThat(event("ReserveAward", "emitted")).isOne();
    assertThat(event("ReservationConfirmed", "delivered")).isOne();
    assertThat(stage("deliver", "delivered")).isOne();
    assertThat(stage("reserve", "success")).isOne();
    assertThat(admission("accepted")).isOne();
  }

  private double delivery(String result) {
    var counter = registry.find("p0_delivery_total").tag("result", result).counter();
    return counter == null ? 0 : counter.count();
  }

  private double admission(String result) {
    var counter = registry.find("p0_admission_total").tag("result", result).counter();
    return counter == null ? 0 : counter.count();
  }

  private double event(String type, String result) {
    var counter =
        registry.find("p0_event_total").tag("type", type).tag("result", result).counter();
    return counter == null ? 0 : counter.count();
  }

  private double stage(String stage, String result) {
    var timer =
        registry.find("p0_stage_seconds").tag("stage", stage).tag("result", result).timer();
    return timer == null ? 0 : timer.count();
  }
}
