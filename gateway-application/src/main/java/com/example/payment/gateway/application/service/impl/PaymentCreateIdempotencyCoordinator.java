package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentCreateResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLock;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLockManager;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.PaymentCreateCommand;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class PaymentCreateIdempotencyCoordinator {

  private final GatewayIdempotencyProperties properties;
  private final PaymentIdempotencyRepository paymentIdempotencyRepository;
  private final PaymentIdempotencyLockManager paymentIdempotencyLockManager;

  public PaymentCreateIdempotencyCoordinator(
      GatewayIdempotencyProperties properties,
      PaymentIdempotencyRepository paymentIdempotencyRepository,
      PaymentIdempotencyLockManager paymentIdempotencyLockManager
  ) {
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
    this.paymentIdempotencyRepository = Objects.requireNonNull(paymentIdempotencyRepository, "paymentIdempotencyRepository must not be null");
    this.paymentIdempotencyLockManager = Objects.requireNonNull(paymentIdempotencyLockManager, "paymentIdempotencyLockManager must not be null");
  }

  public PaymentCreateResponse execute(PaymentCreateCommand command, Supplier<PaymentCreateResponse> handler) {
    Optional<PaymentIdempotencyRecord> existingRecord = paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey());
    if (existingRecord.isPresent()) {
      return replayOrReject(command, existingRecord.get());
    }

    Optional<PaymentIdempotencyLock> maybeLock = paymentIdempotencyLockManager.tryLock(
        command.merchantId(),
        command.idempotencyKey(),
        Duration.ofMillis(properties.getLockWaitMillis()),
        Duration.ofSeconds(properties.getLockLeaseSeconds())
    );
    if (maybeLock.isEmpty()) {
      Optional<PaymentIdempotencyRecord> currentRecord = paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey());
      if (currentRecord.isPresent()) {
        return replayOrReject(command, currentRecord.get());
      }
      throw new GatewayException(
          GatewayResponseCodes.IDEMPOTENCY_IN_PROGRESS,
          409,
          "Payment request is already being processed"
      );
    }

    PaymentIdempotencyLock lock = maybeLock.get();
    try {
      Optional<PaymentIdempotencyRecord> currentRecord = paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey());
      if (currentRecord.isPresent()) {
        return replayOrReject(command, currentRecord.get());
      }

      try {
        PaymentCreateResponse response = handler.get();
        paymentIdempotencyRepository.save(PaymentIdempotencyRecord.success(
            command.merchantId(),
            command.idempotencyKey(),
            command.idempotencyFingerprint(),
            response.gatewayPaymentId(),
            response.status(),
            response.routeCode(),
            response.message()
        ), Duration.ofSeconds(properties.getExpireSeconds()));
        return response;
      } catch (GatewayException exception) {
        paymentIdempotencyRepository.save(PaymentIdempotencyRecord.failure(
            command.merchantId(),
            command.idempotencyKey(),
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

  private PaymentCreateResponse replayOrReject(PaymentCreateCommand command, PaymentIdempotencyRecord record) {
    if (!record.requestFingerprint().equals(command.idempotencyFingerprint())) {
      throw new GatewayException(
          GatewayResponseCodes.IDEMPOTENCY_CONFLICT,
          409,
          "Conflicting request payload for existing idempotency key"
      );
    }
    if (record.storedPaymentResult().success()) {
      return new PaymentCreateResponse(
          record.storedPaymentResult().gatewayPaymentId(),
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
