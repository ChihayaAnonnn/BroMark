package com.jay.trigger.http.v2;

import com.jay.api.v2.SignInRequest;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import com.jay.domain.rebate.model.*;
import com.jay.domain.rebate.service.SignInService;
import com.jay.trigger.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/rebates/sign-ins")
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
public final class SignInController {
  private final CurrentUser user;
  private final SignInService service;
  private final RequestAdmission admission;
  private final AssetWriteGate gate;

  public SignInController(
      CurrentUser user, SignInService service, RequestAdmission admission, AssetWriteGate gate) {
    this.user = user;
    this.service = service;
    this.admission = admission;
    this.gate = gate;
  }

  @PostMapping
  public SignInResult signIn(@Valid @RequestBody SignInRequest request) {
    String id = user.requireUserId();
    gate.check("v2.signin");
    var deadline = new Deadline(Instant.now().plusSeconds(5));
    try (var permit = admission.enter(id, deadline)) {
      return service.signIn(
          new SignInCommand(id, request.requestId(), request.targetDate(), deadline));
    }
  }

  @GetMapping("/{date}")
  public SignInResult get(
      @PathVariable
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate date) {
    String id = user.requireUserId();
    try (var permit = admission.query(id, new Deadline(Instant.now().plusSeconds(5)))) {
      return service.get(id, date);
    }
  }
}
