package com.jay.domain.payment.adapter;

import com.jay.domain.payment.model.*;
import java.math.*;
import java.util.*;

public interface PaymentProvider {
  String name();

  String account();

  String environment();

  ProviderOrder create(String merchantOrderId, BigDecimal amount, String currency);

  ProviderPayment query(String merchantOrderId);

  VerifiedPaymentEvent verify(byte[] raw, Map<String, String> headers);

  default NotificationAck accepted() {
    return new NotificationAck(200, "SUCCESS");
  }
}
