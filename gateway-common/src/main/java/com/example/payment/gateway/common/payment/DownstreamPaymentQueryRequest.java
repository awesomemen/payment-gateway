package com.example.payment.gateway.common.payment;

import java.time.Instant;

public record DownstreamPaymentQueryRequest(
    String merchantId,
    String requestId,
    String gatewayPaymentId,
    String downstreamPaymentId,
    String currentStatus,
    Instant requestTime
) {
}
