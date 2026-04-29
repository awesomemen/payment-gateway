package com.example.payment.gateway.common.payment;

import java.time.Instant;

public record PaymentMqOutboxRecord(
    long id,
    String eventKey,
    String bizType,
    String topic,
    String tag,
    String messageKey,
    String payloadJson,
    int sendStatus,
    int retryCount,
    Instant nextRetryTime,
    String lastErrorMessage
) {
}
