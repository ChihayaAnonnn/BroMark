package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.alibaba.fastjson.JSON;
import com.jay.config.RedisClientConfig;
import com.jay.infrastructure.dao.po.UserCreditOrder;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class MiddlewareSmokeV2Test {
  @Test
  public void configuredRedissonAndSerializationWorkOnJava25() {
    new ApplicationContextRunner()
        .withUserConfiguration(RedisClientConfig.class)
        .withPropertyValues(
            "redis.sdk.config.host="+System.getenv().getOrDefault("V2_REDIS_HOST","host.docker.internal"),
            "redis.sdk.config.port="+System.getenv().getOrDefault("V2_REDIS_PORT","16386"),
            "redis.sdk.config.pool-size=4",
            "redis.sdk.config.min-idle-size=1")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              var redis = c.getBean(RedissonClient.class);
              String key = "bromark:it:" + UUID.randomUUID();
              try {
                var b = redis.<String>getBucket(key);
                b.set("ok", Duration.ofSeconds(30));
                assertThat(b.get()).isEqualTo("ok");
                UserCreditOrder order = new UserCreditOrder();
                order.setTradeAmount(new BigDecimal("1.25"));
                assertThat(
                        JSON.parseObject(JSON.toJSONString(order), UserCreditOrder.class)
                            .getTradeAmount())
                    .isEqualByComparingTo("1.25");
              } finally {
                redis.getBucket(key).delete();
              }
            });
  }

  @Test
  public void rabbitRoundTripUsesIsolatedQueue() {
    var cf = new CachingConnectionFactory(System.getenv().getOrDefault("V2_RABBIT_HOST","host.docker.internal"),Integer.parseInt(System.getenv().getOrDefault("V2_RABBIT_PORT","15686")));
    cf.setUsername("bromark_it");
    cf.setPassword(java.util.Objects.requireNonNull(System.getenv("V2_RABBIT_PASSWORD")));
    String name = "bromark.it." + UUID.randomUUID();
    try {
      var admin = new RabbitAdmin(cf);
      admin.declareQueue(new Queue(name, false, false, true));
      var template = new RabbitTemplate(cf);
      template.convertAndSend("", name, "java25");
      assertThat(template.receiveAndConvert(name, 5000)).isEqualTo("java25");
      admin.deleteQueue(name);
    } finally {
      cf.destroy();
    }
  }
}
