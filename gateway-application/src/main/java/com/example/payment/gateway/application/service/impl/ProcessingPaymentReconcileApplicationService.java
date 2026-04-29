package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentReconcileResponse;
import com.example.payment.gateway.application.service.PaymentReconcileApplicationService;
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
import com.example.payment.gateway.domain.model.PaymentStatusTransitions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessingPaymentReconcileApplicationService implements PaymentReconcileApplicationService {

  private final PaymentOrderRepository paymentOrderRepository;
  private final GatewayRouteRepository gatewayRouteRepository;
  private final DownstreamPaymentQueryGateway downstreamPaymentQueryGateway;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final GatewayReconcileProperties properties;
  private final Clock clock;

  public ProcessingPaymentReconcileApplicationService(
      PaymentOrderRepository paymentOrderRepository,
      GatewayRouteRepository gatewayRouteRepository,
      DownstreamPaymentQueryGateway downstreamPaymentQueryGateway,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      GatewayReconcileProperties properties,
      Clock clock
  ) {
    this.paymentOrderRepository = paymentOrderRepository;
    this.gatewayRouteRepository = gatewayRouteRepository;
    this.downstreamPaymentQueryGateway = downstreamPaymentQueryGateway;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public PaymentReconcileResponse reconcileProcessingOrders() {
    GatewayRouteDefinition route = findQueryRoute();
    List<PaymentOrderRecord> orders = paymentOrderRepository.findByPaymentStatus("PROCESSING", properties.getProcessingLimit());
    int updatedCount = 0;
    int unchangedCount = 0;
    int failedCount = 0;
    List<String> updatedGatewayPaymentIds = new ArrayList<>();
    for (PaymentOrderRecord order : orders) {
      ReconcileResult result = reconcileSingleOrder(order, route);
      if (result.updated()) {
        updatedCount++;
        updatedGatewayPaymentIds.add(order.gatewayPaymentId());
      } else if (result.failed()) {
        failedCount++;
      } else {
        unchangedCount++;
      }
    }
    return new PaymentReconcileResponse(
        orders.size(),
        updatedCount,
        unchangedCount,
        failedCount,
        updatedGatewayPaymentIds
    );
  }

  @Transactional
  protected ReconcileResult reconcileSingleOrder(PaymentOrderRecord order, GatewayRouteDefinition route) {
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    String requestId = reconcileRequestId(order.gatewayPaymentId());
    try {
      DownstreamPaymentQueryResult downstreamResult = downstreamPaymentQueryGateway.query(
          route,
          new DownstreamPaymentQueryRequest(
              order.merchantId(),
              order.requestId(),
              order.gatewayPaymentId(),
              order.downstreamPaymentId(),
              order.paymentStatus(),
              start
          )
      );
      String currentStatus = PaymentStatusTransitions.normalize(order.paymentStatus());
      String targetStatus = PaymentStatusTransitions.normalize(downstreamResult.status());
      if (!PaymentStatusTransitions.isTransitionAllowed(currentStatus, targetStatus)) {
        throw new GatewayException(
            GatewayResponseCodes.PAYMENT_STATUS_CONFLICT,
            409,
            "Illegal payment status transition from " + currentStatus + " to " + targetStatus
        );
      }
      boolean updated = !currentStatus.equals(targetStatus);
      if (updated) {
        paymentOrderRepository.save(order.withPaymentStatus(targetStatus));
      }
      persistRequestLog(traceId, requestId, start, route, order, targetStatus, null);
      return updated ? ReconcileResult.UPDATED : ReconcileResult.UNCHANGED;
    } catch (GatewayException exception) {
      persistExceptionEvent(traceId, requestId, order, exception);
      persistRequestLog(traceId, requestId, start, route, order, null, exception);
      return ReconcileResult.FAILED;
    }
  }

  private GatewayRouteDefinition findQueryRoute() {
    return gatewayRouteRepository.findRoute(PaymentBizTypes.PAY, PaymentBizTypes.QUERY)
        .orElseThrow(() -> new GatewayException(
            GatewayResponseCodes.ROUTE_NOT_FOUND,
            503,
            "No route is configured for payment query reconcile"
        ));
  }

  private void persistRequestLog(
      String traceId,
      String requestId,
      Instant start,
      GatewayRouteDefinition route,
      PaymentOrderRecord order,
      String status,
      GatewayException exception
  ) {
    Instant finish = Instant.now(clock);
    paymentRequestLogRepository.save(new PaymentRequestLogRecord(
        traceId,
        requestId,
        order.idempotencyKey(),
        order.merchantId(),
        null,
        PaymentBizTypes.PAY,
        PaymentBizTypes.RECONCILE,
        "JOB",
        "/internal/reconcile/payments/processing",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        route.routeCode(),
        route.targetService(),
        exception == null ? GatewayResponseCodes.SUCCESS : exception.code(),
        exception == null ? "SUCCESS" : "FAIL",
        exception == null ? null : exception.code(),
        exception == null ? null : exception.getMessage(),
        "{\"gatewayPaymentId\":\"" + order.gatewayPaymentId() + "\",\"paymentStatus\":\"" + order.paymentStatus() + "\"}",
        status == null ? null : "{\"status\":\"" + status + "\"}"
    ));
  }

  private void persistExceptionEvent(
      String traceId,
      String requestId,
      PaymentOrderRecord order,
      GatewayException exception
  ) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        requestId,
        order.merchantId(),
        PaymentBizTypes.PAY,
        PaymentBizTypes.RECONCILE,
        exception.code(),
        exception.statusCode() >= 500 ? "ERROR" : "WARN",
        exception.code(),
        exception.getMessage(),
        "{\"gatewayPaymentId\":\"" + order.gatewayPaymentId() + "\"}"
    ));
  }

  private static String reconcileRequestId(String gatewayPaymentId) {
    String requestId = "RECON-" + gatewayPaymentId;
    return requestId.length() <= 64 ? requestId : requestId.substring(0, 64);
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private enum ReconcileResult {
    UPDATED,
    UNCHANGED,
    FAILED;

    boolean updated() {
      return this == UPDATED;
    }

    boolean failed() {
      return this == FAILED;
    }
  }
}
