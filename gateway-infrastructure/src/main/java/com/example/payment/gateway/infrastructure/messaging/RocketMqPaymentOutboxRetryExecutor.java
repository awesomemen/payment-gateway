package com.example.payment.gateway.infrastructure.messaging;

import com.example.payment.gateway.common.payment.PaymentMqOutboxRecord;
import com.example.payment.gateway.common.payment.PaymentOutboxRetryExecutor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

@Component
@Profile({"local", "docker"})
public class RocketMqPaymentOutboxRetryExecutor implements PaymentOutboxRetryExecutor {

  private final RocketMQTemplate rocketMQTemplate;

  public RocketMqPaymentOutboxRetryExecutor(RocketMQTemplate rocketMQTemplate) {
    this.rocketMQTemplate = rocketMQTemplate;
  }

  @Override
  public void send(PaymentMqOutboxRecord record) {
    if (!StringUtils.hasText(record.topic())) {
      throw new IllegalArgumentException("Outbox topic must not be blank");
    }
    String destination = StringUtils.hasText(record.tag()) ? record.topic() + ":" + record.tag() : record.topic();
    rocketMQTemplate.syncSend(
        destination,
        MessageBuilder.withPayload(record.payloadJson())
            .setHeader("KEYS", record.messageKey())
            .build()
    );
  }
}
