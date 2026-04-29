package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.RefundCreateResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLock;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLockManager;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.RefundCreateCommand;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RefundCreateIdempotencyCoordinator {

  private final GatewayIdempotencyProperties properties;
  private final PaymentIdempotencyRepository paymentIdempotencyRepository;
  private final PaymentIdempotencyLockManager paymentIdempotencyLockManager;

  public RefundCreateIdempotencyCoordinator(
      GatewayIdempotencyProperties properties,
      PaymentIdempotencyRepository paymentIdempotencyRepository,
      PaymentIdempotencyLockManager paymentIdempotencyLockManager
  ) {
    this.properties = properties;
    this.paymentIdempotencyRepository = paymentIdempotencyRepository;
    this.paymentIdempotencyLockManager = paymentIdempotencyLockManager;
  }

  public RefundCreateResponse execute(RefundCreateCommand command, Supplier<RefundCreateResponse> handler) {
    String scopedKey = command.scopedIdempotencyKey();
    Optional<PaymentIdempotencyRecord> existingRecord = paymentIdempotencyRepository.find(command.merchantId(), scopedKey);
    if (existingRecord.isPresent()) {
      return replayOrReject(command, existingRecord.get());
    }
    Optional<PaymentIdempotencyLock> maybeLock = paymentIdempotencyLockManager.tryLock(
        command.merchantId(),
        scopedKey,
        Duration.ofMillis(properties.getLockWaitMillis()),
        Duration.ofSeconds(properties.getLockLeaseSeconds())
    );
    if (maybeLock.isEmpty()) {
      Optional<PaymentIdempotencyRecord> currentRecord = paymentIdempotencyRepository.find(command.merchantId(), scopedKey);
      if (currentRecord.isPresent()) {
        return replayOrReject(command, currentRecord.get());
      }
      throw new GatewayException(GatewayResponseCodes.IDEMPOTENCY_IN_PROGRESS, 409, "Refund request is already being processed");
    }
    PaymentIdempotencyLock lock = maybeLock.get();
    try {
      try {
        RefundCreateResponse response = handler.get();
        paymentIdempotencyRepository.save(PaymentIdempotencyRecord.success(
            command.merchantId(),
            scopedKey,
            command.idempotencyFingerprint(),
            response.gatewayRefundId(),
            response.status(),
            response.routeCode(),
            response.message()
        ), Duration.ofSeconds(properties.getExpireSeconds()));
        return response;
      } catch (GatewayException exception) {
        paymentIdempotencyRepository.save(PaymentIdempotencyRecord.failure(
            command.merchantId(),
            scopedKey,
            command.idempotencyFingerprint(),
            exception.code(),
            exception.statusCode(),
            exception.getMessage()
        ), Duration.ofSeconds(properties.getExpireSeconds()));
        throw exception;
      }
    } finally {
      lock.release();
    }
  }

  private RefundCreateResponse replayOrReject(RefundCreateCommand command, PaymentIdempotencyRecord record) {
    if (!record.requestFingerprint().equals(command.idempotencyFingerprint())) {
      throw new GatewayException(GatewayResponseCodes.IDEMPOTENCY_CONFLICT, 409, "Conflicting refund payload for existing idempotency key");
    }
    if (record.storedPaymentResult().success()) {
      return new RefundCreateResponse(
          record.storedPaymentResult().gatewayPaymentId(),
          null,
          record.storedPaymentResult().paymentStatus(),
          record.storedPaymentResult().routeCode(),
          record.storedPaymentResult().responseMessage()
      );
    }
    throw new GatewayException(
        record.storedPaymentResult().errorCode(),
        record.storedPaymentResult().errorStatus(),
        record.storedPaymentResult().errorMessage()
    );
  }
}
