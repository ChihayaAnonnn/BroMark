package com.jay.test.v2;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.RouteCodec;
import com.jay.infrastructure.transaction.*;
import cn.bugstack.middleware.db.router.DBContextHolder;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.time.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
public class RoutingTransactionV2Test {
 @Test public void reusesThreadRollsBackAndScansEverySlot() throws Exception {Java25RouterIntegrationTest.databaseContext().run(c->{
  assertThat(c).hasNotFailed();var ds=c.getBean(DataSource.class);var codec=new RouteCodec(2,4);var runner=new RoutedTransactionRunner(ds,c.getBean(org.springframework.transaction.PlatformTransactionManager.class),codec);var j=new JdbcTemplate(ds);var clock=new DatabaseBusinessClock(ds);
  assertThat(codec.routeUser("user-0")).isEqualTo(new RouteSlot("02","001"));assertThat(codec.routeOrder("DRAW-02.001-node1-123")).isEqualTo(new RouteSlot("02","001"));assertThatThrownBy(()->codec.routeOrder("DRAW-99.999-n-1")).isInstanceOf(IllegalArgumentException.class);
  var deadline=new Deadline(Instant.now().plusSeconds(30));
  for(String db:new String[]{"01","02"})for(int tb=0;tb<4;tb++){var slot=new RouteSlot(db,String.format("%03d",tb));runner.inSlot(slot,WorkClass.TRADE,deadline,()->{j.execute("create table if not exists tx_probe_"+slot.tableKey()+"(id int primary key) ENGINE=InnoDB");j.update("delete from tx_probe_"+slot.tableKey());return null;});
   assertThatThrownBy(()->runner.inSlot(slot,WorkClass.TRADE,deadline,()->{j.update("insert into tx_probe_"+slot.tableKey()+" values(1)");throw new IllegalStateException("injected");})).isInstanceOf(IllegalStateException.class);
   assertThat(runner.inSlot(slot,WorkClass.QUERY,deadline,()->j.queryForObject("select count(*) from tx_probe_"+slot.tableKey(),Integer.class))).isZero();
   assertThat(DBContextHolder.getDBKey()).isNull();assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }
  runner.inUser("user-0",WorkClass.TRADE,deadline,()->{assertThat(j.queryForObject("select database()",String.class)).isEqualTo("router_two");assertThat(j.queryForObject("select @@transaction_isolation",String.class)).isEqualTo("READ-COMMITTED");BusinessTime time=clock.now();assertThat(time.businessDate()).isEqualTo(time.instant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate());assertThatThrownBy(()->runner.inUser("user-4",WorkClass.TRADE,deadline,()->null)).isInstanceOf(IllegalStateException.class);return null;});
  assertThat(runner.inControl(WorkClass.QUERY,deadline,()->j.queryForObject("select database()",String.class))).isEqualTo("router_control");
  try(var executor=Executors.newSingleThreadExecutor()){assertThat(executor.submit(()->runner.inSlot(new RouteSlot("01","003"),WorkClass.RECOVERY,deadline,()->j.queryForObject("select database()",String.class))).get()).isEqualTo("router_one");}catch(Exception e){throw new AssertionError(e);}
 });}
}
