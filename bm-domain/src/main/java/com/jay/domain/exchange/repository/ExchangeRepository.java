package com.jay.domain.exchange.repository;

import com.jay.domain.asset.model.*;
import com.jay.domain.exchange.model.*;
import java.util.Optional;

public interface ExchangeRepository {
  Optional<ExchangeResult> request(RequestIdentity identity);

  Optional<ExchangeResult> order(String user, String orderId);

  void save(
      RequestIdentity identity,
      AdmissionSnapshot snapshot,
      BusinessTime time,
      ExchangeResult result);
}
