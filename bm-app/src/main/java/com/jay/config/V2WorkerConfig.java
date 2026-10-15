package com.jay.config;
import com.jay.infrastructure.transaction.BoundedDatabaseBudget;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import java.time.Duration;
import java.util.concurrent.*;
@Configuration(proxyBeanMethods=false) @ConditionalOnProperty(name="bm.v2.enabled",havingValue="true")
@EnableConfigurationProperties(V2BudgetProperties.class)
public class V2WorkerConfig {
 @Bean public BoundedDatabaseBudget databaseBudget(V2BudgetProperties props,MeterRegistry metrics){return new BoundedDatabaseBudget(props.getInstances(),metrics);}
 @Bean(destroyMethod="close") public BoundedVirtualExecutor v2WorkerExecutor(){return new BoundedVirtualExecutor("v2-worker",16,Duration.ofSeconds(30));}
 @Bean(destroyMethod="close") public ExecutorService v2ApiExecutor(){return pool("v2-api");}
 @Bean(destroyMethod="close") public ExecutorService v2RelayExecutor(){return pool("v2-relay");}
 @Bean(destroyMethod="close") public ExecutorService v2RecoveryExecutor(){return pool("v2-recovery");}
 private ExecutorService pool(String name){return new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),Thread.ofPlatform().name(name+"-",0).factory(),new ThreadPoolExecutor.AbortPolicy());}
 @Bean public SmartLifecycle v2Shutdown(BoundedDatabaseBudget budget,BoundedVirtualExecutor v2WorkerExecutor,ExecutorService v2ApiExecutor,ExecutorService v2RelayExecutor,ExecutorService v2RecoveryExecutor){return new SmartLifecycle(){
  volatile boolean running;
  public void start(){running=true;}public boolean isRunning(){return running;}public int getPhase(){return Integer.MAX_VALUE;}
  public void stop(){running=false;var workers=java.util.List.of(v2WorkerExecutor,v2ApiExecutor,v2RelayExecutor,v2RecoveryExecutor);workers.forEach(ExecutorService::shutdown);budget.stopAccepting();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);try{for(var worker:workers)if(!worker.awaitTermination(Math.max(0,end-System.nanoTime()),TimeUnit.NANOSECONDS))worker.shutdownNow();if(!budget.awaitIdle(Duration.ofNanos(Math.max(0,end-System.nanoTime()))))throw new IllegalStateException("Database transactions did not drain before shutdown");}catch(InterruptedException e){workers.forEach(ExecutorService::shutdownNow);Thread.currentThread().interrupt();}}
 };}
}
