package com.example.payment.gateway.security;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.boot.context.properties.ConfigurationProperties;

@RefreshScope
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

  private String configSource = "application-properties";
  private long requestExpireSeconds = 300;
  private long replayProtectSeconds = 300;
  private Map<String, MerchantProperties> merchants = new LinkedHashMap<>();

  public String getConfigSource() {
    return configSource;
  }

  public void setConfigSource(String configSource) {
    this.configSource = configSource;
  }

  public long getRequestExpireSeconds() {
    return requestExpireSeconds;
  }

  public void setRequestExpireSeconds(long requestExpireSeconds) {
    this.requestExpireSeconds = requestExpireSeconds;
  }

  public long getReplayProtectSeconds() {
    return replayProtectSeconds;
  }

  public void setReplayProtectSeconds(long replayProtectSeconds) {
    this.replayProtectSeconds = replayProtectSeconds;
  }

  public Map<String, MerchantProperties> getMerchants() {
    return merchants;
  }

  public void setMerchants(Map<String, MerchantProperties> merchants) {
    this.merchants = merchants;
  }

  public record MerchantProperties(boolean enabled, String signatureKey) {
  }
}
