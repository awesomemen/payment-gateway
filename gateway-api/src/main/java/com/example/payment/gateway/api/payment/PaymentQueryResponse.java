package com.example.payment.gateway.api.payment;

public record PaymentQueryResponse(
    String gatewayPaymentId,
    String downstreamPaymentId,
    String status,
    String routeCode,
    String message
) {
}
