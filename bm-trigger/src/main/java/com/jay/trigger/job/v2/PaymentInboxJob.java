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

  private CoalescingJobDispatcher dispatcher;

  public PaymentInboxJob dispatchWith(CoalescingJobDispatcher dispatcher) {
    this.dispatcher = dispatcher;
    return this;
  }

  @Scheduled(fixedDelayString = "${bm.v2.payment.inbox-delay-ms:1000}")
  public void tick() {
    if (dispatcher == null) runWork();
    else dispatcher.submit("provider-inbox", this::runWork);
  }

  private void runWork() {
    ids.prefetch();
    String token = UUID.randomUUID().toString();
    var entries =
        tx.inControl(
            WorkClass.CALLBACK,
            new Deadline(Instant.now().plusSeconds(5)),
            () -> repo.claimInbox(token, 8));
    for (var entry : entries) {
      com.jay.domain.payment.model.VerifiedPaymentEvent event;
      try {
        event = entry.event();
      } catch (RuntimeException corrupt) {
        fail(entry, token, true, "Invalid verified payload/identity");
        continue;
      }
      try {
        tx.inControl(
            WorkClass.WORKER,
            new Deadline(Instant.now().plusSeconds(10)),
            () -> {
              if (!repo.ownsInbox(entry, token)) return null;
              facts.applyInTransaction(event);
              repo.finishInbox(entry, token, true);
              return null;
            });
      } catch (RuntimeException failed) {
        fail(entry, token, false, "Payment apply failed: " + failed.getClass().getSimpleName());
      }
    }
  }

  private void fail(
      com.jay.domain.payment.model.ProviderInboxEntry e,
      String token,
      boolean permanent,
      String reason) {
    try {
      tx.inControl(
          WorkClass.RECOVERY,
          new Deadline(Instant.now().plusSeconds(10)),
          () -> {
            repo.failInbox(e, token, reason, permanent);
            return null;
          });
    } catch (RuntimeException unavailable) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("Provider inbox handoff unavailable; lease retained: {}", e.eventId());
    }
  }
}
