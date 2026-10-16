package com.jay.trigger.listener.v2;

import com.jay.domain.payment.service.PaymentFactService;
import com.jay.domain.task.model.*;
import com.jay.domain.task.service.EventHandler;
import java.util.*;

public final class CreditGrantedListener implements EventHandler {
  private final PaymentFactService service;

  public CreditGrantedListener(PaymentFactService s) {
    service = s;
  }

  public List<EventEnvelope> apply(EventEnvelope e) {
    service.onCreditGranted(e);
    return List.of();
  }
}
