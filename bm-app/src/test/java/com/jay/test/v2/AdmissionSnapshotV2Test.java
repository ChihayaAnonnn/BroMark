package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import org.junit.Test;

public class AdmissionSnapshotV2Test {
  @Test
  public void authoritativeConfigChecksUserBlacklistAndEveryRewardActivity() {
    AssetTestSupport.run(
        h -> {
          var f = new BusinessTestSupport(h);
          var repo = new MySqlAdmissionRepository(h.j.getDataSource());
          var d = new Deadline(Instant.now().plusSeconds(30));
          h.runner.inControl(
              WorkClass.RECOVERY,
              d,
              () -> {
                try {
                  for (String s :
                      java.nio.file.Files.readString(
                              java.nio.file.Path.of(
                                  "../docs/dev-ops/mysql/migrations/002_admission_config.sql"))
                          .split(";")) if (!s.isBlank()) h.j.execute(s);
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
                h.j.update(
                    "insert into v2_admission_config values('SIGNIN','daily',?) on duplicate key"
                        + " update snapshot=values(snapshot)",
                    JsonStore.write(f.snapshot(d)));
                var closed = f.snapshot(d);
                closed =
                    new AdmissionSnapshot(
                        12,
                        "CLOSED",
                        "v1",
                        closed.beginsAt(),
                        closed.endsAt(),
                        closed.readAt(),
                        closed.expiresAt(),
                        false,
                        closed.creditCost(),
                        closed.quota(),
                        java.util.List.of());
                h.j.update(
                    "insert into v2_admission_config values('ACTIVITY','12',?) on duplicate key"
                        + " update snapshot=values(snapshot)",
                    JsonStore.write(closed));
                h.j.update("insert ignore into v2_asset_blacklist(user_id) values('blocked-user')");
                return null;
              });
          var snapshot =
              h.runner.inControl(
                  WorkClass.QUERY, d, () -> repo.signIn("blocked-user", LocalDate.now(), d));
          assertThat(snapshot.blacklisted()).isTrue();
          assertThat(snapshot.accepts(BusinessTime.at(Instant.now()))).isFalse();
          var allowed =
              h.runner.inControl(
                  WorkClass.QUERY, d, () -> repo.signIn("not-blocked", LocalDate.now(), d));
          assertThat(allowed.activityState()).isEqualTo("CLOSED");
        });
  }
}
