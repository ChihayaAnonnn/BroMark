package com.jay.domain.task.service;

import com.jay.domain.asset.model.RouteSlot;

/** 有限标签的发布/恢复观测；业务身份只能进入结构化日志。 */
public interface RelayObservation {
  RelayObservation NOOP =
      new RelayObservation() {
        @Override
        public void duration(
            String stage, RouteSlot slot, String eventType, String outcome, long nanos) {}

        @Override
        public void count(String action, RouteSlot slot, String eventType, String reason, long n) {}
      };

  void duration(String stage, RouteSlot slot, String eventType, String outcome, long nanos);

  void count(String action, RouteSlot slot, String eventType, String reason, long n);

  /** P0 受理结果：accepted/rejected/ratelimited。 */
  default void p0Admission(String result) {}

  /** P0 交付结果：delivered/duplicate/retry/failed。 */
  default void p0Delivery(String result) {}

  /** P0 事件结果；type 只能是有限事件类型。 */
  default void p0Event(String type, String result) {}

  /** P0 阶段耗时；stage=admission/reserve/deliver。 */
  default void p0Stage(String stage, String result, long nanos) {}
}
