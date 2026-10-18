package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.stock.model.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.JsonStore;
import com.jay.infrastructure.adapter.repository.v2.MySqlDrawRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.Test;

/**
 * P0 直接服务端到端验收：create -> confirmOnReserve -> deliver，全程不启动 Rabbit/Redis。
 *
 * <p>三个服务按生产消息的意义串联：从 user 库 outbox 的 {@code ReserveAward} 行解析
 * {@code ReserveCommand}，在 control 事务里确认预占得到 {@code ReservationResult}，再把它包成
 * schema 2 的 {@code ReservationConfirmed} 调用交付。对账覆盖两个 user 库的全部 4 张分表以及
 * control 侧的 {@code award_stock}/{@code award_reservation} 守恒。
 */
public class P0EndToEndV2Test {
  @Test
  public void creditThenNoneThenDuplicateIsConsistent() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String pool = pool(h, 1);
          long activity = DrawAcceptanceV2Test.publish(h, pool);
          // 两个用户落在不同 user 库，确保对账真的跨 2 个库。
          String[] users = usersInDifferentDatabases(h);
          for (String user : users) DrawAcceptanceV2Test.grant(h, user, activity);
          var svc = DrawAcceptanceV2Test.service(h);

          String creditOrder = create(svc, users[0], activity);
          String noneOrder = create(svc, users[1], activity);
          assertThat(record(h, users[0], creditOrder).state()).isEqualTo("PENDING_RESERVATION");
          assertThat(record(h, users[1], noneOrder).state()).isEqualTo("PENDING_RESERVATION");

          // 1) 从 outbox payload 还原真正会发给 control 的 ReserveAward。
          var creditCommand = reserveAward(h, users[0], creditOrder);
          var noneCommand = reserveAward(h, users[1], noneOrder);
          assertThat(creditCommand.command().drawOrderId()).isEqualTo(creditOrder);
          assertThat(creditCommand.command().candidate().type()).isEqualTo("CREDIT");
          assertThat(noneCommand.command().candidate().type()).isEqualTo("CREDIT");

          // 2) control 事务确认：第一单占用唯一库存，第二单落到 fallback NONE。
          var credit = confirm(h, creditCommand.command());
          var none = confirm(h, noneCommand.command());
          assertThat(credit.state()).isEqualTo("CONFIRMED");
          assertThat(credit.finalSnapshot().type()).isEqualTo("CREDIT");
          assertThat(credit.quantity()).isOne();
          assertThat(none.state()).isEqualTo("CONFIRMED");
          assertThat(none.finalSnapshot()).isEqualTo(noneCommand.command().fallback());
          assertThat(none.finalSnapshot().type()).isEqualTo("NONE");
          assertThat(none.quantity()).isZero();
          assertThat(stock(h, pool))
              .containsEntry("total", 1L)
              .containsEntry("available", 0L)
              .containsEntry("reserved", 0L)
              .containsEntry("allocated", 1L);

          // 重复确认返回原回执，不二次占用库存。
          assertThat(confirm(h, creditCommand.command())).isEqualTo(credit);
          assertThat(confirm(h, noneCommand.command())).isEqualTo(none);
          assertThat(stock(h, pool))
              .containsEntry("total", 1L)
              .containsEntry("available", 0L)
              .containsEntry("reserved", 0L)
              .containsEntry("allocated", 1L);

          // 3) 用 schema 2 的 ReservationConfirmed 交付。
          var creditEvent =
              confirmed(h, credit, creditCommand.envelope().aggregateVersion());
          assertThat(creditEvent.schemaVersion()).isEqualTo(2);
          assertThat(creditEvent.eventType()).isEqualTo("ReservationConfirmed");
          deliver(h, creditEvent);
          var deliveredCredit = record(h, users[0], creditOrder);
          assertThat(deliveredCredit.state()).isEqualTo("DELIVERED");
          assertThat(deliveredCredit.finalAward()).isEqualTo(credit.finalSnapshot());
          assertThat(h.balance(users[0]))
              .isEqualByComparingTo(credit.finalSnapshot().amount());
          assertThat(deliveryRows(h, users[0], creditOrder)).isOne();

          var noneEvent = confirmed(h, none, noneCommand.envelope().aggregateVersion());
          deliver(h, noneEvent);
          var deliveredNone = record(h, users[1], noneOrder);
          assertThat(deliveredNone.state()).isEqualTo("DELIVERED");
          assertThat(deliveredNone.finalAward()).isEqualTo(none.finalSnapshot());
          assertThat(h.balance(users[1])).isEqualByComparingTo("0.00");
          assertThat(deliveryRows(h, users[1], noneOrder)).isOne();

