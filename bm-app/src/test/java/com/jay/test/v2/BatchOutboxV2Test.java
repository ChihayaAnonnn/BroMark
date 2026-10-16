package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class BatchOutboxV2Test {
  @Test
  public void claimsFairBatchAndFencesExpiredOwner() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from outbox");
                var writer = new MySqlOutboxWriter(h.j.getDataSource());
                for (int i = 0; i < 100; i++)
                  writer.append(RelayOptimizationTestSupport.event("batch-" + i, "PrepareDraw"));
                for (String type :
                    List.of(
                        "ReserveAward",
                        "ConfirmReservation",
                        "CancelReservation",
                        "CreditGranted",
                        "ReservationHeld",
                        "ReservationConfirmed",
                        "AwardDelivery",
                        "GrantCredit"))
                  writer.append(RelayOptimizationTestSupport.event("sparse-" + type, type));
                return null;
              });
          var first =
              repo.claimBatch(
                  RouteSlot.control(),
                  RelayOptimizationTestSupport.policy(),
                  RelayOptimizationTestSupport.deadline());
          assertThat(first).hasSize(32);
          assertThat(first.stream().map(ClaimedPublish::eventId).distinct()).hasSize(32);
          assertThat(first.stream().map(ClaimedPublish::eventType).distinct().count())
              .isGreaterThanOrEqualTo(8);
          var old = first.getFirst();
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update(
                    "update outbox set lease_until=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND where"
                        + " event_id=?",
                    old.eventId());
                return null;
              });
          var second =
              repo.claimBatch(
                  RouteSlot.control(),
                  RelayOptimizationTestSupport.policy(),
                  RelayOptimizationTestSupport.deadline());
          assertThat(second.stream().map(ClaimedPublish::eventId)).contains(old.eventId());
          var report =
              repo.settleBatch(
                  RouteSlot.control(),
                  first.stream()
                      .map(t -> new PublishResolution(t, PublishOutcome.CONFIRMED, "confirm"))
                      .toList(),
                  RelayOptimizationTestSupport.deadline());
          assertThat(report.lost()).containsExactly(old.eventId());
          assertThat(report.completed()).hasSize(31).doesNotContain(old.eventId());
          assertThat(
                  repo.releaseUnsent(
                      RouteSlot.control(), List.of(old), RelayOptimizationTestSupport.deadline()))
              .isEmpty();
          assertThat(
                  repo.releaseUnsent(
                      RouteSlot.control(), second, RelayOptimizationTestSupport.deadline()))
              .hasSize(32);
        });
  }

  @Test
  public void independentInstancesClaimDisjointBatchesAndExpiredOnlyFillsBatch() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from outbox");
                var w = new MySqlOutboxWriter(h.j.getDataSource());
                for (int i = 0; i < 64; i++)
                  w.append(RelayOptimizationTestSupport.event("concurrent-" + i, "PrepareDraw"));
                return null;
              });
          var batches =
              com.jay.test.v2.support.ConcurrentProbe.run(
                  2,
                  () ->
                      new MySqlOutboxRepository(
                              h.j.getDataSource(),
                              new com.jay.infrastructure.transaction.RoutedTransactionRunner(
                                  h.j.getDataSource(),
                                  new org.springframework.jdbc.datasource
                                      .DataSourceTransactionManager(h.j.getDataSource()),
                                  h.codec))
                          .claimBatch(
                              RouteSlot.control(),
                              RelayOptimizationTestSupport.policy(),
                              RelayOptimizationTestSupport.deadline()));
          assertThat(batches.stream().flatMap(List::stream).map(ClaimedPublish::eventId).distinct())
              .hasSize(64);
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("update outbox set lease_until=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND");
                return null;
              });
          var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
          assertThat(
                  repo.claimBatch(
                      RouteSlot.control(),
                      RelayOptimizationTestSupport.policy(),
                      RelayOptimizationTestSupport.deadline()))
              .hasSize(32);
        });
  }

  @Test
  public void isolatesMalformedEnvelopeAndPreservesUnsentAttempt() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from outbox");
                var w = new MySqlOutboxWriter(h.j.getDataSource());
                w.append(RelayOptimizationTestSupport.event("malformed-envelope", "PrepareDraw"));
                w.append(RelayOptimizationTestSupport.event("valid-envelope", "PrepareDraw"));
                h.j.update("update outbox set payload='{}' where event_id='malformed-envelope'");
                return null;
              });
          var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
          var tasks =
              repo.claimBatch(
                  RouteSlot.control(),
                  RelayOptimizationTestSupport.policy(),
                  RelayOptimizationTestSupport.deadline());
          assertThat(tasks).hasSize(2);
          var bad =
              tasks.stream()
                  .filter(t -> t.eventId().equals("malformed-envelope"))
                  .findFirst()
                  .orElseThrow();
          assertThat(
                  repo.quarantineMalformed(
                      bad, "invalid envelope", RelayOptimizationTestSupport.deadline()))
              .isTrue();
          assertThat(
                  repo.quarantineMalformed(
                      bad, "invalid envelope", RelayOptimizationTestSupport.deadline()))
              .isFalse();
          assertThat(
                  repo.releaseUnsent(
                      RouteSlot.control(),
                      tasks.stream().filter(t -> !t.eventId().equals(bad.eventId())).toList(),
                      RelayOptimizationTestSupport.deadline()))
              .containsExactly("valid-envelope");
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select attempt from outbox where event_id='valid-envelope'",
                            Integer.class))
                    .isZero();
                assertThat(
                        h.j.queryForObject(
                            "select state from outbox where event_id='malformed-envelope'",
                            String.class))
                    .isEqualTo("DEAD");
                return null;
              });
        });
  }

  @Test
  public void retrySnapshotKeepsFailureGenerationAcrossPublishFailures() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          var repo = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner);
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from retry_job");
                repo.schedule(
                    "notification-v2",
                    RelayOptimizationTestSupport.event("retry-snapshot", "ExchangeCompleted"),
                    3,
                    Instant.now());
                return null;
              });
          var task =
              repo.claimBatch(
                      RouteSlot.control(),
                      RelayOptimizationTestSupport.policy(),
                      RelayOptimizationTestSupport.deadline())
                  .getFirst();
          assertThat(task.failureCount()).isEqualTo(3);
          assertThat(task.consumerName()).isEqualTo("notification-v2");
          var report =
              repo.settleBatch(
                  RouteSlot.control(),
                  List.of(new PublishResolution(task, PublishOutcome.NACKED, "nack")),
                  RelayOptimizationTestSupport.deadline());
          assertThat(report.retried()).containsExactly(task.eventId());
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select failure_count from retry_job where event_id=?",
                            Integer.class,
                            task.eventId()))
                    .isEqualTo(3);
                assertThat(
                        h.j.queryForObject(
                            "select attempt from retry_job where event_id=?",
                            Integer.class,
                            task.eventId()))
                    .isEqualTo(1);
                return null;
              });
        });
  }

  @Test
  public void lostRetryRollsBackDeferredFactAndOwnedTwelfthAttemptIsDead() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          DrawAcceptanceV2Test.schema(h);
          String pool = "batch-recovery-" + UUID.randomUUID();
          StockProtocolV2Test.control(
              h,
              () -> {
                StockProtocolV2Test.service(h).createPool(pool, "initial", 1, "test");
                return null;
              });
          var draw =
              DrawCancellationV2Test.accepted(
                  h,
                  "batch-retry-user-" + UUID.randomUUID(),
                  DrawAcceptanceV2Test.publish(h, pool));
          var slot = h.codec.routeUser(draw.userId());
          var recovery =
              new com.jay.domain.task.service.RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("DRAW", new MySqlDrawRepository(h.j.getDataSource())),
                  h.codec::newEventId);
          var repo = new MySqlRetryJobRepository(h.j.getDataSource(), h.runner, recovery);
          var event =
              com.jay.domain.raffle.service.DrawService.event(
                  draw, "ReservationHeld", slot, h.codec.newEventId(), "{}");
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from retry_job");
                repo.schedule("workflow-v2", event, 3, Instant.now());
                h.j.update("update retry_job set attempt=11 where event_id=?", event.eventId());
                return null;
              });
          var old =
              repo.claimBatch(
                      slot,
                      RelayOptimizationTestSupport.policy(),
                      RelayOptimizationTestSupport.deadline())
                  .getFirst();
          recovery.pause(
              slot, "DRAW", draw.orderId(), RecoveryStatus.REVIEW, "paused during confirm");
          String newToken = UUID.randomUUID().toString();
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update(
                    "update retry_job set lease_token=? where event_id=?",
                    newToken,
                    event.eventId());
                return null;
              });
          var lost =
              repo.settleBatch(
                  slot,
                  List.of(new PublishResolution(old, PublishOutcome.UNKNOWN, "late")),
                  RelayOptimizationTestSupport.deadline());
          assertThat(lost.lost()).containsExactly(event.eventId());
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isZero();
                return null;
              });
          var current =
              new ClaimedPublish(
                  old.ownerSlot(),
                  old.eventId(),
                  old.aggregateId(),
                  old.aggregateVersion(),
                  old.eventType(),
                  old.rawEnvelope(),
                  old.payloadHash(),
                  newToken,
                  old.leaseUntil(),
                  old.attempt(),
                  old.failureCount(),
                  old.consumerName(),
                  old.claimedAt());
          var dead =
              repo.settleBatch(
                  slot,
                  List.of(new PublishResolution(current, PublishOutcome.UNKNOWN, "exhausted")),
                  RelayOptimizationTestSupport.deadline());
          assertThat(dead.dead()).containsExactly(event.eventId());
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select state from retry_job where event_id=?",
                            String.class,
                            event.eventId()))
                    .isEqualTo("DEAD");
                assertThat(
                        h.j.queryForObject(
                            "select failure_count from retry_job where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isEqualTo(3);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where event_id=?",
                            Integer.class,
                            event.eventId()))
                    .isOne();
                return null;
              });
        });
  }

  @Test
  public void batchSqlCountsDoNotGrowOneTransactionPerEvent() {
    AssetTestSupport.run(
        h -> {
          RelayOptimizationTestSupport.schema(h);
          h.runner.inControl(
              WorkClass.RECOVERY,
              RelayOptimizationTestSupport.deadline(),
              () -> {
                h.j.update("delete from outbox");
                var w = new MySqlOutboxWriter(h.j.getDataSource());
                for (int i = 0; i < 32; i++)
                  w.append(RelayOptimizationTestSupport.event("sql-count-" + i, "PrepareDraw"));
                return null;
              });
          var counts = new ArrayList<long[]>();
          var measured =
              new com.jay.domain.asset.repository.UserTransactionRunner() {
                public <T> T inUser(
                    String u, WorkClass w, Deadline d, java.util.function.Supplier<T> a) {
                  throw new AssertionError();
                }

                public <T> T inControl(WorkClass w, Deadline d, java.util.function.Supplier<T> a) {
                  throw new AssertionError();
                }

                private long[] stats() {
                  var values = new HashMap<String, Long>();
                  h.j
                      .queryForList(
                          "show session status where Variable_name in ('Com_select','Com_update')")
                      .forEach(
                          row ->
                              values.put(
                                  (String) row.get("Variable_name"),
                                  Long.parseLong((String) row.get("Value"))));
                  return new long[] {values.get("Com_select"), values.get("Com_update")};
                }

                public <T> T inSlot(
                    RouteSlot slot, WorkClass w, Deadline d, java.util.function.Supplier<T> a) {
                  return h.runner.inSlot(
                      slot,
                      w,
                      d,
                      () -> {
                        var before = stats();
                        T result = a.get();
                        var after = stats();
                        counts.add(new long[] {after[0] - before[0], after[1] - before[1]});
                        return result;
                      });
                }
              };
          var repo = new MySqlOutboxRepository(h.j.getDataSource(), measured);
          var tasks =
              repo.claimBatch(
                  RouteSlot.control(),
                  RelayOptimizationTestSupport.policy(),
                  RelayOptimizationTestSupport.deadline());
          repo.settleBatch(
              RouteSlot.control(),
              tasks.stream()
                  .map(t -> new PublishResolution(t, PublishOutcome.CONFIRMED, "confirmed"))
                  .toList(),
              RelayOptimizationTestSupport.deadline());
          assertThat(counts).hasSize(2);
          assertThat(counts.get(0)[0]).isLessThanOrEqualTo(16);
          assertThat(counts.get(0)[1]).isEqualTo(1);
          assertThat(counts.get(1)).containsExactly(2, 1);
          System.out.println(
              "BATCH_SQL_COUNTS claimSelect="
                  + counts.get(0)[0]
                  + " claimUpdate="
                  + counts.get(0)[1]
                  + " settleSelect="
                  + counts.get(1)[0]
                  + " settleUpdate="
                  + counts.get(1)[1]);
        });
  }
  @Test public void disconnectedSettlementNeverReturnsFalseSuccessReport() {
    AssetTestSupport.run(h -> {
      RelayOptimizationTestSupport.schema(h);
      h.runner.inControl(WorkClass.RECOVERY,RelayOptimizationTestSupport.deadline(),()-> {
        h.j.update("delete from outbox");new MySqlOutboxWriter(h.j.getDataSource()).append(RelayOptimizationTestSupport.event("disconnect-settle","PrepareDraw"));return null;
      });
      var repo=new MySqlOutboxRepository(h.j.getDataSource(),h.runner);
      var task=repo.claimBatch(RouteSlot.control(),RelayOptimizationTestSupport.policy(),RelayOptimizationTestSupport.deadline()).getFirst();
      var disconnecting=(com.jay.domain.asset.repository.UserTransactionRunner)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{com.jay.domain.asset.repository.UserTransactionRunner.class},(obj,method,args)-> {
        @SuppressWarnings("unchecked") var action=(java.util.function.Supplier<Object>)args[3];
        args[3]=(java.util.function.Supplier<Object>)()-> {
          var value=action.get();long connection=h.j.queryForObject("select CONNECTION_ID()",Long.class);
          try(var admin=h.j.getDataSource().getConnection();var statement=admin.createStatement()) {statement.execute("KILL CONNECTION "+connection);} catch(java.sql.SQLException e){throw new IllegalStateException(e);}
          return value;
        };
        try{return method.invoke(h.runner,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
      });
      var interrupted=new MySqlOutboxRepository(h.j.getDataSource(),disconnecting);
      assertThatThrownBy(()->interrupted.settleBatch(RouteSlot.control(),List.of(new PublishResolution(task,PublishOutcome.CONFIRMED,"confirmed")),RelayOptimizationTestSupport.deadline())).isInstanceOf(RuntimeException.class);
      h.runner.inControl(WorkClass.RECOVERY,RelayOptimizationTestSupport.deadline(),()-> {
        assertThat(h.j.queryForObject("select state from outbox where event_id=?",String.class,task.eventId())).isEqualTo("PROCESSING");return null;
      });
    });
  }
}
