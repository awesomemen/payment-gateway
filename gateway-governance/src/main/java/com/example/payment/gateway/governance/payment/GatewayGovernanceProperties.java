package com.example.payment.gateway.governance.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope
@Component
@ConfigurationProperties(prefix = "gateway.governance")
public class GatewayGovernanceProperties {

  private int permitsPerMinute = 60;

  public int getPermitsPerMinute() {
    return permitsPerMinute;
  }

  public void setPermitsPerMinute(int permitsPerMinute) {
    this.permitsPerMinute = permitsPerMinute;
  }
}
