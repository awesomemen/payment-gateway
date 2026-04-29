package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentReconcileResponse;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProcessingPaymentReconcileApplicationServiceTest {

  private PaymentOrderRepository paymentOrderRepository;
  private GatewayRouteRepository gatewayRouteRepository;
  private DownstreamPaymentQueryGateway downstreamPaymentQueryGateway;
  private PaymentRequestLogRepository paymentRequestLogRepository;
  private PaymentExceptionEventRepository paymentExceptionEventRepository;
  private ProcessingPaymentReconcileApplicationService service;

  @BeforeEach
  void setUp() {
    paymentOrderRepository = Mockito.mock(PaymentOrderRepository.class);
    gatewayRouteRepository = Mockito.mock(GatewayRouteRepository.class);
    downstreamPaymentQueryGateway = Mockito.mock(DownstreamPaymentQueryGateway.class);
    paymentRequestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    paymentExceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    GatewayReconcileProperties properties = new GatewayReconcileProperties();
    properties.setProcessingLimit(10);
    service = new ProcessingPaymentReconcileApplicationService(
        paymentOrderRepository,
        gatewayRouteRepository,
        downstreamPaymentQueryGateway,
        paymentRequestLogRepository,
        paymentExceptionEventRepository,
        properties,
        Clock.fixed(Instant.parse("2026-04-23T03:12:00Z"), ZoneOffset.UTC)
    );
    given(gatewayRouteRepository.findRoute("PAY", "QUERY")).willReturn(Optional.of(route()));
  }

  @Test
  void shouldUpdateProcessingOrderWhenDownstreamReturnsSucceeded() {
    PaymentOrderRecord order = order("PROCESSING", "GP-RECON-001");
    given(paymentOrderRepository.findByPaymentStatus("PROCESSING", 10)).willReturn(List.of(order));
    given(downstreamPaymentQueryGateway.query(eq(route()), any()))
        .willReturn(new DownstreamPaymentQueryResult("SUCCEEDED", "reconcile succeeded"));

    PaymentReconcileResponse response = service.reconcileProcessingOrders();

    assertThat(response.scannedCount()).isEqualTo(1);
    assertThat(response.updatedCount()).isEqualTo(1);
    assertThat(response.unchangedCount()).isEqualTo(0);
    assertThat(response.failedCount()).isEqualTo(0);
    assertThat(response.updatedGatewayPaymentIds()).containsExactly("GP-RECON-001");
    verify(paymentOrderRepository).save(order.withPaymentStatus("SUCCEEDED"));
    verify(paymentRequestLogRepository).save(any());
  }

  @Test
  void shouldKeepOrderUnchangedWhenDownstreamStillProcessing() {
    PaymentOrderRecord order = order("PROCESSING", "GP-RECON-002");
    given(paymentOrderRepository.findByPaymentStatus("PROCESSING", 10)).willReturn(List.of(order));
    given(downstreamPaymentQueryGateway.query(eq(route()), any()))
        .willReturn(new DownstreamPaymentQueryResult("PROCESSING", "still processing"));

    PaymentReconcileResponse response = service.reconcileProcessingOrders();

    assertThat(response.scannedCount()).isEqualTo(1);
    assertThat(response.updatedCount()).isEqualTo(0);
    assertThat(response.unchangedCount()).isEqualTo(1);
    assertThat(response.failedCount()).isEqualTo(0);
    verify(paymentOrderRepository, never()).save(any());
  }

  @Test
  void shouldRecordFailureWhenDownstreamQueryFails() {
    PaymentOrderRecord order = order("PROCESSING", "GP-RECON-003");
    given(paymentOrderRepository.findByPaymentStatus("PROCESSING", 10)).willReturn(List.of(order));
    given(downstreamPaymentQueryGateway.query(eq(route()), any()))
        .willThrow(new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR,
            502,
            "reconcile query failed"
        ));

    PaymentReconcileResponse response = service.reconcileProcessingOrders();

    assertThat(response.scannedCount()).isEqualTo(1);
    assertThat(response.updatedCount()).isEqualTo(0);
    assertThat(response.unchangedCount()).isEqualTo(0);
    assertThat(response.failedCount()).isEqualTo(1);
    verify(paymentExceptionEventRepository).save(any());
    verify(paymentRequestLogRepository).save(any());
  }

  private static GatewayRouteDefinition route() {
    return new GatewayRouteDefinition(
        "ROUTE_PAY_QUERY",
        "PAY",
        "QUERY",
        "DUBBO",
        "com.example.payment.api.PaymentQueryFacade",
        "queryPayment",
        3000,
        0,
        "gateway:pay:query"
    );
  }

  private static PaymentOrderRecord order(String status, String gatewayPaymentId) {
    return new PaymentOrderRecord(
        gatewayPaymentId,
        "MCH100001",
        "REQ-PROCESSING-RECON",
        "IDEMP-PROCESSING-RECON",
        "ROUTE_PAY_CREATE",
        "com.example.payment.api.PaymentCreateFacade",
        "DSP-RECON-001",
        status,
        "88.5",
        "CNY"
    );
  }
}
