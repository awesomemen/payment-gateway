package com.example.payment.gateway.api.payment;

import java.util.List;

public record PaymentNotificationRetryResponse(
    int scannedCount,
    int succeededCount,
    int failedCount,
    int deadLetteredCount,
    List<String> messageKeys
) {
}
