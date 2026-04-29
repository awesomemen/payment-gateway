package com.example.payment.gateway.common.payment;

public record PaymentExceptionEventRecord(
    String traceId,
    String requestId,
    String merchantId,
    String bizType,
    String apiCode,
    String eventType,
    String eventLevel,
    String eventCode,
    String eventMessage,
    String detailJson
) {
}
