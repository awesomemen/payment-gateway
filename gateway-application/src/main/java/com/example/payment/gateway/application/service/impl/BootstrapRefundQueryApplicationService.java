package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.RefundQueryRequest;
import com.example.payment.gateway.api.payment.RefundQueryResponse;
import com.example.payment.gateway.application.service.RefundQueryApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryResult;
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
import com.example.payment.gateway.domain.model.PaymentStatusTransitions;
import com.example.payment.gateway.domain.model.RefundQueryCommand;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapRefundQueryApplicationService implements RefundQueryApplicationService {

  private final PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private final RefundOrderRepository refundOrderRepository;
  private final GatewayRouteRepository gatewayRouteRepository;
  private final DownstreamRefundQueryGateway downstreamRefundQueryGateway;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final Clock clock;

  public BootstrapRefundQueryApplicationService(
      PaymentRequestSecurityValidator paymentRequestSecurityValidator,
      RefundOrderRepository refundOrderRepository,
      GatewayRouteRepository gatewayRouteRepository,
      DownstreamRefundQueryGateway downstreamRefundQueryGateway,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      Clock clock
  ) {
    this.paymentRequestSecurityValidator = paymentRequestSecurityValidator;
    this.refundOrderRepository = refundOrderRepository;
    this.gatewayRouteRepository = gatewayRouteRepository;
    this.downstreamRefundQueryGateway = downstreamRefundQueryGateway;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.clock = clock;
  }

  @Override
  public RefundQueryResponse query(RefundQueryRequest request) {
    RefundQueryCommand command = new RefundQueryCommand(
        request.merchantId(),
        request.requestId(),
        request.gatewayRefundId(),
        request.requestTime(),
        request.nonce(),
        request.signature()
    );
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    GatewayRouteDefinition route = null;
    try {
      paymentRequestSecurityValidator.validate(command, false);
      route = gatewayRouteRepository.findRoute(PaymentBizTypes.REFUND, PaymentBizTypes.QUERY)
          .orElseThrow(() -> new GatewayException(GatewayResponseCodes.ROUTE_NOT_FOUND, 503, "No route is configured for refund query"));
      RefundQueryResponse response = queryOrder(command, route);
      saveRequestLog(command, traceId, start, route, response, null);
      return response;
    } catch (GatewayException exception) {
      saveExceptionEvent(command, traceId, exception);
      saveRequestLog(command, traceId, start, route, null, exception);
      throw exception;
    }
  }

  @Transactional
  protected RefundQueryResponse queryOrder(RefundQueryCommand command, GatewayRouteDefinition route) {
    RefundOrderRecord order = refundOrderRepository.findByGatewayRefundId(command.gatewayRefundId())
        .filter(record -> command.merchantId().equals(record.merchantId()))
        .orElseThrow(() -> new GatewayException(GatewayResponseCodes.REFUND_ORDER_NOT_FOUND, 404, "Refund order not found"));
    String currentStatus = PaymentStatusTransitions.normalize(order.refundStatus());
    if (PaymentStatusTransitions.isFinal(currentStatus)) {
      return new RefundQueryResponse(order.gatewayRefundId(), order.downstreamRefundId(), currentStatus, route.routeCode(), "Refund status loaded from local order");
    }
    DownstreamRefundQueryResult result = downstreamRefundQueryGateway.query(
        route,
        new DownstreamRefundQueryRequest(
            order.merchantId(),
            order.requestId(),
            order.gatewayRefundId(),
            order.downstreamRefundId(),
            currentStatus,
            command.requestTime()
        )
    );
    String nextStatus = PaymentStatusTransitions.normalize(result.status());
    if (!PaymentStatusTransitions.isTransitionAllowed(currentStatus, nextStatus)) {
      throw new GatewayException(GatewayResponseCodes.REFUND_STATUS_CONFLICT, 409, "Illegal refund status transition from " + currentStatus + " to " + nextStatus);
    }
    refundOrderRepository.save(order.withRefundStatus(nextStatus));
    return new RefundQueryResponse(order.gatewayRefundId(), order.downstreamRefundId(), nextStatus, route.routeCode(), result.message());
  }

  private void saveRequestLog(
      RefundQueryCommand command,
      String traceId,
      Instant start,
      GatewayRouteDefinition route,
      RefundQueryResponse response,
      GatewayException exception
  ) {
    Instant finish = Instant.now(clock);
    paymentRequestLogRepository.save(new PaymentRequestLogRecord(
        traceId,
        command.requestId(),
        null,
        command.merchantId(),
        null,
        PaymentBizTypes.REFUND,
        PaymentBizTypes.QUERY,
        "POST",
        "/api/v1/refunds/query",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        route == null ? null : route.routeCode(),
        route == null ? null : route.targetService(),
        exception == null ? GatewayResponseCodes.SUCCESS : exception.code(),
        exception == null ? "SUCCESS" : "FAIL",
        exception == null ? null : exception.code(),
        exception == null ? null : exception.getMessage(),
        "{\"gatewayRefundId\":\"" + command.gatewayRefundId() + "\"}",
        response == null ? null : "{\"status\":\"" + response.status() + "\"}"
    ));
  }

  private void saveExceptionEvent(RefundQueryCommand command, String traceId, GatewayException exception) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        command.requestId(),
        command.merchantId(),
        PaymentBizTypes.REFUND,
        PaymentBizTypes.QUERY,
        exception.code(),
        exception.statusCode() >= 500 ? "ERROR" : "WARN",
        exception.code(),
        exception.getMessage(),
        "{\"gatewayRefundId\":\"" + command.gatewayRefundId() + "\"}"
    ));
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
