package com.jay.domain.task.service;
import com.jay.domain.task.model.*;
import com.jay.domain.asset.model.Deadline;
import java.util.concurrent.CompletionStage;
public interface ConfirmedEventPublisher {
  CompletionStage<PublishOutcome> publish(EventEnvelope event,Deadline deadline);
  default CompletionStage<PublishOutcome> publishAttempt(EventEnvelope event,String token,int failureCount,Deadline deadline) {return publish(event,deadline);}
}
