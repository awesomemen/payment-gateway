package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.RefundCreateRequest;
import com.example.payment.gateway.api.payment.RefundCreateResponse;
import com.example.payment.gateway.application.service.RefundCreateApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.payment.RefundOrderRecord;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.Money;
import com.example.payment.gateway.domain.model.RefundCreateCommand;
import com.example.payment.gateway.governance.payment.PaymentGovernanceGuard;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapRefundCreateApplicationService implements RefundCreateApplicationService {

  private final PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private final RefundCreateIdempotencyCoordinator refundCreateIdempotencyCoordinator;
  private final DownstreamRefundCreateGateway downstreamRefundCreateGateway;
  private final RefundOrderRepository refundOrderRepository;
  private final GatewayRouteRepository gatewayRouteRepository;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final PaymentGovernanceGuard paymentGovernanceGuard;
  private final Clock clock;

  public BootstrapRefundCreateApplicationService(
      PaymentRequestSecurityValidator paymentRequestSecurityValidator,
      RefundCreateIdempotencyCoordinator refundCreateIdempotencyCoordinator,
      DownstreamRefundCreateGateway downstreamRefundCreateGateway,
      RefundOrderRepository refundOrderRepository,
      GatewayRouteRepository gatewayRouteRepository,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      PaymentGovernanceGuard paymentGovernanceGuard,
      Clock clock
  ) {
    this.paymentRequestSecurityValidator = paymentRequestSecurityValidator;
    this.refundCreateIdempotencyCoordinator = refundCreateIdempotencyCoordinator;
    this.downstreamRefundCreateGateway = downstreamRefundCreateGateway;
    this.refundOrderRepository = refundOrderRepository;
    this.gatewayRouteRepository = gatewayRouteRepository;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.paymentGovernanceGuard = paymentGovernanceGuard;
    this.clock = clock;
  }

  @Override
  public RefundCreateResponse create(RefundCreateRequest request) {
    RefundCreateCommand command = new RefundCreateCommand(
        request.merchantId(),
        request.requestId(),
        request.gatewayPaymentId(),
        request.idempotencyKey(),
        new Money(request.amount(), request.currency()),
        request.requestTime(),
        request.nonce(),
        request.signature()
    );
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    try {
      paymentRequestSecurityValidator.validate(command);
      paymentGovernanceGuard.guardPaymentCreate(command.merchantId());
      RefundCreateResponse response = refundCreateIdempotencyCoordinator.execute(command, () -> createRouted(command));
      saveRequestLog(command, traceId, start, response, null);
      return response;
    } catch (GatewayException exception) {
      saveExceptionEvent(command, traceId, exception);
      saveRequestLog(command, traceId, start, null, exception);
      throw exception;
    }
  }

  @Transactional
  protected RefundCreateResponse createRouted(RefundCreateCommand command) {
    GatewayRouteDefinition route = gatewayRouteRepository.findRoute(PaymentBizTypes.REFUND, PaymentBizTypes.CREATE)
        .orElseThrow(() -> new GatewayException(GatewayResponseCodes.ROUTE_NOT_FOUND, 503, "No route is configured for refund create"));
    String gatewayRefundId = generateGatewayRefundId(command);
    DownstreamRefundCreateResult downstreamResult = downstreamRefundCreateGateway.create(
        route,
        gatewayRefundId,
        new DownstreamRefundCreateRequest(
            command.merchantId(),
            command.requestId(),
            command.gatewayPaymentId(),
            command.idempotencyKey(),
            command.amount().amount().stripTrailingZeros().toPlainString(),
            command.amount().currency(),
            command.requestTime()
        )
    );
    refundOrderRepository.save(new RefundOrderRecord(
        gatewayRefundId,
        command.merchantId(),
        command.requestId(),
        command.gatewayPaymentId(),
        command.idempotencyKey(),
        route.routeCode(),
        route.targetService(),
        downstreamResult.downstreamRefundId(),
        downstreamResult.status(),
        command.amount().amount().stripTrailingZeros().toPlainString(),
        command.amount().currency()
    ));
    return new RefundCreateResponse(
        gatewayRefundId,
        downstreamResult.downstreamRefundId(),
        downstreamResult.status(),
        route.routeCode(),
        downstreamResult.message()
    );
  }

  private void saveRequestLog(
      RefundCreateCommand command,
      String traceId,
      Instant start,
      RefundCreateResponse response,
      GatewayException exception
  ) {
    Instant finish = Instant.now(clock);
    GatewayRouteDefinition route = gatewayRouteRepository.findRoute(PaymentBizTypes.REFUND, PaymentBizTypes.CREATE).orElse(null);
    paymentRequestLogRepository.save(new PaymentRequestLogRecord(
        traceId,
        command.requestId(),
        command.idempotencyKey(),
        command.merchantId(),
        null,
        PaymentBizTypes.REFUND,
        PaymentBizTypes.CREATE,
        "POST",
        "/api/v1/refunds",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        route == null ? null : route.routeCode(),
        route == null ? null : route.targetService(),
        exception == null ? GatewayResponseCodes.SUCCESS : exception.code(),
        exception == null ? "SUCCESS" : "FAIL",
        exception == null ? null : exception.code(),
        exception == null ? null : exception.getMessage(),
        "{\"gatewayPaymentId\":\"" + command.gatewayPaymentId() + "\"}",
        response == null ? null : "{\"status\":\"" + response.status() + "\"}"
    ));
  }

  private void saveExceptionEvent(RefundCreateCommand command, String traceId, GatewayException exception) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        command.requestId(),
        command.merchantId(),
        PaymentBizTypes.REFUND,
        PaymentBizTypes.CREATE,
        exception.code(),
        exception.statusCode() >= 500 ? "ERROR" : "WARN",
        exception.code(),
        exception.getMessage(),
        "{\"gatewayPaymentId\":\"" + command.gatewayPaymentId() + "\"}"
    ));
  }

  private String generateGatewayRefundId(RefundCreateCommand command) {
    return "GR" + Instant.now(clock).toEpochMilli() + Math.abs(command.requestId().hashCode() % 100000);
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
