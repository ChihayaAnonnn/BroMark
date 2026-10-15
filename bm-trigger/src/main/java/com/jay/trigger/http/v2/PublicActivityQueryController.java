package com.jay.trigger.http.v2;

import com.jay.domain.activity.service.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.*;
import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/activities")
@ConditionalOnProperty(name = "bm.v2.raffle.enabled", havingValue = "true")
public final class PublicActivityQueryController {
  private final PublicActivityQueryService query;
  private final RequestAdmission admission;

  public PublicActivityQueryController(PublicActivityQueryService q, RequestAdmission a) {
    query = q;
    admission = a;
  }

  public record Award(
      int awardId, String type, String minAmount, String maxAmount, Long cachedAvailable) {}

  public record Activity(
      long activityId,
      String version,
      String state,
      Instant beginsAt,
      Instant endsAt,
      List<Award> awards) {}

  @GetMapping("/{id}")
  public Activity get(@PathVariable long id) {
    try (var permit =
        admission.query("public-activity", new Deadline(Instant.now().plusSeconds(3)))) {
      var f = query.current(id);
      return new Activity(
          f.activityId(),
          f.version(),
          f.state(),
          f.beginsAt(),
          f.endsAt(),
          f.prizes().stream()
              .map(
                  p ->
                      new Award(
                          p.awardId(),
                          p.type(),
                          p.minAmount() == null ? null : p.minAmount().toPlainString(),
                          p.maxAmount() == null ? null : p.maxAmount().toPlainString(),
                          null))
              .toList());
    }
  }

  @GetMapping("/{id}/awards")
  public List<Award> awards(@PathVariable long id) {
    return get(id).awards();
  }
}
