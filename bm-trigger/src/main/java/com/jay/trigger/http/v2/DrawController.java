package com.jay.trigger.http.v2;

import com.jay.api.v2.DrawRequest;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.raffle.model.*;
import com.jay.domain.raffle.service.*;
import com.jay.domain.task.service.RelayObservation;
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
  private final RelayObservation observation;

  public DrawController(CurrentUser u, DrawService s, RequestAdmission a, AssetWriteGate g) {
    this(u, s, a, g, RelayObservation.NOOP);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public DrawController(
      CurrentUser u,
      DrawService s,
      RequestAdmission a,
      AssetWriteGate g,
      RelayObservation observation) {
    user = u;
    service = s;
    admission = a;
    gate = g;
    this.observation = observation;
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
    long start = System.nanoTime();
    var slot = RouteSlot.control();
    Permit permit = null;
    try {
      permit = admission.enter(u, d);
      observation.duration(
          "http_admission", slot, "PrepareDraw", "success", System.nanoTime() - start);
    } catch (RuntimeException e) {
      if (permit != null) permit.close();
      observation.duration(
          "http_admission", slot, "PrepareDraw", "error", System.nanoTime() - start);
      throw e;
    }
    start = System.nanoTime();
    try {
      admission.checkActivity(Long.toString(request.activityId()), d);
      observation.duration(
          "activity_admission", slot, "PrepareDraw", "success", System.nanoTime() - start);
    } catch (RuntimeException e) {
      permit.close();
      observation.duration(
          "activity_admission", slot, "PrepareDraw", "error", System.nanoTime() - start);
      throw e;
    }
    try (Permit opened = permit) {
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
    try (var permit = admission.cancel(u, deadline())) {
      var r = service.cancel(u, order);
      return ResponseEntity.status(r.state().equals("CANCELLED") ? 200 : 202).body(r);
    }
  }
}
