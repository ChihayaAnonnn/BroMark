package com.jay.domain.task.model;

public record PublishResolution(ClaimedPublish task, PublishOutcome outcome, String reason) {}
