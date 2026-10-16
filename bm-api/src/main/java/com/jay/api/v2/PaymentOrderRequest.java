package com.jay.api.v2;

public record PaymentOrderRequest(
    @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_.:-]{1,128}")
        @jakarta.validation.constraints.NotNull
        String requestId,
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 128)
        String productId,
    @jakarta.validation.constraints.NotBlank String provider) {}
