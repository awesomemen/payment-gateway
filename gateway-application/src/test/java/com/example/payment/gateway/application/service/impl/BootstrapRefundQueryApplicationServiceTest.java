package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.RefundQueryRequest;
import com.example.payment.gateway.api.payment.RefundQueryResponse;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
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

class BootstrapRefundQueryApplicationServiceTest {

  private BootstrapRefundQueryApplicationService service;
  private RefundOrderRepository refundOrderRepository;

  @BeforeEach
  void setUp() {
    PaymentRequestSecurityValidator validator = Mockito.mock(PaymentRequestSecurityValidator.class);
    refundOrderRepository = Mockito.mock(RefundOrderRepository.class);
    GatewayRouteRepository routeRepository = Mockito.mock(GatewayRouteRepository.class);
    DownstreamRefundQueryGateway downstreamGateway = Mockito.mock(DownstreamRefundQueryGateway.class);
    PaymentRequestLogRepository requestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    PaymentExceptionEventRepository exceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    doNothing().when(validator).validate(any(), any(Boolean.class));
    given(routeRepository.findRoute("REFUND", "QUERY")).willReturn(Optional.of(new GatewayRouteDefinition(
        "ROUTE_REFUND_QUERY",
        "REFUND",
        "QUERY",
        "DUBBO",
        "com.example.payment.api.RefundFacade",
        "queryRefund",
        3000,
        0,
        "gateway:refund:query"
    )));
    given(refundOrderRepository.findByGatewayRefundId("GR123")).willReturn(Optional.of(new RefundOrderRecord(
        "GR123",
        "MCH100001",
        "REQ-REFUND-PROCESSING-20260423-0001",
        "GP123456",
        "RIDEMP-0001",
        "ROUTE_REFUND_CREATE",
        "com.example.payment.api.RefundFacade",
        "DSR123",
        "PROCESSING",
        "10.50",
        "CNY"
    )));
    given(downstreamGateway.query(any(), any())).willReturn(new DownstreamRefundQueryResult("SUCCEEDED", "refund succeeded"));
    service = new BootstrapRefundQueryApplicationService(
        validator,
        refundOrderRepository,
        routeRepository,
        downstreamGateway,
        requestLogRepository,
        exceptionEventRepository,
        Clock.fixed(Instant.parse("2026-04-23T02:10:00Z"), ZoneOffset.UTC)
    );
  }

  @Test
  void shouldAdvanceRefundStatusWhenDownstreamSucceeds() {
    RefundQueryResponse response = service.query(new RefundQueryRequest(
        "MCH100001",
        "REQ-REFUND-PROCESSING-20260423-0001",
        "GR123",
        Instant.parse("2026-04-23T02:10:00Z"),
        "nonce-refund-query-001",
        "demo-signature"
    ));

    assertThat(response.status()).isEqualTo("SUCCEEDED");
    verify(refundOrderRepository).save(any());
  }
}
