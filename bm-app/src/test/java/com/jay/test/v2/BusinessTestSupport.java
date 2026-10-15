package com.jay.test.v2;
import com.jay.domain.asset.model.*;import com.jay.domain.asset.service.*;import com.jay.domain.activity.service.*;import com.jay.domain.activity.repository.*;import com.jay.domain.exchange.service.*;import com.jay.domain.rebate.service.*;import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.id.BlockOrderIdGenerator;import java.time.*;import java.math.*;import java.util.*;
final class BusinessTestSupport {
 boolean configAvailable=true;String failure="";final ExchangeService exchange;final SignInService signin;
 BusinessTestSupport(AssetTestSupport h){
  h.runner.inControl(WorkClass.RECOVERY,new Deadline(Instant.now().plusSeconds(30)),()->{try{for(String s:java.nio.file.Files.readString(java.nio.file.Path.of("../docs/dev-ops/mysql/migrations/001_id_block.sql")).split(";"))if(!s.isBlank())h.j.execute(s);}catch(Exception e){throw new IllegalStateException(e);}return null;});
  var ids=new BlockOrderIdGenerator(h.runner,h.j,"test");h.codec.withGenerator(ids);
  AdmissionRepository config=new AdmissionRepository(){public AdmissionSnapshot exchange(long sku,String version,Deadline deadline){return snapshot(deadline);}public AdmissionSnapshot signIn(LocalDate date,Deadline deadline){return snapshot(deadline);} };
  var admission=new AdmissionService(config,h.runner);
  var out=new MySqlOutboxWriter(h.j.getDataSource());var q=new QuotaKernel(h.repo);
  exchange=new ExchangeService(h.runner,h.assets,q,new MySqlExchangeRepository(h.j.getDataSource()),admission,h.codec,ids,out,p->{if(p.equals(failure))throw new IllegalStateException("injected");});
  signin=new SignInService(h.runner,h.assets,q,new MySqlSignInRepository(h.j.getDataSource()),admission,h.codec,ids,out,p->{if(p.equals(failure))throw new IllegalStateException("injected");});
 }
 AdmissionSnapshot snapshot(Deadline d){if(!configAvailable)throw new IllegalStateException("db00 unavailable");return new AdmissionSnapshot(12,"OPEN","v1",Instant.parse("2026-01-01T00:00:00Z"),Instant.parse("2027-01-01T00:00:00Z"),Instant.now(),d.expiresAt(),false,new BigDecimal("10.00"),new QuotaDelta(5,1,2),List.of(new AdmissionSnapshot.Reward("credit",0,new BigDecimal("2.00"),null),new AdmissionSnapshot.Reward("quota",12,null,new QuotaDelta(5,1,2))));}
}
