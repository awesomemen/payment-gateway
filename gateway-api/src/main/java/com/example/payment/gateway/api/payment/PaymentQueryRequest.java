package com.example.payment.gateway.api.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record PaymentQueryRequest(
    @NotBlank String merchantId,
    @NotBlank String requestId,
    @NotBlank String gatewayPaymentId,
    @NotNull Instant requestTime,
    @NotBlank String nonce,
    @NotBlank String signature
) {
}
