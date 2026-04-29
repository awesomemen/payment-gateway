package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.AuditSearchResponse;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class AuditSearchApplicationServiceTest {

  private PaymentRequestLogRepository paymentRequestLogRepository;
  private PaymentExceptionEventRepository paymentExceptionEventRepository;
  private AuditSearchApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    paymentRequestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    paymentExceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    service = new AuditSearchApplicationServiceImpl(paymentRequestLogRepository, paymentExceptionEventRepository);
  }

  @Test
  void shouldReturnRecentLogsAndEvents() {
    given(paymentRequestLogRepository.findRecent("MCH100001", "REQ-1", 10)).willReturn(List.of(new PaymentRequestLogRecord(
        "trace-1", "REQ-1", null, "MCH100001", null, "PAY", "CREATE", "POST", "/api/v1/payments",
        Instant.parse("2026-04-23T03:00:00Z"), Instant.parse("2026-04-23T03:00:01Z"), 1000, "ROUTE_PAY_CREATE",
        "svc", "SUCCESS", "SUCCESS", null, null, "{}", "{}"
    )));
    given(paymentExceptionEventRepository.findRecent("MCH100001", "REQ-1", 10)).willReturn(List.of(new PaymentExceptionEventRecord(
        "trace-2", "REQ-1", "MCH100001", "PAY", "CREATE", "DOWNSTREAM_TIMEOUT", "WARN", "DOWNSTREAM_TIMEOUT", "timeout", "{}"
    )));

    AuditSearchResponse response = service.search("MCH100001", "REQ-1", 10);

    assertThat(response.requestLogs()).hasSize(1);
    assertThat(response.exceptionEvents()).hasSize(1);
  }
}
