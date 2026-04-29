package com.example.payment.gateway.application.service.impl;

import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PaymentEventConsumerTest {

  private PaymentNotificationProcessor paymentNotificationProcessor;
  private PaymentEventConsumer consumer;

  @BeforeEach
  void setUp() {
    paymentNotificationProcessor = Mockito.mock(PaymentNotificationProcessor.class);
    consumer = new PaymentEventConsumer(paymentNotificationProcessor);
  }

  @Test
  void shouldPassMessageKeyAndPayloadToProcessor() {
    MessageExt message = message("MSG-001", "{\"gatewayPaymentId\":\"GP-001\",\"status\":\"SUCCEEDED\"}");
    given(paymentNotificationProcessor.process(
        "MSG-001",
        "{\"gatewayPaymentId\":\"GP-001\",\"status\":\"SUCCEEDED\"}"
    )).willReturn(new PaymentNotificationProcessor.ProcessingResult("MSG-001", true, false, null));

    assertThatCode(() -> consumer.onMessage(message)).doesNotThrowAnyException();

    verify(paymentNotificationProcessor).process(
        "MSG-001",
        "{\"gatewayPaymentId\":\"GP-001\",\"status\":\"SUCCEEDED\"}"
    );
  }

  @Test
  void shouldGenerateFallbackMessageKeyWhenKeysMissing() {
    MessageExt message = message(null, "{\"gatewayPaymentId\":\"GP-002\",\"status\":\"SUCCEEDED\"}");
    given(paymentNotificationProcessor.process(
        Mockito.startsWith("UNKNOWN-"),
        Mockito.eq("{\"gatewayPaymentId\":\"GP-002\",\"status\":\"SUCCEEDED\"}")
    )).willReturn(new PaymentNotificationProcessor.ProcessingResult("UNKNOWN-1", true, false, null));

    assertThatCode(() -> consumer.onMessage(message)).doesNotThrowAnyException();
  }

  @Test
  void shouldThrowWhenProcessorFailsWithoutDeadLetter() {
    MessageExt message = message("MSG-003", "{\"gatewayPaymentId\":\"GP-003\",\"status\":\"FAILED\"}");
    given(paymentNotificationProcessor.process(
        "MSG-003",
        "{\"gatewayPaymentId\":\"GP-003\",\"status\":\"FAILED\"}"
    )).willReturn(new PaymentNotificationProcessor.ProcessingResult("MSG-003", false, false, "retry later"));

    assertThatThrownBy(() -> consumer.onMessage(message))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("retry later");
  }

  @Test
  void shouldSuppressExceptionWhenProcessorDeadLettersMessage() {
    MessageExt message = message("MSG-004", "{\"gatewayPaymentId\":\"GP-004\",\"status\":\"FAILED\"}");
    given(paymentNotificationProcessor.process(
        "MSG-004",
        "{\"gatewayPaymentId\":\"GP-004\",\"status\":\"FAILED\"}"
    )).willReturn(new PaymentNotificationProcessor.ProcessingResult("MSG-004", false, true, "dead lettered"));

    assertThatCode(() -> consumer.onMessage(message)).doesNotThrowAnyException();
  }

  private static MessageExt message(String key, String payload) {
    MessageExt message = new MessageExt();
    message.setKeys(key);
    message.setBody(payload.getBytes(StandardCharsets.UTF_8));
    return message;
  }
}
