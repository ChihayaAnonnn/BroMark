package com.jay.infrastructure.event;
import com.jay.domain.asset.model.Deadline;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.ConfirmedEventPublisher;
import com.jay.infrastructure.adapter.repository.v2.JsonStore;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class V2ConfirmedEventPublisher implements ConfirmedEventPublisher,AutoCloseable {
  private final RabbitTemplate template;
  private final String exchange;
  private final Duration timeout;
  private final Semaphore permits;
  private final ThreadPoolExecutor sends;
  private final ScheduledThreadPoolExecutor timer=new ScheduledThreadPoolExecutor(1,Thread.ofPlatform().name("v2-confirm-timeout").factory());
  private final Set<CompletableFuture<PublishOutcome>> pending=ConcurrentHashMap.newKeySet();
  private volatile boolean closed;
  public V2ConfirmedEventPublisher(RabbitTemplate template,String exchange,int max,Duration timeout) {
    if(max<1||max>128||timeout.isZero()||timeout.isNegative()) throw new IllegalArgumentException("Publisher bounds");
    this.template=template;this.exchange=exchange;this.timeout=timeout;this.permits=new Semaphore(max);
    template.setMandatory(true);timer.setRemoveOnCancelPolicy(true);
    sends=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(max),Thread.ofPlatform().name("v2-send-",0).factory(),new ThreadPoolExecutor.AbortPolicy());
  }
  public CompletionStage<PublishOutcome> publish(EventEnvelope e,Deadline d) {return publishAttempt(e,UUID.randomUUID().toString(),0,d);}
  public synchronized CompletionStage<PublishOutcome> publishAttempt(EventEnvelope e,String token,int failureCount,Deadline d) {
    if(TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Broker I/O outside database transaction");
    long nanos=Math.min(timeout.toNanos(),d.remaining().toNanos());
    if(closed||nanos<=0||!permits.tryAcquire()) return CompletableFuture.completedFuture(PublishOutcome.UNKNOWN);
    var result=new CompletableFuture<PublishOutcome>();pending.add(result);
    var expiry=timer.schedule(()->result.complete(PublishOutcome.UNKNOWN),nanos,TimeUnit.NANOSECONDS);
    result.whenComplete((v,t)->{expiry.cancel(false);pending.remove(result);permits.release();});
    try {sends.execute(()->{
      if(result.isDone()) return;
      var cd=new CorrelationData(e.eventId()+":"+token);
      cd.getFuture().whenComplete((confirm,error)->result.complete(error!=null?PublishOutcome.UNKNOWN:cd.getReturned()!=null?PublishOutcome.RETURNED:confirm.isAck()?PublishOutcome.CONFIRMED:PublishOutcome.NACKED));
      try {
        var props=new MessageProperties();props.setContentType(MessageProperties.CONTENT_TYPE_JSON);props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);props.setMessageId(e.eventId());props.setHeader("x-v2-failure-count",failureCount);
        template.send(exchange,e.eventType(),new Message(JsonStore.write(e).getBytes(java.nio.charset.StandardCharsets.UTF_8),props),cd);
      }catch(Exception ex){result.complete(PublishOutcome.UNKNOWN);}
    });}catch(RejectedExecutionException ex){result.complete(PublishOutcome.UNKNOWN);}
    return result;
  }
  public void close() {
    synchronized(this){closed=true;sends.shutdown();}
    try {if(!sends.awaitTermination(timeout.toMillis()+1000,TimeUnit.MILLISECONDS)) sends.shutdownNow();
      CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).get(timeout.toMillis()+1000,TimeUnit.MILLISECONDS);
    }catch(InterruptedException e){Thread.currentThread().interrupt();}catch(ExecutionException|TimeoutException e){/* Unknown deliveries remain recoverable by their lease. */}
    finally {pending.forEach(f->f.complete(PublishOutcome.UNKNOWN));timer.shutdownNow();}
  }
}
