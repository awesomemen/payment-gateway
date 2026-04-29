package com.example.payment.gateway.api.payment;

public record TransactionDetailResponse(
    String bizType,
    String requestId,
    String gatewayOrderId,
    String downstreamOrderId,
    String status,
    String amount,
    String currency,
    String routeCode,
    String targetService
) {
}
