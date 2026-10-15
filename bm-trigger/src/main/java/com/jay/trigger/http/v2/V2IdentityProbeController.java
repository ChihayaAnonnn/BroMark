package com.jay.trigger.http.v2;

import com.jay.trigger.security.CurrentUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("v2-it")
@ConditionalOnProperty(name = "bm.v2.enabled", havingValue = "true")
public class V2IdentityProbeController {
  private final CurrentUser user;

  public V2IdentityProbeController(CurrentUser user) {
    this.user = user;
  }

  @GetMapping("/api/v2/identity")
  public String identity() {
    return user.requireUserId();
  }
}
