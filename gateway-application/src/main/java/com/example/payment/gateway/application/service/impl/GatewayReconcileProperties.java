package com.example.payment.gateway.application.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.reconcile")
public class GatewayReconcileProperties {

  private boolean enabled;
  private int processingLimit = 20;
  private String processingCron = "0/30 * * * * *";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getProcessingLimit() {
    return processingLimit;
  }

  public void setProcessingLimit(int processingLimit) {
    this.processingLimit = processingLimit;
  }

  public String getProcessingCron() {
    return processingCron;
  }

  public void setProcessingCron(String processingCron) {
    this.processingCron = processingCron;
  }
}
