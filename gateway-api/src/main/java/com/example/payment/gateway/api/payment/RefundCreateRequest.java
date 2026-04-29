package com.example.payment.gateway.api.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record RefundCreateRequest(
    @NotBlank String merchantId,
    @NotBlank String requestId,
    @NotBlank String gatewayPaymentId,
    @NotBlank String idempotencyKey,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank String currency,
    @NotNull Instant requestTime,
    @NotBlank String nonce,
    @NotBlank String signature
) {
}
