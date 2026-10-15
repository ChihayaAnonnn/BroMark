package com.jay.config;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
/** 有界受理，执行线程为虚拟线程；不传播路由或事务上下文。 */
public final class BoundedVirtualExecutor extends AbstractExecutorService {
 private final ExecutorService delegate;private final Semaphore slots;private final Duration timeout;
 public BoundedVirtualExecutor(String name,int capacity,Duration timeout){if(capacity<1||timeout.isNegative())throw new IllegalArgumentException("Invalid worker capacity/timeout");slots=new Semaphore(capacity);this.timeout=timeout;delegate=Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(name+"-",0).factory());}
 public void execute(Runnable task){java.util.Objects.requireNonNull(task);if(isShutdown()||!slots.tryAcquire())throw new RejectedExecutionException("Virtual worker capacity exceeded or stopped");try{delegate.execute(()->{try{task.run();}finally{slots.release();}});}catch(RuntimeException e){slots.release();throw e;}}
 public void shutdown(){delegate.shutdown();}public List<Runnable> shutdownNow(){return delegate.shutdownNow();}public boolean isShutdown(){return delegate.isShutdown();}public boolean isTerminated(){return delegate.isTerminated();}public boolean awaitTermination(long time,TimeUnit unit)throws InterruptedException{return delegate.awaitTermination(time,unit);}
 @Override public void close(){shutdown();try{if(!awaitTermination(timeout.toNanos(),TimeUnit.NANOSECONDS))shutdownNow();}catch(InterruptedException e){shutdownNow();Thread.currentThread().interrupt();}}
}
