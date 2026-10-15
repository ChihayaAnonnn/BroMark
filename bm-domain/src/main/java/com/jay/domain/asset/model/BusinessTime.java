package com.jay.domain.asset.model;

import java.time.*;

public record BusinessTime(Instant instant, LocalDate businessDate, YearMonth businessMonth) {
  public static BusinessTime at(Instant instant) {
    var date = instant.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
    return new BusinessTime(instant, date, YearMonth.from(date));
  }
}
