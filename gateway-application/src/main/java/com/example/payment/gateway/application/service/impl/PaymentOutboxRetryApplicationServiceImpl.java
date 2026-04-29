package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentOutboxRetryResponse;
import com.example.payment.gateway.application.service.PaymentOutboxRetryApplicationService;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentMqOutboxRecord;
import com.example.payment.gateway.common.payment.PaymentMqOutboxRepository;
import com.example.payment.gateway.common.payment.PaymentOutboxRetryExecutor;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "docker"})
public class PaymentOutboxRetryApplicationServiceImpl implements PaymentOutboxRetryApplicationService {

  private final PaymentMqOutboxRepository paymentMqOutboxRepository;
  private final PaymentOutboxRetryExecutor paymentOutboxRetryExecutor;
  private final PaymentRequestLogRepository paymentRequestLogRepository;
  private final PaymentExceptionEventRepository paymentExceptionEventRepository;
  private final GatewayOutboxRetryProperties properties;
  private final Clock clock;

  public PaymentOutboxRetryApplicationServiceImpl(
      PaymentMqOutboxRepository paymentMqOutboxRepository,
      PaymentOutboxRetryExecutor paymentOutboxRetryExecutor,
      PaymentRequestLogRepository paymentRequestLogRepository,
      PaymentExceptionEventRepository paymentExceptionEventRepository,
      GatewayOutboxRetryProperties properties,
      Clock clock
  ) {
    this.paymentMqOutboxRepository = paymentMqOutboxRepository;
    this.paymentOutboxRetryExecutor = paymentOutboxRetryExecutor;
    this.paymentRequestLogRepository = paymentRequestLogRepository;
    this.paymentExceptionEventRepository = paymentExceptionEventRepository;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public PaymentOutboxRetryResponse retryFailedMessages() {
    Instant now = Instant.now(clock);
    List<PaymentMqOutboxRecord> records = paymentMqOutboxRepository.findRetryableFailed(now, properties.getRetryLimit());
    int succeededCount = 0;
    int failedCount = 0;
    List<String> retriedMessageKeys = new ArrayList<>();
    for (PaymentMqOutboxRecord record : records) {
      RetryResult result = retrySingleRecord(record, now);
      if (result.succeeded()) {
        succeededCount++;
        retriedMessageKeys.add(record.messageKey());
      } else {
        failedCount++;
      }
    }
    return new PaymentOutboxRetryResponse(records.size(), succeededCount, failedCount, retriedMessageKeys);
  }

  private RetryResult retrySingleRecord(PaymentMqOutboxRecord record, Instant now) {
    Instant start = Instant.now(clock);
    String traceId = newTraceId();
    String requestId = "OUTBOX-RETRY-" + record.id();
    int nextRetryCount = record.retryCount() + 1;
    try {
      paymentOutboxRetryExecutor.send(record);
      paymentMqOutboxRepository.markSent(record.id(), nextRetryCount);
      persistRequestLog(traceId, requestId, start, record, null);
      return RetryResult.SUCCEEDED;
    } catch (Exception exception) {
      Instant nextRetryTime = now.plusSeconds(properties.getRetryDelaySeconds());
      paymentMqOutboxRepository.markFailed(record.id(), nextRetryCount, nextRetryTime, exception.getMessage());
      persistExceptionEvent(traceId, requestId, record, exception);
      persistRequestLog(traceId, requestId, start, record, exception);
      return RetryResult.FAILED;
    }
  }

  private void persistRequestLog(
      String traceId,
      String requestId,
      Instant start,
      PaymentMqOutboxRecord record,
      Exception exception
  ) {
    Instant finish = Instant.now(clock);
    paymentRequestLogRepository.save(new PaymentRequestLogRecord(
        traceId,
        requestId,
        null,
        null,
        null,
        record.bizType(),
        PaymentBizTypes.RETRY,
        "JOB",
        "/internal/messaging/outbox/retry",
        start,
        finish,
        Math.toIntExact(Duration.between(start, finish).toMillis()),
        null,
        record.topic(),
        exception == null ? GatewayResponseCodes.SUCCESS : GatewayResponseCodes.OUTBOX_RETRY_FAILED,
        exception == null ? "SUCCESS" : "FAIL",
        exception == null ? null : GatewayResponseCodes.OUTBOX_RETRY_FAILED,
        exception == null ? null : exception.getMessage(),
        "{\"eventKey\":\"" + record.eventKey() + "\",\"messageKey\":\"" + record.messageKey() + "\"}",
        exception == null ? "{\"sendStatus\":1}" : null
    ));
  }

  private void persistExceptionEvent(
      String traceId,
      String requestId,
      PaymentMqOutboxRecord record,
      Exception exception
  ) {
    paymentExceptionEventRepository.save(new PaymentExceptionEventRecord(
        traceId,
        requestId,
        null,
        record.bizType(),
        PaymentBizTypes.RETRY,
        GatewayResponseCodes.OUTBOX_RETRY_FAILED,
        "ERROR",
        GatewayResponseCodes.OUTBOX_RETRY_FAILED,
        exception.getMessage(),
        "{\"eventKey\":\"" + record.eventKey() + "\",\"messageKey\":\"" + record.messageKey() + "\"}"
    ));
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private enum RetryResult {
    SUCCEEDED,
    FAILED;

    boolean succeeded() {
      return this == SUCCEEDED;
    }
  }
}
