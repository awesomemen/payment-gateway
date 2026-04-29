package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.RefundCallbackRequest;
import com.example.payment.gateway.api.payment.RefundCallbackResponse;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.payment.RefundOrderRecord;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

class BootstrapRefundCallbackApplicationServiceTest {

  private BootstrapRefundCallbackApplicationService service;
  private RefundOrderRepository refundOrderRepository;

  @BeforeEach
  void setUp() {
    PaymentRequestSecurityValidator validator = Mockito.mock(PaymentRequestSecurityValidator.class);
    refundOrderRepository = Mockito.mock(RefundOrderRepository.class);
    PaymentRequestLogRepository requestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    PaymentExceptionEventRepository exceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    doNothing().when(validator).validate(any());
    given(refundOrderRepository.findByGatewayRefundId("GR123")).willReturn(Optional.of(new RefundOrderRecord(
        "GR123",
        "MCH100001",
        "REQ-REFUND-CALLBACK-20260423-0001",
        "GP123456",
        "RIDEMP-0001",
        "ROUTE_REFUND_CREATE",
        "com.example.payment.api.RefundFacade",
        "DSR123",
        "PROCESSING",
        "10.50",
        "CNY"
    )));
    service = new BootstrapRefundCallbackApplicationService(
        validator,
        refundOrderRepository,
        requestLogRepository,
        exceptionEventRepository,
        Clock.fixed(Instant.parse("2026-04-23T02:20:00Z"), ZoneOffset.UTC)
    );
  }

  @Test
  void shouldAcceptRefundCallback() {
    RefundCallbackResponse response = service.handle(new RefundCallbackRequest(
        "MCH100001",
        "REQ-REFUND-CALLBACK-20260423-0001",
        "GR123",
        "DSR123",
        "SUCCEEDED",
        Instant.parse("2026-04-23T02:20:00Z"),
        "nonce-refund-callback-001",
        "demo-signature",
        "ok"
    ));

    assertThat(response.status()).isEqualTo("SUCCEEDED");
    verify(refundOrderRepository).save(any());
  }
}
