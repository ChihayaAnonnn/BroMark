package com.jay.test.v2;

import com.jay.domain.asset.model.WorkClass;
import com.jay.domain.asset.model.Deadline;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.asset.service.FaultInjector;
import com.jay.domain.task.model.BatchSettleReport;
import com.jay.domain.task.model.ClaimedPublish;
import com.jay.domain.task.model.PublishResolution;
import com.jay.domain.task.model.PublishOutcome;
import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.task.repository.BatchOutboxRepository;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.aop.framework.ProxyFactory;
import org.aopalliance.intercept.MethodInterceptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/** 测试 classpath 专用的完整应用启动器；生产包不含故障入口。 */
public final class RelayFaultProbe {
  private RelayFaultProbe() {}

  static FaultInjector afterConfirm(Path directory) {
    return afterConfirm(directory, () -> {});
  }

  static FaultInjector afterConfirm(Path directory, Runnable lateSubmit) {
    return new FaultInjector() {
      @Override
      public void hit(String point) {}

      @Override
      public void beforeClaim() {
        if (!Files.exists(directory.resolve("gate-claims"))) return;
        try {
          while (!Files.exists(directory.resolve("release-claims"))) {
            Thread.sleep(20);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("claim probe interrupted", e);
        }
      }

      @Override
      public void hit(
          String point, String eventId, String aggregateId, String leaseToken, int attempt) {
        if (!point.equals("relay.after-confirm") || !Files.exists(directory.resolve("arm"))) return;
        try {
          if (!Files.readString(directory.resolve("arm")).strip().equals(eventId)) return;
          var temporary = directory.resolve("confirmed-blocked.tmp");
          Files.writeString(
              temporary,
              ProcessHandle.current().pid()
                  + "\t"
                  + eventId
                  + "\t"
                  + aggregateId
                  + "\t"
                  + leaseToken
                  + "\t"
                  + attempt
                  + "\t"
                  + Instant.now());
          Files.move(
              temporary,
              directory.resolve("confirmed-blocked"),
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
          while (!Files.exists(directory.resolve("release-confirm"))) {
            lateSubmit.run();
            Thread.sleep(20);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("confirm probe interrupted", e);
        } catch (java.io.IOException e) {
          throw new IllegalStateException("confirm probe signal failed", e);
        }
      }
    };
  }

  static BeanPostProcessor settleConnectionProbe(
      GenericApplicationContext context, Path directory) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        if (!name.equals("userTransactionRunner")) return bean;
        return Proxy.newProxyInstance(
            RelayFaultProbe.class.getClassLoader(),
            new Class<?>[] {UserTransactionRunner.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("inSlot")
                  && arguments[1] == WorkClass.RELAY
                  && Files.exists(directory.resolve("arm-settle"))) {
                var action = (Supplier<?>) arguments[3];
                arguments[3] =
                    (Supplier<Object>)
                        () -> {
                          Object value = action.get();
                          if (!(value instanceof BatchSettleReport report)) return value;
                          String target;
                          try {
                            target = Files.readString(directory.resolve("arm-settle")).strip();
                          } catch (java.io.IOException e) {
                            throw new IllegalStateException("settle arm unreadable", e);
                          }
                          if (!report.completed().contains(target)) return value;
                          Long connection =
                              new JdbcTemplate(context.getBean(DataSource.class))
                                  .queryForObject("select CONNECTION_ID()", Long.class);
                          try {
                            var temporary = directory.resolve("settle-connection.tmp");
                            Files.writeString(
                                temporary,
                                ProcessHandle.current().pid()
                                    + "\t"
                                    + target
                                    + "\t"
                                    + connection
                                    + "\t"
                                    + Instant.now()
                                    + "\t"
                                    + ((Deadline) arguments[2]).expiresAt());
                            Files.move(
                                temporary,
                                directory.resolve("settle-connection"),
                                StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                            while (!Files.exists(directory.resolve("release-settle")))
                              Thread.sleep(20);
                          } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("settle probe interrupted", e);
                          } catch (java.io.IOException e) {
                            throw new IllegalStateException("settle probe signal failed", e);
                          }
                          return value;
                        };
              }
              try {
                return method.invoke(bean, arguments);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
            });
      }
    };
  }

  /** 保存真实claim返回对象；独立晚投递使用正常1s新事务，不延长原runOnce截止。 */
  static final class LateSettlement {
    final Path directory;
    final ConcurrentHashMap<String, ClaimedPublish> claimed = new ConcurrentHashMap<>();
    volatile BatchOutboxRepository repository;
    boolean submitted;

