package com.example.payment.gateway.common.payment;

public record RefundOrderRecord(
    String gatewayRefundId,
    String merchantId,
    String requestId,
    String gatewayPaymentId,
    String idempotencyKey,
    String routeCode,
    String targetService,
    String downstreamRefundId,
    String refundStatus,
    String amount,
    String currency
) {

  public RefundOrderRecord withRefundStatus(String nextStatus) {
    return new RefundOrderRecord(
        gatewayRefundId,
        merchantId,
        requestId,
        gatewayPaymentId,
        idempotencyKey,
        routeCode,
        targetService,
        downstreamRefundId,
        nextStatus,
        amount,
        currency
    );
  }
}
