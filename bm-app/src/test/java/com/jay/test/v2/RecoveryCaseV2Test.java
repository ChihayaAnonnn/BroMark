package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.repository.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.springframework.dao.OptimisticLockingFailureException;

public class RecoveryCaseV2Test {
  @Test
  public void pausePreservesStageAndResumeReplaysLateFactsWithVersionFence() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          String user = "paused-" + UUID.randomUUID(), id = UUID.randomUUID().toString();
          var slot = h.codec.routeUser(user);
          h.tx(
              user,
              () -> {
                h.j.execute(
                    "create table if not exists recovery_test_aggregate(id varchar(128) primary"
                        + " key,state varchar(64),recovery_status varchar(32),version bigint)"
                        + " ENGINE=InnoDB");
                h.j.update(
                    "insert into recovery_test_aggregate values(?,'RESULT_RECORDED','ACTIVE',1)",
                    id);
                return null;
              });
          var port =
              new RecoveryAggregatePort() {
                public RecoveryAggregate lock(String key) {
                  return h.j.queryForObject(
                      "select * from recovery_test_aggregate where id=? for update",
                      (r, i) ->
                          new RecoveryAggregate(
                              r.getString("id"),
                              r.getString("state"),
                              RecoveryStatus.valueOf(r.getString("recovery_status")),
                              r.getLong("version")),
                      key);
                }

                public boolean updateStatus(String key, long version, RecoveryStatus status) {
                  return h.j.update(
                          "update recovery_test_aggregate set recovery_status=?,version=version+1"
                              + " where id=? and version=?",
                          status.name(),
                          key,
                          version)
                      == 1;
                }
              };
          var repo = new MySqlRecoveryRepository(h.j.getDataSource());
          var service =
              new RecoveryService(
                  h.runner,
                  repo,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("CREDIT_DELIVERY", port),
                  () -> "replay-" + UUID.randomUUID());
          service.pause(slot, "CREDIT_DELIVERY", id, RecoveryStatus.REVIEW, "amount conflict");
          var inbox = new MySqlConsumerInbox(h.j.getDataSource());
          var uow =
              new ConsumerUnitOfWork(
                  h.runner,
                  inbox,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  new EventRouting(h.codec),
                  service,
                  () -> "response-" + UUID.randomUUID());
          var e =
              new EventEnvelope(
                  "late-" + UUID.randomUUID(),
                  "GrantCredit",
                  id,
                  new BusinessId("GRANT", id, "a".repeat(64)),
                  user,
                  slot,
                  "a".repeat(64),
                  1,
                  1,
                  "{}");
          assertThat(
                  uow.apply(
                      "workflow-v2",
                      e,
                      () -> {
                        throw new AssertionError("paused asset execution");
                      }))
              .isEqualTo(ConsumeOutcome.DEFERRED);
          assertThat(
                  uow.apply(
                      "workflow-v2",
                      e,
                      () -> {
                        throw new AssertionError("paused duplicate execution");
                      }))
              .isEqualTo(ConsumeOutcome.DEFERRED);
          h.tx(
              user,
              () -> {
                var a = port.lock(id);
                assertThat(a.state()).isEqualTo("RESULT_RECORDED");
                assertThat(a.recoveryStatus()).isEqualTo(RecoveryStatus.REVIEW);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where event_id=?",
                            Integer.class,
                            e.eventId()))
                    .isOne();
                return null;
              });
          assertThatThrownBy(
                  () -> service.resume(slot, "CREDIT_DELIVERY", id, 1, "operator", "fixed"))
              .isInstanceOf(OptimisticLockingFailureException.class);
          service.resume(slot, "CREDIT_DELIVERY", id, 2, "operator", "validated original amount");
          assertThatThrownBy(
                  () -> service.resume(slot, "CREDIT_DELIVERY", id, 2, "operator", "duplicate"))
              .isInstanceOf(OptimisticLockingFailureException.class);
          h.tx(
              user,
              () -> {
                var a = port.lock(id);
                assertThat(a.state()).isEqualTo("RESULT_RECORDED");
                assertThat(a.recoveryStatus()).isEqualTo(RecoveryStatus.ACTIVE);
                var json =
                    h.j.queryForObject(
                        "select payload from outbox where aggregate_id=?", String.class, id);
                var replay = JsonStore.read(json, EventEnvelope.class);
                assertThat(replay.eventId()).isNotEqualTo(e.eventId());
                assertThat(replay.bizId()).isEqualTo(e.bizId());
                assertThat(
                        h.j.queryForObject(
                            "select operator_id from recovery_case where aggregate_id=?",
                            String.class,
                            id))
                    .isEqualTo("operator");
                return null;
              });
        });
  }

  @Test
  public void unknownAggregateBindsTaskWithoutInventingBusinessState() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          String id = "unknown-" + UUID.randomUUID();
          var port =
              new RecoveryAggregatePort() {
                public RecoveryAggregate lock(String id) {
                  return null;
                }

                public boolean updateStatus(String id, long version, RecoveryStatus status) {
                  throw new AssertionError("invented aggregate update");
                }
              };
          var service =
              new RecoveryService(
                  h.runner,
                  new MySqlRecoveryRepository(h.j.getDataSource()),
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("CREDIT_DELIVERY", port),
                  () -> "replay-" + UUID.randomUUID());
          service.pause(
              RouteSlot.control(),
              "CREDIT_DELIVERY",
              id,
              RecoveryStatus.REVIEW,
              "original task has no valid aggregate");
          assertThat(
                  h.j.queryForObject(
                      "select business_state from recovery_case where aggregate_id=?",
                      String.class,
                      id))
              .isEqualTo("UNBOUND");
        });
  }

  @Test
  public void largeDeferredBacklogContinuesInBoundedScannerAfterAuditedResume() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          String user = "backlog-" + UUID.randomUUID(), id = UUID.randomUUID().toString();
          var slot = h.codec.routeUser(user);
          var repo = new MySqlRecoveryRepository(h.j.getDataSource());
          var service =
              new RecoveryService(
                  h.runner,
                  repo,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of(),
                  () -> "batch-replay-" + UUID.randomUUID());
          service.pause(slot, "CREDIT_DELIVERY", id, RecoveryStatus.WAITING_FIX, "repair handler");
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                for (int i = 0; i < 130; i++)
                  repo.defer(
                      "workflow-v2",
                      "CREDIT_DELIVERY",
                      ConsumerInboxV2Test.event(h, "batch-" + i + "-" + id, user, id));
                return null;
              });
          service.resume(
              slot, "CREDIT_DELIVERY", id, 1, "operator", "handler repaired and facts verified");
          assertThat(service.scan(slot, 8)).isEqualTo(2);
          h.runner.inSlot(
              slot,
              WorkClass.QUERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from outbox where aggregate_id=?", Integer.class, id))
                    .isEqualTo(130);
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from deferred_fact where aggregate_id=? and"
                                + " replayed_event_id is null",
                            Integer.class,
                            id))
                    .isZero();
                return null;
              });
          assertThat(service.scan(slot, 8)).isZero();
        });
  }

  @Test
  public void terminalResumeClosesCaseWithoutRewindingStageOrCreatingCommands() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          String user = "terminal-" + UUID.randomUUID(), id = UUID.randomUUID().toString();
          var slot = h.codec.routeUser(user);
          h.tx(
              user,
              () -> {
                h.j.execute(
                    "create table if not exists recovery_test_aggregate(id varchar(128) primary"
                        + " key,state varchar(64),recovery_status varchar(32),version bigint)"
                        + " ENGINE=InnoDB");
                h.j.update(
                    "insert into recovery_test_aggregate values(?,'DELIVERED','ACTIVE',1)", id);
                return null;
              });
          var port =
              new RecoveryAggregatePort() {
                public RecoveryAggregate lock(String key) {
                  return h.j.queryForObject(
                      "select * from recovery_test_aggregate where id=? for update",
                      (r, i) ->
                          new RecoveryAggregate(
                              r.getString("id"),
                              r.getString("state"),
                              RecoveryStatus.valueOf(r.getString("recovery_status")),
                              r.getLong("version")),
                      key);
                }

                public boolean updateStatus(String key, long expected, RecoveryStatus next) {
                  return h.j.update(
                          "update recovery_test_aggregate set recovery_status=?,version=version+1"
                              + " where id=? and version=?",
                          next.name(),
                          key,
                          expected)
                      == 1;
                }

                public boolean terminal(RecoveryAggregate a) {
                  return a.state().equals("DELIVERED");
                }
              };
          var repo = new MySqlRecoveryRepository(h.j.getDataSource());
          var service =
              new RecoveryService(
                  h.runner,
                  repo,
                  new MySqlOutboxWriter(h.j.getDataSource()),
                  Map.of("CREDIT_DELIVERY", port),
                  () -> "terminal-" + UUID.randomUUID());
          service.pause(
              slot, "CREDIT_DELIVERY", id, RecoveryStatus.REVIEW, "late fact on terminal");
          h.runner.inSlot(
              slot,
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                repo.defer(
                    ConsumerInboxV2Test.event(h, "late-terminal-" + UUID.randomUUID(), user, id));
                return null;
              });
          service.resume(
              slot, "CREDIT_DELIVERY", id, 2, "operator", "already delivered, evidence reviewed");
          h.runner.inSlot(
              slot,
              WorkClass.QUERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                assertThat(port.lock(id).state()).isEqualTo("DELIVERED");
                assertThat(
                        h.j.queryForObject(
                            "select count(*) from outbox where aggregate_id=?", Integer.class, id))
                    .isZero();
                assertThat(
                        h.j.queryForObject(
                            "select replayed_event_id from deferred_fact where aggregate_id=?",
                            String.class,
                            id))
                    .isEqualTo("TERMINAL_NOOP");
                return null;
              });
        });
  }
}
