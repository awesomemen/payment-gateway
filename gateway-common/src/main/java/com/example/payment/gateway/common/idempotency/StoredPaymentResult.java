package com.example.payment.gateway.common.idempotency;

import java.util.Objects;

public record StoredPaymentResult(
    boolean success,
    String gatewayPaymentId,
    String paymentStatus,
    String routeCode,
    String responseMessage,
    String errorCode,
    int errorStatus,
    String errorMessage
) {

  public StoredPaymentResult {
    if (success) {
      Objects.requireNonNull(paymentStatus, "paymentStatus must not be null when success is true");
    } else {
      Objects.requireNonNull(errorCode, "errorCode must not be null when success is false");
      Objects.requireNonNull(errorMessage, "errorMessage must not be null when success is false");
    }
  }

  public static StoredPaymentResult success(
      String gatewayPaymentId,
      String paymentStatus,
      String routeCode,
      String responseMessage
  ) {
    return new StoredPaymentResult(true, gatewayPaymentId, paymentStatus, routeCode, responseMessage, null, 200, null);
  }

  public static StoredPaymentResult failure(String errorCode, int errorStatus, String errorMessage) {
    return new StoredPaymentResult(false, null, null, null, null, errorCode, errorStatus, errorMessage);
  }
}
