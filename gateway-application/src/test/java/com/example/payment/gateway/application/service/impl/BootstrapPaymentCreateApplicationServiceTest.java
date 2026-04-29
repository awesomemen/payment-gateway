package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentCreateRequest;
import com.example.payment.gateway.api.payment.PaymentCreateResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
import com.example.payment.gateway.common.payment.PaymentAcceptedOutboxEvent;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentOutboxPublisher;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.governance.payment.PaymentGovernanceGuard;
import com.example.payment.gateway.observability.payment.PaymentAuditMetricsRecorder;
import com.example.payment.gateway.security.DefaultPaymentRequestSecurityValidator;
import com.example.payment.gateway.security.GatewaySecurityProperties;
import com.example.payment.gateway.security.InMemoryReplayProtectionStore;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

class BootstrapPaymentCreateApplicationServiceTest {

  private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");
  private static final String MERCHANT_ID = "MCH100001";
  private static final String SIGNATURE_KEY = "demo-signature-key";

  private BootstrapPaymentCreateApplicationService service;
  private PaymentCreateIdempotencyCoordinator paymentCreateIdempotencyCoordinator;
  private PaymentRequestLogRepository paymentRequestLogRepository;
  private DownstreamPaymentCreateGateway downstreamPaymentCreateGateway;
  private PaymentOutboxPublisher paymentOutboxPublisher;
  private PaymentOrderRepository paymentOrderRepository;

  @BeforeEach
  void setUp() {
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.setRequestExpireSeconds(300);
    properties.setReplayProtectSeconds(300);
    properties.getMerchants().put(MERCHANT_ID, new GatewaySecurityProperties.MerchantProperties(true, SIGNATURE_KEY));

    DefaultPaymentRequestSecurityValidator validator = new DefaultPaymentRequestSecurityValidator(
        properties,
        new InMemoryReplayProtectionStore(Clock.fixed(NOW, ZoneOffset.UTC)),
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    paymentCreateIdempotencyCoordinator = Mockito.mock(PaymentCreateIdempotencyCoordinator.class);
    GatewayRouteRepository gatewayRouteRepository = Mockito.mock(GatewayRouteRepository.class);
    paymentRequestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    PaymentExceptionEventRepository paymentExceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    PaymentGovernanceGuard paymentGovernanceGuard = Mockito.mock(PaymentGovernanceGuard.class);
    PaymentAuditMetricsRecorder paymentAuditMetricsRecorder = Mockito.mock(PaymentAuditMetricsRecorder.class);
    paymentOutboxPublisher = Mockito.mock(PaymentOutboxPublisher.class);
    downstreamPaymentCreateGateway = Mockito.mock(DownstreamPaymentCreateGateway.class);
    paymentOrderRepository = Mockito.mock(PaymentOrderRepository.class);

    given(paymentCreateIdempotencyCoordinator.execute(any(), any()))
        .willAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
    given(gatewayRouteRepository.findRoute("PAY", "CREATE"))
        .willReturn(Optional.of(new GatewayRouteDefinition(
            "ROUTE_PAY_CREATE",
            "PAY",
            "CREATE",
            "DUBBO",
            "com.example.payment.api.PaymentCreateFacade",
            "createPayment",
            3000,
            0,
            "gateway:pay:create"
        )));
    doNothing().when(paymentGovernanceGuard).guardPaymentCreate(any());

    service = new BootstrapPaymentCreateApplicationService(
        validator,
        paymentCreateIdempotencyCoordinator,
        downstreamPaymentCreateGateway,
        paymentOrderRepository,
        gatewayRouteRepository,
        paymentRequestLogRepository,
        paymentExceptionEventRepository,
        paymentGovernanceGuard,
        paymentAuditMetricsRecorder,
        paymentOutboxPublisher,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );
  }

  @Test
  void shouldRejectInvalidSignatureBeforeBootstrapBoundary() {
    PaymentCreateRequest request = new PaymentCreateRequest(
        MERCHANT_ID,
        "REQ-20260421-0001",
        "IDEMP-20260421-0001",
        new BigDecimal("88.50"),
        "CNY",
        NOW.minusSeconds(30),
        "nonce-001",
        "invalid-signature"
    );

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.SIGNATURE_INVALID);
  }

