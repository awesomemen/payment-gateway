package com.example.payment.gateway.domain.model;

import java.time.Instant;
import java.util.Objects;

public record RefundCreateCommand(
    String merchantId,
    String requestId,
    String gatewayPaymentId,
    String idempotencyKey,
    Money amount,
    Instant requestTime,
    String nonce,
    String signature
) implements SignedPaymentCommand {

  public RefundCreateCommand {
    requireText(merchantId, "merchantId");
    requireText(requestId, "requestId");
    requireText(gatewayPaymentId, "gatewayPaymentId");
    requireText(idempotencyKey, "idempotencyKey");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(requestTime, "requestTime must not be null");
    requireText(nonce, "nonce");
    requireText(signature, "signature");
  }

  public String scopedIdempotencyKey() {
    return "REFUND:" + idempotencyKey;
  }

  public String idempotencyFingerprint() {
    return String.join("&",
        "bizType=REFUND",
        "merchantId=" + merchantId,
        "requestId=" + requestId,
        "gatewayPaymentId=" + gatewayPaymentId,
        "idempotencyKey=" + idempotencyKey,
        "amount=" + amount.amount().stripTrailingZeros().toPlainString(),
        "currency=" + amount.currency()
    );
  }

  @Override
  public String signaturePayload() {
    return String.join("&",
        "merchantId=" + merchantId,
        "requestId=" + requestId,
        "gatewayPaymentId=" + gatewayPaymentId,
        "idempotencyKey=" + idempotencyKey,
        "amount=" + amount.amount().stripTrailingZeros().toPlainString(),
        "currency=" + amount.currency(),
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
