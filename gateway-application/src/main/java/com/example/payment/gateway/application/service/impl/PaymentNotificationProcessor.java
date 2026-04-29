package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRecord;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationProcessor {

  private final PaymentMessageConsumeRepository paymentMessageConsumeRepository;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final GatewayNotificationProperties properties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public PaymentNotificationProcessor(
      PaymentMessageConsumeRepository paymentMessageConsumeRepository,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      GatewayNotificationProperties properties,
      ObjectMapper objectMapper,
      Clock clock
  ) {
    this.paymentMessageConsumeRepository = paymentMessageConsumeRepository;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public ProcessingResult process(String messageKey, String payloadJson) {
    Instant start = Instant.now(clock);
    String traceId = UUID.randomUUID().toString().replace("-", "");
    String merchantId = null;
    try {
      JsonNode root = objectMapper.readTree(payloadJson);
      merchantId = text(root, "merchantId");
      String gatewayPaymentId = text(root, "gatewayPaymentId");
      String status = text(root, "status");
      if (gatewayPaymentId == null || status == null) {
        throw new IllegalArgumentException("Notification payload must contain gatewayPaymentId and status");
      }

      PaymentMessageConsumeRecord record = paymentMessageConsumeRepository.findByMessageKey(messageKey)
          .orElse(new PaymentMessageConsumeRecord(
              null,
              messageKey,
              PaymentBizTypes.PAY,
              properties.getConsumerGroup(),
              payloadJson,
              "PENDING",
              0,
              false,
              null
          )).success();
      paymentMessageConsumeRepository.save(record);
      saveRequestLog(traceId, merchantId, messageKey, payloadJson, start, null, true);
      return new ProcessingResult(messageKey, true, false, null);
    } catch (Exception exception) {
      PaymentMessageConsumeRecord existing = paymentMessageConsumeRepository.findByMessageKey(messageKey)
          .orElse(new PaymentMessageConsumeRecord(
              null,
              messageKey,
              PaymentBizTypes.PAY,
              properties.getConsumerGroup(),
              payloadJson,
              "PENDING",
              0,
              false,
              null
          ));
      int nextRetryCount = existing.retryCount() + 1;
      boolean deadLetter = nextRetryCount >= properties.getMaxConsumeRetries();
      paymentMessageConsumeRepository.save(existing.failure(nextRetryCount, deadLetter, exception.getMessage()));
      saveExceptionEvent(traceId, merchantId, messageKey, exception.getMessage(), deadLetter);
      saveRequestLog(traceId, merchantId, messageKey, payloadJson, start, exception, false);
      return new ProcessingResult(messageKey, false, deadLetter, exception.getMessage());
    }
  }

  private void saveRequestLog(
      String traceId,
      String merchantId,
      String messageKey,
      String payloadJson,
      Instant start,
      Exception exception,
      boolean success
  ) {
    Instant finish = Instant.now(clock);
    paymentRequestLogRepository.save(new PaymentRequestLogRecord(
        traceId,
        "CONSUME-" + messageKey,
        null,
        merchantId,
        null,
        PaymentBizTypes.PAY,
        PaymentBizTypes.CONSUME,
        "MQ",
        "rocketmq://gateway-payment-events",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        null,
        properties.getConsumerGroup(),
        success ? GatewayResponseCodes.SUCCESS : GatewayResponseCodes.MESSAGE_CONSUME_FAILED,
        success ? "SUCCESS" : "FAIL",
        success ? null : GatewayResponseCodes.MESSAGE_CONSUME_FAILED,
        success ? null : exception.getMessage(),
        payloadJson,
        success ? "{\"deadLetter\":false}" : null
    ));
  }

  private void saveExceptionEvent(String traceId, String merchantId, String messageKey, String errorMessage, boolean deadLetter) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        "CONSUME-" + messageKey,
        merchantId,
        PaymentBizTypes.PAY,
        PaymentBizTypes.CONSUME,
        deadLetter ? "MESSAGE_DEAD_LETTERED" : GatewayResponseCodes.MESSAGE_CONSUME_FAILED,
        deadLetter ? "ERROR" : "WARN",
        GatewayResponseCodes.MESSAGE_CONSUME_FAILED,
        errorMessage,
        "{\"messageKey\":\"" + messageKey + "\",\"deadLetter\":" + deadLetter + "}"
    ));
  }

  private static String text(JsonNode root, String fieldName) {
    JsonNode node = root.get(fieldName);
    return node == null || node.isNull() ? null : node.asText();
  }

  public record ProcessingResult(
      String messageKey,
      boolean success,
      boolean deadLetter,
      String errorMessage
  ) {
  }
}
