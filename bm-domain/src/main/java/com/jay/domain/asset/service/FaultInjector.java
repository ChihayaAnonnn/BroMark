package com.jay.domain.asset.service;

@FunctionalInterface
public interface FaultInjector {
  void hit(String point);

  /** 仅测试探针可选择延迟领取；旧函数式实现及生产默认无动作。 */
  default void beforeClaim() {}

  /** 故障探针可核对本次发布的持久化身份；旧函数式实现保持原行为。 */
  default void hit(
      String point, String eventId, String aggregateId, String leaseToken, int attempt) {
    hit(point);
  }
}
