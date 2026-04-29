package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentNotificationRetryResponse;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRecord;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRepository;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PaymentNotificationRetryApplicationServiceTest {

  private PaymentMessageConsumeRepository paymentMessageConsumeRepository;
  private PaymentRequestLogRepository paymentRequestLogRepository;
  private PaymentExceptionEventRepository paymentExceptionEventRepository;
  private PaymentNotificationRetryApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    paymentMessageConsumeRepository = Mockito.mock(PaymentMessageConsumeRepository.class);
    paymentRequestLogRepository = Mockito.mock(PaymentRequestLogRepository.class);
    paymentExceptionEventRepository = Mockito.mock(PaymentExceptionEventRepository.class);
    GatewayNotificationProperties properties = new GatewayNotificationProperties();
    properties.setRetryLimit(10);
    properties.setMaxConsumeRetries(2);
    properties.setConsumerGroup("gateway-notification-test");
    PaymentNotificationProcessor processor = new PaymentNotificationProcessor(
        paymentMessageConsumeRepository,
        paymentRequestLogRepository,
        paymentExceptionEventRepository,
        properties,
        new ObjectMapper(),
        Clock.fixed(Instant.parse("2026-04-23T10:00:00Z"), ZoneOffset.UTC)
    );
    service = new PaymentNotificationRetryApplicationServiceImpl(
        paymentMessageConsumeRepository,
        processor,
        properties
    );
  }

  @Test
  void shouldRetryFailedNotificationSuccessfully() {
    PaymentMessageConsumeRecord record = new PaymentMessageConsumeRecord(
        1L,
        "MSG-CONSUME-001",
        "PAY",
        "gateway-notification-test",
        "{\"merchantId\":\"MCH100001\",\"gatewayPaymentId\":\"GP1001\",\"status\":\"SUCCEEDED\"}",
        "FAILED",
        1,
        false,
        "previous failure"
    );
    given(paymentMessageConsumeRepository.findRetryableFailed(10)).willReturn(List.of(record));
    given(paymentMessageConsumeRepository.findByMessageKey("MSG-CONSUME-001")).willReturn(Optional.of(record));

    PaymentNotificationRetryResponse response = service.retryFailedNotifications();

    assertThat(response.scannedCount()).isEqualTo(1);
    assertThat(response.succeededCount()).isEqualTo(1);
    assertThat(response.failedCount()).isEqualTo(0);
    verify(paymentMessageConsumeRepository, Mockito.atLeastOnce()).save(any());
  }

  @Test
  void shouldMarkNotificationAsDeadLetterWhenPayloadRemainsInvalid() {
    PaymentMessageConsumeRecord record = new PaymentMessageConsumeRecord(
        2L,
        "MSG-CONSUME-002",
        "PAY",
        "gateway-notification-test",
        "{\"merchantId\":\"MCH100001\"}",
        "FAILED",
        1,
        false,
        "previous failure"
    );
    given(paymentMessageConsumeRepository.findRetryableFailed(10)).willReturn(List.of(record));
    given(paymentMessageConsumeRepository.findByMessageKey(eq("MSG-CONSUME-002"))).willReturn(Optional.of(record));

    PaymentNotificationRetryResponse response = service.retryFailedNotifications();

    assertThat(response.failedCount()).isEqualTo(1);
    assertThat(response.deadLetteredCount()).isEqualTo(1);
    verify(paymentExceptionEventRepository).save(any());
  }
}
