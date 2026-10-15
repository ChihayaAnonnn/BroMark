package com.jay.domain.rebate.model;

import com.jay.domain.asset.model.*;
import java.time.LocalDate;
import java.util.*;

public record SignInResult(
    String orderId,
    LocalDate businessDate,
    String state,
    List<RewardReceipt> rewards,
    String reason) {
  public SignInResult {
    rewards = List.copyOf(rewards);
  }

  public record RewardReceipt(String itemId, AssetReceipt credit, QuotaReceipt quota) {}
}
