package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.AuditEventEntryResponse;
import com.example.payment.gateway.api.payment.AuditLogEntryResponse;
import com.example.payment.gateway.api.payment.AuditSearchResponse;
import com.example.payment.gateway.application.service.AuditSearchApplicationService;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AuditSearchApplicationServiceImpl implements AuditSearchApplicationService {

  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;

  public AuditSearchApplicationServiceImpl(
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository
  ) {
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
  }

  @Override
  public AuditSearchResponse search(String merchantId, String requestId, int limit) {
    return new AuditSearchResponse(
        paymentRequestLogRepository.findRecent(merchantId, requestId, limit).stream()
            .map(record -> new AuditLogEntryResponse(
                record.traceId(),
                record.requestId(),
                record.merchantId(),
                record.bizType(),
                record.apiCode(),
                record.responseCode(),
                record.responseStatus(),
                record.errorType(),
                record.errorMessage()
            ))
            .collect(Collectors.toList()),
        paymentExceptionEventRepository.findRecent(merchantId, requestId, limit).stream()
            .map(record -> new AuditEventEntryResponse(
                record.traceId(),
                record.requestId(),
                record.merchantId(),
                record.bizType(),
                record.apiCode(),
                record.eventType(),
                record.eventLevel(),
                record.eventCode(),
                record.eventMessage()
            ))
            .collect(Collectors.toList())
    );
  }
}
