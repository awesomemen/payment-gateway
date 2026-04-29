package com.example.payment.gateway.api.payment;

public record TransactionDetailRequest(
    String merchantId,
    String requestId,
    String gatewayOrderId
) {
}
