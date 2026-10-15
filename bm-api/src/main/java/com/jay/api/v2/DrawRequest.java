package com.jay.api.v2;

public record DrawRequest(
    @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_.:-]{1,128}")
        String requestId,
    @jakarta.validation.constraints.Positive long activityId) {}
