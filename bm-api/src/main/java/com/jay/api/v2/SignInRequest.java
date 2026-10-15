package com.jay.api.v2;

public record SignInRequest(
    @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_.:-]{1,128}")
        @jakarta.validation.constraints.NotBlank
        String requestId,
    @jakarta.validation.constraints.NotNull java.time.LocalDate targetDate) {}