          // 4) 重复确认 + 重复交付都只加一次积分；NONE 重复交付仍不加积分。
          var duplicateConfirm = confirm(h, creditCommand.command());
          assertThat(duplicateConfirm).isEqualTo(credit);
          deliver(
              h, confirmed(h, duplicateConfirm, creditCommand.envelope().aggregateVersion()));
          assertThat(h.balance(users[0]))
              .isEqualByComparingTo(credit.finalSnapshot().amount());
          deliver(h, confirmed(h, credit, creditCommand.envelope().aggregateVersion()));
          assertThat(h.balance(users[0]))
              .isEqualByComparingTo(credit.finalSnapshot().amount());
          assertThat(record(h, users[0], creditOrder).state()).isEqualTo("DELIVERED");
          assertThat(deliveryRows(h, users[0], creditOrder)).isOne();

          deliver(h, confirmed(h, none, noneCommand.envelope().aggregateVersion()));
          assertThat(h.balance(users[1])).isEqualByComparingTo("0.00");
          assertThat(record(h, users[1], noneOrder).state()).isEqualTo("DELIVERED");
          assertThat(deliveryRows(h, users[1], noneOrder)).isOne();

          // 5) 对账：2 个 user 库 × 4 张分表 + control 库存/预占。
          reconcile(
              h,
              List.of(users),
              List.of(
                  new Delivered(creditOrder, users[0], "CREDIT", credit.finalSnapshot().amount()),
                  new Delivered(noneOrder, users[1], "NONE", null)),
              List.of(),
              Map.of(creditOrder, "CREDIT", noneOrder, "NONE"),
              Map.of(pool, 1L));
          assertThat(stock(h, pool))
              .containsEntry("total", 1L)
              .containsEntry("available", 0L)
              .containsEntry("reserved", 0L)
              .containsEntry("allocated", 1L);
        });
  }

  @Test
  public void tamperedFinalAwardLeavesOrderPendingAndConservesStock() {
    AssetTestSupport.run(
        h -> {
          DrawAcceptanceV2Test.schema(h);
          String pool = pool(h, 1);
          long activity = DrawAcceptanceV2Test.publish(h, pool);
          String user = "p0-e2e-tamper-" + UUID.randomUUID();
          DrawAcceptanceV2Test.grant(h, user, activity);
          var svc = DrawAcceptanceV2Test.service(h);
          String order = create(svc, user, activity);
          var command = reserveAward(h, user, order);
          var confirmed = confirm(h, command.command());
          assertThat(confirmed.finalSnapshot().type()).isEqualTo("CREDIT");

          // finalAward 篡改：金额和 digest 同时偏离 control 的确认结果。
          var award = confirmed.finalSnapshot();
          var tampered =
              new AwardSnapshot(
                  award.awardId(),
                  award.type(),
                  award.amount().add(new BigDecimal("0.01")),
                  award.stockPoolId(),
                  award.quantity(),
                  "f".repeat(64));
          var tamperedEvent =
              confirmed(
                  h,
                  new ReservationResult(
                      confirmed.drawOrderId(),
                      confirmed.userId(),
                      confirmed.identityHash(),
                      confirmed.requestHash(),
                      "CONFIRMED",
                      tampered,
                      tampered.quantity()),
                  command.envelope().aggregateVersion());
          assertThatThrownBy(() -> deliver(h, tamperedEvent))
              .isInstanceOf(AssetConflictException.class);

          assertThat(record(h, user, order).state()).isEqualTo("PENDING_RESERVATION");
          assertThat(record(h, user, order).finalAward()).isNull();
          assertThat(h.balance(user)).isEqualByComparingTo("0.00");
          assertThat(deliveryRows(h, user, order)).isZero();
          // 预占仍然保留，库存守恒，只是订单还没有交付。
          reconcile(
              h,
              List.of(user),
              List.of(),
              List.of(order),
              Map.of(order, "CREDIT"),
              Map.of(pool, 1L));
          assertThat(stock(h, pool))
              .containsEntry("total", 1L)
              .containsEntry("available", 0L)
              .containsEntry("reserved", 0L)
              .containsEntry("allocated", 1L);

          // 篡改不污染订单：正确回执仍可交付一次。
          deliver(h, confirmed(h, confirmed, command.envelope().aggregateVersion()));
          assertThat(record(h, user, order).state()).isEqualTo("DELIVERED");
          assertThat(record(h, user, order).finalAward())
              .isEqualTo(confirmed.finalSnapshot());
          assertThat(h.balance(user))
              .isEqualByComparingTo(confirmed.finalSnapshot().amount());
          assertThat(deliveryRows(h, user, order)).isOne();
          reconcile(
              h,
              List.of(user),
              List.of(
                  new Delivered(order, user, "CREDIT", confirmed.finalSnapshot().amount())),
              List.of(),
              Map.of(order, "CREDIT"),
              Map.of(pool, 1L));
        });
  }

  private record ReserveAward(EventEnvelope envelope, ReserveCommand command) {}

  private record Delivered(String orderId, String userId, String type, BigDecimal amount) {}

  private static String create(DrawService svc, String user, long activity) {
    return svc.create(
            new DrawCommand(
                user,
                "req-" + UUID.randomUUID(),
                activity,
                new Deadline(Instant.now().plusSeconds(60))))
        .orderId();
  }

  private static String pool(AssetTestSupport h, long total) {
    String pool = "p0-e2e-" + UUID.randomUUID();
    StockProtocolV2Test.control(
        h,
        () -> {
          StockProtocolV2Test.service(h).createPool(pool, "initial", total, "p0-e2e");
          return null;
        });
    return pool;
  }

  private static String[] usersInDifferentDatabases(AssetTestSupport h) {
    String first = "p0-e2e-a-" + UUID.randomUUID();
    String second;
    do {
      second = "p0-e2e-b-" + UUID.randomUUID();
    } while (h.codec.routeUser(second).dbKey().equals(h.codec.routeUser(first).dbKey()));
    return new String[] {first, second};
  }

  private static ReserveAward reserveAward(
      AssetTestSupport h, String user, String order) {
    return h.tx(
        user,
        () -> {
          String payload =
              h.j.queryForObject(
                  "select payload from outbox where aggregate_id=? and event_type='ReserveAward'",
                  String.class,
                  order);
          assertThat(
                  h.j.queryForObject(
                      "select count(*) from outbox where aggregate_id=? and"
                          + " event_type='ReserveAward'",
                      Integer.class,
                      order))
              .isOne();
          var envelope = JsonStore.read(payload, EventEnvelope.class);
          assertThat(envelope.eventType()).isEqualTo("ReserveAward");
          assertThat(envelope.aggregateId()).isEqualTo(order);
          assertThat(envelope.userId()).isEqualTo(user);
          assertThat(envelope.schemaVersion()).isEqualTo(2);
          assertThat(envelope.routeSlot()).isEqualTo(RouteSlot.control());
          var command = ReceiptJson.read(envelope.payload(), ReserveCommand.class);
          assertThat(command.drawOrderId()).isEqualTo(order);
          assertThat(command.userId()).isEqualTo(user);
          assertThat(command.requestHash()).isEqualTo(envelope.requestHash());
          // 命令必须就是 create 时冻结并持久化的同一份快照。
          var persisted = new MySqlDrawRepository(h.j.getDataSource()).find(order, false);
          assertThat(command.identityHash()).isEqualTo(persisted.identityHash());
          assertThat(command.requestHash()).isEqualTo(persisted.requestHash());
          assertThat(command.strategyId()).isEqualTo(persisted.strategy().strategyId());
          assertThat(command.version()).isEqualTo(persisted.version());
          assertThat(command.candidate()).isEqualTo(persisted.candidate());
          assertThat(command.fallback()).isEqualTo(persisted.fallback());
          return new ReserveAward(envelope, command);
        });
  }

  private static ReservationResult confirm(
      AssetTestSupport h, ReserveCommand command) {
    return StockProtocolV2Test.control(
        h, () -> StockProtocolV2Test.service(h).confirmOnReserve(command));
  }

  private static EventEnvelope confirmed(
      AssetTestSupport h, ReservationResult result, long version) {
    return new EventEnvelope(
        h.codec.newEventId(),
        "ReservationConfirmed",
        result.drawOrderId(),
        new BusinessId("DRAW", result.drawOrderId(), result.requestHash()),
        result.userId(),
        h.codec.routeUser(result.userId()),
        result.requestHash(),
        version,
        2,
        ReceiptJson.write(result));
  }

  private static void deliver(AssetTestSupport h, EventEnvelope event) {
    h.tx(
        event.userId(),
        () -> {
          new DrawDeliveryService(new MySqlDrawRepository(h.j.getDataSource()), h.assets)
              .deliver(event);
          return null;
        });
  }

  private static DrawRecord record(AssetTestSupport h, String user, String order) {
    return h.tx(user, () -> new MySqlDrawRepository(h.j.getDataSource()).find(order, false));
  }

  private static int deliveryRows(AssetTestSupport h, String user, String order) {
    String table = "v2_award_delivery_" + h.codec.routeOrder(order).tableKey();
    return h.tx(
        user,
        () ->
            h.j.queryForObject(
                "select count(*) from " + table + " where order_id=?", Integer.class, order));
  }

  private static Map<String, Long> stock(AssetTestSupport h, String pool) {
    return StockProtocolV2Test.control(
        h,
        () -> {
          var row =
              h.j.queryForMap(
                  "select total,available,reserved,allocated from award_stock where"
                      + " stock_pool_id=?",
                  pool);
          return Map.of(
              "total", number(row, "total"),
              "available", number(row, "available"),
              "reserved", number(row, "reserved"),
              "allocated", number(row, "allocated"));
        });
  }

  private static long number(Map<String, Object> row, String column) {
    return ((Number) row.get(column)).longValue();
  }

  /**
   * 对账：先按用户核对余额与 delivery 行，再扫 2 个 user 库 × 4 张分表的 4 类表，
   * 最后在 control 核对 award_stock 守恒与 award_reservation 状态。
   *
   * <p>{@code confirmedTypes} 记录每个订单在 control 侧确认出的奖项类型，用来交叉验证
   * 预占回执、交付记录和订单最终状态没有互相漂移。
   */
  private static void reconcile(
      AssetTestSupport h,
      List<String> users,
      List<Delivered> delivered,
      List<String> pendingOrders,
      Map<String, String> confirmedTypes,
      Map<String, Long> allocatedByPool) {
    List<String> orders = new ArrayList<>();
    delivered.forEach(x -> orders.add(x.orderId()));
    orders.addAll(pendingOrders);
    assertThat(confirmedTypes.keySet()).containsExactlyInAnyOrderElementsOf(orders);

    for (Delivered row : delivered) {
      // 订单号里的路由必须与用户路由一致，否则对账扫到错误的库表。
      assertThat(h.codec.routeOrder(row.orderId())).isEqualTo(h.codec.routeUser(row.userId()));
      assertThat(row.type()).isEqualTo(confirmedTypes.get(row.orderId()));
      assertThat(deliveryRows(h, row.userId(), row.orderId())).isOne();
      String table = "v2_award_delivery_" + h.codec.routeOrder(row.orderId()).tableKey();
      var persisted =
          h.tx(
              row.userId(),
              () ->
                  h.j.queryForMap(
                      "select award_hash,snapshot from "
                          + table
                          + " where order_id=?",
                      row.orderId()));
      var award = JsonStore.read((String) persisted.get("snapshot"), AwardSnapshot.class);
      assertThat(persisted.get("award_hash")).isEqualTo(award.configHash());
      assertThat(award.type()).isEqualTo(row.type());
      assertThat(award).isEqualTo(record(h, row.userId(), row.orderId()).finalAward());
    }

    for (String user : users) {
      BigDecimal expected =
          delivered.stream()
              .filter(x -> x.userId().equals(user) && "CREDIT".equals(x.type()))
              .map(Delivered::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      assertThat(h.balance(user)).isEqualByComparingTo(expected);
    }

    // control：预占状态 + 库存守恒。
    StockProtocolV2Test.control(
        h,
        () -> {
          String placeholders = placeholders(orders.size());
          var reservations =
              h.j.queryForList(
                  "select draw_order_id,state,quantity,snapshot from award_reservation where"
                      + " draw_order_id in ("
                      + placeholders
                      + ")",
                  orders.toArray());
          assertThat(reservations).hasSize(orders.size());
          long allocatedQuantity = 0;
          for (var reservation : reservations) {
            assertThat(reservation.get("state")).isEqualTo("CONFIRMED");
            long quantity = number(reservation, "quantity");
            allocatedQuantity += quantity;
            var receipt =
                ReceiptJson.read((String) reservation.get("snapshot"), ReservationResult.class);
            assertThat(receipt.state()).isEqualTo("CONFIRMED");
            assertThat(receipt.drawOrderId()).isEqualTo(reservation.get("draw_order_id"));
            assertThat(receipt.quantity()).isEqualTo(quantity);
            assertThat(receipt.finalSnapshot().quantity()).isEqualTo(quantity);
            String type = confirmedTypes.get((String) reservation.get("draw_order_id"));
            assertThat(receipt.finalSnapshot().type()).isEqualTo(type);
            assertThat(quantity).isEqualTo("CREDIT".equals(type) ? 1 : 0);
          }
          long expectedAllocated =
              allocatedByPool.values().stream().mapToLong(Long::longValue).sum();
          assertThat(allocatedQuantity).isEqualTo(expectedAllocated);

          for (var pool : allocatedByPool.entrySet()) {
            var row =
                h.j.queryForMap(
                    "select total,available,reserved,allocated from award_stock where"
                        + " stock_pool_id=?",
                    pool.getKey());
            long total = number(row, "total");
            long available = number(row, "available");
            long reserved = number(row, "reserved");
            long allocated = number(row, "allocated");
            assertThat(available + reserved + allocated).isEqualTo(total);
            assertThat(reserved).isZero();
            assertThat(allocated).isEqualTo(pool.getValue());
            assertThat(available).isEqualTo(total - allocated);
          }
          return null;
        });

    // user 库：2 库 × 4 张分表，每张分表检查订单/交付/奖励记录/积分流水 4 类表。
    for (String db : List.of("01", "02"))
      for (int shard = 0; shard < 4; shard++) {
        var slot = new RouteSlot(db, String.format("%03d", shard));
        h.runner.inSlot(
            slot,
            WorkClass.RECOVERY,
            new Deadline(Instant.now().plusSeconds(60)),
            () -> {
              String table = slot.tableKey();
              String in = placeholders(users.size());
              Object[] args = users.toArray();
              List<Delivered> inSlot =
                  delivered.stream()
                      .filter(x -> slot.equals(h.codec.routeUser(x.userId())))
                      .toList();
              List<Delivered> credits =
                  inSlot.stream().filter(x -> "CREDIT".equals(x.type())).toList();
              BigDecimal creditSum =
                  credits.stream()
                      .map(Delivered::amount)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              long pending =
                  pendingOrders.stream()
                      .filter(order -> slot.equals(h.codec.routeOrder(order)))
                      .count();

              assertThat(
                      count(
                          h,
                          "select count(*) from v2_draw_order_"
                              + table
                              + " where user_id in ("
                              + in
                              + ") and state='DELIVERED'",
                          args))
                  .isEqualTo(inSlot.size());
              assertThat(
                      count(
                          h,
                          "select count(*) from v2_draw_order_"
                              + table
                              + " where user_id in ("
                              + in
                              + ") and state='PENDING_RESERVATION'",
                          args))
                  .isEqualTo((int) pending);
              assertThat(
                      count(
                          h,
                          "select count(*) from v2_award_delivery_"
                              + table
                              + " where user_id in ("
                              + in
                              + ")",
                          args))
                  .isEqualTo(inSlot.size());
              assertThat(
                      count(
                          h,
                          "select count(*) from v2_award_record_"
                              + table
                              + " where user_id in ("
                              + in
                              + ") and state='DELIVERED'",
                          args))
                  .isEqualTo(inSlot.size());
              assertThat(
                      count(
                          h,
                          "select count(*) from user_credit_ledger_"
                              + table
                              + " where user_id in ("
                              + in
                              + ") and biz_type='DRAW_AWARD'",
                          args))
                  .isEqualTo(credits.size());
              assertThat(
                      h.j.queryForObject(
                          "select coalesce(sum(delta),0) from user_credit_ledger_"
                              + table
                              + " where user_id in ("
                              + in
                              + ") and biz_type='DRAW_AWARD'",
                          BigDecimal.class,
                          args))
                  .isEqualByComparingTo(creditSum);
              // 订单快照的最终奖项必须与交付记录一致：CREDIT 数一致。
              assertThat(
                      count(
                          h,
                          "select count(*) from v2_draw_order_"
                              + table
                              + " where user_id in ("
                              + in
                              + ") and state='DELIVERED' and"
                              + " json_unquote(json_extract(snapshot,'$.finalAward.type'))='CREDIT'",
                          args))
                  .isEqualTo(credits.size());
              assertThat(
                      count(
                          h,
                          "select count(*) from v2_award_delivery_"
                              + table
                              + " where user_id in ("
                              + in
                              + ") and"
                              + " json_unquote(json_extract(snapshot,'$.type'))='CREDIT'",
                          args))
                  .isEqualTo(credits.size());
              return null;
            });
      }
  }

  private static int count(AssetTestSupport h, String sql, Object... args) {
    return h.j.queryForObject(sql, Integer.class, args);
  }

  private static String placeholders(int count) {
    if (count <= 0) throw new IllegalArgumentException("At least one identity required");
    return String.join(",", Collections.nCopies(count, "?"));
  }
}
