package com.example.payment.gateway.common.idempotency;

public record PaymentIdempotencyRecord(
    String merchantId,
    String idempotencyKey,
    String requestFingerprint,
    StoredPaymentResult storedPaymentResult
) {

  public static PaymentIdempotencyRecord success(
      String merchantId,
      String idempotencyKey,
      String requestFingerprint,
      String gatewayPaymentId,
      String paymentStatus,
      String routeCode,
      String responseMessage
  ) {
    return new PaymentIdempotencyRecord(
        merchantId,
        idempotencyKey,
        requestFingerprint,
        StoredPaymentResult.success(gatewayPaymentId, paymentStatus, routeCode, responseMessage)
    );
  }

  public static PaymentIdempotencyRecord failure(
      String merchantId,
      String idempotencyKey,
      String requestFingerprint,
      String errorCode,
      int errorStatus,
      String errorMessage
  ) {
    return new PaymentIdempotencyRecord(
        merchantId,
        idempotencyKey,
        requestFingerprint,
        StoredPaymentResult.failure(errorCode, errorStatus, errorMessage)
    );
  }
}
