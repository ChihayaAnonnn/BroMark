package com.jay.trigger.http.v2;

import com.jay.api.v2.PaymentOrderRequest;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import com.jay.trigger.security.CurrentUser;
import java.time.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/payment/orders")
@ConditionalOnProperty(name = "bm.v2.payment.enabled", havingValue = "true")
public final class PaymentOrderController {
  private final CurrentUser user;
  private final PaymentService service;
  private final RequestAdmission admission;
  private final AssetWriteGate gate;

  public PaymentOrderController(
      CurrentUser user, PaymentService service, RequestAdmission admission, AssetWriteGate gate) {
    this.user = user;
    this.service = service;
    this.admission = admission;
    this.gate = gate;
  }

  @PostMapping
  public ResponseEntity<PaymentView> create(
      @jakarta.validation.Valid @RequestBody PaymentOrderRequest r) {
    String u = user.requireUserId();
    gate.check("v2.payment");
    var d = new Deadline(Instant.now().plusSeconds(5));
    try (var permit = admission.enter(u, d)) {
      var v = service.create(new PaymentCommand(u, r.requestId(), r.productId(), r.provider(), d));
      return ResponseEntity.status(v.state().equals("DELIVERED") ? 200 : 202).body(v);
    }
  }

  @GetMapping("/{id}")
  public PaymentView get(@PathVariable String id) {
    String u = user.requireUserId();
    try (var permit = admission.query(u, new Deadline(Instant.now().plusSeconds(5)))) {
      return service.get(u, id);
    }
  }
}
