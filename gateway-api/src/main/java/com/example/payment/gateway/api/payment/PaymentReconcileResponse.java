package com.example.payment.gateway.api.payment;

import java.util.List;

public record PaymentReconcileResponse(
    int scannedCount,
    int updatedCount,
    int unchangedCount,
    int failedCount,
    List<String> updatedGatewayPaymentIds
) {
}
