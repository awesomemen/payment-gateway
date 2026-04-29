package com.example.payment.gateway.api.payment;

public record PaymentCallbackResponse(
    String gatewayPaymentId,
    String status,
    String message
) {
}
