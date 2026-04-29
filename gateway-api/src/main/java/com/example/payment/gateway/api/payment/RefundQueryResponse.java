package com.example.payment.gateway.api.payment;

public record RefundQueryResponse(
    String gatewayRefundId,
    String downstreamRefundId,
    String status,
    String routeCode,
    String message
) {
}
