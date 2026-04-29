package com.example.payment.gateway.common.payment;

import java.util.Objects;

public record PaymentMessageConsumeRecord(
    Long id,
    String messageKey,
    String bizType,
    String consumerGroup,
    String payloadJson,
    String consumeStatus,
    int retryCount,
    boolean deadLetter,
    String lastErrorMessage
) {

  public PaymentMessageConsumeRecord {
    Objects.requireNonNull(messageKey, "messageKey must not be null");
    Objects.requireNonNull(bizType, "bizType must not be null");
    Objects.requireNonNull(consumerGroup, "consumerGroup must not be null");
    Objects.requireNonNull(payloadJson, "payloadJson must not be null");
    Objects.requireNonNull(consumeStatus, "consumeStatus must not be null");
  }

  public PaymentMessageConsumeRecord success() {
    return new PaymentMessageConsumeRecord(
        id,
        messageKey,
        bizType,
        consumerGroup,
        payloadJson,
        "SUCCESS",
        retryCount,
        false,
        null
    );
  }

  public PaymentMessageConsumeRecord failure(int nextRetryCount, boolean nextDeadLetter, String errorMessage) {
    return new PaymentMessageConsumeRecord(
        id,
        messageKey,
        bizType,
        consumerGroup,
        payloadJson,
        "FAILED",
        nextRetryCount,
        nextDeadLetter,
        errorMessage
    );
  }
}
