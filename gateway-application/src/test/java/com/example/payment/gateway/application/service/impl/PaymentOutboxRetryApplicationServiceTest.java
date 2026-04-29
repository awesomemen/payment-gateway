package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentOutboxRetryResponse;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentMqOutboxRecord;
import com.example.payment.gateway.common.payment.PaymentMqOutboxRepository;
import com.example.payment.gateway.common.payment.PaymentOutboxRetryExecutor;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PaymentOutboxRetryApplicationServiceTest {

  private PaymentMqOutboxRepository paymentMqOutboxRepository;
  private PaymentOutboxRetryExecutor paymentOutboxRetryExecutor;
  private PaymentRequestLogRepository paymentRequestLogRepository;
  private PaymentExceptionEventRepository paymentExceptionEventRepository;
  private PaymentOutboxRetryApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    paymentMqOutboxRepository = Mockito.mock(PaymentMqOutboxRepository.class);
    paymentOutboxRetryExecutor = Mockito.mock(PaymentOutboxRetryExecutor.class);
    paymentRequestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    paymentExceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    GatewayOutboxRetryProperties properties = new GatewayOutboxRetryProperties();
    properties.setEnabled(true);
    properties.setRetryEnabled(true);
    properties.setRetryLimit(10);
    properties.setRetryDelaySeconds(300);
    service = new PaymentOutboxRetryApplicationServiceImpl(
        paymentMqOutboxRepository,
        paymentOutboxRetryExecutor,
        paymentRequestLogRepository,
        paymentExceptionEventRepository,
        properties,
        Clock.fixed(Instant.parse("2026-04-23T03:40:00Z"), ZoneOffset.UTC)
    );
  }

  @Test
  void shouldRetryFailedOutboxMessageSuccessfully() {
    PaymentMqOutboxRecord record = record(101L, "MSG-RETRY-001", "gateway-payment-events", "PAYMENT_CREATED");
    given(paymentMqOutboxRepository.findRetryableFailed(any(), Mockito.eq(10))).willReturn(List.of(record));

    PaymentOutboxRetryResponse response = service.retryFailedMessages();

    assertThat(response.scannedCount()).isEqualTo(1);
    assertThat(response.succeededCount()).isEqualTo(1);
    assertThat(response.failedCount()).isEqualTo(0);
    assertThat(response.retriedMessageKeys()).containsExactly("MSG-RETRY-001");
    verify(paymentOutboxRetryExecutor).send(record);
    verify(paymentMqOutboxRepository).markSent(101L, 1);
    verify(paymentRequestLogRepository).save(any());
  }

  @Test
  void shouldMarkOutboxMessageAsFailedWhenRetryThrowsException() {
    PaymentMqOutboxRecord record = record(102L, "MSG-RETRY-002", "", "");
    given(paymentMqOutboxRepository.findRetryableFailed(any(), Mockito.eq(10))).willReturn(List.of(record));
    Mockito.doThrow(new IllegalArgumentException("Outbox topic must not be blank"))
        .when(paymentOutboxRetryExecutor)
        .send(record);

    PaymentOutboxRetryResponse response = service.retryFailedMessages();

    assertThat(response.scannedCount()).isEqualTo(1);
    assertThat(response.succeededCount()).isEqualTo(0);
    assertThat(response.failedCount()).isEqualTo(1);
    verify(paymentMqOutboxRepository).markFailed(Mockito.eq(102L), Mockito.eq(1), any(), Mockito.eq("Outbox topic must not be blank"));
    verify(paymentExceptionEventRepository).save(any());
    verify(paymentRequestLogRepository).save(any());
  }

  private static PaymentMqOutboxRecord record(long id, String messageKey, String topic, String tag) {
    return new PaymentMqOutboxRecord(
        id,
        "OUTBOX-" + messageKey,
        "PAY",
        topic,
        tag,
        messageKey,
        "{\"messageKey\":\"" + messageKey + "\"}",
        2,
        0,
        null,
        "send failed"
    );
  }
}