  @Test
  void shouldInvokeDownstreamGatewayAndReturnMappedResponseAfterSecurityValidationPasses() {
    given(downstreamPaymentCreateGateway.create(any(), any(), any()))
        .willReturn(new DownstreamPaymentCreateResult(
            "DSP-20260421-0001",
            "ACCEPTED",
            "Payment request accepted by mock downstream facade"
        ));
    PaymentCreateRequest request = validRequest("nonce-001", NOW.minusSeconds(30));

    PaymentCreateResponse response = service.create(request);

    assertThat(response.status()).isEqualTo("ACCEPTED");
    assertThat(response.routeCode()).isEqualTo("ROUTE_PAY_CREATE");
    assertThat(response.message()).isEqualTo("Payment request accepted by mock downstream facade");
    verify(paymentCreateIdempotencyCoordinator).execute(Mockito.any(), Mockito.any());
    verify(downstreamPaymentCreateGateway).create(any(), any(), any());
    verify(paymentRequestLogRepository).save(any());
    ArgumentCaptor<PaymentOrderRecord> paymentOrderCaptor = ArgumentCaptor.forClass(PaymentOrderRecord.class);
    verify(paymentOrderRepository).save(paymentOrderCaptor.capture());
    Assertions.assertEquals("DSP-20260421-0001", paymentOrderCaptor.getValue().downstreamPaymentId());
    Assertions.assertEquals("ROUTE_PAY_CREATE", paymentOrderCaptor.getValue().routeCode());
    ArgumentCaptor<PaymentAcceptedOutboxEvent> captor = ArgumentCaptor.forClass(PaymentAcceptedOutboxEvent.class);
    verify(paymentOutboxPublisher).publishPaymentAccepted(captor.capture());
    Assertions.assertEquals("DSP-20260421-0001", captor.getValue().downstreamPaymentId());
  }