    LateSettlement(Path directory) { this.directory = directory; }

    BeanPostProcessor capture() {
      return new BeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String name) {
          if (!name.equals("v2OutboxRepository")) return bean;
          repository = (BatchOutboxRepository) bean;
          var factory = new ProxyFactory(bean);
          factory.setProxyTargetClass(true);
          factory.addAdvice((MethodInterceptor) invocation -> {
            Object result = invocation.proceed();
            if (invocation.getMethod().getName().equals("claimBatch")
                && Files.exists(directory.resolve("arm-late"))) {
              for (Object item : (List<?>) result) {
                var task = (ClaimedPublish) item;
                if (Files.readString(directory.resolve("arm-late")).strip().equals(task.eventId()))
                  claimed.putIfAbsent(task.eventId(), task);
              }
            }
            return result;
          });
          return factory.getProxy();
        }
      };
    }

    synchronized void submitIfTriggered() {
      if (submitted || !Files.exists(directory.resolve("submit-late"))) return;
      submitted = true;
      try {
        String target = Files.readString(directory.resolve("arm-late")).strip();
        var task = java.util.Objects.requireNonNull(claimed.get(target), "original claim missing");
        Instant started = Instant.now();
        var deadline = new Deadline(started.plusSeconds(1));
        var report = repository.settleBatch(task.ownerSlot(),
            List.of(new PublishResolution(task, PublishOutcome.CONFIRMED, "CONFIRMED")), deadline);
        var json = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        var result = java.util.Map.of("originalClaim", task, "report", report,
            "started", started, "deadline", deadline.expiresAt(), "finished", Instant.now(),
            "scope", "test-only fresh 1s repository transaction; original runOnce deadline unchanged");
        Files.writeString(directory.resolve("late-settle-result.tmp"), json.writeValueAsString(result));
        Files.move(directory.resolve("late-settle-result.tmp"), directory.resolve("late-settle-result.json"),
            StandardCopyOption.ATOMIC_MOVE);
      } catch (Exception e) {
        try { Files.writeString(directory.resolve("late-settle-failure"), e.toString()); }
        catch (java.io.IOException ignored) { }
        throw new IllegalStateException("late settlement probe failed", e);
      }
    }
  }

  /** 仅测试传输层阻塞真实ReservationHeld字节；暂停/恢复调用实际RecoveryService。 */
  static final class LateReceipt {
    final Path directory;
    final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    LateReceipt(Path directory) { this.directory = directory; }

    BeanPostProcessor transport() {
      return new BeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String name) {
          if (!(bean instanceof com.jay.config.V2RabbitConfig.V2Transport)
              || !Files.exists(directory.resolve("arm-receipt"))) return bean;
          try {
            var field = bean.getClass().getDeclaredField("template");
            field.setAccessible(true);
            var template = (org.springframework.amqp.rabbit.core.RabbitTemplate) field.get(bean);
            template.addBeforePublishPostProcessors(message -> {
              try {
                var event = json.readValue(message.getBody(), com.jay.domain.task.model.EventEnvelope.class);
                if (!event.eventType().equals("ReservationHeld")) return message;
                var marker = java.util.Map.of("event", event, "bodyBase64",
                    java.util.Base64.getEncoder().encodeToString(message.getBody()),
                    "headers", message.getMessageProperties().getHeaders(),
                    "messageId", message.getMessageProperties().getMessageId(),
                    "wireSha256", java.util.HexFormat.of().formatHex(
                        java.security.MessageDigest.getInstance("SHA-256").digest(message.getBody())),
                    "at", Instant.now(), "pid", ProcessHandle.current().pid());
                Files.writeString(directory.resolve("receipt-blocked.tmp"), json.writeValueAsString(marker));
                Files.move(directory.resolve("receipt-blocked.tmp"), directory.resolve("receipt-blocked.json"),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                while (!Files.exists(directory.resolve("release-receipt"))) Thread.sleep(20);
                return message;
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("receipt transport interrupted", e);
              } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException("receipt marker failed", e);
              }
            });
          } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("test transport template unavailable", e);
          }
          return bean;
        }
      };
    }

    void commands(org.springframework.context.ConfigurableApplicationContext context) {
      Thread.ofPlatform().daemon().name("test-only-recovery-commands").start(() -> {
        var executed = new java.util.HashSet<String>();
        while (context.isActive()) {
          try {
            for (String action : List.of("pause", "resume", "stop-consumers")) {
              Path command = directory.resolve("command-" + action + ".json");
              if (!Files.exists(command) || !executed.add(action)) continue;
              var input = json.readTree(Files.readString(command));
              String run = input.get("runId").asText();
              if (!run.matches("[a-z0-9_]{1,16}")
                  || !("router_perf_it_" + run + "_control").equals(System.getenv("V2_DB00")))
                throw new IllegalArgumentException("recovery command target run mismatch");
              if (action.equals("stop-consumers")) {
                var registry = context.getBean(org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry.class);
                registry.stop();
                var states = new java.util.LinkedHashMap<String, Boolean>();
                for (String id : List.of("v2-workflow", "v2-notification")) {
                  var container = java.util.Objects.requireNonNull(registry.getListenerContainer(id));
                  states.put(id, container.isRunning());
                }
                if (states.values().stream().anyMatch(Boolean::booleanValue))
                  throw new IllegalStateException("test consumer stop not complete");
                Files.writeString(directory.resolve(action + "-result.tmp"), json.writeValueAsString(
                    java.util.Map.of("command", input, "states", states, "at", Instant.now(), "success", true)));
                Files.move(directory.resolve(action + "-result.tmp"), directory.resolve(action + "-result.json"),
                    StandardCopyOption.ATOMIC_MOVE);
                continue;
              }
              var slot = new RouteSlot(input.get("dbKey").asText(), input.get("tableKey").asText());
              var recovery = context.getBean(com.jay.domain.task.service.RecoveryService.class);
              String order = input.get("orderId").asText();
              if (action.equals("pause"))
                recovery.pause(slot, "DRAW", order, com.jay.domain.task.model.RecoveryStatus.REVIEW,
                    "isolated test late ReservationHeld transport");
              else
                recovery.resume(slot, "DRAW", order, input.get("expectedVersion").asLong(),
                    "task8-isolated-operator", "verified original late ReservationHeld fact");
              Files.writeString(directory.resolve(action + "-result.tmp"), json.writeValueAsString(
                  java.util.Map.of("action", action, "orderId", order, "at", Instant.now(), "success", true,
                      "command", input, "operator", "task8-isolated-operator",
                      "reason", "verified original late ReservationHeld fact")));
              Files.move(directory.resolve(action + "-result.tmp"), directory.resolve(action + "-result.json"),
                  StandardCopyOption.ATOMIC_MOVE);
            }
            Thread.sleep(20);
          } catch (Exception e) {
            try { Files.writeString(directory.resolve("recovery-command-failure"), e.toString()); }
            catch (java.io.IOException ignored) { }
            return;
          }
        }
      });
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 2)
      throw new IllegalArgumentException("evidence directory and app arguments required");
    var directory = Path.of(args[0]);
    if (!Files.isDirectory(directory))
      throw new IllegalArgumentException("evidence directory missing");
    var late = new LateSettlement(directory);
    var receipt = new LateReceipt(directory);
    var builder = new SpringApplicationBuilder(com.jay.Application.class).profiles("v2-it");
    builder.initializers(
        context -> {
          var generic = (GenericApplicationContext) context;
          generic.registerBean(
              "task8ConfirmProbe",
              FaultInjector.class,
              () -> afterConfirm(directory, late::submitIfTriggered),
              definition -> definition.setPrimary(true));
          generic.registerBean("task8LateCapture", BeanPostProcessor.class, late::capture);
          generic.registerBean("task8ReceiptTransport", BeanPostProcessor.class, receipt::transport);
          generic.registerBean(
              "task8SettleProbe",
              BeanPostProcessor.class,
              () -> settleConnectionProbe(generic, directory));
        });
    var app = builder.run(Arrays.copyOfRange(args, 1, args.length));
    if (Files.exists(directory.resolve("arm-receipt")) || Files.exists(directory.resolve("arm-admin")))
      receipt.commands(app);
    if (Files.exists(directory.resolve("arm-sampler"))) {
      var prometheus = app.getBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class);
      Thread.ofPlatform().daemon().name("task8-metrics-snapshot").start(() -> {
        while (!Thread.currentThread().isInterrupted()) {
          try {
            Files.writeString(directory.resolve("metrics.tmp"), prometheus.scrape());
            Files.move(directory.resolve("metrics.tmp"), directory.resolve("metrics.prom"),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(1000);
          } catch (Exception error) {
            try { Files.writeString(directory.resolve("metrics-failure"), error.toString()); }
            catch (java.io.IOException ignored) { }
            return;
          }
        }
      });
    }
    int port =
        ((org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext) app)
            .getWebServer()
            .getPort();
    Files.writeString(
        directory.resolve("ready"),
        ProcessHandle.current().pid() + " " + port + " " + Instant.now());
  }
}
