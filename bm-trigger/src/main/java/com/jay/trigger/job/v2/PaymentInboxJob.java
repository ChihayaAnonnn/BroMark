package com.jay.trigger.job.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.payment.repository.*;
import com.jay.domain.payment.service.*;
import java.time.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;

public final class PaymentInboxJob {
  private final UserTransactionRunner tx;
  private final PaymentRepository repo;
  private final PaymentFactService facts;
  private final com.jay.domain.asset.service.OrderIdGenerator ids;

  public PaymentInboxJob(
      UserTransactionRunner tx,
      PaymentRepository repo,
      PaymentFactService facts,
      com.jay.domain.asset.service.OrderIdGenerator ids) {
    this.tx = tx;
    this.repo = repo;
    this.facts = facts;
    this.ids = ids;
  }

  @Scheduled(fixedDelayString = "${bm.v2.payment.inbox-delay-ms:1000}")
  public void tick() {
    ids.prefetch();
    String token = UUID.randomUUID().toString();
    var events =
        tx.inControl(
            WorkClass.CALLBACK,
            new Deadline(Instant.now().plusSeconds(5)),
            () -> repo.claimInbox(token, 8));
    for (var e : events)
      try {
        tx.inControl(
            WorkClass.WORKER,
            new Deadline(Instant.now().plusSeconds(10)),
            () -> {
              if (!repo.ownsInbox(e, token)) return null;
              facts.applyInTransaction(e);
              repo.finishInbox(e, token, true);
              return null;
            });
      } catch (RuntimeException failed) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Payment inbox retained for lease retry: {}", e.eventId());
      }
  }
}
