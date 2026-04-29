package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentCreateResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLock;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLockManager;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.Money;
import com.example.payment.gateway.domain.model.PaymentCreateCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class PaymentCreateIdempotencyCoordinatorTest {

  @Mock
  private PaymentIdempotencyRepository paymentIdempotencyRepository;

  @Mock
  private PaymentIdempotencyLockManager paymentIdempotencyLockManager;

  private PaymentCreateIdempotencyCoordinator coordinator;

  @BeforeEach
  void setUp() {
    coordinator = new PaymentCreateIdempotencyCoordinator(
        new GatewayIdempotencyProperties(),
        paymentIdempotencyRepository,
        paymentIdempotencyLockManager
    );
  }

  @Test
  void shouldReplayStoredFailureForDuplicateRequest() {
    PaymentCreateCommand command = validCommand("IDEMP-001", "REQ-001", new BigDecimal("88.50"));
    PaymentIdempotencyRecord storedRecord = PaymentIdempotencyRecord.failure(
        command.merchantId(),
        command.idempotencyKey(),
        command.idempotencyFingerprint(),
        GatewayResponseCodes.PAYMENT_CREATE_NOT_READY,
        501,
        "支付创建主链路骨架已建立，下一阶段将接入安全校验、幂等、防重放、路由与持久化能力。"
    );
    given(paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey()))
        .willReturn(Optional.of(storedRecord));

    assertThatThrownBy(() -> coordinator.execute(command, () -> {
      throw new IllegalStateException("should not invoke supplier");
    }))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.PAYMENT_CREATE_NOT_READY);
  }

  @Test
  void shouldReplayStoredDownstreamRejectedFailureForDuplicateRequest() {
    PaymentCreateCommand command = validCommand("IDEMP-REJECT-001", "REQ-REJECT-001", new BigDecimal("88.50"));
    PaymentIdempotencyRecord storedRecord = PaymentIdempotencyRecord.failure(
        command.merchantId(),
        command.idempotencyKey(),
        command.idempotencyFingerprint(),
        GatewayResponseCodes.DOWNSTREAM_REJECTED,
        422,
        "payment rejected by downstream rule"
    );
    given(paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey()))
        .willReturn(Optional.of(storedRecord));

    assertThatThrownBy(() -> coordinator.execute(command, () -> {
      throw new IllegalStateException("should not invoke supplier");
    }))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_REJECTED);
  }

  @Test
  void shouldReplayStoredDownstreamTimeoutFailureForDuplicateRequest() {
    PaymentCreateCommand command = validCommand("IDEMP-TIMEOUT-001", "REQ-TIMEOUT-001", new BigDecimal("88.50"));
    PaymentIdempotencyRecord storedRecord = PaymentIdempotencyRecord.failure(
        command.merchantId(),
        command.idempotencyKey(),
        command.idempotencyFingerprint(),
        GatewayResponseCodes.DOWNSTREAM_TIMEOUT,
        504,
        "payment create timed out in mock downstream facade"
    );
    given(paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey()))
        .willReturn(Optional.of(storedRecord));

    assertThatThrownBy(() -> coordinator.execute(command, () -> {
      throw new IllegalStateException("should not invoke supplier");
    }))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_TIMEOUT);
  }

  @Test
  void shouldRejectConflictingDuplicateRequest() {
    PaymentCreateCommand firstCommand = validCommand("IDEMP-001", "REQ-001", new BigDecimal("88.50"));
    PaymentCreateCommand conflictingCommand = validCommand("IDEMP-001", "REQ-999", new BigDecimal("99.99"));
    PaymentIdempotencyRecord storedRecord = PaymentIdempotencyRecord.failure(
        firstCommand.merchantId(),
        firstCommand.idempotencyKey(),
        firstCommand.idempotencyFingerprint(),
        GatewayResponseCodes.PAYMENT_CREATE_NOT_READY,
        501,
        "支付创建主链路骨架已建立，下一阶段将接入安全校验、幂等、防重放、路由与持久化能力。"
    );
    given(paymentIdempotencyRepository.find(conflictingCommand.merchantId(), conflictingCommand.idempotencyKey()))
        .willReturn(Optional.of(storedRecord));

    assertThatThrownBy(() -> coordinator.execute(conflictingCommand, () -> new PaymentCreateResponse("GP-001", "ACCEPTED", "BOOTSTRAP", "OK")))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void shouldPersistFailureResultWhenHandlerThrowsGatewayException() {
    PaymentCreateCommand command = validCommand("IDEMP-001", "REQ-001", new BigDecimal("88.50"));
    PaymentIdempotencyLock lock = mock(PaymentIdempotencyLock.class);
    given(paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey()))
        .willReturn(Optional.empty());
    given(paymentIdempotencyLockManager.tryLock(eq(command.merchantId()), eq(command.idempotencyKey()), any(), any()))
        .willReturn(Optional.of(lock));

    assertThatThrownBy(() -> coordinator.execute(command, () -> {
      throw new GatewayException(GatewayResponseCodes.PAYMENT_CREATE_NOT_READY, 501, "not ready");
    }))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.PAYMENT_CREATE_NOT_READY);

    verify(paymentIdempotencyRepository).save(any(), any());
    verify(lock).release();
  }

  @Test
  void shouldRejectWhenLockCannotBeAcquiredAndNoStoredResultExists() {
    PaymentCreateCommand command = validCommand("IDEMP-001", "REQ-001", new BigDecimal("88.50"));
    given(paymentIdempotencyRepository.find(command.merchantId(), command.idempotencyKey()))
        .willReturn(Optional.empty());
    given(paymentIdempotencyLockManager.tryLock(eq(command.merchantId()), eq(command.idempotencyKey()), any(), any()))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> coordinator.execute(command, () -> new PaymentCreateResponse("GP-001", "ACCEPTED", "BOOTSTRAP", "OK")))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.IDEMPOTENCY_IN_PROGRESS);
  }

  private static PaymentCreateCommand validCommand(String idempotencyKey, String requestId, BigDecimal amount) {
    return new PaymentCreateCommand(
        "MCH100001",
        requestId,
        idempotencyKey,
        new Money(amount, "CNY"),
        Instant.parse("2026-04-21T10:00:00Z"),
        "nonce-001",
        "demo-signature"
    );
  }
}
