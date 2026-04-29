package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentQueryRequest;
import com.example.payment.gateway.api.payment.PaymentQueryResponse;
import com.example.payment.gateway.application.service.PaymentQueryApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.PaymentQueryCommand;
import com.example.payment.gateway.domain.model.PaymentStatusTransitions;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapPaymentQueryApplicationService implements PaymentQueryApplicationService {

  private final PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private final PaymentOrderRepository paymentOrderRepository;
  private final GatewayRouteRepository gatewayRouteRepository;
  private final DownstreamPaymentQueryGateway downstreamPaymentQueryGateway;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final Clock clock;

  public BootstrapPaymentQueryApplicationService(
      PaymentRequestSecurityValidator paymentRequestSecurityValidator,
      PaymentOrderRepository paymentOrderRepository,
      GatewayRouteRepository gatewayRouteRepository,
      DownstreamPaymentQueryGateway downstreamPaymentQueryGateway,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      Clock clock
  ) {
    this.paymentRequestSecurityValidator = paymentRequestSecurityValidator;
    this.paymentOrderRepository = paymentOrderRepository;
    this.gatewayRouteRepository = gatewayRouteRepository;
    this.downstreamPaymentQueryGateway = downstreamPaymentQueryGateway;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.clock = clock;
  }

  @Override
  public PaymentQueryResponse query(PaymentQueryRequest request) {
    PaymentQueryCommand command = new PaymentQueryCommand(
        request.merchantId(),
        request.requestId(),
        request.gatewayPaymentId(),
        request.requestTime(),
        request.nonce(),
        request.signature()
    );
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    GatewayRouteDefinition route = null;
    try {
      paymentRequestSecurityValidator.validate(command, false);
      route = findQueryRoute();
      PaymentOrderRecord order = findOrder(command.gatewayPaymentId(), command.merchantId());
      PaymentQueryResponse response = queryOrder(command, order, route);
      persistRequestLog(command, traceId, start, route, null, null);
      return response;
    } catch (GatewayException exception) {
      persistExceptionEvent(command, traceId, exception);
      persistRequestLog(command, traceId, start, route, null, exception);
      throw exception;
    }
  }

  @Transactional
  protected PaymentQueryResponse queryOrder(
      PaymentQueryCommand command,
      PaymentOrderRecord order,
      GatewayRouteDefinition route
  ) {
    String currentStatus = PaymentStatusTransitions.normalize(order.paymentStatus());
    if (PaymentStatusTransitions.isFinal(currentStatus)) {
      return new PaymentQueryResponse(
          order.gatewayPaymentId(),
          order.downstreamPaymentId(),
          currentStatus,
          route.routeCode(),
          "Payment status loaded from local order"
      );
    }

    if (!"DUBBO".equalsIgnoreCase(route.targetProtocol())) {
      throw new GatewayException(
          GatewayResponseCodes.UNSUPPORTED_ROUTE_PROTOCOL,
          503,
          "Unsupported route protocol: " + route.targetProtocol()
      );
    }

    DownstreamPaymentQueryResult downstreamResult = downstreamPaymentQueryGateway.query(
        route,
        new DownstreamPaymentQueryRequest(
            order.merchantId(),
            order.requestId(),
            order.gatewayPaymentId(),
            order.downstreamPaymentId(),
            currentStatus,
            command.requestTime()
        )
    );
    String nextStatus = PaymentStatusTransitions.normalize(downstreamResult.status());
    if (!PaymentStatusTransitions.isTransitionAllowed(currentStatus, nextStatus)) {
      throw new GatewayException(
          GatewayResponseCodes.PAYMENT_STATUS_CONFLICT,
          409,
          "Illegal payment status transition from " + currentStatus + " to " + nextStatus
      );
    }
    PaymentOrderRecord updated = order.withPaymentStatus(nextStatus);
    paymentOrderRepository.save(updated);
    return new PaymentQueryResponse(
        updated.gatewayPaymentId(),
        updated.downstreamPaymentId(),
        nextStatus,
        route.routeCode(),
        downstreamResult.message()
    );
  }

  private PaymentOrderRecord findOrder(String gatewayPaymentId, String merchantId) {
    return paymentOrderRepository.findByGatewayPaymentId(gatewayPaymentId)
        .filter(record -> merchantId.equals(record.merchantId()))
        .orElseThrow(() -> new GatewayException(
            GatewayResponseCodes.PAYMENT_ORDER_NOT_FOUND,
            404,
            "Payment order not found"
        ));
  }

  private GatewayRouteDefinition findQueryRoute() {
    return gatewayRouteRepository.findRoute(PaymentBizTypes.PAY, PaymentBizTypes.QUERY)
        .orElseThrow(() -> new GatewayException(
            GatewayResponseCodes.ROUTE_NOT_FOUND,
            503,
            "No route is configured for payment query"
        ));
  }

  private void persistRequestLog(
      PaymentQueryCommand command,
      String traceId,
      Instant start,
      GatewayRouteDefinition route,
      PaymentQueryResponse response,
      GatewayException exception
  ) {
    Instant finish = Instant.now(clock);
    paymentRequestLogRepository.save(new PaymentRequestLogRecord(
        traceId,
        command.requestId(),
        null,
        command.merchantId(),
        null,
        PaymentBizTypes.PAY,
        PaymentBizTypes.QUERY,
        "POST",
        "/api/v1/payments/query",
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

  private void persistExceptionEvent(PaymentQueryCommand command, String traceId, GatewayException exception) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        command.requestId(),
        command.merchantId(),
        PaymentBizTypes.PAY,
        PaymentBizTypes.QUERY,
        exception.code(),
        exception.statusCode() >= 500 ? "ERROR" : "WARN",
        exception.code(),
        exception.getMessage(),
        "{\"gatewayPaymentId\":\"" + command.gatewayPaymentId() + "\"}"
    ));
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
