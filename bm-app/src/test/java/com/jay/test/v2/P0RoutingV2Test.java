package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jay.config.V2BudgetProperties;
import com.jay.config.V2RabbitConfig;
import com.jay.domain.asset.model.BusinessId;
import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.asset.service.RouteCodec;
import com.jay.domain.task.model.EventEnvelope;
import com.jay.domain.task.service.EventRouting;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class P0RoutingV2Test {
  private static final String HASH = "a".repeat(64);
  private final RouteCodec codec = new RouteCodec(2, 4);
  private final EventRouting routing = new EventRouting(codec);
  private final String user = "u5";
  private final RouteSlot userSlot = new RouteSlot("01", "000");

  private EventEnvelope envelope(String type, RouteSlot slot, int schema) {
    return new EventEnvelope(
        "e-" + type + "-" + schema,
        type,
        "o1",
        new BusinessId("DRAW", "o1", HASH),
        user,
        slot,
        HASH,
        1,
        schema,
        "{}");
  }

  private EventEnvelope reserveAward() {
    return envelope("ReserveAward", RouteSlot.control(), 2);
  }

  private EventEnvelope reservationConfirmed() {
    return envelope("ReservationConfirmed", userSlot, 2);
  }

  private EventEnvelope notificationSchemaOne() {
    return envelope("ExchangeCompleted", userSlot, 1);
  }

  private EventEnvelope prepareDraw() {
    return envelope("PrepareDraw", userSlot, 1);
  }

  @Test
  public void routesOnlyP0Events() {
    assertThat(routing.route(reserveAward())).isEqualTo(RouteSlot.control());
    assertThat(routing.route(reservationConfirmed())).isEqualTo(new RouteSlot("01", "000"));
    assertThat(routing.route(notificationSchemaOne())).isEqualTo(userSlot);
    assertThatThrownBy(() -> routing.route(prepareDraw()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> routing.route(envelope("ConfirmReservation", RouteSlot.control(), 1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> routing.route(envelope("AwardDelivery", userSlot, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void rejectsSchemaVersionsOutsideOneAndTwo() {
    var schemaThree = mock(EventEnvelope.class);
    when(schemaThree.schemaVersion()).thenReturn(3);
    assertThatThrownBy(() -> routing.route(schemaThree))
        .isInstanceOf(IllegalArgumentException.class);

    var schemaZero = mock(EventEnvelope.class);
    when(schemaZero.schemaVersion()).thenReturn(0);
    assertThatThrownBy(() -> routing.route(schemaZero))
        .isInstanceOf(IllegalArgumentException.class);

    var schemaNegative = mock(EventEnvelope.class);
    when(schemaNegative.schemaVersion()).thenReturn(-1);
    assertThatThrownBy(() -> routing.route(schemaNegative))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void declaresReserveDeliverNotifyQueues() {
    var declarations = V2RabbitConfig.p0Topology("p0", "quorum").getDeclarables();
    var queues =
        declarations.stream()
            .filter(org.springframework.amqp.core.Queue.class::isInstance)
            .map(org.springframework.amqp.core.Queue.class::cast)
            .collect(
                java.util.stream.Collectors.toMap(
                    org.springframework.amqp.core.Queue::getName, q -> q));
    assertThat(queues.keySet())
        .containsExactlyInAnyOrder("p0.reserve", "p0.deliver", "p0.notify", "p0.dlq");
    assertThat(queues.keySet()).doesNotContain("p0.workflow", "p0.notifications");
    assertThat(queues.values())
        .allSatisfy(
            q -> {
              assertThat(q.isDurable()).isTrue();
              assertThat(q.isAutoDelete()).isFalse();
              assertThat(q.getArguments()).containsEntry("x-queue-type", "quorum");
            });

    var exchanges =
        declarations.stream()
            .filter(DirectExchange.class::isInstance)
            .map(DirectExchange.class::cast)
            .toList();
    assertThat(exchanges).hasSize(1);
    var exchange = exchanges.getFirst();
    assertThat(exchange.getName()).isEqualTo("p0.events");
    assertThat(exchange.isDurable()).isTrue();
    assertThat(exchange.isAutoDelete()).isFalse();

    var bindings = new TreeMap<String, Set<String>>();
    for (Declarable d : declarations)
      if (d instanceof Binding b) {
        assertThat(b.getExchange()).isEqualTo("p0.events");
        bindings.computeIfAbsent(b.getDestination(), k -> new TreeSet<>()).add(b.getRoutingKey());
      }
    assertThat(bindings)
        .containsOnlyKeys("p0.reserve", "p0.deliver", "p0.notify")
        .containsEntry("p0.reserve", Set.of("ReserveAward"))
        .containsEntry("p0.deliver", Set.of("ReservationConfirmed"))
        .containsEntry("p0.notify", Set.of("ExchangeCompleted", "SignInCompleted"));
  }

  @Test
  public void declaresStageListenersWithConcurrency() {
    var listeners = new TreeMap<String, RabbitListener>();
    for (var method : V2RabbitConfig.MessagingWorkers.class.getDeclaredMethods()) {
      var listener = method.getAnnotation(RabbitListener.class);
      if (listener != null) listeners.put(listener.id(), listener);
    }
    assertThat(listeners)
        .containsOnlyKeys("p0-reserve", "p0-deliver", "p0-notify");
    assertThat(listeners.get("p0-reserve").queues())
        .containsExactly("${bm.v2.messaging.prefix:bromark.v2}.reserve");
    assertThat(listeners.get("p0-deliver").queues())
        .containsExactly("${bm.v2.messaging.prefix:bromark.v2}.deliver");
    assertThat(listeners.get("p0-notify").queues())
        .containsExactly("${bm.v2.messaging.prefix:bromark.v2}.notify");
    assertThat(listeners.get("p0-reserve").concurrency()).isEqualTo("8");
    assertThat(listeners.get("p0-deliver").concurrency()).isEqualTo("16");
    assertThat(listeners.get("p0-notify").concurrency()).isEqualTo("4");
    assertThat(listeners.values())
        .allSatisfy(l -> assertThat(l.containerFactory()).isEqualTo("v2MessageContainerFactory"));
  }

  @Test
  public void containerFactoryUsesBudgetPrefetchWithoutFixedConcurrency() throws Exception {
    var config = new V2RabbitConfig();
    var transport = config.v2Transport("127.0.0.1", 5672, "guest", "guest", "/");
    try {
      var props = new V2BudgetProperties();
      props.setPrefetch(8);
      var factory = config.v2MessageContainerFactory(transport, props);
      assertThat(field(factory, "prefetchCount")).isEqualTo(8);
      assertThat(field(factory, "concurrentConsumers")).isNull();
      assertThat(field(factory, "maxConcurrentConsumers")).isNull();
    } finally {
      transport.close();
    }
  }

  private static Object field(Object target, String name) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
      } catch (NoSuchFieldException missing) {
        // continue up the hierarchy
      }
    }
    throw new NoSuchFieldException(name);
  }
}
