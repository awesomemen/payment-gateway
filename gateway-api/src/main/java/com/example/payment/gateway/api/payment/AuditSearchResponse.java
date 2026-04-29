package com.example.payment.gateway.api.payment;

import java.util.List;

public record AuditSearchResponse(
    List<AuditLogEntryResponse> requestLogs,
    List<AuditEventEntryResponse> exceptionEvents
) {
}
