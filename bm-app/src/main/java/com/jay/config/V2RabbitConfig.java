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

    private final Runnable physicalClose;

    V2Transport(CachingConnectionFactory c) {
      this(c, c::destroy);
    }

    V2Transport(CachingConnectionFactory c, Runnable physicalClose) {
      this.physicalClose = physicalClose;
      connection = c;
      template = new RabbitTemplate(c);
      template.setMandatory(true);
      admin = new RabbitAdmin(c);
    }

    private CompletableFuture<Void> closing;

    public synchronized void close() {
      if (closing != null) return;
      closing = new CompletableFuture<>();
      try {
        physicalClose.run();
        closing.complete(null);
      } catch (RuntimeException e) {
        closing.completeExceptionally(e);
        throw e;
      }
    }

    public boolean closeBefore(long end) {
      CompletableFuture<Void> completion;
      synchronized (this) {
        if (closing == null) {
          closing = new CompletableFuture<>();
          Thread.ofVirtual()
              .name("v2-rabbit-cleanup")
              .start(
                  () -> {
                    try {
                      physicalClose.run();
                      closing.complete(null);
                    } catch (Throwable e) {
                      closing.completeExceptionally(e);
                      org.slf4j.LoggerFactory.getLogger(getClass())
                          .warn("Deferred Rabbit cleanup failed", e);
                    }
                  });
        }
        completion = closing;
      }
      try {
        completion.get(V2WorkerConfig.remaining(end).toNanos(), TimeUnit.NANOSECONDS);
        return true;
      } catch (TimeoutException e) {
        return false;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      } catch (ExecutionException e) {
        throw new IllegalStateException("Rabbit cleanup failed", e.getCause());
      }
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
  public SimpleRabbitListenerContainerFactory v2MessageContainerFactory(
      V2Transport transport, V2BudgetProperties props) {
    var c = transport.connection;
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(c);
    f.setAutoStartup(false);
    f.setPhase(Integer.MAX_VALUE - 2);
    f.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    f.setPrefetchCount(props.getPrefetch());
    f.setConcurrentConsumers(props.getWorkerConcurrency());
    f.setMaxConcurrentConsumers(props.getWorkerConcurrency());
    f.setDefaultRequeueRejected(false);
    f.setContainerCustomizer(cn -> cn.setShutdownTimeout(10000L));
    return f;
  }

  @Bean(destroyMethod = "")
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
      DataSource ds,
      UserTransactionRunner tx,
      RecoveryService recovery,
      org.springframework.beans.factory.ObjectProvider<MessagingWorkers> workers) {
    return new MySqlRetryJobRepository(
        ds,
        tx,
        recovery,
        () -> {
          var active = workers.getIfAvailable();
          if (active != null) active.wakeRetry();
        });
  }

  @Bean
  public RecoveryService v2RecoveryService(
      DataSource ds,
      UserTransactionRunner tx,
      RouteCodec codec,
      Map<String, RecoveryAggregatePort> ports,
      MySqlOutboxWriter writer,
      V2RecoveryRedriveProperties policy) {
    return new RecoveryService(
            tx, new MySqlRecoveryRepository(ds), writer, ports, codec::newEventId)
        .dedicatedWorkflows("guarded".equals(policy.getMode()));
  }

  @Bean
  public WorkflowRedrive workflowRedrive(
      DataSource ds,
      UserTransactionRunner tx,
      RecoveryService recovery,
      MySqlOutboxWriter writer,
      V2RecoveryRedriveProperties policy) {
    var j = new JdbcTemplate(ds);
    if ("guarded".equals(policy.getMode())) {
      for (var slot :
          List.of(
              com.jay.domain.asset.model.RouteSlot.control(),
              new com.jay.domain.asset.model.RouteSlot("01", "000"),
              new com.jay.domain.asset.model.RouteSlot("02", "000"))) {
        tx.inSlot(
            slot,
            com.jay.domain.asset.model.WorkClass.RECOVERY,
            new com.jay.domain.asset.model.Deadline(java.time.Instant.now().plusSeconds(5)),
            () -> {
              // 仅验证必需结构，绝不在启动时DDL或静默退回legacy。
              j.queryForList(
                  "select"
                      + " aggregate_type,aggregate_id,observed_state,observed_version,last_event_type,last_event_id,last_redrive_at,next_redrive_at,redrive_count"
                      + " from v2_recovery_dispatch limit 0");
              return null;
            });
      }
    }
    return new WorkflowRedrive(
        tx,
        new MySqlRecoveryDispatchRepository(ds),
        new MySqlDeliveryEvidenceRepository(ds),
        recovery,
        writer,
        () ->
            j.queryForObject("select UTC_TIMESTAMP(6)", java.time.LocalDateTime.class)
                .toInstant(java.time.ZoneOffset.UTC),
        Duration.ofSeconds(policy.getStaleSeconds()),
        Duration.ofSeconds(policy.getIntervalSeconds()),
        policy.getMaxPerStage());
  }

  @Bean
  public ConsumerUnitOfWork v2ConsumerWork(
      DataSource ds,
      UserTransactionRunner tx,
      RouteCodec codec,
      RecoveryService recovery,
      MySqlOutboxWriter writer) {
    return new ConsumerUnitOfWork(
        tx,
        new MySqlConsumerInbox(ds),
        writer,
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
  public RecoveryScanJob v2RecoveryJob(
      RecoveryService service, CoalescingJobDispatcher dispatcher, BlockOrderIdGenerator ids) {
    return new RecoveryScanJob(service).dispatchWith(dispatcher).prefetchWith(ids::prefetch);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "bm.v2.recovery.redrive", name = "mode", havingValue = "guarded")
  public RetryTransportRecoveryJob v2RetryTransportRecoveryJob(
      DataSource ds,
      UserTransactionRunner tx,
      MySqlRetryJobRepository retries,
      RecoveryService recovery,
      CoalescingJobDispatcher dispatcher,
      V2RecoveryRedriveProperties policy) {
    var j = new JdbcTemplate(ds);
    for (var slot : OutboxRelayJob.SOURCES)
      tx.inSlot(
          slot,
          com.jay.domain.asset.model.WorkClass.RECOVERY,
          new com.jay.domain.asset.model.Deadline(java.time.Instant.now().plusSeconds(5)),
          () -> {
            j.queryForList(
                "select published_at,event_id from retry_job FORCE INDEX(published_recovery)"
                    + " where state='PUBLISHED' limit 0");
            return null;
          });
    var service =
        new RetryTransportRecovery(
            tx, retries, new MySqlDeliveryEvidenceRepository(ds), recovery);
    service.budget(
        policy.getCandidateLimit(),
        policy.getEmissionLimit(),
        Duration.ofSeconds(policy.getRoundTimeoutSeconds()));
    return new RetryTransportRecoveryJob(service, dispatcher);
  }

  @Bean
  public MessagingWorkers v2MessagingWorkers(
      V2MessageListener listener,
      OutboxRelayJob outbox,
      RetryPublishJob retry,
      RecoveryScanJob recovery,
      org.springframework.beans.factory.ObjectProvider<RetryTransportRecoveryJob> transportRecovery,
      BlockOrderIdGenerator ids,
      RabbitListenerEndpointRegistry registry,
      @Qualifier("v2OutboxRepository") MySqlOutboxRepository outboxRepo,
      MySqlRetryJobRepository retryRepo,
      V2ConfirmedEventPublisher publisher,
      FaultInjector faults,
      V2RelayProperties relay,
      @Qualifier("v2RelayExecutor") ExecutorService executor,
      V2Transport transport) {
    var outboxBounded =
        "bounded".equals(relay.getMode())
            ? new BoundedOutboxRelay(outboxRepo, publisher, relay.policy(), faults)
            : null;
    var retryBounded =
        "bounded".equals(relay.getMode())
            ? new BoundedOutboxRelay(retryRepo, publisher, relay.policy(), faults)
            : null;
    return new MessagingWorkers(
        listener,
        outbox,
        retry,
        recovery,
        transportRecovery.getIfAvailable(),
        ids,
        registry,
        outboxBounded,
        retryBounded,
        relay,
        executor,
        publisher,
        transport);
  }

  public static final class MessagingWorkers implements SmartLifecycle {
    private final V2MessageListener listener;
    private final OutboxRelayJob outbox;
    private final RetryPublishJob retry;
    private final RecoveryScanJob recovery;
    private final RetryTransportRecoveryJob transportRecovery;
    private final BlockOrderIdGenerator ids;
    private final RabbitListenerEndpointRegistry registry;
    private final ScheduledThreadPoolExecutor pauses =
        new ScheduledThreadPoolExecutor(
            1, Thread.ofPlatform().name("v2-consumer-backoff").factory());
    private final Set<String> paused = ConcurrentHashMap.newKeySet();
    private volatile boolean running;
    private final BoundedOutboxRelay outboxBounded, retryBounded;
    private final RelayDrainWorker outboxWorker, retryWorker;
    private final ExecutorService executor;
    private final V2ConfirmedEventPublisher publisher;
    private final V2Transport transport;

    MessagingWorkers(
        V2MessageListener listener,
        OutboxRelayJob outbox,
        RetryPublishJob retry,
        RecoveryScanJob recovery,
        RetryTransportRecoveryJob transportRecovery,
        BlockOrderIdGenerator ids,
        RabbitListenerEndpointRegistry registry,
        BoundedOutboxRelay outboxBounded,
        BoundedOutboxRelay retryBounded,
        V2RelayProperties relay,
        ExecutorService executor,
        V2ConfirmedEventPublisher publisher,
        V2Transport transport) {
      this.listener = listener;
      this.outbox = outbox;
      this.retry = retry;
      this.recovery = recovery;
      this.transportRecovery = transportRecovery;
      this.ids = ids;
      this.registry = registry;
      this.outboxBounded = outboxBounded;
      this.retryBounded = retryBounded;
      this.outboxWorker =
          outboxBounded == null
              ? null
              : new RelayDrainWorker(
                  slot -> {
                    ids.prefetch();
                    return outboxBounded.runOnce(slot);
                  },
                  relay.policy().idlePoll());
      this.retryWorker =
          retryBounded == null
              ? null
              : new RelayDrainWorker(
                  slot -> {
                    ids.prefetch();
                    return retryBounded.runOnce(slot);
                  },
                  relay.policy().idlePoll());
      this.executor = executor;
      this.publisher = publisher;
      this.transport = transport;
    }

    public void start() {
      ids.prefetch();
      running = true;
      if (outboxWorker != null) {
        outbox.quiesce();
        retry.quiesce();
        executor.execute(outboxWorker);
        executor.execute(retryWorker);
      }
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
      return Integer.MAX_VALUE - 1;
    }

    @Scheduled(fixedDelayString = "${bm.v2.messaging.poll-ms:1000}")
    public void relay() {
      if (running && outboxWorker == null) {
        ids.prefetch();
        outbox.tick();
        retry.tick();
      }
    }

    @Scheduled(fixedDelay = 10000)
    public void recover() {
      if (running) {
        recovery.tick();
        if (transportRecovery != null) transportRecovery.tick();
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

    public void wakeOutbox() {
      if (outboxWorker != null) outboxWorker.wake();
    }

    public void wakeRetry() {
      if (retryWorker != null) retryWorker.wake();
    }

    public void stop() {
      running = false;
      outbox.quiesce();
      retry.quiesce();
      recovery.stop();
      if (transportRecovery != null) transportRecovery.stop();
      if (outboxWorker != null) {
        outboxWorker.quiesce();
        retryWorker.quiesce();
        outboxBounded.quiesce();
        retryBounded.quiesce();
      }
      pauses.shutdownNow();
    }

    public void shutdown(long end) {
      stop();
      var stopped = new CountDownLatch(1);
      RuntimeException failure = null;
      try {
        if (outboxBounded != null) {
          outboxBounded.stopAndDrain(V2WorkerConfig.remaining(end));
          retryBounded.stopAndDrain(V2WorkerConfig.remaining(end));
        } else {
          outbox.stopAndDrain(V2WorkerConfig.remaining(end));
          retry.stopAndDrain(V2WorkerConfig.remaining(end));
        }
      } catch (RuntimeException e) {
        failure = e;
      }
      try {
        registry.stop(stopped::countDown);
        if (!stopped.await(V2WorkerConfig.remaining(end).toNanos(), TimeUnit.NANOSECONDS))
          failure = new IllegalStateException("V2 consumers did not stop before shared deadline");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        failure = new IllegalStateException(e);
      } catch (RuntimeException e) {
        failure = e;
      }
      publisher.close(V2WorkerConfig.remaining(end));
      if (!transport.closeBefore(end))
        failure = new IllegalStateException("Rabbit cleanup continues after shared deadline");
      if (failure != null) throw failure;
    }
  }
}
