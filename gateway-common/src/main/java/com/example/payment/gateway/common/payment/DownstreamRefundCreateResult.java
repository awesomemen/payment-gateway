package com.example.payment.gateway.common.payment;

public record DownstreamRefundCreateResult(
    String downstreamRefundId,
    String status,
    String message
) {
}