  @Test
  void shouldNotPersistPaymentOrderOrOutboxWhenDownstreamRejectsRequest() {
    given(downstreamPaymentCreateGateway.create(any(), any(), any()))
        .willThrow(new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_REJECTED,
            422,
            "payment rejected by downstream rule"
        ));
    PaymentCreateRequest request = validRequest("nonce-002", NOW.minusSeconds(20));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_REJECTED);

    verify(paymentCreateIdempotencyCoordinator).execute(Mockito.any(), Mockito.any());
    ArgumentCaptor<PaymentRequestLogRecord> requestLogCaptor = ArgumentCaptor.forClass(PaymentRequestLogRecord.class);
    verify(paymentRequestLogRepository).save(requestLogCaptor.capture());
    Assertions.assertEquals("ROUTE_PAY_CREATE", requestLogCaptor.getValue().routeCode());
    Assertions.assertEquals("com.example.payment.api.PaymentCreateFacade", requestLogCaptor.getValue().targetService());
    verify(paymentOrderRepository, never()).save(any());
    verify(paymentOutboxPublisher, never()).publishPaymentAccepted(any());
  }

  @Test
  void shouldPersistProcessingStatusWhenDownstreamReturnsProcessing() {
    given(downstreamPaymentCreateGateway.create(any(), any(), any()))
        .willReturn(new DownstreamPaymentCreateResult(
            "DSP-20260421-0002",
            "PROCESSING",
            "payment is processing in downstream facade"
        ));
    PaymentCreateRequest request = validRequest("nonce-003", NOW.minusSeconds(10));

    PaymentCreateResponse response = service.create(request);

    assertThat(response.status()).isEqualTo("PROCESSING");
    assertThat(response.routeCode()).isEqualTo("ROUTE_PAY_CREATE");
    assertThat(response.message()).isEqualTo("payment is processing in downstream facade");
    ArgumentCaptor<PaymentOrderRecord> paymentOrderCaptor = ArgumentCaptor.forClass(PaymentOrderRecord.class);
    verify(paymentOrderRepository).save(paymentOrderCaptor.capture());
    Assertions.assertEquals("PROCESSING", paymentOrderCaptor.getValue().paymentStatus());
    Assertions.assertEquals("DSP-20260421-0002", paymentOrderCaptor.getValue().downstreamPaymentId());
    ArgumentCaptor<PaymentAcceptedOutboxEvent> outboxCaptor = ArgumentCaptor.forClass(PaymentAcceptedOutboxEvent.class);
    verify(paymentOutboxPublisher).publishPaymentAccepted(outboxCaptor.capture());
    Assertions.assertEquals("PROCESSING", outboxCaptor.getValue().status());
  }

  @Test
  void shouldNotPersistPaymentOrderOrOutboxWhenDownstreamTimesOut() {
    given(downstreamPaymentCreateGateway.create(any(), any(), any()))
        .willThrow(new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_TIMEOUT,
            504,
            "payment create timed out in mock downstream facade"
        ));
    PaymentCreateRequest request = validRequest("nonce-004", NOW.minusSeconds(8));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_TIMEOUT);

    verify(paymentCreateIdempotencyCoordinator).execute(Mockito.any(), Mockito.any());
    ArgumentCaptor<PaymentRequestLogRecord> requestLogCaptor = ArgumentCaptor.forClass(PaymentRequestLogRecord.class);
    verify(paymentRequestLogRepository).save(requestLogCaptor.capture());
    Assertions.assertEquals("ROUTE_PAY_CREATE", requestLogCaptor.getValue().routeCode());
    Assertions.assertEquals("com.example.payment.api.PaymentCreateFacade", requestLogCaptor.getValue().targetService());
    verify(paymentOrderRepository, never()).save(any());
    verify(paymentOutboxPublisher, never()).publishPaymentAccepted(any());
  }

  @Test
  void shouldNotPersistPaymentOrderOrOutboxWhenDownstreamInvocationFails() {
    given(downstreamPaymentCreateGateway.create(any(), any(), any()))
        .willThrow(new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR,
            502,
            "payment create failed in mock downstream facade"
        ));
    PaymentCreateRequest request = validRequest("nonce-005", NOW.minusSeconds(6));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR);

    verify(paymentCreateIdempotencyCoordinator).execute(Mockito.any(), Mockito.any());
    ArgumentCaptor<PaymentRequestLogRecord> requestLogCaptor = ArgumentCaptor.forClass(PaymentRequestLogRecord.class);
    verify(paymentRequestLogRepository).save(requestLogCaptor.capture());
    Assertions.assertEquals("ROUTE_PAY_CREATE", requestLogCaptor.getValue().routeCode());
    Assertions.assertEquals("com.example.payment.api.PaymentCreateFacade", requestLogCaptor.getValue().targetService());
    verify(paymentOrderRepository, never()).save(any());
    verify(paymentOutboxPublisher, never()).publishPaymentAccepted(any());
  }

  private static PaymentCreateRequest validRequest(String nonce, Instant requestTime) {
    return new PaymentCreateRequest(
        MERCHANT_ID,
        "REQ-20260421-0001",
        "IDEMP-20260421-0001",
        new BigDecimal("88.50"),
        "CNY",
        requestTime,
        nonce,
        sign(requestTime, nonce)
    );
  }

  private static String sign(Instant requestTime, String nonce) {
    String payload = String.join("&",
        "merchantId=" + MERCHANT_ID,
        "requestId=REQ-20260421-0001",
        "idempotencyKey=IDEMP-20260421-0001",
        "amount=88.5",
        "currency=CNY",
        "requestTime=" + requestTime,
        "nonce=" + nonce
    );
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(SIGNATURE_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to sign payload for test", exception);
    }
  }
}
