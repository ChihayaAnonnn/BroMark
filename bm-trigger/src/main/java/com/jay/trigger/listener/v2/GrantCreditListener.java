package com.jay.trigger.listener.v2;

import com.jay.domain.payment.service.PaymentCreditService;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.EventHandler;
import java.util.*;

public final class GrantCreditListener implements EventHandler {
  private final PaymentCreditService service;

  public GrantCreditListener(PaymentCreditService s) {
    service = s;
  }

  public List<EventEnvelope> apply(EventEnvelope e) {
    return service.handle(e);
  }
}
