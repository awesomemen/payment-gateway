package com.example.payment.gateway.security;

import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PropertiesMerchantCredentialProvider implements MerchantCredentialProvider {

  private final GatewaySecurityProperties properties;

  public PropertiesMerchantCredentialProvider(GatewaySecurityProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  @Override
  public Optional<MerchantCredential> find(String merchantId) {
    GatewaySecurityProperties.MerchantProperties merchant = properties.getMerchants().get(merchantId);
    if (merchant == null) {
      return Optional.empty();
    }
    return Optional.of(new MerchantCredential(
        merchantId,
        merchant.enabled(),
        merchant.signatureKey(),
        properties.getConfigSource()
    ));
  }
}
