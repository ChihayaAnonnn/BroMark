package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.stock.model.ReserveCommand;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.repository.StrategyVersionRepository;
import com.jay.domain.task.model.EventEnvelope;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.cache.P0AdmissionCache;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class P0AdmissionV2Test {
  static String pool(AssetTestSupport h) {
    String pool = "p0-pool-" + UUID.randomUUID();
    StockProtocolV2Test.control(
        h,
        () -> {
          StockProtocolV2Test.service(h).createPool(pool, "initial", 1, "p0");
          return null;
        });
    return pool;
  }

  static int outbox(AssetTestSupport h, String user, String order, String type) {
    return h.tx(
        user,
        () ->
            h.j.queryForObject(
                "select count(*) from outbox where aggregate_id=? and event_type=?",
                Integer.class,
                order,
                type));
  }

  static EventEnvelope envelope(String eventId, String payload) {
    return new EventEnvelope(
        eventId,
        "ReserveAward",
        "order-" + eventId,
        new BusinessId("DRAW", "order-" + eventId, "b".repeat(64)),
        "user",
        RouteSlot.control(),
        "a".repeat(64),
        0,
        2,
        payload);
  }

  @Test
  public void admissionPersistsCandidateAndReserveAwardWithoutPrepareDraw() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long activity = DrawAcceptanceV2Test.publish(h, pool(h));
          String user = "p0-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, user, activity);
          var svc = DrawAcceptanceV2Test.service(h);
          var view =
              svc.create(
                  new DrawCommand(
                      user,
                      "req-" + UUID.randomUUID(),
                      activity,
                      new Deadline(Instant.now().plusSeconds(60))));
          var r =
              h.tx(
                  user,
                  () ->
                      new MySqlDrawRepository(h.j.getDataSource()).find(view.orderId(), false));
          assertThat(r.state()).isEqualTo("PENDING_RESERVATION");
          assertThat(r.candidate()).isNotNull();
          assertThat(r.fallback().type()).isEqualTo("NONE");
          assertThat(outbox(h, user, view.orderId(), "ReserveAward")).isOne();
          assertThat(outbox(h, user, view.orderId(), "PrepareDraw")).isZero();

          String body =
              h.tx(
                  user,
                  () ->
                      h.j.queryForObject(
                          "select payload from outbox where aggregate_id=? and"
                              + " event_type='ReserveAward'",
                          String.class,
                          view.orderId()));
          var event = new ObjectMapper().readTree(body);
          assertThat(event.path("schemaVersion").asInt()).isEqualTo(2);
          var command = ReceiptJson.read(event.path("payload").asText(), ReserveCommand.class);
          assertThat(command.drawOrderId()).isEqualTo(view.orderId());
          assertThat(command.userId()).isEqualTo(user);
          assertThat(command.strategyId()).isEqualTo(r.strategy().strategyId());
          assertThat(command.version()).isEqualTo(r.version());
          assertThat(command.candidate()).isEqualTo(r.candidate());
          assertThat(command.fallback()).isEqualTo(r.fallback());
        });
  }

  @Test
  public void sameRequestReturnsSameOrderAndOneOutboxRow() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long activity = DrawAcceptanceV2Test.publish(h, pool(h));
          String user = "p0-repeat-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, user, activity);
          var svc = DrawAcceptanceV2Test.service(h);
          var command =
              new DrawCommand(
                  user,
                  "req-" + UUID.randomUUID(),
                  activity,
                  new Deadline(Instant.now().plusSeconds(60)));
          var first = svc.create(command);
          var second = svc.create(command);
          assertThat(second.orderId()).isEqualTo(first.orderId());
          assertThat(outbox(h, user, first.orderId(), "ReserveAward")).isOne();
          assertThat(
                  h.tx(
                      user,
                      () -> {
                        var t = h.assets.lockAccounts(user, List.of(activity));
                        return h.repo.quotaState(user, activity, t).total();
                      }))
              .isEqualTo(9);
        });
  }

  @Test
  public void admissionCacheAvoidsRepeatedConfigRead() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          long activity = DrawAcceptanceV2Test.publish(h, pool(h));
          var real = new MySqlStrategyVersionRepository(h.j.getDataSource());
          var calls = new AtomicInteger();
          var repo =
              new StrategyVersionRepository() {
                public StrategyVersionRepository.DrawAdmission admit(
                    String user, long id, Deadline deadline) {
                  calls.incrementAndGet();
                  return new StrategyVersionRepository.DrawAdmission(
                      real.current(id), false, Instant.now().plusSeconds(30));
                }

                public void publish(FrozenStrategy strategy) {
                  real.publish(strategy);
                }

                public FrozenStrategy load(long strategy, String version) {
                  return real.load(strategy, version);
                }

                public FrozenStrategy current(long id) {
                  return real.current(id);
                }
              };
          var cache = new P0AdmissionCache(h.runner, repo, Duration.ofSeconds(2));
          var deadline = new Deadline(Instant.now().plusSeconds(30));
          String user = "p0-cache-" + UUID.randomUUID();
          assertThat(cache.admit(user, activity, deadline).strategy().activityId())
              .isEqualTo(activity);
          assertThat(cache.admit(user, activity, deadline).strategy().activityId())
              .isEqualTo(activity);
          assertThat(calls).hasValue(1);
          cache.invalidate(user, activity);
          assertThat(cache.admit(user, activity, deadline).strategy().activityId())
              .isEqualTo(activity);
          assertThat(calls).hasValue(2);
        });
  }

  @Test
  public void outboxWriterAcceptsNewInsertAndRejectsChangedDuplicate() {
    AssetTestSupport.run(
        h -> {
          ConsumerInboxV2Test.schema(h);
          var writer = new MySqlOutboxWriter(h.j.getDataSource());
          String eventId = "evt-" + UUID.randomUUID();
          var first = envelope(eventId, "{\"amount\":\"1.00\"}");
          var same = envelope(eventId, "{\"amount\":\"1.00\"}");
          var changed = envelope(eventId, "{\"amount\":\"2.00\"}");
          h.runner.inControl(
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(30)),
              () -> {
                writer.append(first);
                writer.append(same);
                return null;
              });
          assertThat(
                  h.j.queryForObject(
                      "select count(*) from outbox where event_id=?", Integer.class, eventId))
              .isOne();
          String hashBefore =
              h.j.queryForObject(
                  "select payload_hash from outbox where event_id=?", String.class, eventId);
          assertThatThrownBy(
                  () ->
                      h.runner.inControl(
                          WorkClass.RECOVERY,
                          new Deadline(Instant.now().plusSeconds(30)),
                          () -> {
                            writer.append(first);
                            writer.append(changed);
                            return null;
                          }))
              .isInstanceOf(AssetConflictException.class);
          assertThat(
                  h.j.queryForObject(
                      "select count(*) from outbox where event_id=?", Integer.class, eventId))
              .isOne();
          assertThat(
                  h.j.queryForObject(
                      "select payload_hash from outbox where event_id=?", String.class, eventId))
              .isEqualTo(hashBefore);
          assertThat(
                  h.j.queryForObject(
                      "select payload from outbox where event_id=?", String.class, eventId))
              .contains("1.00");
        });
  }
}
