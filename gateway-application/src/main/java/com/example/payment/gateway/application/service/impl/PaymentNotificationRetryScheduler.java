package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.application.service.PaymentNotificationRetryApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationRetryScheduler {

  private final PaymentNotificationRetryApplicationService paymentNotificationRetryApplicationService;
  private final GatewayNotificationProperties properties;

  public PaymentNotificationRetryScheduler(
      PaymentNotificationRetryApplicationService paymentNotificationRetryApplicationService,
      GatewayNotificationProperties properties
  ) {
    this.paymentNotificationRetryApplicationService = paymentNotificationRetryApplicationService;
    this.properties = properties;
  }

  @Scheduled(cron = "${gateway.messaging.notification.retry-cron:0 */1 * * * *}")
  public void retryFailedNotifications() {
    if (!properties.isEnabled()) {
      return;
    }
    paymentNotificationRetryApplicationService.retryFailedNotifications();
  }
}
