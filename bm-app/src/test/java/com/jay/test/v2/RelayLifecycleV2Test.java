package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.V2WorkerConfig.InboundGate;
import com.jay.trigger.job.v2.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.springframework.mock.web.*;

public class RelayLifecycleV2Test {
  @Test
  public void coalescesTicksAndKeepsLegacyMutuallyExclusive() {
    var queued = new ArrayList<Runnable>();
    var executor =
        new AbstractExecutorService() {
          public void execute(Runnable r) {
            queued.add(r);
          }

          public void shutdown() {}

          public List<Runnable> shutdownNow() {
            return List.of();
          }

          public boolean isShutdown() {
            return false;
          }

          public boolean isTerminated() {
            return false;
          }

          public boolean awaitTermination(long n, TimeUnit u) {
            return true;
          }
        };
    var dispatcher = new CoalescingJobDispatcher(executor);
    var calls = new AtomicInteger();
    for (int i = 0; i < 100; i++) dispatcher.submit("draw", calls::incrementAndGet);
    assertThat(queued).hasSize(1);
    queued.removeFirst().run();
    assertThat(calls).hasValue(1);
    assertThat(
            dispatcher.submit(
                "draw",
                () -> {
                  throw new IllegalStateException("probe");
                }))
        .isTrue();
    queued.removeFirst().run();
    assertThat(dispatcher.submit("draw", calls::incrementAndGet)).isTrue();
    dispatcher.quiesce();
    assertThat(dispatcher.submit("stock", calls::incrementAndGet)).isFalse();
  }

  @Test
  public void rejectionNeverRunsOnCallerAndCanRetry() {
    var executor = Executors.newSingleThreadExecutor();
    executor.shutdown();
    var dispatcher = new CoalescingJobDispatcher(executor);
    var calls = new AtomicInteger();
    assertThat(dispatcher.submit("draw", calls::incrementAndGet)).isFalse();
    assertThat(dispatcher.submit("draw", calls::incrementAndGet)).isFalse();
    assertThat(calls).hasValue(0);
  }

  @Test
  public void busyRoundsRotateAndIdleWakeDoesNotWaitForPoll() throws Exception {
    var visited = new CopyOnWriteArrayList<String>();
    var rounds = new AtomicInteger();
    var worker =
        new RelayDrainWorker(
            slot -> {
              visited.add(slot.dbKey());
              return rounds.getAndIncrement() < 6 ? 1 : 0;
            },
            Duration.ofSeconds(5));
    var thread = new Thread(worker);
    thread.start();
    org.awaitility.Awaitility.await()
        .atMost(Duration.ofSeconds(2))
        .until(() -> visited.size() >= 9);
    assertThat(visited.subList(0, 6)).containsExactly("00", "01", "02", "01", "02", "00");
    int before = visited.size();
    worker.wake();
    org.awaitility.Awaitility.await()
        .atMost(Duration.ofSeconds(1))
        .until(() -> visited.size() > before);
    worker.quiesce();
    thread.join(1000);
    assertThat(thread.isAlive()).isFalse();
    int stopped = visited.size();
    worker.wake();
    assertThat(visited).hasSize(stopped);
  }

  @Test
  public void shutdownRejectsHttpIncludingProviderCallback() throws Exception {
    var gate = new InboundGate();
    var calls = new AtomicInteger();
    gate.stopAccepting();
    for (String path : List.of("/api/v2/draw", "/api/v2/payment/notifications/sandbox")) {
      var request = new MockHttpServletRequest("POST", path);
      var response = new MockHttpServletResponse();
      gate.doFilter(request, response, (r, s) -> calls.incrementAndGet());
      assertThat(response.getStatus()).isEqualTo(503);
    }
    assertThat(calls).hasValue(0);
  }

