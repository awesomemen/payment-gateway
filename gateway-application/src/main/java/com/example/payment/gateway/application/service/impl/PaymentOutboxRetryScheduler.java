package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.application.service.PaymentOutboxRetryApplicationService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class PaymentOutboxRetryScheduler {

  private final PaymentOutboxRetryApplicationService paymentOutboxRetryApplicationService;
  private final GatewayOutboxRetryProperties properties;

  public PaymentOutboxRetryScheduler(
      PaymentOutboxRetryApplicationService paymentOutboxRetryApplicationService,
      GatewayOutboxRetryProperties properties
  ) {
    this.paymentOutboxRetryApplicationService = paymentOutboxRetryApplicationService;
    this.properties = properties;
  }

  @Scheduled(cron = "${gateway.messaging.rocketmq.retry-cron:0 */1 * * * *}")
  public void retryFailedMessages() {
    if (!properties.isEnabled() || !properties.isRetryEnabled()) {
      return;
    }
    paymentOutboxRetryApplicationService.retryFailedMessages();
  }
}
