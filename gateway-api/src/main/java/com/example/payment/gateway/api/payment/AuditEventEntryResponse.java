package com.example.payment.gateway.api.payment;

public record AuditEventEntryResponse(
    String traceId,
    String requestId,
    String merchantId,
    String bizType,
    String apiCode,
    String eventType,
    String eventLevel,
    String eventCode,
    String eventMessage
) {
}
