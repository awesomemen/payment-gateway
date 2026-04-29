package com.example.payment.gateway.application.service.impl;

import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
@RocketMQMessageListener(
    topic = "${gateway.messaging.rocketmq.topic:gateway-payment-events}",
    consumerGroup = "${gateway.messaging.notification.consumer-group:gateway-notification-consumer}",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING
)
public class PaymentEventConsumer implements RocketMQListener<MessageExt> {

  private final PaymentNotificationProcessor paymentNotificationProcessor;

  public PaymentEventConsumer(PaymentNotificationProcessor paymentNotificationProcessor) {
    this.paymentNotificationProcessor = paymentNotificationProcessor;
  }

  @Override
  public void onMessage(MessageExt message) {
    String messageKey = message.getKeys();
    if (messageKey == null || messageKey.isBlank()) {
      messageKey = "UNKNOWN-" + System.currentTimeMillis();
    }
    String payload = new String(message.getBody(), StandardCharsets.UTF_8);
    PaymentNotificationProcessor.ProcessingResult result =
        paymentNotificationProcessor.process(messageKey, payload);
    if (!result.success() && !result.deadLetter()) {
      throw new IllegalStateException(result.errorMessage());
    }
  }
}
