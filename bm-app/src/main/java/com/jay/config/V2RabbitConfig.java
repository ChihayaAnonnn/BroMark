package com.jay.config;

import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.asset.service.*;
import com.jay.domain.task.repository.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.event.V2ConfirmedEventPublisher;
import com.jay.infrastructure.id.BlockOrderIdGenerator;
import com.jay.trigger.job.v2.*;
import com.jay.trigger.listener.v2.V2MessageListener;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.*;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/** Dedicated V2 connection, topology and consumers; existing legacy template remains unchanged. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "bm.v2",
    name = {"enabled", "messaging.enabled"},
    havingValue = "true")
public class V2RabbitConfig {
  public static final class V2Transport implements AutoCloseable {
    final CachingConnectionFactory connection;
    final RabbitTemplate template;
    final RabbitAdmin admin;

    V2Transport(CachingConnectionFactory c) {
      connection = c;
      template = new RabbitTemplate(c);
      template.setMandatory(true);
      admin = new RabbitAdmin(c);
    }

    public void close() {
      connection.destroy();
    }
  }

  @Bean(destroyMethod = "close")
  public V2Transport v2Transport(
      @Value("${spring.rabbitmq.host}") String host,
      @Value("${spring.rabbitmq.port:5672}") int port,
      @Value("${spring.rabbitmq.username}") String user,
      @Value("${spring.rabbitmq.password}") String password,
      @Value("${spring.rabbitmq.virtual-host:/}") String vhost) {
    var c = new CachingConnectionFactory(host, port);
    c.setUsername(user);
    c.setPassword(password);
    c.setVirtualHost(vhost);
    c.setConnectionTimeout(1000);
    c.setChannelCacheSize(18);
    c.setChannelCheckoutTimeout(1000);
    c.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    c.setPublisherReturns(true);
    return new V2Transport(c);
  }

  @Bean
  public Declarables v2MessageTopology(
      V2Transport transport,
      @Value("${bm.v2.messaging.prefix:bromark.v2}") String prefix,
      @Value("${bm.v2.messaging.queue-type:quorum}") String queueType) {
    if (!Set.of("quorum", "classic").contains(queueType) || !prefix.matches("[A-Za-z0-9._-]{1,96}"))
      throw new IllegalArgumentException("Explicit V2 topology");
    var objects = new ArrayList<Declarable>();
    var exchange = new DirectExchange(prefix + ".events", true, false);
    objects.add(exchange);
    var workflow =
        new org.springframework.amqp.core.Queue(
            prefix + ".workflow", true, false, false, Map.of("x-queue-type", queueType));
    var notifications =
        new org.springframework.amqp.core.Queue(
            prefix + ".notifications", true, false, false, Map.of("x-queue-type", queueType));
    objects.add(workflow);
    objects.add(notifications);
    for (String type : EventRouting.CONTROL)
      objects.add(BindingBuilder.bind(workflow).to(exchange).with(type));
    for (String type : EventRouting.USER)
      objects.add(BindingBuilder.bind(workflow).to(exchange).with(type));
    for (String type : EventRouting.NOTIFICATIONS)
      objects.add(BindingBuilder.bind(notifications).to(exchange).with(type));
    transport.admin.declareExchange(exchange);
    transport.admin.declareQueue(workflow);
    transport.admin.declareQueue(notifications);
    objects.stream()
        .filter(d -> d instanceof Binding)
        .forEach(d -> transport.admin.declareBinding((Binding) d));
    objects.forEach(d -> ((AbstractDeclarable) d).setShouldDeclare(false));
    return new Declarables(objects);
  }

  @Bean(name = "v2MessageContainerFactory")
  public SimpleRabbitListenerContainerFactory v2MessageContainerFactory(V2Transport transport) {
    var c = transport.connection;
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(c);
    f.setAutoStartup(false);
    f.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    f.setPrefetchCount(8);
    f.setConcurrentConsumers(1);
    f.setMaxConcurrentConsumers(1);
    f.setDefaultRequeueRejected(false);
    f.setContainerCustomizer(cn -> cn.setShutdownTimeout(10000L));
    return f;
  }

  @Bean(destroyMethod = "close")
  public V2ConfirmedEventPublisher v2ConfirmedPublisher(
      V2Transport transport, @Value("${bm.v2.messaging.prefix:bromark.v2}") String prefix) {
    return new V2ConfirmedEventPublisher(
        transport.template, prefix + ".events", 16, Duration.ofSeconds(3));
  }

  @Bean
  public MySqlOutboxRepository v2OutboxRepository(DataSource ds, UserTransactionRunner tx) {
    return new MySqlOutboxRepository(ds, tx);
  }

  @Bean
  public MySqlRetryJobRepository v2RetryRepository(
      DataSource ds, UserTransactionRunner tx, RecoveryService recovery) {
    return new MySqlRetryJobRepository(ds, tx, recovery);
  }

  @Bean
  public RecoveryService v2RecoveryService(
      DataSource ds,
      UserTransactionRunner tx,
      RouteCodec codec,
      Map<String, RecoveryAggregatePort> ports) {
    return new RecoveryService(
        tx, new MySqlRecoveryRepository(ds), new MySqlOutboxWriter(ds), ports, codec::newEventId);
  }

  @Bean
  public ConsumerUnitOfWork v2ConsumerWork(
      DataSource ds, UserTransactionRunner tx, RouteCodec codec, RecoveryService recovery) {
    return new ConsumerUnitOfWork(
        tx,
        new MySqlConsumerInbox(ds),
        new MySqlOutboxWriter(ds),
        new EventRouting(codec),
        recovery,
        codec::newEventId);
  }

  @Bean
  public V2MessageListener v2MessageListener(
      DataSource ds,
      UserTransactionRunner tx,
      ConsumerUnitOfWork work,
      MySqlRetryJobRepository retries,
      RecoveryService recovery,
      RouteCodec codec,
      Map<String, EventHandler> handlers,
      FaultInjector faults) {
    var registered = new HashMap<>(handlers);
    var j = new JdbcTemplate(ds);
    EventHandler notification =
        e -> {
          j.update(
              "insert into notification_projection values(?,?,?,?,UTC_TIMESTAMP(6)) ON DUPLICATE"
                  + " KEY UPDATE event_id=event_id",
              e.eventId(),
              e.eventType(),
              e.aggregateId(),
              e.payload());
          return List.of();
        };
    registered.put("ExchangeCompleted", notification);
    registered.put("SignInCompleted", notification);
    return new V2MessageListener(
        tx,
        work,
        retries,
        recovery,
        new EventRouting(codec),
        body ->
            JsonStore.read(
                new String(body, StandardCharsets.UTF_8),
                com.jay.domain.task.model.EventEnvelope.class),
        new MySqlRawMessageStore(ds),
        registered,
        faults);
  }

  @Bean
  public OutboxRelayJob v2OutboxJob(
      @Qualifier("v2OutboxRepository") MySqlOutboxRepository repo,
      V2ConfirmedEventPublisher pub,
      FaultInjector faults) {
    return new OutboxRelayJob(new OutboxRelay(repo, pub, new RetryPolicy(), faults));
  }

  @Bean
  public RetryPublishJob v2RetryJob(
      MySqlRetryJobRepository repo, V2ConfirmedEventPublisher pub, FaultInjector faults) {
    return new RetryPublishJob(new OutboxRelay(repo, pub, new RetryPolicy(), faults));
  }

  @Bean
  public RecoveryScanJob v2RecoveryJob(RecoveryService service) {
    return new RecoveryScanJob(service);
  }

  @Bean
  public MessagingWorkers v2MessagingWorkers(
      V2MessageListener listener,
      OutboxRelayJob outbox,
      RetryPublishJob retry,
      RecoveryScanJob recovery,
      BlockOrderIdGenerator ids,
      RabbitListenerEndpointRegistry registry) {
    return new MessagingWorkers(listener, outbox, retry, recovery, ids, registry);
  }

  public static final class MessagingWorkers implements SmartLifecycle {
    private final V2MessageListener listener;
    private final OutboxRelayJob outbox;
    private final RetryPublishJob retry;
    private final RecoveryScanJob recovery;
    private final BlockOrderIdGenerator ids;
    private final RabbitListenerEndpointRegistry registry;
    private final ScheduledThreadPoolExecutor pauses =
        new ScheduledThreadPoolExecutor(
            1, Thread.ofPlatform().name("v2-consumer-backoff").factory());
    private final Set<String> paused = ConcurrentHashMap.newKeySet();
    private volatile boolean running;

    MessagingWorkers(
        V2MessageListener listener,
        OutboxRelayJob outbox,
        RetryPublishJob retry,
        RecoveryScanJob recovery,
        BlockOrderIdGenerator ids,
        RabbitListenerEndpointRegistry registry) {
      this.listener = listener;
      this.outbox = outbox;
      this.retry = retry;
      this.recovery = recovery;
      this.ids = ids;
      this.registry = registry;
    }

    public void start() {
      ids.prefetch();
      running = true;
      for (String id : List.of("v2-workflow", "v2-notification")) {
        var container = registry.getListenerContainer(id);
        if (container == null) throw new IllegalStateException("Missing V2 consumer");
        container.start();
      }
    }

    public boolean isRunning() {
      return running;
    }

    public int getPhase() {
      return Integer.MAX_VALUE;
    }

    @Scheduled(fixedDelayString = "${bm.v2.messaging.poll-ms:1000}")
    public void relay() {
      if (running) {
        ids.prefetch();
        outbox.tick();
        retry.tick();
      }
    }

    @Scheduled(fixedDelay = 10000)
    public void recover() {
      if (running) {
        ids.prefetch();
        recovery.tick();
      }
    }

    @RabbitListener(
        id = "v2-workflow",
        queues = "${bm.v2.messaging.prefix:bromark.v2}.workflow",
        containerFactory = "v2MessageContainerFactory")
    public void workflow(Message m, com.rabbitmq.client.Channel c) {
      consume("workflow-v2", "v2-workflow", m, c);
    }

    @RabbitListener(
        id = "v2-notification",
        queues = "${bm.v2.messaging.prefix:bromark.v2}.notifications",
        containerFactory = "v2MessageContainerFactory")
    public void notification(Message m, com.rabbitmq.client.Channel c) {
      consume("notification-v2", "v2-notification", m, c);
    }

    private void consume(
        String consumer, String container, Message m, com.rabbitmq.client.Channel c) {
      if (!running || paused.contains(container)) return;
      try {
        ids.prefetch();
      } catch (RuntimeException e) {
        pause(container);
        return;
      }
      Object header = m.getMessageProperties().getHeaders().getOrDefault("x-v2-failure-count", 0);
      int failures = header instanceof Integer n ? n : -1;
      listener.handle(
          consumer,
          m.getBody(),
          failures,
          new V2MessageListener.Delivery() {
            public void ack() throws Exception {
              c.basicAck(m.getMessageProperties().getDeliveryTag(), false);
            }

            public void pause() {
              MessagingWorkers.this.pause(container);
            }
          });
    }

    private void pause(String id) {
      if (!paused.add(id) || !running) return;
      pauses.execute(
          () -> {
            var container = registry.getListenerContainer(id);
            if (container != null) container.stop();
            if (running)
              pauses.schedule(
                  () -> {
                    if (running && container != null) {
                      paused.remove(id);
                      container.start();
                    }
                  },
                  5,
                  TimeUnit.SECONDS);
          });
    }

    public void stop() {
      running = false;
      outbox.stop();
      retry.stop();
      recovery.stop();
      registry.stop();
      pauses.shutdownNow();
    }
  }
}
