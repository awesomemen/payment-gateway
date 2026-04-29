package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentCreateRequest;
import com.example.payment.gateway.api.payment.PaymentCreateResponse;
import com.example.payment.gateway.application.service.PaymentCreateApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentAcceptedOutboxEvent;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.common.payment.PaymentOutboxPublisher;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.Money;
import com.example.payment.gateway.domain.model.PaymentCreateCommand;
import com.example.payment.gateway.governance.payment.PaymentGovernanceGuard;
import com.example.payment.gateway.observability.payment.PaymentAuditMetricsRecorder;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class BootstrapPaymentCreateApplicationService implements PaymentCreateApplicationService {

  private final PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private final PaymentCreateIdempotencyCoordinator paymentCreateIdempotencyCoordinator;
  private final DownstreamPaymentCreateGateway downstreamPaymentCreateGateway;
  private final PaymentOrderRepository paymentOrderRepository;
  private final GatewayRouteRepository gatewayRouteRepository;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final PaymentGovernanceGuard paymentGovernanceGuard;
  private final PaymentAuditMetricsRecorder paymentAuditMetricsRecorder;
  private final PaymentOutboxPublisher paymentOutboxPublisher;
  private final Clock clock;

  public BootstrapPaymentCreateApplicationService(
      PaymentRequestSecurityValidator paymentRequestSecurityValidator,
      PaymentCreateIdempotencyCoordinator paymentCreateIdempotencyCoordinator,
      DownstreamPaymentCreateGateway downstreamPaymentCreateGateway,
      PaymentOrderRepository paymentOrderRepository,
      GatewayRouteRepository gatewayRouteRepository,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      PaymentGovernanceGuard paymentGovernanceGuard,
      PaymentAuditMetricsRecorder paymentAuditMetricsRecorder,
      PaymentOutboxPublisher paymentOutboxPublisher,
      Clock clock
  ) {
    this.paymentRequestSecurityValidator = paymentRequestSecurityValidator;
    this.paymentCreateIdempotencyCoordinator = paymentCreateIdempotencyCoordinator;
    this.downstreamPaymentCreateGateway = downstreamPaymentCreateGateway;
    this.paymentOrderRepository = paymentOrderRepository;
    this.gatewayRouteRepository = gatewayRouteRepository;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.paymentGovernanceGuard = paymentGovernanceGuard;
    this.paymentAuditMetricsRecorder = paymentAuditMetricsRecorder;
    this.paymentOutboxPublisher = paymentOutboxPublisher;
    this.clock = clock;
  }

  @Override
  public PaymentCreateResponse create(PaymentCreateRequest request) {
    PaymentCreateCommand command = toCommand(request);
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    try {
      paymentRequestSecurityValidator.validate(command);
      paymentGovernanceGuard.guardPaymentCreate(command.merchantId());
      PaymentCreateResponse response = paymentCreateIdempotencyCoordinator.execute(command, () -> createRoutedResponse(command));
      paymentAuditMetricsRecorder.recordSuccess();
      persistRequestLog(command, traceId, start, response, null);
      return response;
    } catch (GatewayException exception) {
      paymentAuditMetricsRecorder.recordFailure();
      persistExceptionEvent(command, traceId, exception);
      persistRequestLog(command, traceId, start, null, exception);
      throw exception;
    }
  }

  @Transactional
  protected PaymentCreateResponse createRoutedResponse(PaymentCreateCommand command) {
    GatewayRouteDefinition route = gatewayRouteRepository.findRoute(PaymentBizTypes.PAY, PaymentBizTypes.CREATE)
        .orElseThrow(() -> new GatewayException(
            GatewayResponseCodes.ROUTE_NOT_FOUND,
            503,
            "No route is configured for payment create"
        ));

    if (!"DUBBO".equalsIgnoreCase(route.targetProtocol())) {
      throw new GatewayException(
          GatewayResponseCodes.UNSUPPORTED_ROUTE_PROTOCOL,
          503,
          "Unsupported route protocol: " + route.targetProtocol()
      );
    }

    String gatewayPaymentId = generateGatewayPaymentId(command);
    DownstreamPaymentCreateResult downstreamResult = downstreamPaymentCreateGateway.create(
        route,
        gatewayPaymentId,
        new DownstreamPaymentCreateRequest(
            command.merchantId(),
            command.requestId(),
            command.idempotencyKey(),
            command.amount().amount().stripTrailingZeros().toPlainString(),
            command.amount().currency(),
            command.requestTime()
        )
    );
    PaymentCreateResponse response = new PaymentCreateResponse(
        gatewayPaymentId,
        downstreamResult.status(),
        route.routeCode(),
        downstreamResult.message()
    );
    paymentOrderRepository.save(new PaymentOrderRecord(
        gatewayPaymentId,
        command.merchantId(),
        command.requestId(),
        command.idempotencyKey(),
        route.routeCode(),
        route.targetService(),
        downstreamResult.downstreamPaymentId(),
        downstreamResult.status(),
        command.amount().amount().stripTrailingZeros().toPlainString(),
        command.amount().currency()
    ));
    paymentOutboxPublisher.publishPaymentAccepted(new PaymentAcceptedOutboxEvent(
        command.merchantId(),
        command.requestId(),
        command.idempotencyKey(),
        response.gatewayPaymentId(),
        downstreamResult.downstreamPaymentId(),
        response.status(),
        route.routeCode(),
        route.targetService(),
        command.amount().amount().stripTrailingZeros().toPlainString(),
        command.amount().currency()
    ));
    return response;
  }

  private PaymentCreateCommand toCommand(PaymentCreateRequest request) {
    return new PaymentCreateCommand(
        request.merchantId(),
        request.requestId(),
        request.idempotencyKey(),
        new Money(request.amount(), request.currency()),
        request.requestTime(),
        request.nonce(),
        request.signature()
    );
  }

  private void persistRequestLog(
      PaymentCreateCommand command,
      String traceId,
      Instant start,
      PaymentCreateResponse response,
      GatewayException exception
  ) {
    Instant finish = Instant.now(clock);
    GatewayRouteDefinition route = resolveRouteForLogging(response, exception);
    paymentRequestLogRepository.save(new PaymentRequestLogRecord(
        traceId,
        command.requestId(),
        command.idempotencyKey(),
        command.merchantId(),
        null,
        PaymentBizTypes.PAY,
        PaymentBizTypes.CREATE,
        "POST",
        "/api/v1/payments",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        response == null ? (route == null ? null : route.routeCode()) : response.routeCode(),
        route == null ? null : route.targetService(),
        exception == null ? GatewayResponseCodes.SUCCESS : exception.code(),
        exception == null ? "SUCCESS" : "FAIL",
        exception == null ? null : exception.code(),
        exception == null ? null : exception.getMessage(),
        "{\"amount\":\"" + command.amount().amount().stripTrailingZeros().toPlainString() + "\",\"currency\":\"" + command.amount().currency() + "\"}",
        null
    ));
  }

  private GatewayRouteDefinition resolveRouteForLogging(
      PaymentCreateResponse response,
      GatewayException exception
  ) {
    if (response != null) {
      return gatewayRouteRepository.findRoute(PaymentBizTypes.PAY, PaymentBizTypes.CREATE).orElse(null);
    }
    if (exception == null || !isPostRoutingFailure(exception.code())) {
      return null;
    }
    return gatewayRouteRepository.findRoute(PaymentBizTypes.PAY, PaymentBizTypes.CREATE).orElse(null);
  }

  private static boolean isPostRoutingFailure(String code) {
    return GatewayResponseCodes.UNSUPPORTED_ROUTE_PROTOCOL.equals(code)
        || GatewayResponseCodes.DOWNSTREAM_TIMEOUT.equals(code)
        || GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR.equals(code)
        || GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE.equals(code)
        || GatewayResponseCodes.DOWNSTREAM_REJECTED.equals(code)
        || GatewayResponseCodes.DOWNSTREAM_FAILED.equals(code);
  }

  private void persistExceptionEvent(PaymentCreateCommand command, String traceId, GatewayException exception) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        command.requestId(),
        command.merchantId(),
        PaymentBizTypes.PAY,
        PaymentBizTypes.CREATE,
        exception.code(),
        exception.statusCode() >= 500 ? "ERROR" : "WARN",
        exception.code(),
        exception.getMessage(),
        "{\"idempotencyKey\":\"" + command.idempotencyKey() + "\"}"
    ));
  }

  private String generateGatewayPaymentId(PaymentCreateCommand command) {
    return "GP" + Instant.now(clock).toEpochMilli() + Math.abs(command.requestId().hashCode() % 100000);
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
