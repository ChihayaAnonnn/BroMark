package com.jay.trigger.http.v2;

import com.jay.domain.payment.service.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/payment/notifications")
@ConditionalOnProperty(name = "bm.v2.payment.enabled", havingValue = "true")
public final class PaymentNotificationController {
  private final PaymentNotificationService service;

  public PaymentNotificationController(PaymentNotificationService s) {
    service = s;
  }

  @PostMapping("/{provider}")
  public ResponseEntity<String> receive(
      @PathVariable String provider, jakarta.servlet.http.HttpServletRequest request)
      throws java.io.IOException {
    if (request.getContentLengthLong() > 65536)
      throw new IllegalArgumentException("Payload too large");
    byte[] raw = request.getInputStream().readNBytes(65537);
    if (raw.length > 65536) throw new IllegalArgumentException("Payload too large");
    var headers = new HashMap<String, String>();
    var names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String n = names.nextElement();
      headers.put(n, request.getHeader(n));
    }
    var ack = service.receive(provider, raw, headers);
    return ResponseEntity.status(ack.httpStatus())
        .contentType(MediaType.TEXT_PLAIN)
        .body(ack.body());
  }
}
