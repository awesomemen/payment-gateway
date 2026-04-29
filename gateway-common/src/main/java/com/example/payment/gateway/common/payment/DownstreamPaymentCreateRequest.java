package com.example.payment.gateway.common.payment;

import java.time.Instant;
import java.util.Objects;

public record DownstreamPaymentCreateRequest(
    String merchantId,
    String requestId,
    String idempotencyKey,
    String amount,
    String currency,
    Instant requestTime
) {

  public DownstreamPaymentCreateRequest {
    Objects.requireNonNull(merchantId, "merchantId must not be null");
    Objects.requireNonNull(requestId, "requestId must not be null");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(requestTime, "requestTime must not be null");
  }
}
