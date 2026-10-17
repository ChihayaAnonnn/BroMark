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
}
