package com.jay.domain.exchange.service;
import com.jay.domain.asset.model.*;import com.jay.domain.asset.service.*;import com.jay.domain.asset.repository.UserTransactionRunner;import com.jay.domain.activity.service.AdmissionService;import com.jay.domain.exchange.model.*;import com.jay.domain.exchange.repository.ExchangeRepository;import com.jay.domain.task.repository.OutboxWriter;import com.jay.domain.task.model.EventEnvelope;import java.util.*;
public final class ExchangeService {
 private final UserTransactionRunner tx;private final AssetKernel assets;private final QuotaKernel quota;private final ExchangeRepository repo;private final AdmissionService admission;private final RouteCodec codec;private final OrderIdGenerator ids;private final OutboxWriter out;private final FaultInjector faults;
 public ExchangeService(UserTransactionRunner tx,AssetKernel assets,QuotaKernel quota,ExchangeRepository repo,AdmissionService admission,RouteCodec codec,OrderIdGenerator ids,OutboxWriter out,FaultInjector faults){this.tx=tx;this.assets=assets;this.quota=quota;this.repo=repo;this.admission=admission;this.codec=codec;this.ids=ids;this.out=out;this.faults=faults;}
 private RequestIdentity identity(ExchangeCommand c){if(c.sku()<=0||c.configVersion()==null||c.configVersion().isBlank()||c.configVersion().length()>64)throw new IllegalArgumentException("Invalid SKU/version");return new RequestIdentity(c.userId(),"EXCHANGE",c.requestId(),new RequestHasher().hash("EXCHANGE",Map.of("sku",Long.toString(c.sku()),"version",c.configVersion())));}
 public ExchangeResult exchange(ExchangeCommand c){
  var identity=identity(c);var old=tx.inUser(c.userId(),WorkClass.QUERY,c.deadline(),()->repo.request(identity));if(old.isPresent())return old.get();
  var snapshot=admission.forExchange(c.userId(),c.sku(),c.configVersion(),c.deadline());ids.prefetch();String order=codec.newOrderId("EX",c.userId());String event=codec.newEventId();
  return tx.inUser(c.userId(),WorkClass.TRADE,c.deadline(),()->{
   var time=assets.lockAccounts(c.userId(),List.of(snapshot.activityId()));var repeat=repo.request(identity);if(repeat.isPresent())return repeat.get();
   ExchangeResult result;var biz=new BusinessId("EXCHANGE",order,identity.requestHash());
   if(!snapshot.accepts(time)||!snapshot.version().equals(c.configVersion()))result=new ExchangeResult(order,"REJECTED",null,null,"ACTIVITY_UNAVAILABLE");
   else {var debit=assets.debit(c.userId(),biz,snapshot.creditCost());if(debit.isEmpty())result=new ExchangeResult(order,"REJECTED",null,null,"INSUFFICIENT_CREDIT");else{
    faults.hit("exchange.after-debit");var grant=quota.grant(c.userId(),snapshot.activityId(),biz,snapshot.quota(),time);result=new ExchangeResult(order,"DELIVERED",debit.get(),grant,null);
   }}
   repo.save(identity,snapshot,time,result);
   if("DELIVERED".equals(result.state()))out.append(new EventEnvelope(event,"ExchangeCompleted",order,biz,c.userId(),codec.routeUser(c.userId()),identity.requestHash(),1,1,com.jay.domain.asset.service.ReceiptJson.write(result)));
   return result;
  });
 }
 public ExchangeResult get(String user,String order){if(!codec.routeOrder(order).equals(codec.routeUser(user)))throw new AssetOwnershipException();return tx.inUser(user,WorkClass.QUERY,new Deadline(java.time.Instant.now().plusSeconds(5)),()->repo.order(user,order).orElseThrow(AssetNotFoundException::new));}
}

