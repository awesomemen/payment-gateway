package com.example.payment.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesMerchantCredentialProviderTest {

  @Test
  void shouldResolveMerchantCredentialFromProperties() {
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.setConfigSource("nacos:gateway-security.json");
    properties.getMerchants().put("MCH100001", new GatewaySecurityProperties.MerchantProperties(true, "demo-signature-key"));

    PropertiesMerchantCredentialProvider provider = new PropertiesMerchantCredentialProvider(properties);

    assertThat(provider.find("MCH100001"))
        .hasValueSatisfying(credential -> {
          assertThat(credential.merchantId()).isEqualTo("MCH100001");
          assertThat(credential.enabled()).isTrue();
          assertThat(credential.signatureKey()).isEqualTo("demo-signature-key");
          assertThat(credential.source()).isEqualTo("nacos:gateway-security.json");
        });
  }

  @Test
  void shouldReturnEmptyWhenMerchantIsMissing() {
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    PropertiesMerchantCredentialProvider provider = new PropertiesMerchantCredentialProvider(properties);

    assertThat(provider.find("MCH404")).isEmpty();
  }
}
