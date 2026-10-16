package com.jay.domain.task.model;

import java.time.Instant;

/** 上界只在一轮内固定；耗尽后调用方回绕，不作业务事实。 */
public record RecoveryScanPosition(Instant after, String afterId, Instant upper, String upperId) {}
