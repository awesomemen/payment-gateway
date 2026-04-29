package com.example.payment.gateway.common.payment;

import java.util.Objects;

public record PaymentOrderRecord(
    String gatewayPaymentId,
    String merchantId,
    String requestId,
    String idempotencyKey,
    String routeCode,
    String targetService,
    String downstreamPaymentId,
    String paymentStatus,
    String amount,
    String currency
) {

  public PaymentOrderRecord {
    Objects.requireNonNull(gatewayPaymentId, "gatewayPaymentId must not be null");
    Objects.requireNonNull(merchantId, "merchantId must not be null");
    Objects.requireNonNull(requestId, "requestId must not be null");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(routeCode, "routeCode must not be null");
    Objects.requireNonNull(targetService, "targetService must not be null");
    Objects.requireNonNull(downstreamPaymentId, "downstreamPaymentId must not be null");
    Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
  }

  public PaymentOrderRecord withPaymentStatus(String newPaymentStatus) {
    return new PaymentOrderRecord(
        gatewayPaymentId,
        merchantId,
        requestId,
        idempotencyKey,
        routeCode,
        targetService,
        downstreamPaymentId,
        newPaymentStatus,
        amount,
        currency
    );
  }
}
