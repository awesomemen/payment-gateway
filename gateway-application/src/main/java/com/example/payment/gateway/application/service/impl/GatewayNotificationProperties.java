package com.example.payment.gateway.application.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.messaging.notification")
public class GatewayNotificationProperties {

  private boolean enabled = false;
  private int retryLimit = 20;
  private int maxConsumeRetries = 3;
  private String retryCron = "0 */1 * * * *";
  private String consumerGroup = "gateway-notification-consumer";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getRetryLimit() {
    return retryLimit;
  }

  public void setRetryLimit(int retryLimit) {
    this.retryLimit = retryLimit;
  }

  public int getMaxConsumeRetries() {
    return maxConsumeRetries;
  }

  public void setMaxConsumeRetries(int maxConsumeRetries) {
    this.maxConsumeRetries = maxConsumeRetries;
  }

  public String getRetryCron() {
    return retryCron;
  }

  public void setRetryCron(String retryCron) {
    this.retryCron = retryCron;
  }

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }
}
