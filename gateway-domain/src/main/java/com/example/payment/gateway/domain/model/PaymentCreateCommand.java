package com.example.payment.gateway.domain.model;

import java.time.Instant;
import java.util.Objects;

public record PaymentCreateCommand(
    String merchantId,
    String requestId,
    String idempotencyKey,
    Money amount,
    Instant requestTime,
    String nonce,
    String signature
) implements SignedPaymentCommand {

  public PaymentCreateCommand {
    requireText(merchantId, "merchantId");
    requireText(requestId, "requestId");
    requireText(idempotencyKey, "idempotencyKey");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(requestTime, "requestTime must not be null");
    requireText(nonce, "nonce");
    requireText(signature, "signature");
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }

  public String idempotencyFingerprint() {
    return String.join("&",
        "merchantId=" + merchantId,
        "requestId=" + requestId,
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
        "idempotencyKey=" + idempotencyKey,
        "amount=" + amount.amount().stripTrailingZeros().toPlainString(),
        "currency=" + amount.currency(),
        "requestTime=" + requestTime,
        "nonce=" + nonce
    );
  }
}
