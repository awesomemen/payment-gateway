package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.application.service.PaymentReconcileApplicationService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class ProcessingPaymentReconcileScheduler {

  private final PaymentReconcileApplicationService paymentReconcileApplicationService;
  private final GatewayReconcileProperties properties;

  public ProcessingPaymentReconcileScheduler(
      PaymentReconcileApplicationService paymentReconcileApplicationService,
      GatewayReconcileProperties properties
  ) {
    this.paymentReconcileApplicationService = paymentReconcileApplicationService;
    this.properties = properties;
  }

  @Scheduled(cron = "${gateway.reconcile.processing-cron:0/30 * * * * *}")
  public void reconcileProcessingOrders() {
    if (!properties.isEnabled()) {
      return;
    }
    paymentReconcileApplicationService.reconcileProcessingOrders();
  }
}
