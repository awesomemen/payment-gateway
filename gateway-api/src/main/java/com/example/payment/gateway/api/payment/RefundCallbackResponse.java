package com.example.payment.gateway.api.payment;

public record RefundCallbackResponse(
    String gatewayRefundId,
    String status,
    String message
) {
}
