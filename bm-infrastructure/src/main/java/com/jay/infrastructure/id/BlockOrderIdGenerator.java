package com.jay.infrastructure.id;
import com.jay.domain.asset.service.OrderIdGenerator;import com.jay.domain.asset.repository.UserTransactionRunner;import com.jay.domain.asset.model.*;
import java.time.Instant;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.transaction.support.TransactionSynchronizationManager;
public final class BlockOrderIdGenerator implements OrderIdGenerator {
 private final UserTransactionRunner runner;private final JdbcTemplate j;private final String node;private long next,end;
 public BlockOrderIdGenerator(UserTransactionRunner runner,JdbcTemplate j,String node){if(node==null||!node.matches("[A-Za-z0-9]{1,24}"))throw new IllegalArgumentException("Invalid node");this.runner=runner;this.j=j;this.node=node;}
 public synchronized void prefetch(){
  if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Never allocate inside business transaction");
  if(next<end)return;
  long start=runner.inControl(WorkClass.RECOVERY,new Deadline(Instant.now().plusSeconds(10)),()->{
   Long value=j.queryForObject("select next_value from id_block where name='global' for update",Long.class);
   if(value==null||value<=0||value>Long.MAX_VALUE-1000)throw new IllegalStateException("ID sequence exhausted");
   if(j.update("update id_block set next_value=? where name='global' and next_value=?",value+1000,value)!=1)throw new IllegalStateException("ID allocation conflict");return value;
  }); // local publication happens only AFTER confirmed transaction return
  next=start;end=start+1000;
 }
 public synchronized String next(String type,RouteSlot slot){
  if(type==null||!type.matches("[A-Z][A-Z0-9_]{0,31}"))throw new IllegalArgumentException("Invalid ID type");
  if(next>=end)throw new IllegalStateException("ID block empty; prefetch outside transaction");
  String route=slot.dbKey()+"."+(slot.tableKey()==null?"000":slot.tableKey());
  return type+"-"+route+"-"+node+"-"+next++;
 }
}
