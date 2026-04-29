package com.example.payment.gateway.domain.model;

import java.time.Instant;
import java.util.Objects;

public record PaymentCallbackCommand(
    String merchantId,
    String requestId,
    String gatewayPaymentId,
    String downstreamPaymentId,
    String status,
    Instant requestTime,
    String nonce,
    String signature
) implements SignedPaymentCommand {

  public PaymentCallbackCommand {
    requireText(merchantId, "merchantId");
    requireText(requestId, "requestId");
    requireText(gatewayPaymentId, "gatewayPaymentId");
    requireText(downstreamPaymentId, "downstreamPaymentId");
    requireText(status, "status");
    Objects.requireNonNull(requestTime, "requestTime must not be null");
    requireText(nonce, "nonce");
    requireText(signature, "signature");
  }

  @Override
  public String signaturePayload() {
    return String.join("&",
        "merchantId=" + merchantId,
        "requestId=" + requestId,
        "gatewayPaymentId=" + gatewayPaymentId,
        "downstreamPaymentId=" + downstreamPaymentId,
        "status=" + status,
        "requestTime=" + requestTime,
        "nonce=" + nonce
    );
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
