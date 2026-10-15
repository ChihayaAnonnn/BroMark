package com.jay.test.v2;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.infrastructure.adapter.repository.v2.MySqlAssetRepository;
import com.jay.infrastructure.transaction.*;
import javax.sql.DataSource;
import java.nio.file.*;
import java.time.*;
import java.util.function.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
public final class AssetTestSupport {
 final JdbcTemplate j; final RoutedTransactionRunner runner; final AssetKernel assets; final MySqlAssetRepository repo;
 final RouteCodec codec=new RouteCodec(2,4);
 AssetTestSupport(DataSource ds,PlatformTransactionManager tm){
  j=new JdbcTemplate(ds);runner=new RoutedTransactionRunner(ds,tm,codec);
  repo=new MySqlAssetRepository(ds);assets=new AssetKernel(repo,new DatabaseBusinessClock(ds));
 }
 interface CheckedConsumer {void accept(AssetTestSupport h) throws Exception;}
 static void run(CheckedConsumer test){Java25RouterIntegrationTest.databaseContext().run(c->{
  org.assertj.core.api.Assertions.assertThat(c).hasNotFailed();
  var h=new AssetTestSupport(c.getBean(DataSource.class),c.getBean(PlatformTransactionManager.class));
  for(String db: new String[]{"01","02"})for(int i=0;i<4;i++){
   var slot=new RouteSlot(db,String.format("%03d",i));
   h.runner.inSlot(slot,WorkClass.RECOVERY,new Deadline(Instant.now().plusSeconds(60)),()->{
    try {for(String file: new String[]{"001_asset_additive.sql","001_outbox_base.sql","002_signin_exchange.sql"}){
     var p=Path.of("../docs/dev-ops/mysql/migrations/"+file);if(!Files.exists(p))continue;
     for(String s:Files.readString(p).replace("{{suffix}}",slot.tableKey()).split(";"))if(!s.isBlank())h.j.execute(s);
    }}catch(Exception e){throw new IllegalStateException(e);}return null;
   });
  }
  try{test.accept(h);}catch(Exception e){throw new AssertionError(e);}
 });}
 <T>T tx(String u,Supplier<T> action){return runner.inUser(u,WorkClass.TRADE,new Deadline(Instant.now().plusSeconds(120)),()->{assets.lockAccounts(u,java.util.List.of());return action.get();});}
 java.math.BigDecimal balance(String u){return tx(u,()->j.queryForObject("select balance from user_credit_account where user_id=?",java.math.BigDecimal.class,u));}
}
