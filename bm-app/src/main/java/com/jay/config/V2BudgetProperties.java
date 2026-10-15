package com.jay.config;
import com.jay.domain.asset.model.WorkClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.*;
@ConfigurationProperties("bm.v2.budget")
public class V2BudgetProperties {
 private Map<String,String> physical=new HashMap<>(Map.of("00","mysql-it","01","mysql-it","02","mysql-it"));
 private Map<String,Map<WorkClass,Integer>> instances=new HashMap<>(Map.of("mysql-it",new EnumMap<>(Map.of(WorkClass.TRADE,2,WorkClass.CALLBACK,1,WorkClass.QUERY,1,WorkClass.RELAY,1,WorkClass.WORKER,1,WorkClass.RECOVERY,1))));
 public Map<String,String> getPhysical(){return physical;}public void setPhysical(Map<String,String> value){physical=value;}
 public Map<String,Map<WorkClass,Integer>> getInstances(){return instances;}public void setInstances(Map<String,Map<WorkClass,Integer>> value){instances=value;}
}