  @Test
  public void writerAndRetryWakeOnlyAfterCommitNotRollback() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var wakes = new AtomicInteger();
          var writer =
              new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                  h.j.getDataSource(), wakes::incrementAndGet);
          var retry =
              new com.jay.infrastructure.adapter.repository.v2.MySqlRetryJobRepository(
                  h.j.getDataSource(), h.runner, null, wakes::incrementAndGet);
          var event = PublisherConfirmV2Test.event("wake-" + UUID.randomUUID());
          assertThatThrownBy(
                  () ->
                      h.runner.inControl(
                          com.jay.domain.asset.model.WorkClass.TRADE,
                          new com.jay.domain.asset.model.Deadline(Instant.now().plusSeconds(5)),
                          () -> {
                            writer.append(event);
                            retry.schedule(event, 1, Instant.now());
                            assertThat(wakes).hasValue(0);
                            throw new IllegalStateException("rollback");
                          }))
              .isInstanceOf(IllegalStateException.class);
          assertThat(wakes).hasValue(0);
          h.runner.inControl(
              com.jay.domain.asset.model.WorkClass.TRADE,
              new com.jay.domain.asset.model.Deadline(Instant.now().plusSeconds(5)),
              () -> {
                writer.append(event);
                retry.schedule(event, 1, Instant.now());
                assertThat(wakes).hasValue(0);
                return null;
              });
          assertThat(wakes).hasValue(2);
          assertThatThrownBy(() -> writer.append(event)).isInstanceOf(IllegalStateException.class);
          assertThat(wakes).hasValue(2);
        });
  }

  @Test
  public void acceptedHttpDrainsWhileNewRequestsAreRejected() throws Exception {
    var gate = new InboundGate();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var pool = Executors.newSingleThreadExecutor()) {
      var existing =
          pool.submit(
              () -> {
                try {
                  gate.doFilter(
                      new MockHttpServletRequest("POST", "/api/v2/draw"),
                      new MockHttpServletResponse(),
                      (r, s) -> {
                        entered.countDown();
                        try {
                          release.await();
                        } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                        }
                      });
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
              });
      assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
      gate.stopAccepting();
      assertThat(gate.awaitIdle(Duration.ofMillis(20))).isFalse();
      release.countDown();
      existing.get(1, TimeUnit.SECONDS);
      assertThat(gate.awaitIdle(Duration.ofMillis(20))).isTrue();
    } finally {
      release.countDown();
    }
  }

  @Test
  public void quiescedRelayStartsNoClaimOnEitherChannel() {
    var repo =
        new BoundedRelayV2Test.Repo(0) {
          public java.util.List<com.jay.domain.task.model.ClaimedPublish> claimBatch(
              com.jay.domain.asset.model.RouteSlot slot,
              com.jay.domain.task.model.RelayPolicy policy,
              com.jay.domain.asset.model.Deadline deadline) {
            throw new AssertionError("claim after quiesce");
          }
        };
    com.jay.domain.task.service.ConfirmedEventPublisher publisher =
        (event, deadline) ->
            CompletableFuture.completedFuture(com.jay.domain.task.model.PublishOutcome.CONFIRMED);
    var relay =
        new com.jay.domain.task.service.BoundedOutboxRelay(
            repo, publisher, new com.jay.config.V2RelayProperties().policy(), point -> {});
    relay.quiesce();
    assertThat(relay.runOnce(com.jay.domain.asset.model.RouteSlot.control())).isZero();
    var legacy =
        new com.jay.domain.task.service.OutboxRelay(
            repo, publisher, new com.jay.domain.task.service.RetryPolicy(), point -> {});
    legacy.quiesce();
    assertThat(legacy.runOnce(com.jay.domain.asset.model.RouteSlot.control(), 8)).isZero();
  }

  @Test
  public void deadlineCleanupDefersPoolsUntilTransactionsStopAndSpringDestroyDoesNotWaitAgain()
      throws Exception {
    var permitIdle = new AtomicBoolean();
    var closing = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var closes = new AtomicInteger();
    class BlockingSource extends org.springframework.jdbc.datasource.AbstractDataSource
        implements AutoCloseable {
      public java.sql.Connection getConnection() {
        throw new UnsupportedOperationException();
      }

      public java.sql.Connection getConnection(String u, String p) {
        throw new UnsupportedOperationException();
      }

      public void close() throws InterruptedException {
        closes.incrementAndGet();
        closing.countDown();
        release.await();
      }
    }
    var source = new BlockingSource();
    var router = new cn.bugstack.middleware.db.router.dynamic.DynamicDataSource();
    router.setTargetDataSources(Map.of("db00", source));
    router.setDefaultTargetDataSource(source);
    router.afterPropertiesSet();
    long began = System.nanoTime();
    assertThat(router.closeBefore(began + TimeUnit.MILLISECONDS.toNanos(30), permitIdle::get))
        .isFalse();
    assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began)).isLessThan(500);
    assertThat(closes).hasValue(0);
    router.close(); // Spring destroy不重新同步等待；活跃事务期间不碰pool。
    assertThat(closes).hasValue(0);
    permitIdle.set(true);
    try {
      assertThat(closing.await(1, TimeUnit.SECONDS)).isTrue();
    } finally {
      release.countDown();
    }
    assertThat(closes).hasValue(1);
  }

  @Test
  public void rabbitPhysicalCleanupConsumesOnlyRemainingWaitAndNeverRepeatsDestroy()
      throws Exception {
    var release = new CountDownLatch(1);
    var calls = new AtomicInteger();
    var connection = new org.springframework.amqp.rabbit.connection.CachingConnectionFactory();
    Runnable cleanup =
        () -> {
          calls.incrementAndGet();
          try {
            release.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        };
    var constructor =
        com.jay.config.V2RabbitConfig.V2Transport.class.getDeclaredConstructor(
            org.springframework.amqp.rabbit.connection.CachingConnectionFactory.class,
            Runnable.class);
    constructor.setAccessible(true);
    var transport = constructor.newInstance(connection, cleanup);
    long began = System.nanoTime();
    try {
      assertThat(transport.closeBefore(began + TimeUnit.MILLISECONDS.toNanos(30))).isFalse();
      assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began)).isLessThan(500);
      transport.close();
      assertThat(calls).hasValue(1);
    } finally {
      release.countDown();
    }
    assertThat(transport.closeBefore(System.nanoTime() + TimeUnit.SECONDS.toNanos(1))).isTrue();
    assertThat(calls).hasValue(1);
  }

  /** 独立JVM运行真实Application；仅测试注入故障和输出线程/阶段标记。 */
  public static class ProcessProbe {
    public static void main(String[] args) throws Exception {
      var directory = java.nio.file.Path.of(args[0]);
      var builder =
          new org.springframework.boot.builder.SpringApplicationBuilder(com.jay.Application.class)
              .profiles("v2-it");
      builder.initializers(
          c ->
              ((org.springframework.context.support.GenericApplicationContext) c)
                  .registerBean(
                      "processRelayFault",
                      com.jay.domain.asset.service.FaultInjector.class,
                      () ->
                          point -> {
                            if (point.equals("relay.after-confirm")
                                && java.nio.file.Files.exists(directory.resolve("arm"))) {
                              try {
                                java.nio.file.Files.writeString(
                                    directory.resolve("confirmed-blocked"),
                                    "confirmed, source update blocked");
                                new CountDownLatch(1).await();
                              } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("forced shutdown", e);
                              } catch (java.io.IOException e) {
                                throw new IllegalStateException(e);
                              }
                            }
                          },
                      bd -> bd.setPrimary(true)));
      var context = builder.run(java.util.Arrays.copyOfRange(args, 1, args.length));
      var dispatcher = context.getBean(CoalescingJobDispatcher.class);
      if (java.nio.file.Files.exists(directory.resolve("block-recovery")))
        dispatcher.submit(
            "process-recovery-probe",
            () -> {
              try {
                java.nio.file.Files.writeString(
                    directory.resolve("recovery-blocked"), Thread.currentThread().getName());
                new CountDownLatch(1).await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
              }
            });
      var executor = (ThreadPoolExecutor) context.getBean("v2RelayExecutor");
      int port =
          ((org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext)
                  context)
              .getWebServer()
              .getPort();
      java.nio.file.Files.writeString(
          directory.resolve("ready"),
          ProcessHandle.current().pid() + ":" + executor.getActiveCount() + ":" + port);
    }
  }

  @Test
  public void twoIndependentProcessesRelayDuringBlockedRecoveryAndSigtermSharesThirtySeconds()
      throws Exception {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          for (var slot : OutboxRelayJob.SOURCES)
            h.runner.inSlot(
                slot,
                com.jay.domain.asset.model.WorkClass.RECOVERY,
                new com.jay.domain.asset.model.Deadline(Instant.now().plusSeconds(10)),
                () -> {
                  h.j.update("delete from outbox");
                  h.j.update("delete from retry_job");
                  return null;
                });
        });
    var evidence =
        java.nio.file.Path.of("../.superpowers/sdd/2026-10-16-outbox-relay-recovery-optimization")
            .toAbsolutePath();
    var first = java.nio.file.Files.createTempDirectory(evidence, "bromark-relay-20261016-app-1-");
    var second = java.nio.file.Files.createTempDirectory(evidence, "bromark-relay-20261016-app-2-");
    java.nio.file.Files.writeString(first.resolve("block-recovery"), "test");
    Process one = null, two = null;
    try {
      one = startProbe(first);
      two = startProbe(second);
      final Process childOne = one, childTwo = two;
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(30))
          .untilAsserted(
              () -> {
                assertThat(childOne.isAlive())
                    .withFailMessage(java.nio.file.Files.readString(first.resolve("process.log")))
                    .isTrue();
                assertThat(childTwo.isAlive())
                    .withFailMessage(java.nio.file.Files.readString(second.resolve("process.log")))
                    .isTrue();
                assertThat(java.nio.file.Files.exists(first.resolve("ready"))).isTrue();
                assertThat(java.nio.file.Files.exists(second.resolve("ready"))).isTrue();
                assertThat(java.nio.file.Files.exists(first.resolve("recovery-blocked"))).isTrue();
              });
      assertThat(one.pid()).isNotEqualTo(two.pid());
      assertThat(java.nio.file.Files.readString(first.resolve("ready")).split(":")[1])
          .isEqualTo("2");
      assertThat(java.nio.file.Files.readString(second.resolve("ready")).split(":")[1])
          .isEqualTo("2");
      assertThat(java.nio.file.Files.readString(first.resolve("recovery-blocked")))
          .startsWith("v2-recovery-");
      two.destroy();
      assertThat(two.waitFor(15, TimeUnit.SECONDS)).isTrue();
      String normal = "process-progress-" + UUID.randomUUID();
      appendProcessEvent(normal);
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(() -> assertThat(processState(normal)).isEqualTo("PUBLISHED"));
      java.nio.file.Files.writeString(first.resolve("arm"), "stall source settlement");
      String stalled = "process-stalled-" + UUID.randomUUID();
      appendProcessEvent(stalled);
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(10))
          .until(() -> java.nio.file.Files.exists(first.resolve("confirmed-blocked")));
      int httpPort =
          Integer.parseInt(java.nio.file.Files.readString(first.resolve("ready")).split(":")[2]);
      long began = System.nanoTime();
      one.destroy();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(3))
          .untilAsserted(
              () -> {
                var connection =
                    (java.net.HttpURLConnection)
                        new java.net.URL(
                                "http://127.0.0.1:"
                                    + httpPort
                                    + "/api/v2/payment/notifications/probe")
                            .openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(500);
                connection.setReadTimeout(500);
                try {
                  assertThat(connection.getResponseCode()).isEqualTo(503);
                } finally {
                  connection.disconnect();
                }
              });
      assertThat(one.waitFor(35, TimeUnit.SECONDS))
          .withFailMessage(java.nio.file.Files.readString(first.resolve("process.log")))
          .isTrue();
      long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began);
      assertThat(millis).isBetween(28000L, 35000L);
      assertThat(processState(stalled)).isEqualTo("PROCESSING");
      System.out.println(
          "Task4 process evidence: pids="
              + one.pid()
              + ","
              + two.pid()
              + " sigtermMillis="
              + millis
              + " logs="
              + first
              + ","
              + second);
    } finally {
      for (Process process : new Process[] {one, two})
        if (process != null && process.isAlive()) {
          process.destroyForcibly();
          process.waitFor(5, TimeUnit.SECONDS);
        }
    }
  }

  private static Process startProbe(java.nio.file.Path directory) throws Exception {
    var args =
        new ArrayList<String>(
            List.of(
                java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx256m",
                "-cp",
                System.getProperty("java.class.path"),
                ProcessProbe.class.getName(),
                directory.toString(),
                "--spring.profiles.active=v2-it",
                "--server.port=0",
                "--bm.v2.enabled=true",
                "--bm.v2.asset-write-mode=V2",
                "--bm.v2.messaging.enabled=true",
                "--bm.v2.messaging.relay.mode=bounded",
                "--bm.v2.budget.replica-count=2",
                "--bm.v2.budget.physical-connection-limits.mysql-it=80",
                "--bm.v2.messaging.prefix=bromark.it.process." + UUID.randomUUID(),
                "--bm.v2.auth.issuer=https://issuer.test",
                "--bm.v2.auth.audience=bromark",
                "--bm.v2.auth.public-key="
                    + Base64.getEncoder().encodeToString(V2IdentityV2Test.publicBytes()),
                "--bm.v2.local-fallback-rps=1"));
    for (String key :
        List.of("user-rps", "activity-rps", "max-in-flight", "query-rps", "query-max-in-flight"))
      args.add("--bm.v2." + key + "=20");
    return new ProcessBuilder(args)
        .redirectErrorStream(true)
        .redirectOutput(directory.resolve("process.log").toFile())
        .start();
  }

  private static void appendProcessEvent(String id) {
    AssetTestSupport.run(
        h ->
            h.runner.inControl(
                com.jay.domain.asset.model.WorkClass.TRADE,
                new com.jay.domain.asset.model.Deadline(Instant.now().plusSeconds(10)),
                () -> {
                  new com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter(
                          h.j.getDataSource())
                      .append(PublisherConfirmV2Test.event(id));
                  return null;
                }));
  }

  private static String processState(String id) {
    var state = new java.util.concurrent.atomic.AtomicReference<String>();
    AssetTestSupport.run(
        h ->
            state.set(
                h.runner.inControl(
                    com.jay.domain.asset.model.WorkClass.QUERY,
                    new com.jay.domain.asset.model.Deadline(Instant.now().plusSeconds(5)),
                    () ->
                        h.j.queryForObject(
                            "select state from outbox where event_id=?", String.class, id))));
    return state.get();
  }

  @Test
  public void idlePollingSurvivesLostWakeAndRetryHasIndependentLane() throws Exception {
    var idleCalls = new AtomicInteger();
    var idle =
        new RelayDrainWorker(
            slot -> {
              idleCalls.incrementAndGet();
              return 0;
            },
            Duration.ofMillis(10));
    var busyCalls = new AtomicInteger();
    var busy =
        new RelayDrainWorker(
            slot -> {
              busyCalls.incrementAndGet();
              return 1;
            },
            Duration.ofMillis(250));
    try (var executor = Executors.newFixedThreadPool(2)) {
      executor.execute(busy);
      executor.execute(idle);
      try {
        org.awaitility.Awaitility.await()
            .atMost(Duration.ofSeconds(2))
            .until(() -> idleCalls.get() >= 9 && busyCalls.get() > 9);
      } finally {
        busy.quiesce();
        idle.quiesce();
      }
    }
  }

  @Test
  public void guardedDispatcherStillForbidsCallerRuns() {
    var executor =
        new ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.CallerRunsPolicy());
    try {
      assertThatThrownBy(() -> new CoalescingJobDispatcher(executor))
          .isInstanceOf(IllegalArgumentException.class);
      var properties = new com.jay.config.V2RecoveryRedriveProperties();
      properties.setMode("guarded");
      assertThatThrownBy(
              () -> new com.jay.config.V2WorkerConfig().v2JobDispatcher(executor, properties))
          .isInstanceOf(IllegalArgumentException.class);
    } finally {
      executor.shutdownNow();
    }
  }
}
