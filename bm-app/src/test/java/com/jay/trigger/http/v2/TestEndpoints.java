package com.jay.trigger.http.v2;

import com.jay.domain.asset.model.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
public class TestEndpoints {
  public record Input(@jakarta.validation.constraints.NotBlank String value) {}

  @PostMapping("/api/v2/test/validate")
  public String validate(@jakarta.validation.Valid @RequestBody Input input) {
    return input.value();
  }

  @GetMapping("/api/v2/activities/{id}")
  public String activity() {
    return "public";
  }

  @GetMapping("/api/v2/test/error/{code}")
  public String error(@PathVariable int code) {
    switch (code) {
      case 400:
        throw new IllegalArgumentException();
      case 403:
        throw new AccessDeniedException("denied");
      case 409:
        throw new AssetConflictException("conflict");
      case 429:
        throw new BudgetExceededException("full");
      default:
        throw new IllegalStateException("secret details");
    }
  }
}
