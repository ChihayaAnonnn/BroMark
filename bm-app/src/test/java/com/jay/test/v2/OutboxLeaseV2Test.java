package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class OutboxLeaseV2Test {
  @Test public void claimsEachSourceOnceAndFencesExpiredOwner() {
    AssetTestSupport.run(h -> {
      var slot = RouteSlot.control();
      var repo = new MySqlOutboxRepository(h.j.getDataSource(), h.runner);
      var writer = new MySqlOutboxWriter(h.j.getDataSource());
      String prefix = "lease-" + UUID.randomUUID();
      h.runner.inSlot(slot, WorkClass.RECOVERY, new Deadline(Instant.now().plusSeconds(30)), () -> {
        h.j.update("delete from outbox");
        for(int i=0;i<20;i++) writer.append(new EventEnvelope(prefix+i, i==19?"SignInCompleted":"ExchangeCompleted", "order", new BusinessId("EX","biz","a".repeat(64)), "u", new RouteSlot("02","003"), "a".repeat(64),1,1,"{}"));
        return null;
      });
      var claims = com.jay.test.v2.support.ConcurrentProbe.run(2, () -> repo.claim(slot, 10, Duration.ofSeconds(30)));
      var all = claims.stream().flatMap(List::stream).toList();
      assertThat(all).hasSize(20);
      assertThat(all.stream().map(x -> x.event().eventId()).distinct().count()).isEqualTo(20);
      assertThat(all.stream().anyMatch(x -> x.event().eventType().equals("SignInCompleted"))).isTrue();
      var old = all.getFirst();
      assertThat(old.ownerSlot()).isEqualTo(slot);
      assertThat(old.event().routeSlot()).isEqualTo(new RouteSlot("02","003"));
      h.runner.inSlot(slot, WorkClass.RECOVERY,new Deadline(Instant.now().plusSeconds(30)), () -> {h.j.update("update outbox set lease_until=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND where event_id=?",old.event().eventId());return null;});
      var current = repo.claim(slot,1,Duration.ofSeconds(30)).getFirst();
      assertThat(current.attempt()).isEqualTo(2);
      assertThat(repo.complete(old)).isFalse();
      assertThat(repo.retry(old,Instant.now(),"late")).isFalse();
      assertThat(repo.complete(current)).isTrue();
      assertThat(repo.complete(current)).isFalse();
      assertThat(h.j.queryForList("EXPLAIN SELECT event_id FROM outbox FORCE INDEX(pending) WHERE state='PENDING' AND next_attempt_at<=UTC_TIMESTAMP(6) ORDER BY next_attempt_at,event_id LIMIT 10 FOR UPDATE SKIP LOCKED").getFirst().get("key")).isEqualTo("pending");
    });
  }
}
