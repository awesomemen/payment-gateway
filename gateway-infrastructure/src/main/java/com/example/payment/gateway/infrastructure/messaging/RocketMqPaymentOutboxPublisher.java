package com.example.payment.gateway.infrastructure.messaging;

import com.example.payment.gateway.common.payment.PaymentAcceptedOutboxEvent;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentOutboxPublisher;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayMqOutboxEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayMqOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class RocketMqPaymentOutboxPublisher implements PaymentOutboxPublisher {

  private final GatewayMqOutboxMapper gatewayMqOutboxMapper;
  private final RocketMQTemplate rocketMQTemplate;
  private final GatewayMessagingProperties properties;
  private final ObjectMapper objectMapper;

  public RocketMqPaymentOutboxPublisher(
      GatewayMqOutboxMapper gatewayMqOutboxMapper,
      RocketMQTemplate rocketMQTemplate,
      GatewayMessagingProperties properties,
      ObjectMapper objectMapper
  ) {
    this.gatewayMqOutboxMapper = gatewayMqOutboxMapper;
    this.rocketMQTemplate = rocketMQTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publishPaymentAccepted(PaymentAcceptedOutboxEvent event) {
    if (!properties.isEnabled()) {
      return;
    }
    GatewayMqOutboxEntity entity = new GatewayMqOutboxEntity();
    try {
      String payload = objectMapper.writeValueAsString(payload(event));
      entity.setEventKey("OUTBOX-" + event.gatewayPaymentId());
      entity.setBizType(PaymentBizTypes.PAY);
      entity.setTopic(properties.getTopic());
      entity.setTag(properties.getTag());
      entity.setMessageKey(event.gatewayPaymentId());
      entity.setPayloadJson(payload);
      entity.setSendStatus(0);
      entity.setRetryCount(0);
      gatewayMqOutboxMapper.insert(entity);

      String destination = properties.getTopic() + ":" + properties.getTag();
      var sendResult = rocketMQTemplate.syncSend(
          destination,
          MessageBuilder.withPayload(payload)
              .setHeader("KEYS", event.gatewayPaymentId())
              .build()
      );
      entity.setSendStatus(sendResult != null && sendResult.getSendStatus() == SendStatus.SEND_OK ? 1 : 2);
      entity.setLastErrorMessage(sendResult == null ? "RocketMQ send result is null" : null);
      gatewayMqOutboxMapper.updateById(entity);
    } catch (Exception exception) {
      entity.setEventKey(entity.getEventKey() == null ? "OUTBOX-" + event.gatewayPaymentId() : entity.getEventKey());
      entity.setBizType(PaymentBizTypes.PAY);
      entity.setTopic(properties.getTopic());
      entity.setTag(properties.getTag());
      entity.setMessageKey(event.gatewayPaymentId());
      entity.setPayloadJson(entity.getPayloadJson() == null ? "{\"gatewayPaymentId\":\"" + event.gatewayPaymentId() + "\"}" : entity.getPayloadJson());
      entity.setSendStatus(2);
      entity.setRetryCount(entity.getRetryCount() == null ? 1 : entity.getRetryCount() + 1);
      entity.setNextRetryTime(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
      entity.setLastErrorMessage(exception.getMessage());
      if (entity.getId() == null) {
        gatewayMqOutboxMapper.insert(entity);
      } else {
        gatewayMqOutboxMapper.updateById(entity);
      }
    }
  }

  private static Map<String, Object> payload(PaymentAcceptedOutboxEvent event) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("merchantId", event.merchantId());
    payload.put("requestId", event.requestId());
    payload.put("idempotencyKey", event.idempotencyKey());
    payload.put("gatewayPaymentId", event.gatewayPaymentId());
    payload.put("downstreamPaymentId", event.downstreamPaymentId());
    payload.put("status", event.status());
    payload.put("routeCode", event.routeCode());
    payload.put("targetService", event.targetService());
    payload.put("amount", event.amount());
    payload.put("currency", event.currency());
    payload.put("acceptedAt", LocalDateTime.now(ZoneOffset.UTC).toString());
    return payload;
  }
}
