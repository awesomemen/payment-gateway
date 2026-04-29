package com.example.payment.gateway.api.payment;

public record PaymentCreateResponse(
    String gatewayPaymentId,
    String status,
    String routeCode,
    String message
) {
}
