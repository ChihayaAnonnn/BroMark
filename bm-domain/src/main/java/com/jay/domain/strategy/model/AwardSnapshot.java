package com.jay.domain.strategy.model;
import java.math.BigDecimal;
public record AwardSnapshot(int awardId,String type,BigDecimal amount,String stockPoolId,long quantity,String configHash) {
 public AwardSnapshot { if(!java.util.Set.of("CREDIT","NONE").contains(type)||quantity<0||quantity>1||quantity==1&&stockPoolId==null)throw new IllegalArgumentException("Invalid award"); if(type.equals("CREDIT"))amount=com.jay.domain.asset.service.RequestHasher.amount(amount);else if(amount!=null||stockPoolId!=null||quantity!=0)throw new IllegalArgumentException("Invalid NONE"); }
}
