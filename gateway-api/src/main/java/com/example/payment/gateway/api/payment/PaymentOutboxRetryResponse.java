package com.example.payment.gateway.api.payment;

import java.util.List;

public record PaymentOutboxRetryResponse(
    int scannedCount,
    int succeededCount,
    int failedCount,
    List<String> retriedMessageKeys
) {
}
