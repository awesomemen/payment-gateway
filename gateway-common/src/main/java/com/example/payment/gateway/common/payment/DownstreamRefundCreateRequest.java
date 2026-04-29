package com.example.payment.gateway.common.payment;

import java.time.Instant;

public record DownstreamRefundCreateRequest(
    String merchantId,
    String requestId,
    String gatewayPaymentId,
    String idempotencyKey,
    String amount,
    String currency,
    Instant requestTime
) {
}
