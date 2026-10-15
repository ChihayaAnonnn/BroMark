package com.jay.domain.exchange.repository;
import java.util.Optional;import com.jay.domain.asset.model.*;import com.jay.domain.exchange.model.*;
public interface ExchangeRepository {
 Optional<ExchangeResult> request(RequestIdentity identity);
 Optional<ExchangeResult> order(String user,String orderId);
 void save(RequestIdentity identity,AdmissionSnapshot snapshot,BusinessTime time,ExchangeResult result);
}

