package com.example.payment.gateway.common.payment;

public record PaymentAcceptedOutboxEvent(
    String merchantId,
    String requestId,
    String idempotencyKey,
    String gatewayPaymentId,
    String downstreamPaymentId,
    String status,
    String routeCode,
    String targetService,
    String amount,
    String currency
) {
}
