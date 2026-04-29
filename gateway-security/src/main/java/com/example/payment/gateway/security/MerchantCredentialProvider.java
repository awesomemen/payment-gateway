package com.example.payment.gateway.security;

import java.util.Optional;

public interface MerchantCredentialProvider {

  Optional<MerchantCredential> find(String merchantId);
}
