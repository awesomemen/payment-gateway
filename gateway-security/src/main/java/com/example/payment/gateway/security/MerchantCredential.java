package com.example.payment.gateway.security;

public record MerchantCredential(
    String merchantId,
    boolean enabled,
    String signatureKey,
    String source
) {
}
