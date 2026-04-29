package com.example.payment.gateway.common.payment;

import java.util.Objects;

public record DownstreamPaymentCreateResult(
    String downstreamPaymentId,
    String status,
    String message
) {

  public DownstreamPaymentCreateResult {
    Objects.requireNonNull(downstreamPaymentId, "downstreamPaymentId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(message, "message must not be null");
  }
}
