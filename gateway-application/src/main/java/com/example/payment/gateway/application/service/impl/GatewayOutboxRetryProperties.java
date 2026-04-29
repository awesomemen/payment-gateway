package com.example.payment.gateway.application.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
@ConfigurationProperties(prefix = "gateway.messaging.rocketmq")
public class GatewayOutboxRetryProperties {

  private boolean enabled = true;
  private boolean retryEnabled = true;
  private int retryLimit = 20;
  private int retryDelaySeconds = 300;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isRetryEnabled() {
    return retryEnabled;
  }

  public void setRetryEnabled(boolean retryEnabled) {
    this.retryEnabled = retryEnabled;
  }

  public int getRetryLimit() {
    return retryLimit;
  }

  public void setRetryLimit(int retryLimit) {
    this.retryLimit = retryLimit;
  }

  public int getRetryDelaySeconds() {
    return retryDelaySeconds;
  }

  public void setRetryDelaySeconds(int retryDelaySeconds) {
    this.retryDelaySeconds = retryDelaySeconds;
  }
}
