package com.jay.test.v2;
import static org.assertj.core.api.Assertions.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.*;
import com.jay.infrastructure.event.V2ConfirmedEventPublisher;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.*;

public class PublisherConfirmV2Test {
  static CachingConnectionFactory connection() {
    var c=new CachingConnectionFactory(System.getenv().getOrDefault("V2_RABBIT_HOST","host.docker.internal"),Integer.parseInt(System.getenv().getOrDefault("V2_RABBIT_PORT","15686")));
    c.setUsername("bromark_it");c.setPassword(Objects.requireNonNull(System.getenv("V2_RABBIT_PASSWORD")));
    c.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);c.setPublisherReturns(true);c.setConnectionTimeout(1000);
    return c;
  }
  static EventEnvelope event(String id) {return new EventEnvelope(id,"ExchangeCompleted","order",new BusinessId("EX",id,"a".repeat(64)),"owner",new RouteSlot("01","000"),"a".repeat(64),1,1,"{}");}
  @Test public void requiresConfirmWithoutReturnAndRetriesUnknownCommit() throws Exception {
    var cf=connection();
    try {
      var admin=new RabbitAdmin(cf);String ex="v2.it."+UUID.randomUUID(),q=ex+".q";
      admin.declareExchange(new DirectExchange(ex,true,false));admin.declareQueue(new org.springframework.amqp.core.Queue(q,true));admin.declareBinding(new Binding(q,Binding.DestinationType.QUEUE,ex,"ExchangeCompleted",null));
      var template=new RabbitTemplate(cf);template.setMandatory(true);
      try(var publisher=new V2ConfirmedEventPublisher(template,ex,4,Duration.ofSeconds(2))) {
        assertThat(publisher.publish(event("ok"),new Deadline(Instant.now().plusSeconds(3))).toCompletableFuture().get(4,TimeUnit.SECONDS)).isEqualTo(PublishOutcome.CONFIRMED);
        var wrong=new EventEnvelope("bad","NoBinding","order",new BusinessId("EX","bad","a".repeat(64)),null,RouteSlot.control(),"a".repeat(64),1,1,"{}");
        assertThat(publisher.publish(wrong,new Deadline(Instant.now().plusSeconds(3))).toCompletableFuture().get(4,TimeUnit.SECONDS)).isEqualTo(PublishOutcome.RETURNED);
        AssetTestSupport.run(h -> {
          var repo=new MySqlOutboxRepository(h.j.getDataSource(),h.runner);var writer=new MySqlOutboxWriter(h.j.getDataSource());
          h.runner.inControl(WorkClass.RECOVERY,new Deadline(Instant.now().plusSeconds(30)),()->{h.j.update("delete from outbox");writer.append(event("confirm-"+UUID.randomUUID()));writer.append(wrong);return null;});
          var relay=new OutboxRelay(repo,publisher,new RetryPolicy(),point -> {if(point.equals("relay.after-confirm")) throw new IllegalStateException("crash before completion");});
          assertThatThrownBy(()->relay.runOnce(RouteSlot.control(),2)).isInstanceOf(IllegalStateException.class);
          h.j.update("update outbox set lease_until=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND where state='PROCESSING'");
          new OutboxRelay(repo,publisher,new RetryPolicy(),point->{}).runOnce(RouteSlot.control(),2);
          assertThat(h.j.queryForObject("select state from outbox where event_id='bad'",String.class)).isEqualTo("PENDING");
          assertThat(h.j.queryForObject("select count(*) from outbox where state='PUBLISHED'",Integer.class)).isOne();
          assertThat(admin.getQueueInfo(q).getMessageCount()).isGreaterThanOrEqualTo(3);
        });
        assertThat(publisher.publish(event("expired"),new Deadline(Instant.now().minusSeconds(1))).toCompletableFuture().join()).isEqualTo(PublishOutcome.UNKNOWN);
      } finally {admin.deleteQueue(q);admin.deleteExchange(ex);}
    } finally {cf.destroy();}
  }
}
