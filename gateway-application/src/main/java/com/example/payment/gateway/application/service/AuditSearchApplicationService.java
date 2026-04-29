package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.AuditSearchResponse;

public interface AuditSearchApplicationService {

  AuditSearchResponse search(String merchantId, String requestId, int limit);
}
