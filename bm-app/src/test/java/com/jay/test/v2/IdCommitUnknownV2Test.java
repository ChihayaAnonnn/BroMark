package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.infrastructure.id.BlockOrderIdGenerator;
import java.time.*;
import java.util.function.Supplier;
import org.junit.Test;

public class IdCommitUnknownV2Test {
  @Test
  public void unknownCommitNeverPublishesAllocatedNumbers() {
    AssetTestSupport.run(
        h -> {
          h.runner.inControl(
              WorkClass.RECOVERY,
              new Deadline(Instant.now().plusSeconds(10)),
              () -> {
                try {
                  for (String s :
                      java.nio.file.Files.readString(
                              java.nio.file.Path.of(
                                  "../docs/dev-ops/mysql/migrations/001_id_block.sql"))
                          .split(";")) if (!s.isBlank()) h.j.execute(s);
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
                return null;
              });
          UserTransactionRunner unknown =
              new UserTransactionRunner() {
                public <T> T inUser(String u, WorkClass w, Deadline d, Supplier<T> a) {
                  return h.runner.inUser(u, w, d, a);
                }

                public <T> T inSlot(RouteSlot s, WorkClass w, Deadline d, Supplier<T> a) {
                  return h.runner.inSlot(s, w, d, a);
                }

                public <T> T inControl(WorkClass w, Deadline d, Supplier<T> a) {
                  h.runner.inControl(w, d, a);
                  throw new IllegalStateException("commit outcome unknown");
                }
              };
          var lost = new BlockOrderIdGenerator(unknown, h.j, "lost");
          assertThatThrownBy(lost::prefetch).hasMessageContaining("unknown");
          assertThatThrownBy(() -> lost.next("EX", h.codec.routeUser("u")))
              .hasMessageContaining("empty");
          var good = new BlockOrderIdGenerator(h.runner, h.j, "good");
          good.prefetch();
          String id = good.next("EX", h.codec.routeUser("u"));
          assertThat(h.codec.routeOrder(id)).isEqualTo(h.codec.routeUser("u"));
        });
  }
}
