package com.jay.domain.asset.service;
import com.jay.domain.asset.model.*;import com.jay.domain.asset.repository.AssetRepository;import java.math.BigDecimal;import java.util.*;
public final class AssetKernel {
 private final AssetRepository repo;private final BusinessClock clock;
 public AssetKernel(AssetRepository repo,BusinessClock clock){this.repo=repo;this.clock=clock;}
 public BusinessTime lockAccounts(String user,List<Long> activities){repo.lockAccounts(user,activities);return clock.now();}
 public AssetReceipt credit(String user,BusinessId id,BigDecimal amount){return change(user,id,RequestHasher.amount(amount),false).orElseThrow();}
 public Optional<AssetReceipt> debit(String user,BusinessId id,BigDecimal amount){return change(user,id,RequestHasher.amount(amount).negate(),true);}
 private Optional<AssetReceipt> change(String user,BusinessId id,BigDecimal delta,boolean debit){
  repo.requireLocked(user,null);var old=repo.creditReceipt(user,id);
  if(old.isPresent()){if(!old.get().bizId().hash().equals(id.hash())||old.get().delta().compareTo(delta)!=0)throw new AssetConflictException("Business identity content changed");return old;}
  var balance=repo.balance(user).add(delta);if(balance.signum()<0)return Optional.empty();
  if(balance.compareTo(new BigDecimal("9999999999999999.99"))>0)throw new IllegalArgumentException("Balance overflow");
  var receipt=new AssetReceipt(id,delta,balance);repo.adjustCredit(user,delta,receipt);return Optional.of(receipt);
 }
}

