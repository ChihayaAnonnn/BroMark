package com.jay.api.v2;

public record ExchangeRequest(
    @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_.:-]{1,128}")
        @jakarta.validation.constraints.NotBlank
        String requestId,
    @jakarta.validation.constraints.Positive long sku,
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 64)
        String configVersion) {}
