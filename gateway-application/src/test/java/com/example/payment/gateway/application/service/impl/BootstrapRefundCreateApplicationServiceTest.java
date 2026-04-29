package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.RefundCreateRequest;
import com.example.payment.gateway.api.payment.RefundCreateResponse;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import com.example.payment.gateway.governance.payment.PaymentGovernanceGuard;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.math.BigDecimal;
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

class BootstrapRefundCreateApplicationServiceTest {

  private BootstrapRefundCreateApplicationService service;
  private RefundOrderRepository refundOrderRepository;

  @BeforeEach
  void setUp() {
    PaymentRequestSecurityValidator validator = Mockito.mock(PaymentRequestSecurityValidator.class);
    RefundCreateIdempotencyCoordinator coordinator = Mockito.mock(RefundCreateIdempotencyCoordinator.class);
    DownstreamRefundCreateGateway downstreamGateway = Mockito.mock(DownstreamRefundCreateGateway.class);
    refundOrderRepository = Mockito.mock(RefundOrderRepository.class);
    GatewayRouteRepository routeRepository = Mockito.mock(GatewayRouteRepository.class);
    PaymentRequestLogRepository requestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    PaymentExceptionEventRepository exceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    PaymentGovernanceGuard governanceGuard = Mockito.mock(PaymentGovernanceGuard.class);
    doNothing().when(validator).validate(any());
    doNothing().when(governanceGuard).guardPaymentCreate(any());
    given(routeRepository.findRoute("REFUND", "CREATE")).willReturn(Optional.of(new GatewayRouteDefinition(
        "ROUTE_REFUND_CREATE",
        "REFUND",
        "CREATE",
        "DUBBO",
        "com.example.payment.api.RefundFacade",
        "createRefund",
        3000,
        0,
        "gateway:refund:create"
    )));
    given(downstreamGateway.create(any(), any(), any()))
        .willReturn(new DownstreamRefundCreateResult("DSR1001", "ACCEPTED", "refund accepted"));
    given(coordinator.execute(any(), any())).willAnswer(invocation -> invocation.<java.util.function.Supplier<RefundCreateResponse>>getArgument(1).get());

    service = new BootstrapRefundCreateApplicationService(
        validator,
        coordinator,
        downstreamGateway,
        refundOrderRepository,
        routeRepository,
        requestLogRepository,
        exceptionEventRepository,
        governanceGuard,
        Clock.fixed(Instant.parse("2026-04-23T02:00:00Z"), ZoneOffset.UTC)
    );
  }

  @Test
  void shouldPersistRefundOrderWhenRefundIsAccepted() {
    RefundCreateResponse response = service.create(new RefundCreateRequest(
        "MCH100001",
        "REQ-REFUND-20260423-0001",
        "GP123456",
        "RIDEMP-0001",
        new BigDecimal("10.50"),
        "CNY",
        Instant.parse("2026-04-23T02:00:00Z"),
        "nonce-refund-001",
        "demo-signature"
    ));

    assertThat(response.status()).isEqualTo("ACCEPTED");
    verify(refundOrderRepository).save(any());
  }
}
