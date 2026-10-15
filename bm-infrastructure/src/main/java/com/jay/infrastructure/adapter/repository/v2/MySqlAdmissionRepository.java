package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.activity.repository.AdmissionRepository;
import com.jay.domain.asset.model.*;
import java.time.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public final class MySqlAdmissionRepository implements AdmissionRepository {
  private final JdbcTemplate j;

  public MySqlAdmissionRepository(DataSource ds) {
    j = new JdbcTemplate(ds);
  }

  private AdmissionSnapshot blacklist(String user, AdmissionSnapshot s) {
    boolean blocked =
        j.queryForObject(
                "select count(*) from v2_asset_blacklist where user_id=?", Integer.class, user)
            > 0;
    return new AdmissionSnapshot(
        s.activityId(),
        s.activityState(),
        s.version(),
        s.beginsAt(),
        s.endsAt(),
        s.readAt(),
        s.expiresAt(),
        s.blacklisted() || blocked,
        s.creditCost(),
        s.quota(),
        s.rewards());
  }

  private AdmissionSnapshot load(String type, String key, Deadline deadline) {
    var rows =
        j.query(
            "select snapshot from v2_admission_config where config_type=? and config_key=?",
            (rs, n) -> JsonStore.read(rs.getString(1), AdmissionSnapshot.class),
            type,
            key);
    if (rows.isEmpty()) throw new IllegalArgumentException("Unpublished configuration");
    var s = rows.getFirst();
    return new AdmissionSnapshot(
        s.activityId(),
        s.activityState(),
        s.version(),
        s.beginsAt(),
        s.endsAt(),
        Instant.now(),
        deadline.expiresAt(),
        s.blacklisted(),
        s.creditCost(),
        s.quota(),
        s.rewards());
  }

  public AdmissionSnapshot exchange(String user, long sku, String version, Deadline deadline) {
    return blacklist(user, load("EXCHANGE", Long.toString(sku), deadline));
  }

  public AdmissionSnapshot signIn(String user, LocalDate date, Deadline deadline) {
    var s = load("SIGNIN", "daily", deadline);
    var begin = s.beginsAt();
    var end = s.endsAt();
    String state = s.activityState();
    for (long activity :
        s.rewards().stream()
            .filter(x -> x.quota() != null)
            .mapToLong(x -> x.activityId())
            .distinct()
            .toArray()) {
      var reward = load("ACTIVITY", Long.toString(activity), deadline);
      if (!"OPEN".equals(reward.activityState())) state = "CLOSED";
      if (reward.beginsAt().isAfter(begin)) begin = reward.beginsAt();
      if (reward.endsAt().isBefore(end)) end = reward.endsAt();
    }
    return blacklist(
        user,
        new AdmissionSnapshot(
            s.activityId(),
            state,
            s.version(),
            begin,
            end,
            s.readAt(),
            s.expiresAt(),
            s.blacklisted(),
            s.creditCost(),
            s.quota(),
            s.rewards()));
  }
}
