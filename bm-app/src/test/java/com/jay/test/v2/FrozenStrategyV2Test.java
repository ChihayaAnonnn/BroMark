package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.cache.StrategyVersionCache;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class FrozenStrategyV2Test {
  static PublishStrategyCommand command(long id, String pool) {
    return new PublishStrategyCommand(
        id,
        id,
        "OPEN",
        Instant.parse("2020-01-01T00:00:00Z"),
        Instant.parse("2099-01-01T00:00:00Z"),
        new ArrayList<>(
            List.of(
                new PublishStrategyCommand.Prize(
                    101,
                    "CREDIT",
                    new BigDecimal("1.00"),
                    new BigDecimal("2.00"),
                    pool,
                    BigDecimal.ONE,
                    0))),
        new PublishStrategyCommand.Prize(0, "NONE", null, null, null, BigDecimal.ONE, 0),
        Map.of(),
        null,
        "blacklist-v1");
  }

  static void schema(AssetTestSupport h) throws Exception {
    ConsumerInboxV2Test.schema(h);
    h.runner.inControl(
        com.jay.domain.asset.model.WorkClass.RECOVERY,
        new com.jay.domain.asset.model.Deadline(Instant.now().plusSeconds(30)),
        () -> {
          try {
            for (String s :
                java.nio.file.Files.readString(
                        java.nio.file.Path.of(
                            "../docs/dev-ops/mysql/migrations/004_strategy_stock_draw.sql"))
                    .split(";")) if (!s.isBlank()) h.j.execute(s);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
          return null;
        });
  }

  @Test
  public void freezesPublishedInputsAndRestoresOldVersionAfterEviction() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          long id = Math.abs(UUID.randomUUID().getLeastSignificantBits() >>> 1);
          var repo = new MySqlStrategyVersionRepository(h.j.getDataSource());
          var pub = new StrategyPublicationService(h.runner, repo);
          var c = command(id, null);
          String v = pub.publish(c);
          c.prizes().clear();
          String v2=pub.publish(command(id, null));
          var cache = new StrategyVersionCache(h.runner, repo, null, 1);
          cache.load(id,v);cache.load(id,v2);var old = cache.load(id, v);
          assertThat(old.prizes()).hasSize(1);
          assertThat(old.prizes().getFirst().minAmount()).isEqualByComparingTo("1.00");
          assertThatThrownBy(() -> old.prizes().clear())
              .isInstanceOf(UnsupportedOperationException.class);
          var engine = new FrozenRaffleEngine();
          var input = new RuleInputSnapshot(Instant.now(), 1, 1, false, "blacklist-v1");
          var low =
              engine.choose(
                  old,
                  input,
                  new Random() {
                    public int nextInt(int n) {
                      return 0;
                    }

                    public long nextLong(long n) {
                      return 0;
                    }
                  });
          assertThat(low.amount()).isEqualByComparingTo("1.00");
          var high =
              engine.choose(
                  old,
                  input,
                  new Random() {
                    public int nextInt(int n) {
                      return 0;
                    }

                    public long nextLong(long n) {
                      return n - 1;
                    }
                  });
          assertThat(high.amount()).isEqualByComparingTo("2.00");
          assertThat(
                  engine
                      .choose(
                          old,
                          new RuleInputSnapshot(Instant.now(), 1, 1, true, "blacklist-v1"),
                          new Random())
                      .type())
              .isEqualTo("NONE");
        });
  }

  @Test
  public void rejectsUnsupportedAwardsAndInvalidFallback() {
    AssetTestSupport.run(
        h -> {
          schema(h);
          var pub =
              new StrategyPublicationService(
                  h.runner, new MySqlStrategyVersionRepository(h.j.getDataSource()));
          var c = command(991, null);
          assertThatThrownBy(
                  () ->
                      pub.publish(
                          new PublishStrategyCommand(
                              991,
                              991,
                              "OPEN",
                              c.beginsAt(),
                              c.endsAt(),
                              List.of(
                                  new PublishStrategyCommand.Prize(
                                      1, "PHYSICAL", null, null, null, BigDecimal.ONE, 0)),
                              c.fallback(),
                              Map.of(),
                              null,
                              "v")))
              .isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(
                  () ->
                      pub.publish(
                          new PublishStrategyCommand(
                              991,
                              991,
                              "OPEN",
                              c.beginsAt(),
                              c.endsAt(),
                              c.prizes(),
                              new PublishStrategyCommand.Prize(
                                  0, "NONE", null, null, "fake", BigDecimal.ONE, 0),
                              Map.of(),
                              null,
                              "v")))
              .isInstanceOf(IllegalArgumentException.class);
        });
  }
}
