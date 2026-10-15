package com.jay.infrastructure.transaction;
import cn.bugstack.middleware.db.router.DBContextHolder;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.RouteCodec;
import com.jay.domain.asset.repository.UserTransactionRunner;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
public final class RoutedTransactionRunner implements UserTransactionRunner {
 private final TransactionTemplate tx;private final RouteCodec codec;
 private record Execution(RouteSlot slot,WorkClass work){}
 private final ThreadLocal<Execution> current=new ThreadLocal<>();
 public RoutedTransactionRunner(DataSource ds,PlatformTransactionManager manager,RouteCodec codec){this.codec=codec;tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);}
 public <T>T inUser(String user,WorkClass work,Deadline deadline,Supplier<T> action){return inSlot(codec.routeUser(user),work,deadline,action);}
 public <T>T inControl(WorkClass work,Deadline deadline,Supplier<T> action){return inSlot(RouteSlot.control(),work,deadline,action);}
 public <T>T inSlot(RouteSlot slot,WorkClass work,Deadline deadline,Supplier<T> action){
  java.util.Objects.requireNonNull(slot);java.util.Objects.requireNonNull(work);java.util.Objects.requireNonNull(action);
  if(deadline.remaining().isNegative()||deadline.remaining().isZero())throw new IllegalStateException("Deadline exceeded");
  Execution outer=current.get();if(outer!=null){if(!outer.slot().equals(slot)||outer.work()!=work)throw new IllegalStateException("Nested route/work change is forbidden");return action.get();}
  if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Enter routed runner before starting a transaction");
  String previousDb=DBContextHolder.getDBKey(),previousTb=DBContextHolder.getTBKey();
  try{DBContextHolder.setRoute(slot.dbKey(),slot.tableKey());current.set(new Execution(slot,work));return tx.execute(s->action.get());}
  finally{current.remove();DBContextHolder.setRoute(previousDb,previousTb);}
 }
}
