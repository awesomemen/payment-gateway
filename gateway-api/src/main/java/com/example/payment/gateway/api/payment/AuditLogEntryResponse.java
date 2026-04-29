package com.example.payment.gateway.api.payment;

public record AuditLogEntryResponse(
    String traceId,
    String requestId,
    String merchantId,
    String bizType,
    String apiCode,
    String responseCode,
    String responseStatus,
    String errorType,
    String errorMessage
) {
}
