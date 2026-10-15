package com.jay.test.v2;
import static org.assertj.core.api.Assertions.*;
import com.jay.domain.asset.model.*;import com.jay.domain.task.model.EventEnvelope;import com.jay.infrastructure.adapter.repository.v2.MySqlOutboxWriter;
import java.math.BigDecimal;import java.util.*;import org.junit.Test;
public class OutboxAtomicV2Test {
 @Test public void commitsWithAssetAndRollsBackOnFailure(){AssetTestSupport.run(h->{
  String u="outbox-"+UUID.randomUUID();var writer=new MySqlOutboxWriter(h.j.getDataSource());
  var event=new EventEnvelope("e-"+UUID.randomUUID(),"ExchangeCompleted","order",new BusinessId("EX","x","a".repeat(64)),u,h.codec.routeUser(u),"a".repeat(64),1,1,"{\"credit\":\"1.00\"}");
  assertThatThrownBy(()->h.tx(u,()->{h.assets.credit(u,event.bizId(),BigDecimal.ONE);writer.append(event);throw new IllegalStateException("injected");})).isInstanceOf(IllegalStateException.class);
  assertThat(h.balance(u)).isEqualByComparingTo("0");
  assertThat(h.tx(u,()->h.j.queryForObject("select count(*) from outbox where event_id=?",Integer.class,event.eventId()))).isZero();
  h.tx(u,()->{h.assets.credit(u,event.bizId(),BigDecimal.ONE);writer.append(event);writer.append(event);return null;});
  assertThat(h.tx(u,()->h.j.queryForObject("select count(*) from outbox where event_id=? and state='PENDING'",Integer.class,event.eventId()))).isOne();
  assertThatThrownBy(()->writer.append(event)).isInstanceOf(IllegalStateException.class);
 });}
}
