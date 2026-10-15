package com.jay.domain.strategy.service;
import com.jay.domain.strategy.model.*;import com.jay.domain.strategy.repository.*;import com.jay.domain.asset.model.*;import com.jay.domain.asset.repository.*;import com.jay.domain.asset.service.*;import java.util.*;import java.time.*;import com.jay.domain.strategy.model.entity.StrategyAwardEntity;import com.jay.domain.strategy.service.armory.algorithm.impl.AliasMethodAlgorithm;
public final class StrategyPublicationService {
 private final UserTransactionRunner tx;private final StrategyVersionRepository repo;
 public StrategyPublicationService(UserTransactionRunner t,StrategyVersionRepository r){tx=t;repo=r;}
 public String publish(PublishStrategyCommand c){
 if(c.activityId()<=0||c.strategyId()<=0||!Set.of("OPEN","CLOSED").contains(c.state())||!c.beginsAt().isBefore(c.endsAt())||c.prizes().isEmpty()||c.prizes().size()>1024)throw new IllegalArgumentException("Invalid strategy");
 var ids=new HashSet<Integer>();for(var p:c.prizes()){validate(p);if(!ids.add(p.awardId()))throw new IllegalArgumentException("Duplicate award");}validate(c.fallback());if(c.fallback().stockPoolId()!=null||c.fallback().unlockAfter()!=0)throw new IllegalArgumentException("Fallback must be unlimited");
 if(c.blacklistAwardId()!=null&&!ids.contains(c.blacklistAwardId()))throw new IllegalArgumentException("Missing blacklist award");
 var tables=new HashMap<String,FrozenStrategy.Table>();tables.put("default",build(c.prizes()));
 for(var e:c.weightedPools().entrySet()){if(e.getKey()<0||e.getValue().isEmpty()||new HashSet<>(e.getValue()).size()!=e.getValue().size()||!ids.containsAll(e.getValue()))throw new IllegalArgumentException("Missing weighted award");tables.put(e.getKey().toString(),build(c.prizes().stream().filter(p->e.getValue().contains(p.awardId())).toList()));}
 String v=UUID.randomUUID().toString();var f=new FrozenStrategy(c.activityId(),c.strategyId(),v,c.state(),c.beginsAt(),c.endsAt(),c.prizes(),c.fallback(),c.weightedPools(),c.blacklistAwardId(),c.blacklistVersion(),tables);
 tx.inControl(WorkClass.TRADE,new Deadline(Instant.now().plusSeconds(5)),()->{repo.publish(f);return null;});return v;
 }
 static void validate(PublishStrategyCommand.Prize p){if(p==null||p.rate()==null||p.rate().signum()<0||p.unlockAfter()<0||!Set.of("CREDIT","NONE").contains(p.type()))throw new IllegalArgumentException("Invalid prize");if(p.type().equals("CREDIT")){RequestHasher.amount(p.minAmount());RequestHasher.amount(p.maxAmount());if(p.minAmount().compareTo(p.maxAmount())>0)throw new IllegalArgumentException("Range reversed");}else if(p.minAmount()!=null||p.maxAmount()!=null||p.stockPoolId()!=null)throw new IllegalArgumentException("NONE has no benefit");}
 static FrozenStrategy.Table build(List<PublishStrategyCommand.Prize> ps){var a=new AliasMethodAlgorithm().build(ps.stream().map(p->{var e=new StrategyAwardEntity();e.setAwardId(p.awardId());e.setAwardRate(p.rate());return e;}).toList());return new FrozenStrategy.Table(Arrays.stream(a.getAwardIds()).boxed().toList(),Arrays.stream(a.getProbabilities()).boxed().toList(),Arrays.stream(a.getAliases()).boxed().toList());}
}
