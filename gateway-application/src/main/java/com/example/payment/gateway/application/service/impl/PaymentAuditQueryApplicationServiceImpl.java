package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentAuditSummaryResponse;
import com.example.payment.gateway.application.service.PaymentAuditQueryApplicationService;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.observability.payment.PaymentAuditMetricsRecorder;
import org.springframework.stereotype.Service;

@Service
public class PaymentAuditQueryApplicationServiceImpl implements PaymentAuditQueryApplicationService {

  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final PaymentAuditMetricsRecorder paymentAuditMetricsRecorder;

  public PaymentAuditQueryApplicationServiceImpl(
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      PaymentAuditMetricsRecorder paymentAuditMetricsRecorder
  ) {
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.paymentAuditMetricsRecorder = paymentAuditMetricsRecorder;
  }

  @Override
  public PaymentAuditSummaryResponse currentSummary() {
    return new PaymentAuditSummaryResponse(
        paymentRequestLogRepository.countByResponseStatus("SUCCESS"),
        paymentRequestLogRepository.countByResponseStatus("FAIL"),
        paymentExceptionEventRepository.countAll(),
        paymentAuditMetricsRecorder.currentCount("success"),
        paymentAuditMetricsRecorder.currentCount("failure")
    );
  }
}
