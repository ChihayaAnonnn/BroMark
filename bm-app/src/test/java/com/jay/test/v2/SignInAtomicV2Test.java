package com.jay.test.v2;
import static org.assertj.core.api.Assertions.*;import com.jay.domain.asset.model.*;import com.jay.domain.rebate.model.*;
import java.time.*;import java.util.*;import org.junit.Test;import com.jay.test.v2.support.ConcurrentProbe;
public class SignInAtomicV2Test {
 @Test public void differentKeysCannotMultiplyRewardsAndFailureRollsBackAll(){AssetTestSupport.run(h->{
  var f=new BusinessTestSupport(h);String u="sign-"+UUID.randomUUID();var date=LocalDate.now(ZoneId.of("Asia/Shanghai"));var deadline=new Deadline(Instant.now().plusSeconds(120));
  var results=ConcurrentProbe.run(100,()->f.signin.signIn(new SignInCommand(u,UUID.randomUUID().toString(),date,deadline)));
  assertThat(results.stream().map(x->x.orderId()).distinct().count()).isEqualTo(1);assertThat(h.balance(u)).isEqualByComparingTo("2.00");
  f.configAvailable=false;assertThat(f.signin.signIn(new SignInCommand(u,"new",date,deadline)).orderId()).isEqualTo(results.getFirst().orderId());f.configAvailable=true;
  String v="sign-fault-"+UUID.randomUUID();f.failure="signin.after-reward";assertThatThrownBy(()->f.signin.signIn(new SignInCommand(v,"key",date,deadline))).isInstanceOf(IllegalStateException.class);assertThat(h.balance(v)).isEqualByComparingTo("0");f.failure="";
  assertThat(f.signin.signIn(new SignInCommand(v,"key",date,deadline)).state()).isEqualTo("DELIVERED");
  assertThat(f.signin.signIn(new SignInCommand(v,"past",date.minusDays(1),deadline)).state()).isEqualTo("REJECTED");
  assertThatThrownBy(()->f.signin.signIn(new SignInCommand(v,"key",date.plusDays(1),deadline))).isInstanceOf(AssetConflictException.class);
 });}
}
