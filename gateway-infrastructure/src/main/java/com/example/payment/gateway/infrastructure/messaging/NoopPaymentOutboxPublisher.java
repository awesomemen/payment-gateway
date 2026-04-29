package com.example.payment.gateway.infrastructure.messaging;

import com.example.payment.gateway.common.payment.PaymentAcceptedOutboxEvent;
import com.example.payment.gateway.common.payment.PaymentOutboxPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class NoopPaymentOutboxPublisher implements PaymentOutboxPublisher {

  @Override
  public void publishPaymentAccepted(PaymentAcceptedOutboxEvent event) {
  }
}
