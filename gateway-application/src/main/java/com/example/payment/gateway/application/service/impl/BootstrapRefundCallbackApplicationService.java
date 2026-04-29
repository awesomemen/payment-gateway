package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.RefundCallbackRequest;
import com.example.payment.gateway.api.payment.RefundCallbackResponse;
import com.example.payment.gateway.application.service.RefundCallbackApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.payment.RefundOrderRecord;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.PaymentStatusTransitions;
import com.example.payment.gateway.domain.model.RefundCallbackCommand;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapRefundCallbackApplicationService implements RefundCallbackApplicationService {

  private final PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private final RefundOrderRepository refundOrderRepository;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final Clock clock;

  public BootstrapRefundCallbackApplicationService(
      PaymentRequestSecurityValidator paymentRequestSecurityValidator,
      RefundOrderRepository refundOrderRepository,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      Clock clock
  ) {
    this.paymentRequestSecurityValidator = paymentRequestSecurityValidator;
    this.refundOrderRepository = refundOrderRepository;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.clock = clock;
  }

  @Override
  public RefundCallbackResponse handle(RefundCallbackRequest request) {
    RefundCallbackCommand command = new RefundCallbackCommand(
        request.merchantId(),
        request.requestId(),
        request.gatewayRefundId(),
        request.downstreamRefundId(),
        request.status(),
        request.requestTime(),
        request.nonce(),
        request.signature()
    );
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    try {
      paymentRequestSecurityValidator.validate(command);
      RefundCallbackResponse response = applyCallback(command, request.message());
      saveRequestLog(command, traceId, start, response, null);
      return response;
    } catch (GatewayException exception) {
      saveExceptionEvent(command, traceId, exception);
      saveRequestLog(command, traceId, start, null, exception);
      throw exception;
    }
  }

  @Transactional
  protected RefundCallbackResponse applyCallback(RefundCallbackCommand command, String callbackMessage) {
    RefundOrderRecord order = refundOrderRepository.findByGatewayRefundId(command.gatewayRefundId())
        .filter(record -> command.merchantId().equals(record.merchantId()))
        .orElseThrow(() -> new GatewayException(GatewayResponseCodes.REFUND_ORDER_NOT_FOUND, 404, "Refund order not found"));
    if (!order.downstreamRefundId().equals(command.downstreamRefundId())) {
      throw new GatewayException(GatewayResponseCodes.REFUND_ORDER_MISMATCH, 409, "Refund order downstream id does not match callback payload");
    }
    String currentStatus = PaymentStatusTransitions.normalize(order.refundStatus());
    String targetStatus = PaymentStatusTransitions.normalize(command.status());
    if (!PaymentStatusTransitions.isTransitionAllowed(currentStatus, targetStatus)) {
      throw new GatewayException(GatewayResponseCodes.REFUND_STATUS_CONFLICT, 409, "Illegal refund status transition from " + currentStatus + " to " + targetStatus);
    }
    refundOrderRepository.save(order.withRefundStatus(targetStatus));
    return new RefundCallbackResponse(
        order.gatewayRefundId(),
        targetStatus,
        callbackMessage == null || callbackMessage.isBlank() ? "Refund callback accepted" : callbackMessage
    );
  }

  private void saveRequestLog(
      RefundCallbackCommand command,
      String traceId,
      Instant start,
      RefundCallbackResponse response,
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
        PaymentBizTypes.CALLBACK,
        "POST",
        "/api/v1/refunds/callback",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        null,
        null,
        exception == null ? GatewayResponseCodes.SUCCESS : exception.code(),
        exception == null ? "SUCCESS" : "FAIL",
        exception == null ? null : exception.code(),
        exception == null ? null : exception.getMessage(),
        "{\"gatewayRefundId\":\"" + command.gatewayRefundId() + "\",\"status\":\"" + command.status() + "\"}",
        response == null ? null : "{\"status\":\"" + response.status() + "\"}"
    ));
  }

  private void saveExceptionEvent(RefundCallbackCommand command, String traceId, GatewayException exception) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        command.requestId(),
        command.merchantId(),
        PaymentBizTypes.REFUND,
        PaymentBizTypes.CALLBACK,
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
