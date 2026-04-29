package com.example.payment.gateway.domain.model;

import java.time.Instant;
import java.util.Objects;

public record RefundQueryCommand(
    String merchantId,
    String requestId,
    String gatewayRefundId,
    Instant requestTime,
    String nonce,
    String signature
) implements SignedPaymentCommand {

  public RefundQueryCommand {
    requireText(merchantId, "merchantId");
    requireText(requestId, "requestId");
    requireText(gatewayRefundId, "gatewayRefundId");
    Objects.requireNonNull(requestTime, "requestTime must not be null");
    requireText(nonce, "nonce");
    requireText(signature, "signature");
  }

  @Override
  public String signaturePayload() {
    return String.join("&",
        "merchantId=" + merchantId,
        "requestId=" + requestId,
        "gatewayRefundId=" + gatewayRefundId,
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
