package com.jay.domain.asset.service;

import com.jay.domain.asset.model.BusinessId;
import java.util.Optional;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** 仅接受审计导入的精确映射；未知身份必须持久化核查，不能猜测新业务键。 */
public final class LegacyIdentityAdapter {
  private final BiFunction<String,String,Optional<BusinessId>> lookup;
  private final BiConsumer<String,String> review;
  public LegacyIdentityAdapter(BiFunction<String,String,Optional<BusinessId>> lookup,
                               BiConsumer<String,String> review) {
    this.lookup=Objects.requireNonNull(lookup); this.review=Objects.requireNonNull(review);
  }
  public Optional<BusinessId> resolve(String entry,String legacyId) {
    if(entry==null || entry.isBlank() || legacyId==null || legacyId.isBlank())
      throw new IllegalArgumentException("Legacy identity is required");
    var result=Objects.requireNonNull(lookup.apply(entry,legacyId));
    if(result.isEmpty()) review.accept(entry,legacyId);
    return result;
  }
}
