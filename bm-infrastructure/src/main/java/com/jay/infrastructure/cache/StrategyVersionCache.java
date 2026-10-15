package com.jay.infrastructure.cache;

import com.google.common.cache.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.*;
import com.jay.domain.strategy.model.*;
import com.jay.domain.strategy.repository.*;
import com.jay.infrastructure.adapter.repository.v2.JsonStore;
import java.time.*;
import org.redisson.api.RedissonClient;

public final class StrategyVersionCache {
  private record Pointer(FrozenStrategy value, RuntimeException failure) {}

  private final LoadingCache<String, FrozenStrategy> versions;
  private final LoadingCache<Long, Pointer> pointers;

  public StrategyVersionCache(
      UserTransactionRunner tx, StrategyVersionRepository repo, RedissonClient redis, int max) {
    if (max < 1) throw new IllegalArgumentException("Cache bound");
    versions =
        CacheBuilder.newBuilder()
            .maximumSize(max)
            .build(
                new CacheLoader<>() {
                  public FrozenStrategy load(String key) {
                    int split = key.indexOf(':');
                    long id = Long.parseLong(key.substring(0, split));
                    String version = key.substring(split + 1);
                    if (redis != null)
                      try {
                        String s = redis.<String>getBucket("bm:v2:strategy:" + key).get();
                        if (s != null) {
                          var f = JsonStore.read(s, FrozenStrategy.class);
                          if (f.strategyId() == id && f.version().equals(version)) return f;
                        }
                      } catch (RuntimeException ignored) {
                      }
                    var f =
                        tx.inControl(
                            WorkClass.QUERY,
                            new Deadline(Instant.now().plusSeconds(3)),
                            () -> repo.load(id, version));
                    if (redis != null)
                      try {
                        redis
                            .getBucket("bm:v2:strategy:" + key)
                            .set(JsonStore.write(f), java.time.Duration.ofHours(1));
                      } catch (RuntimeException ignored) {
                      }
                    return f;
                  }
                });
    pointers =
        CacheBuilder.newBuilder()
            .maximumSize(max)
            .expireAfterWrite(Duration.ofSeconds(3))
            .build(
                new CacheLoader<>() {
                  public Pointer load(Long id) {
                    try {
                      return new Pointer(
                          tx.inControl(
                              WorkClass.QUERY,
                              new Deadline(Instant.now().plusSeconds(3)),
                              () -> repo.current(id)),
                          null);
                    } catch (RuntimeException e) {
                      return new Pointer(null, e);
                    }
                  }
                });
  }

  public FrozenStrategy load(long id, String version) {
    return versions.getUnchecked(id + ":" + version);
  }

  public FrozenStrategy current(long id) {
    var p = pointers.getUnchecked(id);
    if (p.failure() != null) throw p.failure();
    return p.value();
  }

  public void warm(FrozenStrategy f) {
    versions.put(f.strategyId() + ":" + f.version(), f);
    pointers.put(f.activityId(), new Pointer(f, null));
  }

  public long estimatedVersions() {
    return versions.size();
  }
}
