package com.example.payment.gateway.application.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.idempotency")
public class GatewayIdempotencyProperties {

  private long expireSeconds = 86400;
  private long lockWaitMillis = 0;
  private long lockLeaseSeconds = 10;

  public long getExpireSeconds() {
    return expireSeconds;
  }

  public void setExpireSeconds(long expireSeconds) {
    this.expireSeconds = expireSeconds;
  }

  public long getLockWaitMillis() {
    return lockWaitMillis;
  }

  public void setLockWaitMillis(long lockWaitMillis) {
    this.lockWaitMillis = lockWaitMillis;
  }

  public long getLockLeaseSeconds() {
    return lockLeaseSeconds;
  }

  public void setLockLeaseSeconds(long lockLeaseSeconds) {
    this.lockLeaseSeconds = lockLeaseSeconds;
  }
}
