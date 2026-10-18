package com.jay.test.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.infrastructure.transaction.BoundedDatabaseBudget;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.*;

/** 仅供隔离压测启动器使用，不打入生产包。 */
final class UnlimitedLoadMode {
  static void validate(Environment e) {
    if (!e.acceptsProfiles(Profiles.of("v2-it")))
      throw new IllegalArgumentException("Unlimited load requires v2-it");
    var matcher =
        java.util.regex.Pattern.compile("router_perf_it_([a-z0-9]{1,16})_control")
            .matcher(e.getProperty("V2_DB00", ""));
    if (!matcher.matches()) throw new IllegalArgumentException("Isolated load schema required");
    String run = matcher.group(1);
    if (!("router_perf_it_" + run + "_one").equals(e.getProperty("V2_DB01"))
        || !("router_perf_it_" + run + "_two").equals(e.getProperty("V2_DB02")))
      throw new IllegalArgumentException("Matching isolated load schemas required");
  }

  static final class UnlimitedAdmission implements RequestAdmission {
    private Permit admit(Deadline deadline) {
      if (deadline.remaining().isNegative() || deadline.remaining().isZero())
        throw new BudgetExceededException("Request deadline exceeded");
      return () -> {};
    }

    public Permit enter(String user, Deadline d) {
      return admit(d);
    }

    public Permit query(String user, Deadline d) {
      return admit(d);
    }

    public Permit cancel(String user, Deadline d) {
      return admit(d);
    }

    public void checkActivity(String activity, Deadline d) {
      admit(d);
    }
  }

  static BeanPostProcessor processor(
      Map<String, Map<WorkClass, Integer>> configured, MeterRegistry registry) {
    return new BeanPostProcessor() {
      public Object postProcessAfterInitialization(Object bean, String name) {
        if (name.equals("requestAdmission") && bean instanceof RequestAdmission)
          return new UnlimitedAdmission();
        if (name.equals("databaseBudget") && bean instanceof BoundedDatabaseBudget) {
          var limits = new HashMap<String, Map<WorkClass, Integer>>();
          for (String physical : configured.keySet()) {
            var work = new EnumMap<WorkClass, Integer>(WorkClass.class);
            for (var w : WorkClass.values()) work.put(w, Integer.MAX_VALUE);
            limits.put(physical, work);
          }
          // 业务信号量不再形成瓶颈；继续复用指标、在途计数及有序关闭。
          return new BoundedDatabaseBudget(limits, registry, Duration.ofSeconds(1));
        }
        return bean;
      }
    };
  }

  static void install(GenericApplicationContext context) {
    if (!context.getEnvironment().getProperty("probe.unlimited", Boolean.class, false)) return;
    validate(context.getEnvironment());
    context.registerBean(
        "unlimitedLoadPostProcessor",
        BeanPostProcessor.class,
        () ->
            new BeanPostProcessor() {
              public Object postProcessAfterInitialization(Object bean, String name) {
                if (!name.equals("requestAdmission") && !name.equals("databaseBudget")) return bean;
                return processor(
                        context.getBean(com.jay.config.V2BudgetProperties.class).getInstances(),
                        context.getBean(MeterRegistry.class))
                    .postProcessAfterInitialization(bean, name);
              }
            });
  }

  static void verifyRuntime(GenericApplicationContext context, Path directory) throws Exception {
    if (!context.getEnvironment().getProperty("probe.unlimited", Boolean.class, false)) return;
    if (!(context.getBean(RequestAdmission.class) instanceof UnlimitedAdmission))
      throw new IllegalStateException("Unlimited admission was not installed");
    var budget = context.getBean(BoundedDatabaseBudget.class);
    var permits = new TreeMap<String, Integer>();
    for (String physical :
        context.getBean(com.jay.config.V2BudgetProperties.class).getInstances().keySet())
      for (var work : WorkClass.values()) {
        int n = budget.available(physical, work);
        if (n < 1_000_000_000) throw new IllegalStateException("Database permit bypass missing");
        permits.put(physical + ":" + work.name(), n);
      }
    var result =
        Map.of(
            "mode",
            "test-only-unlimited",
            "entryRpsDisabled",
            true,
            "entryInFlightDisabled",
            true,
            "databasePermitsPerClass",
            Integer.MAX_VALUE,
            "availablePermitsAtStartup",
            permits,
            "retained",
            List.of(
                "2 CPU / 4 GiB enforced by Docker",
                "Hikari pool capacity",
                "worker scheduling capacity",
                "request and SQL deadlines",
                "authentication",
                "business invariants",
                "shutdown tracking"));
    Files.writeString(
        directory.resolve("unlimited-runtime.json"),
        new com.fasterxml.jackson.databind.ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(result));
  }
}
