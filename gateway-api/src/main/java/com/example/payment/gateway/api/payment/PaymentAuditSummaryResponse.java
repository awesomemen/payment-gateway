package com.example.payment.gateway.api.payment;

public record PaymentAuditSummaryResponse(
    long successRequestCount,
    long failureRequestCount,
    long exceptionEventCount,
    double successMetricCount,
    double failureMetricCount
) {
}
