package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentAuditSummaryResponse;

public interface PaymentAuditQueryApplicationService {

  PaymentAuditSummaryResponse currentSummary();
}
