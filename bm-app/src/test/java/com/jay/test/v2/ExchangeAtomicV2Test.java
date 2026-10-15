package com.jay.test.v2;
import static org.assertj.core.api.Assertions.*;
import com.jay.domain.asset.model.*;import com.jay.domain.asset.service.*;import com.jay.domain.exchange.model.*;
import java.math.BigDecimal;import java.time.*;import java.util.*;import org.junit.Test;
import com.jay.test.v2.support.ConcurrentProbe;
public class ExchangeAtomicV2Test {
 @Test public void retriesRejectsAndRollbackAreAtomic(){AssetTestSupport.run(h->{
  var f=new BusinessTestSupport(h);String u="ex-"+UUID.randomUUID();
  h.tx(u,()->h.assets.credit(u,new BusinessId("OPEN","1","a".repeat(64)),new BigDecimal("100")));
  var cmd=new ExchangeCommand(u,"same",123,"v1",new Deadline(Instant.now().plusSeconds(120)));
  var results=ConcurrentProbe.run(100,()->f.exchange.exchange(cmd));assertThat(new HashSet<>(results)).hasSize(1);assertThat(results.getFirst().state()).isEqualTo("DELIVERED");assertThat(h.balance(u)).isEqualByComparingTo("90");
  f.configAvailable=false;assertThat(f.exchange.exchange(cmd)).isEqualTo(results.getFirst());f.configAvailable=true;
  assertThatThrownBy(()->f.exchange.exchange(new ExchangeCommand(u,"same",999,"v1",cmd.deadline()))).isInstanceOf(AssetConflictException.class);
  f.failure="exchange.after-debit";assertThatThrownBy(()->f.exchange.exchange(new ExchangeCommand(u,"fault",123,"v1",cmd.deadline()))).isInstanceOf(IllegalStateException.class);assertThat(h.balance(u)).isEqualByComparingTo("90");f.failure="";
  var poor=new ExchangeCommand("poor-"+UUID.randomUUID(),"poor",123,"v1",cmd.deadline());assertThat(f.exchange.exchange(poor).state()).isEqualTo("REJECTED");h.tx(poor.userId(),()->h.assets.credit(poor.userId(),new BusinessId("OPEN","x","a".repeat(64)),new BigDecimal("100")));assertThat(f.exchange.exchange(poor).state()).isEqualTo("REJECTED");
 });}
}
