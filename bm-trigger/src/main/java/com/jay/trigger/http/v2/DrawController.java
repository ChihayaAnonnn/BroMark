package com.jay.trigger.http.v2;

import com.jay.api.v2.DrawRequest;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.trigger.security.CurrentUser;
import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/draws")
@ConditionalOnProperty(name = "bm.v2.raffle.enabled", havingValue = "true")
public final class DrawController {
  private final CurrentUser user;
  private final DrawService service;
  private final RequestAdmission admission;
  private final AssetWriteGate gate;

  public DrawController(CurrentUser u, DrawService s, RequestAdmission a, AssetWriteGate g) {
    user = u;
    service = s;
    admission = a;
    gate = g;
  }

  private Deadline deadline() {
    return new Deadline(Instant.now().plusSeconds(5));
  }

  @PostMapping
  public ResponseEntity<DrawView> create(
      @jakarta.validation.Valid @RequestBody DrawRequest request) {
    String u = user.requireUserId();
    gate.check("v2.draw");
    var d = deadline();
    try (var permit = admission.enter(u, d)) {
      admission.checkActivity(Long.toString(request.activityId()), d);
      var r = service.create(new DrawCommand(u, request.requestId(), request.activityId(), d));
      return ResponseEntity.status(
              Set.of("REJECTED", "DELIVERED", "CANCELLED").contains(r.state()) ? 200 : 202)
          .body(r);
    }
  }

  @GetMapping("/{order}")
  public DrawView get(@PathVariable String order) {
    String u = user.requireUserId();
    try (var permit = admission.query(u, deadline())) {
      return service.get(u, order);
    }
  }

  @PostMapping("/{order}/cancel")
  public ResponseEntity<DrawView> cancel(@PathVariable String order) {
    String u = user.requireUserId();
    gate.check("v2.draw.cancel");
    try (var permit = admission.enter(u, deadline())) {
      var r = service.cancel(u, order);
      return ResponseEntity.status(r.state().equals("CANCELLED") ? 200 : 202).body(r);
    }
  }
}
