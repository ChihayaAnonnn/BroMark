package com.jay.test.v2;
import static org.assertj.core.api.Assertions.*;
import com.jay.domain.asset.model.*;import com.jay.domain.asset.service.*;import com.jay.infrastructure.id.BlockOrderIdGenerator;
import java.time.*;import java.util.*;import org.junit.Test;
public class OrderIdV2Test {
 @Test public void committedBlocksAreUniqueAcrossInstancesAndRestart() {AssetTestSupport.run(h->{
  h.runner.inControl(WorkClass.RECOVERY,new Deadline(Instant.now().plusSeconds(30)),()->{try{for(String s:java.nio.file.Files.readString(java.nio.file.Path.of("../docs/dev-ops/mysql/migrations/001_id_block.sql")).split(";"))if(!s.isBlank())h.j.execute(s);}catch(Exception e){throw new IllegalStateException(e);}return null;});
  var a=new BlockOrderIdGenerator(h.runner,h.j,"node1");var b=new BlockOrderIdGenerator(h.runner,h.j,"node2");
  a.prefetch();b.prefetch();var ids=new HashSet<String>();var slot=h.codec.routeUser("user-0");
  for(int i=0;i<1000;i++){assertThat(ids.add(a.next("EX",slot))).isTrue();assertThat(ids.add(b.next("EX",slot))).isTrue();}
  assertThatThrownBy(()->a.next("EX",slot)).isInstanceOf(IllegalStateException.class);
  a.prefetch();String next=a.next("EX",slot);assertThat(h.codec.routeOrder(next)).isEqualTo(slot);assertThat(ids.add(next)).isTrue();
  var restart=new BlockOrderIdGenerator(h.runner,h.j,"node1");restart.prefetch();assertThat(ids.add(restart.next("EX",slot))).isTrue();
  assertThatThrownBy(()->h.tx("user-0",()->{restart.prefetch();return null;})).isInstanceOf(IllegalStateException.class);
 });}
}
