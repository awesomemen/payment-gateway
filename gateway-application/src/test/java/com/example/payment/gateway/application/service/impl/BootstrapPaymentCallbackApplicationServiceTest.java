package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentCallbackRequest;
import com.example.payment.gateway.api.payment.PaymentCallbackResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class BootstrapPaymentCallbackApplicationServiceTest {

  private static final Instant NOW = Instant.parse("2026-04-22T04:20:00Z");

  private PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private PaymentOrderRepository paymentOrderRepository;
  private PaymentRequestLogRepository paymentRequestLogRepository;
  private PaymentExceptionEventRepository paymentExceptionEventRepository;
  private BootstrapPaymentCallbackApplicationService service;

  @BeforeEach
  void setUp() {
    paymentRequestSecurityValidator = Mockito.mock(PaymentRequestSecurityValidator.class);
    paymentOrderRepository = Mockito.mock(PaymentOrderRepository.class);
    paymentRequestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    paymentExceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    service = new BootstrapPaymentCallbackApplicationService(
        paymentRequestSecurityValidator,
        paymentOrderRepository,
        paymentRequestLogRepository,
        paymentExceptionEventRepository,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );
  }

  @Test
  void shouldUpdatePaymentStatusWhenCallbackIsAccepted() {
    PaymentOrderRecord order = order("PROCESSING");
    given(paymentOrderRepository.findByGatewayPaymentId("GP-CALLBACK-001")).willReturn(Optional.of(order));

    PaymentCallbackResponse response = service.handle(request("SUCCEEDED"));

    Assertions.assertEquals("SUCCEEDED", response.status());
    verify(paymentOrderRepository).save(order.withPaymentStatus("SUCCEEDED"));
  }

  @Test
  void shouldRejectIllegalFinalStateTransition() {
    given(paymentOrderRepository.findByGatewayPaymentId("GP-CALLBACK-001"))
        .willReturn(Optional.of(order("SUCCEEDED")));

    assertThatThrownBy(() -> service.handle(request("FAILED")))
        .isInstanceOf(GatewayException.class)
        .extracting(exception -> ((GatewayException) exception).code())
        .isEqualTo(GatewayResponseCodes.PAYMENT_STATUS_CONFLICT);
  }

  @Test
  void shouldRejectMismatchedDownstreamPaymentId() {
    given(paymentOrderRepository.findByGatewayPaymentId("GP-CALLBACK-001"))
        .willReturn(Optional.of(order("PROCESSING")));

    assertThatThrownBy(() -> service.handle(new PaymentCallbackRequest(
        "MCH100001",
        "REQ-CALLBACK-20260422-0001",
        "GP-CALLBACK-001",
        "DSP-OTHER-0001",
        "SUCCEEDED",
        NOW.minusSeconds(20),
        "nonce-callback-001",
        "demo-signature",
        "ok"
    )))
        .isInstanceOf(GatewayException.class)
        .extracting(exception -> ((GatewayException) exception).code())
        .isEqualTo(GatewayResponseCodes.PAYMENT_ORDER_MISMATCH);
  }

  private static PaymentCallbackRequest request(String status) {
    return new PaymentCallbackRequest(
        "MCH100001",
        "REQ-CALLBACK-20260422-0001",
        "GP-CALLBACK-001",
        "DSP-CALLBACK-0001",
        status,
        NOW.minusSeconds(20),
        "nonce-callback-001",
        "demo-signature",
        "ok"
    );
  }

  private static PaymentOrderRecord order(String status) {
    return new PaymentOrderRecord(
        "GP-CALLBACK-001",
        "MCH100001",
        "REQ-CALLBACK-20260422-0001",
        "IDEMP-CALLBACK-20260422-0001",
        "ROUTE_PAY_CREATE",
        "com.example.payment.api.PaymentCreateFacade",
        "DSP-CALLBACK-0001",
        status,
        "88.5",
        "CNY"
    );
  }
}
