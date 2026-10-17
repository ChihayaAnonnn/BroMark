package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.Test;

public class RelayOptimizationFaultV2Test {
  @Test
  public void confirmProbeSignalsOnlyAfterRealConfirmHook() throws Exception {
    var dir = Files.createTempDirectory("task8-confirm-probe-");
    var hook = RelayFaultProbe.afterConfirm(dir);
    try (var executor = Executors.newSingleThreadExecutor()) {
      hook.hit("relay.before-confirm", "e1", "o1", "lease-1", 1);
      assertThat(Files.exists(dir.resolve("confirmed-blocked"))).isFalse();
      Files.writeString(dir.resolve("arm"), "e1");
      hook.hit("relay.after-confirm", "wrong", "o1", "lease-other", 1);
      assertThat(Files.exists(dir.resolve("confirmed-blocked"))).isFalse();
      var task = executor.submit(() -> hook.hit("relay.after-confirm", "e1", "o1", "lease-1", 1));
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(2))
          .until(() -> Files.exists(dir.resolve("confirmed-blocked")));
      assertThat(task.isDone()).isFalse();
      assertThat(Files.readString(dir.resolve("confirmed-blocked")).split("\t")[1]).isEqualTo("e1");
      task.cancel(true);
    }
  }
  @Test
  public void lateProbeRetainsRealClaimAndUsesSeparateBoundedTransaction() throws Exception {
    var dir = Files.createTempDirectory("task8-late-probe-");
    var late = new RelayFaultProbe.LateSettlement(dir);
    var repo = org.mockito.Mockito.mock(
        com.jay.infrastructure.adapter.repository.v2.MySqlOutboxRepository.class);
    var slot = new com.jay.domain.asset.model.RouteSlot("01", "000");
    var original = new com.jay.domain.task.model.ClaimedPublish(slot, "event", "order", 1,
        "PrepareDraw", "{}", "hash", "old-token", java.time.Instant.now().minusSeconds(1),
        1, 0, "", java.time.Instant.now().minusSeconds(31));
    org.mockito.Mockito.when(repo.claimBatch(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(java.util.List.of(original));
    org.mockito.Mockito.when(repo.settleBatch(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
          java.util.List<com.jay.domain.task.model.PublishResolution> results = invocation.getArgument(1);
          assertThat(results.getFirst().task()).isSameAs(original);
          com.jay.domain.asset.model.Deadline deadline = invocation.getArgument(2);
          assertThat(deadline.expiresAt()).isAfter(java.time.Instant.now())
              .isBefore(java.time.Instant.now().plusSeconds(2));
          return new com.jay.domain.task.model.BatchSettleReport(java.util.Set.of(), java.util.Set.of(),
              java.util.Set.of(), java.util.Set.of("event"), java.time.Instant.now());
        });
    Files.writeString(dir.resolve("arm-late"), "event");
    var captured = (com.jay.domain.task.repository.BatchOutboxRepository)
        late.capture().postProcessAfterInitialization(repo, "v2OutboxRepository");
    captured.claimBatch(slot, null, null);
    assertThat(late.claimed.get("event")).isSameAs(original);
    late.submitIfTriggered();
    assertThat(Files.exists(dir.resolve("late-settle-result.json"))).isFalse();
    Files.writeString(dir.resolve("submit-late"), "go");
    late.submitIfTriggered();
    late.submitIfTriggered();
    org.mockito.Mockito.verify(repo, org.mockito.Mockito.times(1)).settleBatch(
        org.mockito.ArgumentMatchers.eq(slot), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    assertThat(Files.readString(dir.resolve("late-settle-result.json")))
        .contains("old-token", "test-only fresh 1s", "lost");
  }
}
