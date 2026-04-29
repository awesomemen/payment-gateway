package com.example.payment.gateway.common.payment;

import java.time.Instant;

public record DownstreamRefundQueryRequest(
    String merchantId,
    String requestId,
    String gatewayRefundId,
    String downstreamRefundId,
    String currentStatus,
    Instant requestTime
) {
}
