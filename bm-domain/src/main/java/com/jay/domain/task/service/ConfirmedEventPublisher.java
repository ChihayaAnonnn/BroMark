package com.jay.domain.task.service;

import com.jay.domain.asset.model.Deadline;
import com.jay.domain.task.model.*;
import java.util.concurrent.CompletionStage;

public interface ConfirmedEventPublisher {
  /** 旧实现保留兼容；未知结果没有物理完成证明时必须保守停止追加发送。 */
  default int capacity() {
    return 16;
  }

  default boolean isAttemptComplete(CompletionStage<PublishOutcome> attempt) {
    var future = attempt.toCompletableFuture();
    return future.isDone()
        && !future.isCompletedExceptionally()
        && future.getNow(PublishOutcome.UNKNOWN) != PublishOutcome.UNKNOWN;
  }

  CompletionStage<PublishOutcome> publish(EventEnvelope event, Deadline deadline);

  default CompletionStage<PublishOutcome> publishAttempt(
      EventEnvelope event, String token, int failureCount, Deadline deadline) {
    return publish(event, deadline);
  }
}
