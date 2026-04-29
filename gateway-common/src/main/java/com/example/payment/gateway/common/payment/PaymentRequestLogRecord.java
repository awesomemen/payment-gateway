package com.example.payment.gateway.common.payment;

import java.time.Instant;

public record PaymentRequestLogRecord(
    String traceId,
    String requestId,
    String idempotencyKey,
    String merchantId,
    String appId,
    String bizType,
    String apiCode,
    String httpMethod,
    String requestUri,
    Instant requestTime,
    Instant finishTime,
    Integer durationMillis,
    String routeCode,
    String targetService,
    String responseCode,
    String responseStatus,
    String errorType,
    String errorMessage,
    String requestSummaryJson,
    String extJson
) {
}
