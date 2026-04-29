package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentQueryRequest;
import com.example.payment.gateway.api.payment.PaymentQueryResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BootstrapPaymentQueryApplicationServiceTest {

  private static final Instant NOW = Instant.parse("2026-04-22T04:10:00Z");

  private PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private PaymentOrderRepository paymentOrderRepository;
  private GatewayRouteRepository gatewayRouteRepository;
  private DownstreamPaymentQueryGateway downstreamPaymentQueryGateway;
  private PaymentRequestLogRepository paymentRequestLogRepository;
  private PaymentExceptionEventRepository paymentExceptionEventRepository;
  private BootstrapPaymentQueryApplicationService service;

  @BeforeEach
  void setUp() {
    paymentRequestSecurityValidator = Mockito.mock(PaymentRequestSecurityValidator.class);
    paymentOrderRepository = Mockito.mock(PaymentOrderRepository.class);
    gatewayRouteRepository = Mockito.mock(GatewayRouteRepository.class);
    downstreamPaymentQueryGateway = Mockito.mock(DownstreamPaymentQueryGateway.class);
    paymentRequestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    paymentExceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    service = new BootstrapPaymentQueryApplicationService(
        paymentRequestSecurityValidator,
        paymentOrderRepository,
        gatewayRouteRepository,
        downstreamPaymentQueryGateway,
        paymentRequestLogRepository,
        paymentExceptionEventRepository,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );
    given(gatewayRouteRepository.findRoute("PAY", "QUERY")).willReturn(Optional.of(route()));
  }

  @Test
  void shouldReturnLocalFinalStatusWithoutCallingDownstream() {
    given(paymentOrderRepository.findByGatewayPaymentId("GP-FINAL-001"))
        .willReturn(Optional.of(order("GP-FINAL-001", "SUCCEEDED")));

    PaymentQueryResponse response = service.query(request("GP-FINAL-001"));

    Assertions.assertEquals("SUCCEEDED", response.status());
    verify(downstreamPaymentQueryGateway, never()).query(any(), any());
  }

  @Test
  void shouldQueryDownstreamAndUpdateOrderWhenStatusIsStillActive() {
    PaymentOrderRecord order = order("GP-PROCESSING-001", "PROCESSING");
    given(paymentOrderRepository.findByGatewayPaymentId("GP-PROCESSING-001")).willReturn(Optional.of(order));
    given(downstreamPaymentQueryGateway.query(eq(route()), any()))
        .willReturn(new DownstreamPaymentQueryResult("SUCCEEDED", "payment succeeded in mock downstream facade"));

    PaymentQueryResponse response = service.query(request("GP-PROCESSING-001"));

    Assertions.assertEquals("SUCCEEDED", response.status());
    verify(paymentOrderRepository).save(order.withPaymentStatus("SUCCEEDED"));
  }

  @Test
  void shouldThrowWhenPaymentOrderIsMissing() {
    given(paymentOrderRepository.findByGatewayPaymentId("GP-MISSING-001")).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.query(request("GP-MISSING-001")))
        .isInstanceOf(GatewayException.class)
        .extracting(exception -> ((GatewayException) exception).code())
        .isEqualTo(GatewayResponseCodes.PAYMENT_ORDER_NOT_FOUND);
  }

  private static PaymentQueryRequest request(String gatewayPaymentId) {
    return new PaymentQueryRequest(
        "MCH100001",
        "REQ-QUERY-20260422-0001",
        gatewayPaymentId,
        NOW.minusSeconds(30),
        "nonce-query-001",
        "demo-signature"
    );
  }

  private static PaymentOrderRecord order(String gatewayPaymentId, String status) {
    return new PaymentOrderRecord(
        gatewayPaymentId,
        "MCH100001",
        "REQ-QUERY-20260422-0001",
        "IDEMP-QUERY-20260422-0001",
        "ROUTE_PAY_CREATE",
        "com.example.payment.api.PaymentCreateFacade",
        "DSP-QUERY-0001",
        status,
        "88.5",
        "CNY"
    );
  }

  private static GatewayRouteDefinition route() {
    return new GatewayRouteDefinition(
        "ROUTE_PAY_QUERY",
        "PAY",
        "QUERY",
        "DUBBO",
        "com.example.payment.api.PaymentQueryFacade",
        "queryPayment",
        2000,
        0,
        "gateway:pay:query"
    );
  }
}
