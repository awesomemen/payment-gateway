package com.example.payment.gateway.api.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record PaymentCallbackRequest(
    @NotBlank String merchantId,
    @NotBlank String requestId,
    @NotBlank String gatewayPaymentId,
    @NotBlank String downstreamPaymentId,
    @NotBlank String status,
    @NotNull Instant requestTime,
    @NotBlank String nonce,
    @NotBlank String signature,
    String message
) {
}
