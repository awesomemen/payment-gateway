package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentCallbackRequest;
import com.example.payment.gateway.api.payment.PaymentCallbackResponse;
import com.example.payment.gateway.application.service.PaymentCallbackApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.PaymentCallbackCommand;
import com.example.payment.gateway.domain.model.PaymentStatusTransitions;
import com.example.payment.gateway.security.PaymentRequestSecurityValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapPaymentCallbackApplicationService implements PaymentCallbackApplicationService {

  private final PaymentRequestSecurityValidator paymentRequestSecurityValidator;
  private final PaymentOrderRepository paymentOrderRepository;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final Clock clock;

  public BootstrapPaymentCallbackApplicationService(
      PaymentRequestSecurityValidator paymentRequestSecurityValidator,
      PaymentOrderRepository paymentOrderRepository,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      Clock clock
  ) {
    this.paymentRequestSecurityValidator = paymentRequestSecurityValidator;
    this.paymentOrderRepository = paymentOrderRepository;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.clock = clock;
  }

  @Override
  public PaymentCallbackResponse handle(PaymentCallbackRequest request) {
    PaymentCallbackCommand command = new PaymentCallbackCommand(
        request.merchantId(),
        request.requestId(),
        request.gatewayPaymentId(),
        request.downstreamPaymentId(),
        request.status(),
        request.requestTime(),
        request.nonce(),
        request.signature()
    );
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    try {
      paymentRequestSecurityValidator.validate(command);
      PaymentCallbackResponse response = applyCallback(command, request.message());
      persistRequestLog(command, traceId, start, response, null);
      return response;
    } catch (GatewayException exception) {
      persistExceptionEvent(command, traceId, exception);
      persistRequestLog(command, traceId, start, null, exception);
      throw exception;
    }
  }

  @Transactional
  protected PaymentCallbackResponse applyCallback(PaymentCallbackCommand command, String callbackMessage) {
    PaymentOrderRecord order = paymentOrderRepository.findByGatewayPaymentId(command.gatewayPaymentId())
        .filter(record -> command.merchantId().equals(record.merchantId()))
        .orElseThrow(() -> new GatewayException(
            GatewayResponseCodes.PAYMENT_ORDER_NOT_FOUND,
            404,
            "Payment order not found"
        ));
    if (!order.downstreamPaymentId().equals(command.downstreamPaymentId())) {
      throw new GatewayException(
          GatewayResponseCodes.PAYMENT_ORDER_MISMATCH,
          409,
          "Payment order downstream id does not match callback payload"
      );
    }

    String currentStatus = PaymentStatusTransitions.normalize(order.paymentStatus());
    String targetStatus = PaymentStatusTransitions.normalize(command.status());
    if (!PaymentStatusTransitions.isTransitionAllowed(currentStatus, targetStatus)) {
      throw new GatewayException(
          GatewayResponseCodes.PAYMENT_STATUS_CONFLICT,
          409,
          "Illegal payment status transition from " + currentStatus + " to " + targetStatus
      );
    }

    paymentOrderRepository.save(order.withPaymentStatus(targetStatus));
    return new PaymentCallbackResponse(
        order.gatewayPaymentId(),
        targetStatus,
        callbackMessage == null || callbackMessage.isBlank() ? "Payment callback accepted" : callbackMessage
    );
  }

  private void persistRequestLog(
      PaymentCallbackCommand command,
      String traceId,
      Instant start,
      PaymentCallbackResponse response,
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
        PaymentBizTypes.CALLBACK,
        "POST",
        "/api/v1/payments/callback",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        null,
        null,
        exception == null ? GatewayResponseCodes.SUCCESS : exception.code(),
        exception == null ? "SUCCESS" : "FAIL",
        exception == null ? null : exception.code(),
        exception == null ? null : exception.getMessage(),
        "{\"gatewayPaymentId\":\"" + command.gatewayPaymentId() + "\",\"status\":\"" + command.status() + "\"}",
        response == null ? null : "{\"status\":\"" + response.status() + "\"}"
    ));
  }

  private void persistExceptionEvent(PaymentCallbackCommand command, String traceId, GatewayException exception) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        command.requestId(),
        command.merchantId(),
        PaymentBizTypes.PAY,
        PaymentBizTypes.CALLBACK,
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
