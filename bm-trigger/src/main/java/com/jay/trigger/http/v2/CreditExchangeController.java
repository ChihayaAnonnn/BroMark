package com.jay.trigger.http.v2;

import com.jay.api.v2.ExchangeRequest;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.exchange.model.*;
import com.jay.domain.exchange.service.ExchangeService;
import com.jay.trigger.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/credit/exchanges")
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
public final class CreditExchangeController {
  private final CurrentUser user;
  private final ExchangeService service;
  private final RequestAdmission admission;
  private final AssetWriteGate gate;

  public CreditExchangeController(
      CurrentUser user, ExchangeService service, RequestAdmission admission, AssetWriteGate gate) {
    this.user = user;
    this.service = service;
    this.admission = admission;
    this.gate = gate;
  }

  @PostMapping
  public ExchangeResult exchange(@Valid @RequestBody ExchangeRequest request) {
    String id = user.requireUserId();
    gate.check("v2.exchange");
    var deadline = new Deadline(Instant.now().plusSeconds(5));
    try (var permit = admission.enter(id, deadline)) {
      return service.exchange(
          new ExchangeCommand(
              id, request.requestId(), request.sku(), request.configVersion(), deadline));
    }
  }

  @GetMapping("/{order}")
  public ExchangeResult get(@PathVariable String order) {
    String id = user.requireUserId();
    try (var permit = admission.query(id, new Deadline(Instant.now().plusSeconds(5)))) {
      return service.get(id, order);
    }
  }
}
