package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.config.V2BudgetProperties;
import com.jay.config.V2FeatureProperties;
import com.jay.domain.asset.model.WorkClass;
import com.jay.infrastructure.transaction.BoundedDatabaseBudget;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

public class P0BudgetV2Test {
  private String profile() throws Exception {
    return new String(
        getClass()
            .getClassLoader()
            .getResourceAsStream("application-v2-p0.yml")
            .readAllBytes());
  }

  @Test
  public void p0ProfileDeclaresPoolAndBudget() throws Exception {
    var yaml = profile();
    assertThat(yaml).contains("maximum-pool-size: 72");
    assertThat(yaml)
        .contains("TRADE: 24")
        .contains("WORKER: 28")
        .contains("QUERY: 8")
        .contains("RELAY: 2")
        .contains("RECOVERY: 2");
    assertThat(yaml).contains("physical-connection-limits").contains("240");
    assertThat(yaml).contains("prefix: p0");
  }

  @Test
  public void p0ProfileDeclaresRemainingP0Settings() throws Exception {
    var yaml = profile();
    assertThat(yaml)
        .contains("minimum-idle: 0")
        .contains("connection-timeout: 1000")
        .contains("reserved-connections: 20")
        .contains("prefetch: 8")
        .contains("mode: bounded")
        .contains("max_connections=300");
  }

  @Test
  public void p0ProfileDeclaresAdmissionAndRaffle() throws Exception {
    var environment = new StandardEnvironment();
    new YamlPropertySourceLoader()
        .load("v2-p0", new ClassPathResource("application-v2-p0.yml"))
        .forEach(environment.getPropertySources()::addLast);
    assertThat(environment.getProperty("bm.v2.raffle.enabled", Boolean.class)).isTrue();
    var props =
        Binder.get(environment).bind("bm.v2", Bindable.of(V2FeatureProperties.class)).get();
    assertThat(props.getAssetWriteMode()).isEqualTo("V2");
    assertThat(props.getUserRps()).isEqualTo(20);
    assertThat(props.getActivityRps()).isEqualTo(100);
    assertThat(props.getMaxInFlight()).isEqualTo(24);
    assertThat(props.getLocalFallbackRps()).isEqualTo(1);
    assertThat(props.getQueryRps()).isEqualTo(100);
    assertThat(props.getQueryMaxInFlight()).isEqualTo(8);
    assertThat(props.getCancelRps()).isEqualTo(10);
    assertThat(props.getCancelMaxInFlight()).isEqualTo(2);
    props.validateAdmission();
    assertThat(props.getLocalFallbackRps())
        .isLessThanOrEqualTo(
            Math.min(
                props.getUserRps(), Math.min(props.getActivityRps(), props.getQueryRps())));
  }

  @Test
  public void p0ProfileBindsToStartupReadyBudget() throws Exception {
    var environment = new StandardEnvironment();
    new YamlPropertySourceLoader()
        .load("v2-p0", new ClassPathResource("application-v2-p0.yml"))
        .forEach(environment.getPropertySources()::addLast);
    var props =
        Binder.get(environment).bind("bm.v2.budget", Bindable.of(V2BudgetProperties.class)).get();
    assertThat(props.getInstances().get("mysql-it"))
        .containsEntry(WorkClass.TRADE, 24)
        .containsEntry(WorkClass.WORKER, 28)
        .containsEntry(WorkClass.QUERY, 8)
        .containsEntry(WorkClass.RELAY, 2)
        .containsEntry(WorkClass.RECOVERY, 2)
        .containsEntry(WorkClass.CALLBACK, 1);
    assertThat(props.getPhysicalConnectionLimits()).containsEntry("mysql-it", 240);
    assertThat(props.getReservedConnections()).isEqualTo(20);
    assertThat(props.getPrefetch()).isEqualTo(8);
    var pools = Map.of("00", 72, "01", 72, "02", 72);
    props.validateConnections(pools);
    assertThatCode(
            () ->
                new BoundedDatabaseBudget(
                    props.getInstances(),
                    new SimpleMeterRegistry(),
                    props.getTradePermitMaxWait()))
        .doesNotThrowAnyException();
  }

  @Test
  public void acceptsP0PoolAndBudget() {
    var p = new V2BudgetProperties();
    p.getInstances().get("mysql-it").put(WorkClass.TRADE, 24);
    p.getInstances().get("mysql-it").put(WorkClass.WORKER, 28);
    p.getInstances().get("mysql-it").put(WorkClass.QUERY, 8);
    p.getInstances().get("mysql-it").put(WorkClass.RELAY, 2);
    p.getInstances().get("mysql-it").put(WorkClass.RECOVERY, 2);
    p.setPhysicalConnectionLimits(Map.of("mysql-it", 240));
    p.validateConnections(Map.of("00", 72, "01", 72, "02", 72)); // 不抛
  }

  @Test
  public void rejectsBudgetNotBelowPool() {
    var p = new V2BudgetProperties();
    p.setPhysicalConnectionLimits(Map.of("mysql-it", 240));
    p.getInstances().get("mysql-it").put(WorkClass.TRADE, 72);
    assertThatThrownBy(() -> p.validateConnections(Map.of("00", 72, "01", 72, "02", 72)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
