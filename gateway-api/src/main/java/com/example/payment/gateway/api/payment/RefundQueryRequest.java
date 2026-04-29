package com.example.payment.gateway.api.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RefundQueryRequest(
    @NotBlank String merchantId,
    @NotBlank String requestId,
    @NotBlank String gatewayRefundId,
    @NotNull Instant requestTime,
    @NotBlank String nonce,
    @NotBlank String signature
) {
}
